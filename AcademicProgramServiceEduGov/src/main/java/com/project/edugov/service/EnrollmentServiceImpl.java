package com.project.edugov.service;

import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.FacultyClient;
import com.project.edugov.client.NotificationClient;
import com.project.edugov.client.StudentClient;
import com.project.edugov.client.UserClient;
import com.project.edugov.dto.EnrollmentResponseDTO;
import com.project.edugov.dto.FacultyFeignDTO;
import com.project.edugov.dto.StudentFeignDTO;
import com.project.edugov.dto.UserFeignDTO;
import com.project.edugov.exception.APIException;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Course;
import com.project.edugov.model.Enrollment;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.repository.CourseRepository;
import com.project.edugov.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

	private final EnrollmentRepository enrollmentRepo;
	private final CourseRepository courseRepo;
	private final StudentClient studentClient;
	private final FacultyClient facultyClient;
	private final UserClient userClient;
	private final ModelMapper modelMapper;
	private final NotificationClient notificationClient;
	
	// 1. INJECT AUDIT LOGGER
	private final AsyncAuditLogger auditLogger;

	private EnrollmentResponseDTO mapToCustomDto(Enrollment e) {
		EnrollmentResponseDTO dto = modelMapper.map(e, EnrollmentResponseDTO.class);
		dto.setEnrollmentDate(e.getDate());

		try {
			StudentFeignDTO student = studentClient.getStudentById(e.getStudentId());
			if (student != null) {
				dto.setStudentId(student.getStudentId());
				dto.setStudentName(student.getName());
				dto.setStudentEmail(student.getEmail());
			}
		} catch (Exception ex) {
			dto.setStudentName("Information temporarily unavailable");
		}

		if (e.getCourse() != null) {
			dto.setCourseId(e.getCourse().getCourseId());
			dto.setCourseTitle(e.getCourse().getTitle());
			Long fId = e.getCourse().getFacultyId();
			dto.setFacultyId(fId);
		}

		if (e.getApprovedByAdminId() != null) {
			try {
				UserFeignDTO admin = userClient.getUserById(e.getApprovedByAdminId());
				if (admin != null) {
					dto.setApprovedByAdminId(admin.getUserId());
					dto.setApprovedByAdminName(admin.getName());
				}
			} catch (Exception ex) {
				dto.setApprovedByAdminName("Information temporarily unavailable");
			}
		}
		return dto;
	}

	@Override
	public EnrollmentResponseDTO applyForCourse(Long sId, Long cId) {
		StudentFeignDTO student = null;
		try {
			student = studentClient.getStudentById(sId);
		} catch (Exception e) {
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Registration Service unavailable.");
		}

		Course course = courseRepo.findById(cId).orElseThrow(() -> 
			new ResourceNotFoundException("Course not found."));

		if (course.getStatus() != Status.ACTIVE) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Course is INACTIVE.");
		}

		if (enrollmentRepo.existsByStudentIdAndCourse_CourseId(sId, cId)) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Duplicate application.");
		}

		Enrollment enrollment = new Enrollment();
		enrollment.setStudentId(sId);
		enrollment.setCourse(course);
		enrollment.setStatus(Status.PENDING);
		enrollment.setDate(java.time.LocalDateTime.now());

		Enrollment savedEntity = enrollmentRepo.save(enrollment);
		
		// 2. FIRE AUDIT LOG (Logged under the Student's ID)
		auditLogger.fireAndForgetLog(sId, "APPLY_COURSE", "Course ID: " + cId);

		try {
			Long creatorAdminId = course.getCreatedByAdminId();
			UserFeignDTO admin = userClient.getUserById(creatorAdminId);
			if (admin != null) {
				notificationClient.sendNotification(admin.getUserId(), savedEntity.getEnrollmentId(), 
						"Student applied for course: " + course.getTitle(), "ENROLLMENT_APPROVAL_REQUIRED", admin.getEmail());
			}
		} catch (Exception e) {
			log.error("Failed to notify course creator");
		}

		return mapToCustomDto(savedEntity);
	}

	@Override
	public EnrollmentResponseDTO updateEnrollmentStatus(Map<String, Object> data) {
		Long eId = Long.valueOf(data.get("enrollmentId").toString());
		Long aId = Long.valueOf(data.get("adminId").toString());
		Status status = Status.valueOf(data.get("status").toString().toUpperCase());

		Enrollment enrollment = enrollmentRepo.findById(eId).orElseThrow(() -> 
			new ResourceNotFoundException("Enrollment not found."));

		UserFeignDTO admin = null;
		try {
			admin = userClient.getUserById(aId);
		} catch (Exception e) {
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service unavailable.");
		}

		if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
			throw new APIException(HttpStatus.FORBIDDEN, "Access Denied: Only Admin can approve.");
		}

		if (enrollment.getStatus().equals(status) && aId.equals(enrollment.getApprovedByAdminId())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Enrollment is already in requested status.");
		}

		enrollment.setStatus(status);
		enrollment.setApprovedByAdminId(aId);
		Enrollment savedEnrollment = enrollmentRepo.save(enrollment);
		
		// 3. FIRE AUDIT LOG (Logged under Admin's ID)
		auditLogger.fireAndForgetLog(aId, "UPDATE_ENROLLMENT", "Enrollment ID: " + eId + " to " + status);

		try {
			StudentFeignDTO student = studentClient.getStudentById(enrollment.getStudentId());
			if (student != null) {
				notificationClient.sendNotification(student.getUserId(), savedEnrollment.getEnrollmentId(), 
						"Status updated to: " + status, "ENROLLMENT_STATUS_UPDATE", student.getEmail());
			}
		} catch (Exception e) {
			log.error("Failed to notify student");
		}

		return mapToCustomDto(savedEnrollment);
	}

	@Override
	public void deleteEnrollment(Long enrollmentId, Long adminId) {
		if (adminId == null || adminId <= 0) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Valid Admin ID required.");
		}

		Enrollment enrollment = enrollmentRepo.findById(enrollmentId).orElseThrow(() -> 
			new ResourceNotFoundException("Enrollment not found."));

		try {
			UserFeignDTO admin = userClient.getUserById(adminId);
			if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
				throw new APIException(HttpStatus.FORBIDDEN, "Access Denied.");
			}
		} catch (Exception e) {
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service unreachable.");
		}

		enrollmentRepo.delete(enrollment);
		
		// 4. FIRE AUDIT LOG (Logged under Admin's ID)
		auditLogger.fireAndForgetLog(adminId, "DELETE_ENROLLMENT", "Enrollment ID: " + enrollmentId);
	}

	@Override
	public List<EnrollmentResponseDTO> getEnrollmentsByStatus(Status status) {
		List<Enrollment> list = enrollmentRepo.findByStatus(status);
		if (list.isEmpty()) throw new ResourceNotFoundException("No records found.");
		return list.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<EnrollmentResponseDTO> getAllEnrollments() {
		List<Enrollment> list = enrollmentRepo.findAll();
		if (list.isEmpty()) throw new ResourceNotFoundException("No records found.");
		return list.stream().map(this::mapToCustomDto).toList();
	}
}
package com.project.edugov.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.FacultyClient;
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

	// Helper method
	private EnrollmentResponseDTO mapToCustomDto(Enrollment e) {
		EnrollmentResponseDTO dto = modelMapper.map(e, EnrollmentResponseDTO.class);
		dto.setEnrollmentDate(e.getDate());

		// Fetch Student Details
		try {
			StudentFeignDTO student = studentClient.getStudentById(e.getStudentId());
			if (student != null) {
				dto.setStudentId(student.getStudentId());
				dto.setStudentName(student.getName());
				dto.setStudentEmail(student.getEmail());
			}
		} catch (Exception ex) {
			log.error("Remote Alert: Student details missing for ID {}", e.getStudentId());
			dto.setStudentId(e.getStudentId());
			dto.setStudentName("Student Not Found");
		}

		// Fetch Faculty Details
		if (e.getCourse() != null) {
			dto.setCourseId(e.getCourse().getCourseId());
			dto.setCourseTitle(e.getCourse().getTitle());
			Long fId = e.getCourse().getFacultyId();
			dto.setFacultyId(fId);
			if (fId != null) {
				try {
					FacultyFeignDTO faculty = facultyClient.getFacultyById(fId);
					if (faculty != null) {
						dto.setFacultyName(faculty.getName());
					}
				} catch (Exception ex) {
					log.error("Remote Alert: Faculty details missing for ID {}", fId);
					dto.setFacultyName("Faculty Not Found");
				}
			}
		}

		// Fetch Admin Details
		if (e.getApprovedByAdminId() != null) {
			try {
				UserFeignDTO admin = userClient.getUserById(e.getApprovedByAdminId());
				if (admin != null) {
					dto.setApprovedByAdminId(admin.getUserId());
					dto.setApprovedByAdminName(admin.getName());
				}
			} catch (Exception ex) {
				log.error("Remote Alert: Admin details missing for ID {}", e.getApprovedByAdminId());
				dto.setApprovedByAdminId(e.getApprovedByAdminId());
				dto.setApprovedByAdminName("Admin Service Unavailable");
			}
		}
		return dto;
	}

	@Override
	public EnrollmentResponseDTO applyForCourse(Long sId, Long cId) {
		log.info("Service: Checking application conditions for Student {} and Course {}", sId, cId);
		// 1. Verify Student via Student Service
		try {
			StudentFeignDTO student = studentClient.getStudentById(sId);
			if (student == null) {
				throw new ResourceNotFoundException("Student not found with ID: " + sId);
			}
		} catch (feign.FeignException.NotFound e) {
			log.warn("Feign: Student ID {} not found in Student Service", sId);
			throw new ResourceNotFoundException("Student not found with ID: " + sId);
		} catch (Exception e) {
			log.error("Feign: Student Service is down/unreachable");
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Student Service is currently unreachable.");
		}
		// Verify Course
		Course course = courseRepo.findById(cId).orElseThrow(() -> {
			log.warn("Apply Failed: Course ID {} not found", cId);
			return new ResourceNotFoundException("Course not found with ID: " + cId);
		});
		// Course Status Check
		if (course.getStatus() != Status.ACTIVE) {
			log.warn("Apply Failed: Course {} is not ACTIVE", cId);
			throw new APIException(HttpStatus.BAD_REQUEST, "Cannot apply: Course is currently INACTIVE.");
		}
		// Duplicate Enrollment Check
		if (enrollmentRepo.existsByStudentIdAndCourse_CourseId(sId, cId)) {
			log.warn("Apply Failed: Student {} already enrolled in Course {}", sId, cId);
			throw new APIException(HttpStatus.BAD_REQUEST,
					"Duplicate enrollment: You are already applied for this course.");
		}
		// Create Enrollment
		Enrollment enrollment = new Enrollment();
		enrollment.setStudentId(sId);
		enrollment.setCourse(course);
		enrollment.setStatus(Status.PENDING);
		enrollment.setDate(LocalDateTime.now());
		log.info("Service: Saving new Enrollment for Student {}", sId);
		return mapToCustomDto(enrollmentRepo.save(enrollment));
	}

	@Override
	public EnrollmentResponseDTO updateEnrollmentStatus(Map<String, Object> data) {
		Long eId = Long.valueOf(data.get("enrollmentId").toString());
		Long aId = Long.valueOf(data.get("adminId").toString());
		Status status = Status.valueOf(data.get("status").toString().toUpperCase());
		log.info("Service: Processing status update for Enrollment {}", eId);
		// Verify Enrollment
		Enrollment enrollment = enrollmentRepo.findById(eId).orElseThrow(() -> {
			log.warn("Update Failed: Enrollment {} not found", eId);
			return new ResourceNotFoundException("Enrollment not found with ID: " + eId);
		});
		// Verify Admin via Identity Service
		try {
			UserFeignDTO admin = userClient.getUserById(aId);
			if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
				throw new APIException(HttpStatus.FORBIDDEN,
						"Access Denied: Only University Admins can update status.");
			}
		} catch (feign.FeignException e) {
			log.error("Feign Error: Status Code {}", e.status());
			// This handles the "Not Found" logic even if the other service throws
			// 500/RuntimeException
			if (e.status() == 404 || e.status() == 500 || e.status() == 403
					|| e.contentUTF8().toLowerCase().contains("not found")) {
				throw new ResourceNotFoundException("Admin not found with ID: " + aId);
			}
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service is unreachable.");
		}
		if (enrollment.getStatus().equals(status) && aId.equals(enrollment.getApprovedByAdminId())) {
			log.info("Service: No changes detected for Enrollment {}.", eId);
			throw new APIException(HttpStatus.BAD_REQUEST, "No changes detected. Enrollment is already " + status);
		}
		enrollment.setStatus(status);
		enrollment.setApprovedByAdminId(aId);
		log.info("Service: Enrollment {} updated to {}", eId, status);
		return mapToCustomDto(enrollmentRepo.save(enrollment));
	}

	@Override
	public List<EnrollmentResponseDTO> getEnrollmentsByStatus(Status status) {
		log.info("Service: Fetching records for status: {}", status);
		List<Enrollment> list = enrollmentRepo.findByStatus(status);
		if (list.isEmpty()) {
			throw new ResourceNotFoundException("No enrollment records found with status: " + status);
		}
		return list.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<EnrollmentResponseDTO> getAllEnrollments() {
		log.info("Service: Retrieving all enrollment records");
		List<Enrollment> list = enrollmentRepo.findAll();
		if (list.isEmpty()) {
			throw new ResourceNotFoundException("No enrollment records found in the database.");
		}
		return list.stream().map(this::mapToCustomDto).toList();
	}
}
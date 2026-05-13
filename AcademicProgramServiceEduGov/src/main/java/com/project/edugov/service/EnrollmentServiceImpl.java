package com.project.edugov.service;

import java.time.LocalDateTime;
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

	// Helper method
	private EnrollmentResponseDTO mapToCustomDto(Enrollment e) {
		EnrollmentResponseDTO dto = modelMapper.map(e, EnrollmentResponseDTO.class);
		dto.setEnrollmentDate(e.getDate());

		// Fetch Student Details from Student Service
		try {
			StudentFeignDTO student = studentClient.getStudentById(e.getStudentId());
			if (student != null) {
				dto.setStudentId(student.getStudentId());
				dto.setStudentName(student.getName());
				dto.setStudentEmail(student.getEmail());
			}
		} catch (Exception ex) {
			log.error("error: student service unreachable for student id {}", e.getStudentId());
			dto.setStudentId(e.getStudentId());
			dto.setStudentName("Student Not Found");
		}

		// Fetch Faculty Details from Registration Service
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
					log.error("error: registration service unreachable for faculty id {}", fId);
					dto.setFacultyName("Faculty Not Found");
				}
			}
		}

		// Fetch Admin Details from Identity Service
		if (e.getApprovedByAdminId() != null) {
			try {
				UserFeignDTO admin = userClient.getUserById(e.getApprovedByAdminId());
				if (admin != null) {
					dto.setApprovedByAdminId(admin.getUserId());
					dto.setApprovedByAdminName(admin.getName());
				}
			} catch (Exception ex) {
				log.error("error: identity service unreachable for admin id {}", e.getApprovedByAdminId());
				dto.setApprovedByAdminId(e.getApprovedByAdminId());
				dto.setApprovedByAdminName("Admin Service Unavailable");
			}
		}
		return dto;
	}

	@Override
	public EnrollmentResponseDTO applyForCourse(Long sId, Long cId) {
		log.info("creating new enrollment for student {} in course {}", sId, cId);
		StudentFeignDTO student = null;
		Course course = null;
		try {
			student = studentClient.getStudentById(sId);
			if (student == null) {
				log.warn("not found: student {} missing in student service", sId);
				throw new ResourceNotFoundException("Student not found with ID: " + sId);
			}
		} catch (feign.FeignException.NotFound e) {
			throw new ResourceNotFoundException("Student not found with ID: " + sId);
		} catch (Exception e) {
			log.error("error: student service is down/unreachable");
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Student Service is unreachable.");
		}
		course = courseRepo.findById(cId).orElseThrow(() -> {
			log.warn("apply failed: course {} not found in db", cId);
			return new ResourceNotFoundException("Course not found with ID: " + cId);
		});
		if (course.getStatus() != Status.ACTIVE) {
			log.warn("apply failed: course {} is inactive", cId);
			throw new APIException(HttpStatus.BAD_REQUEST, "Cannot apply: Course is currently INACTIVE.");
		}
		if (enrollmentRepo.existsByStudentIdAndCourse_CourseId(sId, cId)) {
			log.warn("apply failed: student {} already applied for course {}", sId, cId);
			throw new APIException(HttpStatus.BAD_REQUEST, "Duplicate enrollment: Already applied for this course.");
		}
		Enrollment enrollment = new Enrollment();
		enrollment.setStudentId(sId);
		enrollment.setCourse(course);
		enrollment.setStatus(Status.PENDING);
		enrollment.setDate(LocalDateTime.now());

		Enrollment savedEntity = enrollmentRepo.save(enrollment);
		log.info("done! enrollment created for student {} with id {}", sId, savedEntity.getEnrollmentId());
		try {
			Long creatorAdminId = course.getCreatedByAdminId();
			log.info("ACTION: Notifying Course Creator (Admin ID: {}) about enrollment request", creatorAdminId);
			UserFeignDTO admin = userClient.getUserById(creatorAdminId);
			if (admin != null) {
				String message = "Hello " + admin.getName() + ", a new student (" + student.getName()
						+ ") has applied for your course: " + course.getTitle() + ". Please review the enrollment (ID: "
						+ savedEntity.getEnrollmentId() + ") for approval.";
				notificationClient.sendNotification(admin.getUserId(), savedEntity.getEnrollmentId(), message,
						"ENROLLMENT_APPROVAL_REQUIRED", admin.getEmail());
			}
		} catch (Exception e) {
			log.error("NOTIFICATION ERROR: Failed to notify course creator: " + e.getMessage());
		}
		return mapToCustomDto(savedEntity);
	}

	@Override
	public EnrollmentResponseDTO updateEnrollmentStatus(Map<String, Object> data) {
		Long eId = Long.valueOf(data.get("enrollmentId").toString());
		Long aId = Long.valueOf(data.get("adminId").toString());
		Status status = Status.valueOf(data.get("status").toString().toUpperCase());
		log.info("updating enrollment {} to status: {}", eId, status);
		Enrollment enrollment = enrollmentRepo.findById(eId).orElseThrow(() -> {
			log.warn("update failed: enrollment {} not found in db", eId);
			return new ResourceNotFoundException("Enrollment not found with ID: " + eId);
		});
		try {
			UserFeignDTO admin = userClient.getUserById(aId);
			if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
				log.warn("unauthorized: user {} is not an admin", aId);
				throw new APIException(HttpStatus.FORBIDDEN,
						"Access Denied: Only University Admins can update status.");
			}
		} catch (feign.FeignException e) {
			log.error("Identity Service error: status code {}", e.status());
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service is unreachable.");
		}
		if (enrollment.getStatus().equals(status) && aId.equals(enrollment.getApprovedByAdminId())) {
			log.info("no changes detected for enrollment {}", eId);
			throw new APIException(HttpStatus.BAD_REQUEST, "No changes detected. Enrollment is already " + status);
		}
		enrollment.setStatus(status);
		enrollment.setApprovedByAdminId(aId);
		Enrollment savedEnrollment = enrollmentRepo.save(enrollment);
		try {
			log.info("ACTION: Notifying Student via Student Service for Enrollment {}", eId);
			StudentFeignDTO student = studentClient.getStudentById(enrollment.getStudentId());
			if (student != null) {
				String message = (status == Status.ACTIVE || status == Status.APPROVE)
						? "Your enrollment has been APPROVED."
						: "Your enrollment has been REJECTED.";
				notificationClient.sendNotification(student.getUserId(), savedEnrollment.getEnrollmentId(), message,
						"ENROLLMENT_STATUS_UPDATE", student.getEmail());
			}
		} catch (Exception e) {
			log.error("NOTIFICATION ERROR: " + e.getMessage());
		}
		return mapToCustomDto(savedEnrollment);
	}
	
	
	
	@Override
	public List<EnrollmentResponseDTO> getEnrollmentsByStudentId(Long studentId) {
	    // 1. Get the list from the repo you just fixed
	    List<Enrollment> enrollments = enrollmentRepo.findByStudentId(studentId);

	    // 2. Convert to DTOs so React gets the Title and Status
	    return enrollments.stream().map(e -> {
	        EnrollmentResponseDTO dto = new EnrollmentResponseDTO();
	        dto.setEnrollmentId(e.getEnrollmentId());
	        dto.setStatus(e.getStatus());
	        dto.setCourseId(e.getCourse().getCourseId());
	        dto.setCourseTitle(e.getCourse().getTitle()); // This is crucial for the card!
	        return dto;
	    }).toList();
	}

	@Override
	public List<EnrollmentResponseDTO> getEnrollmentsByStatus(Status status) {
		log.info("fetching enrollments with status: {}", status);
		List<Enrollment> list = enrollmentRepo.findByStatus(status);
		if (list.isEmpty()) {
			log.warn("not found: no records found with status {}", status);
			throw new ResourceNotFoundException("No enrollment records found with status: " + status);
		}

		log.info("getting {} enrollments with status {}", list.size(), status);
		return list.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<EnrollmentResponseDTO> getAllEnrollments() {
		log.info("fetching full list of enrollments...");
		List<Enrollment> list = enrollmentRepo.findAll();
		if (list.isEmpty()) {
			log.warn("db is empty: no enrollments found");
			throw new ResourceNotFoundException("No enrollment records found in the database.");
		}

		log.info("getting total of {} enrollment records", list.size());
		return list.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<EnrollmentResponseDTO> findByStudentId(Long studentId) {
	    log.info("Fetching all enrollment records for student ID: {}", studentId);

	    // 1. Fetch the entities from the repository
	    List<Enrollment> enrollments = enrollmentRepo.findByStudentId(studentId);

	    // 2. Handle empty results gracefully for the UI
	    if (enrollments.isEmpty()) {
	        log.warn("No enrollments found for student {}", studentId);
	        return List.of(); // Return empty list so React can show "No Courses Found"
	    }

	    // 3. Convert Entities to DTOs using your existing helper
	    // This ensures courseTitle and facultyName are populated!
	    return enrollments.stream()
	            .map(this::mapToCustomDto) 
	            .toList();
	}
	
	
	
	

}
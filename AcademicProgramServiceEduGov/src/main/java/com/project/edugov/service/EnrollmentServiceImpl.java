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

	// INJECT AUDIT LOGGER
	private final AsyncAuditLogger auditLogger;

	// Maps enrollment to DTO
	private EnrollmentResponseDTO mapToCustomDto(Enrollment e) {
		EnrollmentResponseDTO dto = modelMapper.map(e, EnrollmentResponseDTO.class);
		dto.setEnrollmentDate(e.getDate());

		// Fetch Student metadata
		try {
			StudentFeignDTO student = studentClient.getStudentById(e.getStudentId());
			if (student != null) {
				dto.setStudentId(student.getStudentId());
				dto.setStudentName(student.getName());
				dto.setStudentEmail(student.getEmail());
			}
		} catch (Exception ex) {
			log.error("[SYSTEM ERROR] Student Service unreachable for Student ID: {}", e.getStudentId());
			dto.setStudentId(e.getStudentId());
			dto.setStudentName("Information temporarily unavailable");
		}

		// Fetch Faculty metadata
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
					log.error("[SYSTEM ERROR] Registration Service unreachable for Faculty ID: {}", fId);
					dto.setFacultyName("Information temporarily unavailable");
				}
			}
		}

		// Fetch Admin metadata
		if (e.getApprovedByAdminId() != null) {
			try {
				UserFeignDTO admin = userClient.getUserById(e.getApprovedByAdminId());
				if (admin != null) {
					dto.setApprovedByAdminId(admin.getUserId());
					dto.setApprovedByAdminName(admin.getName());
				}
			} catch (Exception ex) {
				log.error("[SYSTEM ERROR] Identity Service unreachable for Admin ID: {}", e.getApprovedByAdminId());
				dto.setApprovedByAdminId(e.getApprovedByAdminId());
				dto.setApprovedByAdminName("Information temporarily unavailable");
			}
		}
		return dto;
	}

	@Override
	public EnrollmentResponseDTO applyForCourse(Long sId, Long cId) {
		// Log entry point
		log.info("[START PROCESS] [POST] Request to /enrollments/apply");
		log.info("[ACTION] Applying Student ID {} for Course ID {}", sId, cId);

		// Verify student existence
		StudentFeignDTO student = null;
		try {
			student = studentClient.getStudentById(sId);
		} catch (Exception e) {
			Throwable root = e;
			while (root.getCause() != null)
				root = root.getCause();
			String errorMsg = root.getMessage() != null ? root.getMessage() : "";

			if (errorMsg.contains("404") || errorMsg.contains("NotFound")) {
				log.warn("[DATA NOT FOUND] Student ID {} missing", sId);
				throw new ResourceNotFoundException("The requested student record could not be found.");
			} else {
				log.error("[CRITICAL] Student Service is currently unreachable");
				throw new APIException(HttpStatus.SERVICE_UNAVAILABLE,
						"Registration Service is unavailable. Please try again later.");
			}
		}

		// Verify course existence
		Course course = courseRepo.findById(cId).orElseThrow(() -> {
			log.warn("[DATA NOT FOUND] Course ID {} record missing", cId);
			return new ResourceNotFoundException("The requested course record could not be found.");
		});

		// Check course status
		if (course.getStatus() != Status.ACTIVE) {
			log.warn("[VALIDATION FAILED] Course ID {} is INACTIVE", cId);
			throw new APIException(HttpStatus.BAD_REQUEST, "Application denied: This course is currently INACTIVE.");
		}

		// Prevent duplicate applications
		if (enrollmentRepo.existsByStudentIdAndCourse_CourseId(sId, cId)) {
			log.warn("[CONFLICT] Student {} already applied for Course {}", sId, cId);
			throw new APIException(HttpStatus.BAD_REQUEST,
					"Duplicate application: You have already applied for this course.");
		}

		// Create enrollment entity
		Enrollment enrollment = new Enrollment();
		enrollment.setStudentId(sId);
		enrollment.setCourse(course);
		enrollment.setStatus(Status.PENDING);
		enrollment.setDate(java.time.LocalDateTime.now());

		// Save to database
		Enrollment savedEntity = enrollmentRepo.save(enrollment);
		log.info("[DATABASE SUCCESS] Enrollment saved ID: {}", savedEntity.getEnrollmentId());

		// FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(sId, "APPLY_COURSE", "Course ID: " + cId);

		// Notify course creator
		try {
			Long creatorAdminId = course.getCreatedByAdminId();
			UserFeignDTO admin = userClient.getUserById(creatorAdminId);
			if (admin != null) {
				String message = "Hello " + admin.getName() + ", a new student (" + student.getName()
						+ ") has applied for your course: " + course.getTitle() + ". Review ID: "
						+ savedEntity.getEnrollmentId();
				notificationClient.sendNotification(admin.getUserId(), savedEntity.getEnrollmentId(), message,
						"ENROLLMENT_APPROVAL_REQUIRED", admin.getEmail());
				log.info("[NOTIFICATION] Admin alerted for approval");
			}
		} catch (Exception e) {
			log.error("[SYSTEM] Failed to notify course creator Admin ID: {}", course.getCreatedByAdminId());
		}

		log.info("[SUCCESS] Application process complete");
		return mapToCustomDto(savedEntity);
	}

	@Override
	public EnrollmentResponseDTO updateEnrollmentStatus(Map<String, Object> data) {
		// Log update attempt
		log.info("[START PROCESS] [PUT] Request to /enrollments/update-status");

		Long eId = Long.valueOf(data.get("enrollmentId").toString());
		Long aId = Long.valueOf(data.get("adminId").toString());
		Status status = Status.valueOf(data.get("status").toString().toUpperCase());

		log.info("[ACTION] Updating Enrollment ID {} to status: {}", eId, status);

		// Verify enrollment existence
		Enrollment enrollment = enrollmentRepo.findById(eId).orElseThrow(() -> {
			log.warn("[DATA NOT FOUND] Enrollment ID {} missing", eId);
			return new ResourceNotFoundException("The requested enrollment record was not found.");
		});

		// Verify Admin permissions
		UserFeignDTO admin = null;
		try {
			admin = userClient.getUserById(aId);
		} catch (Exception e) {
			Throwable root = e;
			while (root.getCause() != null)
				root = root.getCause();
			String errorMsg = root.getMessage() != null ? root.getMessage() : "";

			if (errorMsg.contains("404") || errorMsg.contains("403") || errorMsg.contains("NotFound")) {
				log.warn("[AUTH FAILED] Admin ID {} verification failed", aId);
				admin = null;
			} else {
				log.error("[CRITICAL] Identity Service is currently unreachable");
				throw new APIException(HttpStatus.SERVICE_UNAVAILABLE,
						"Identity Service is unreachable. Unable to verify permissions.");
			}
		}

		// Check role authority
		if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
			log.warn("[AUTH FAILED] Access denied for User ID: {}", aId);
			throw new APIException(HttpStatus.FORBIDDEN,
					"Access Denied: Only University Administrators can approve or reject enrollments.");
		}

		// Prevent redundant updates
		if (enrollment.getStatus().equals(status) && aId.equals(enrollment.getApprovedByAdminId())) {
			log.warn("[VALIDATION FAILED] No status changes detected");
			throw new APIException(HttpStatus.BAD_REQUEST,
					"No changes detected. Enrollment is already set to the requested status.");
		}

		// Persist status change
		enrollment.setStatus(status);
		enrollment.setApprovedByAdminId(aId);
		Enrollment savedEnrollment = enrollmentRepo.save(enrollment);
		log.info("[DATABASE SUCCESS] Enrollment status updated");

		// FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(aId, "UPDATE_ENROLLMENT", "Enrollment ID: " + eId + " to " + status);

		// Notify student decision
		try {
			StudentFeignDTO student = studentClient.getStudentById(enrollment.getStudentId());
			if (student != null) {
				String message = (status == Status.ACTIVE || status == Status.APPROVE)
						? "Congratulations! Your enrollment request has been APPROVED."
						: "Regretfully, your enrollment request has been REJECTED.";
				notificationClient.sendNotification(student.getUserId(), savedEnrollment.getEnrollmentId(), message,
						"ENROLLMENT_STATUS_UPDATE", student.getEmail());
				log.info("[NOTIFICATION] Student notified of decision");
			}
		} catch (Exception e) {
			log.error("[SYSTEM] Failed to notify student about status update");
		}

		log.info("[SUCCESS] Status update process complete");
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
		log.info("[START PROCESS] [GET] Request to /enrollments/status/{}", status);
		List<Enrollment> list = enrollmentRepo.findByStatus(status);
		if (list.isEmpty()) {
			log.warn("[DATA NOT FOUND] No records for status {}", status);
			throw new ResourceNotFoundException("No enrollment records found matching the requested status.");
		}
		log.info("[SUCCESS] Filtered records retrieved");
		return list.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<EnrollmentResponseDTO> getAllEnrollments() {
		log.info("[START PROCESS] [GET] Request to /enrollments/all");
		List<Enrollment> list = enrollmentRepo.findAll();
		if (list.isEmpty()) {
			log.warn("[DATA NOT FOUND] Database table empty");
			throw new ResourceNotFoundException("No enrollment records were found in the system.");
		}
		log.info("[SUCCESS] All enrollments retrieved");
		return list.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public void deleteEnrollment(Long enrollmentId, Long adminId) {
		// Log delete request
		log.info("[START PROCESS] [DELETE] Request to /enrollments/delete/{}", enrollmentId);

		// Validate Admin ID
		if (adminId == null || adminId <= 0) {
			log.warn("[VALIDATION FAILED] Invalid Admin ID provided");
			throw new APIException(HttpStatus.BAD_REQUEST, "A valid Administrator reference is required for deletion.");
		}

		// Verify enrollment exists
		Enrollment enrollment = enrollmentRepo.findById(enrollmentId).orElseThrow(() -> {
			log.warn("[DATA NOT FOUND] Deletion target missing ID: {}", enrollmentId);
			return new ResourceNotFoundException("Cannot delete: The requested enrollment record was not found.");
		});

		// Verify Admin authority
		UserFeignDTO admin = null;
		try {
			admin = userClient.getUserById(adminId);
		} catch (Exception e) {
			log.error("[CRITICAL] Identity Service connection failed");
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE,
					"Identity Service unreachable. Cannot verify deletion authority.");
		}

		// Role check authority
		if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
			log.warn("[AUTH FAILED] User {} unauthorized for deletion", adminId);
			throw new APIException(HttpStatus.FORBIDDEN,
					"Access Denied: You do not have permission to delete enrollment records.");
		}

		// Notify student deletion
		try {
			StudentFeignDTO student = studentClient.getStudentById(enrollment.getStudentId());
			if (student != null) {
				String message = "Notice: Your enrollment for the course '" + enrollment.getCourse().getTitle()
						+ "' has been deleted by the system administrator.";
				notificationClient.sendNotification(student.getUserId(), enrollmentId, message, "ENROLLMENT_DELETED",
						student.getEmail());
				log.info("[NOTIFICATION] Student alerted of record removal");
			}
		} catch (Exception e) {
			log.error("[SYSTEM] Failed to notify student regarding record deletion");
		}

		// Final record removal
		enrollmentRepo.delete(enrollment);

		// FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(adminId, "DELETE_ENROLLMENT", "Enrollment ID: " + enrollmentId);

		log.info("[DATABASE SUCCESS] Enrollment {} permanently removed by Admin {}", enrollmentId, adminId);
		log.info("[SUCCESS] Deletion process complete");
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
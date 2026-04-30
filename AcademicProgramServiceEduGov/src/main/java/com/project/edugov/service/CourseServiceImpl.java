package com.project.edugov.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.FacultyClient;
import com.project.edugov.client.NotificationClient;
import com.project.edugov.client.UserClient;
import com.project.edugov.dto.CourseDTO;
import com.project.edugov.dto.FacultyFeignDTO;
import com.project.edugov.dto.UserFeignDTO;
import com.project.edugov.exception.APIException;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Course;
import com.project.edugov.model.Program;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.repository.CourseRepository;
import com.project.edugov.repository.ProgramRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

	private final CourseRepository courseRepo;
	private final ProgramRepository programRepo;
	private final UserClient userClient;
	private final FacultyClient facultyClient;
	private final ModelMapper modelMapper;
	private final NotificationClient notificationClient;

	// Helper method
	private CourseDTO mapToCustomDto(Course c) {
		CourseDTO dto = modelMapper.map(c, CourseDTO.class);

		// 1. Map Admin Details from Identity Service
		try {
			UserFeignDTO admin = userClient.getUserById(c.getCreatedByAdminId());
			if (admin != null) {
				dto.setAdminId(admin.getUserId());
				dto.setAdminName(admin.getName());
			}
		} catch (Exception e) {
			log.error("error: identity service unreachable for admin id {}", c.getCreatedByAdminId());
			dto.setAdminId(c.getCreatedByAdminId());
			dto.setAdminName("Identity Service Unavailable");
		}

		// 2. Map Faculty Details from Registration Service
		try {
			FacultyFeignDTO faculty = facultyClient.getFacultyById(c.getFacultyId());
			if (faculty != null) {
				dto.setFacultyId(faculty.getFacultyId());
				dto.setFacultyName(faculty.getName());
				dto.setFacultyEmail(faculty.getEmail());
			}
		} catch (Exception e) {
			log.error("error: registration service unreachable for faculty id {}", c.getFacultyId());
			dto.setFacultyId(c.getFacultyId());
			dto.setFacultyName("Registration Service Unavailable");
			dto.setFacultyEmail("N/A");
		}

		// 3. Map Program Details
		if (c.getProgram() != null) {
			dto.setProgramId(c.getProgram().getProgramId());
			dto.setProgramTitle(c.getProgram().getTitle());
			dto.setProgramStatus(c.getProgram().getStatus().toString());
		}
		return dto;
	}

	@Override
	@CircuitBreaker(name = "courseService", fallbackMethod = "createCourseFallback")
	public CourseDTO createCourse(Course course) {
		log.info("Creating new course: '{}'", course.getTitle());

		Long pId = (course.getProgram() != null) ? course.getProgram().getProgramId() : null;
		Long fId = course.getFacultyId();
		Long aId = course.getCreatedByAdminId();

		if (pId == null || fId == null || aId == null) {
			log.warn("create failed: missing program, faculty, or admin ids");
			throw new APIException(HttpStatus.BAD_REQUEST, "Program, Faculty, and Admin IDs are required.");
		}

		// 1. Verify Admin via Identity Service
		UserFeignDTO admin = userClient.getUserById(aId);
		if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
			log.warn("unauthorized: user {} is not an admin", aId);
			throw new APIException(HttpStatus.FORBIDDEN, "Access Denied: Only University Admins can create courses.");
		}

		// 2. Verify Program in Local DB
		Program program = programRepo.findById(pId)
				.orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + pId));

		if (program.getStatus() != Status.ACTIVE) {
			log.warn("bad request: program {} is currently inactive", pId);
			throw new APIException(HttpStatus.BAD_REQUEST, "Program is INACTIVE.");
		}

		// 3. Verify Faculty via Registration Service
		FacultyFeignDTO faculty = facultyClient.getFacultyById(fId);
		if (faculty == null) {
			throw new ResourceNotFoundException("Faculty not found with ID: " + fId);
		}

		// 4. Duplicate Check
		if (courseRepo.existsByTitleIgnoreCase(course.getTitle())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Course title already exists.");
		}

		// 5. Save and Notify
		course.setProgram(program);
		Course savedCourse = courseRepo.save(course);
		log.info("Course saved successfully with ID: {}", savedCourse.getCourseId());

		// Notification Logic (Separate try-catch so it doesn't trip the main circuit if
		// it fails)
		try {
			log.info("ACTION: Notifying Faculty {} about new course assignment", fId);
			notificationClient.sendNotification(faculty.getFacultyId(), savedCourse.getCourseId(),
					"Dear " + faculty.getName() + ", you have been assigned to: " + savedCourse.getTitle(),
					"COURSE_ASSIGNMENT", faculty.getEmail());
		} catch (Exception e) {
			log.error("NOTIFICATION ERROR: {}", e.getMessage());
		}

		return mapToCustomDto(savedCourse);
	}

	// FALLBACK for createCourse
	public CourseDTO createCourseFallback(Course course, Throwable t) {
		log.error("FALLBACK: External service verification failed. Reason: {}", t.getMessage());
		throw new APIException(HttpStatus.SERVICE_UNAVAILABLE,
				"External services (Identity/Registration) are currently unavailable. Please try again later.");
	}

	@Override
	@CircuitBreaker(name = "registrationService", fallbackMethod = "getCoursesByFacultyFallback")
	public List<CourseDTO> getCoursesByFacultyId(Long facultyId) {
		log.info("fetching courses assigned to faculty id: {}", facultyId);

		// Verify Faculty via Registration Service
		if (facultyClient.getFacultyById(facultyId) == null) {
			throw new ResourceNotFoundException("Faculty not found with ID: " + facultyId);
		}

		List<Course> courses = courseRepo.findByFacultyId(facultyId);
		if (courses.isEmpty()) {
			log.warn("no courses found for faculty id {}", facultyId);
			throw new ResourceNotFoundException("No courses are currently available for Faculty ID: " + facultyId);
		}

		return courses.stream().map(this::mapToCustomDto).toList();
	}

	// FALLBACK for getCoursesByFacultyId
	public List<CourseDTO> getCoursesByFacultyFallback(Long facultyId, Throwable t) {
		log.error("FALLBACK: Registration service down for faculty id {}. Error: {}", facultyId, t.getMessage());
		throw new APIException(HttpStatus.SERVICE_UNAVAILABLE,
				"Registration Service is unreachable. Cannot verify faculty.");
	}

	@Override
	public List<CourseDTO> getCoursesByProgramId(Long programId) {
		log.info("fetching courses for program id: {}", programId);
		if (!programRepo.existsById(programId)) {
			log.warn("not found: program id {} does not exist", programId);
			throw new ResourceNotFoundException("Program not found with ID: " + programId);
		}

		List<Course> courses = courseRepo.findByProgram_ProgramId(programId);
		if (courses.isEmpty()) {
			log.warn("no courses found for program id {}", programId);
			throw new ResourceNotFoundException("No courses are currently registered under Program ID: " + programId);
		}

		return courses.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public CourseDTO getCourseById(Long courseId) {
		log.info("fetching record for course id: {}", courseId);
		Course course = courseRepo.findById(courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

		return mapToCustomDto(course);
	}

	@Override
	public CourseDTO updateCourse(Long id, Course details) {
		log.info("updating course {} with new info...", id);
		Course existing = courseRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));

		boolean isChanged = false;
		if (details.getTitle() != null && !details.getTitle().equalsIgnoreCase(existing.getTitle())) {
			if (courseRepo.existsByTitleIgnoreCaseAndProgram_ProgramId(details.getTitle(),
					existing.getProgram().getProgramId())) {
				throw new APIException(HttpStatus.BAD_REQUEST, "Title already used in this program.");
			}
			existing.setTitle(details.getTitle());
			isChanged = true;
		}
		if (details.getDescription() != null && !details.getDescription().equals(existing.getDescription())) {
			existing.setDescription(details.getDescription());
			isChanged = true;
		}
		if (details.getStatus() != null && !details.getStatus().equals(existing.getStatus())) {
			existing.setStatus(details.getStatus());
			isChanged = true;
		}

		if (!isChanged) {
			throw new APIException(HttpStatus.BAD_REQUEST, "No changes detected.");
		}

		return mapToCustomDto(courseRepo.save(existing));
	}

	@Override
	public List<CourseDTO> getAllCourses() {
		log.info("fetching full list of courses...");
		List<Course> courses = courseRepo.findAll();
		if (courses.isEmpty()) {
			throw new ResourceNotFoundException("No courses found in the database.");
		}

		return courses.stream().map(this::mapToCustomDto).toList();
	}
}
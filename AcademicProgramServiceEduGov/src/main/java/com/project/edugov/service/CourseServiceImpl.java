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
	
	// 1. INJECT AUDIT LOGGER
	private final AsyncAuditLogger auditLogger;

	private CourseDTO mapToCustomDto(Course c) {
		CourseDTO dto = modelMapper.map(c, CourseDTO.class);

		try {
			UserFeignDTO admin = userClient.getUserById(c.getCreatedByAdminId());
			if (admin != null) {
				dto.setAdminId(admin.getUserId());
				dto.setAdminName(admin.getName());
			}
		} catch (Exception e) {
			log.error("[SYSTEM ERROR] Identity Service unreachable for Admin ID: {}", c.getCreatedByAdminId());
			dto.setAdminId(c.getCreatedByAdminId());
			dto.setAdminName("Information temporarily unavailable");
		}

		try {
			FacultyFeignDTO faculty = facultyClient.getFacultyById(c.getFacultyId());
			if (faculty != null) {
				dto.setFacultyId(faculty.getFacultyId());
				dto.setFacultyName(faculty.getName());
				dto.setFacultyEmail(faculty.getEmail());
			}
		} catch (Exception e) {
			log.error("[SYSTEM ERROR] Registration Service unreachable for Faculty ID: {}", c.getFacultyId());
			dto.setFacultyId(c.getFacultyId());
			dto.setFacultyName("Information temporarily unavailable");
		}

		if (c.getProgram() != null) {
			dto.setProgramId(c.getProgram().getProgramId());
			dto.setProgramTitle(c.getProgram().getTitle());
			dto.setProgramStatus(c.getProgram().getStatus().toString());
		}
		return dto;
	}

	@Override
	public CourseDTO createCourse(Course course) {
		log.info("[START PROCESS] [POST] Request to /courses/save");
		Long pId = (course.getProgram() != null) ? course.getProgram().getProgramId() : null;
		Long fId = course.getFacultyId();
		Long aId = course.getCreatedByAdminId();

		if (pId == null || fId == null || aId == null) {
			throw new APIException(HttpStatus.BAD_REQUEST, "A valid Program, Faculty, and Admin reference are required.");
		}

		UserFeignDTO admin = null;
		try {
			admin = userClient.getUserById(aId);
		} catch (Exception e) {
			Throwable root = e;
			while (root.getCause() != null) root = root.getCause();
			if (root.getMessage() != null && (root.getMessage().contains("404") || root.getMessage().contains("403"))) {
				admin = null;
			} else {
				throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "System unable to verify credentials.");
			}
		}

		if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
			throw new APIException(HttpStatus.FORBIDDEN, "Access Denied: Not authorized as Admin.");
		}

		Program program = programRepo.findById(pId).orElseThrow(() -> 
			new ResourceNotFoundException("Program not found."));

		if (program.getStatus() != Status.ACTIVE) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Linked program is INACTIVE.");
		}

		FacultyFeignDTO faculty = null;
		try {
			faculty = facultyClient.getFacultyById(fId);
		} catch (Exception e) {
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify Faculty.");
		}

		if (faculty == null) {
			throw new ResourceNotFoundException("Faculty record missing.");
		}

		if (courseRepo.existsByTitleIgnoreCase(course.getTitle())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Course title already exists.");
		}

		course.setProgram(program);
		Course savedCourse = courseRepo.save(course);
		
		// 2. FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(aId, "CREATE_COURSE", "Course Title: " + savedCourse.getTitle());

		try {
			notificationClient.sendNotification(faculty.getFacultyId(), savedCourse.getCourseId(),
					"New Course Assignment: '" + savedCourse.getTitle() + "'", "COURSE_ASSIGNMENT", faculty.getEmail());
		} catch (Exception e) {
			log.error("[SYSTEM] Failed to notify Faculty ID: {}", fId);
		}

		return mapToCustomDto(savedCourse);
	}

	@Override
	public CourseDTO updateCourse(Long id, Course details) {
		log.info("[START PROCESS] [PATCH] Request to /courses/update/{}", id);
		Course existing = courseRepo.findById(id).orElseThrow(() -> 
			new ResourceNotFoundException("Course not found."));

		Long oldFacultyId = existing.getFacultyId();
		boolean isChanged = false;
		boolean facultyChanged = false;

		if (details.getTitle() != null && !details.getTitle().equalsIgnoreCase(existing.getTitle())) {
			if (courseRepo.existsByTitleIgnoreCase(details.getTitle())) {
				throw new APIException(HttpStatus.BAD_REQUEST, "Title already in use.");
			}
			existing.setTitle(details.getTitle());
			isChanged = true;
		}

		if (details.getDescription() != null && !details.getDescription().equals(existing.getDescription())) {
			existing.setDescription(details.getDescription());
			isChanged = true;
		}

		if (details.getFacultyId() != null && !details.getFacultyId().equals(existing.getFacultyId())) {
			existing.setFacultyId(details.getFacultyId());
			isChanged = true;
			facultyChanged = true;
		}

		if (details.getStatus() != null && !details.getStatus().equals(existing.getStatus())) {
			existing.setStatus(details.getStatus());
			isChanged = true;
		}

		if (!isChanged) {
			throw new APIException(HttpStatus.BAD_REQUEST, "No changes detected.");
		}

		Course saved = courseRepo.save(existing);
		
		// 3. FIRE AUDIT LOG (Fallback to creator's ID since adminId isn't passed here)
		auditLogger.fireAndForgetLog(saved.getCreatedByAdminId(), "UPDATE_COURSE", "Course ID: " + id);

		try {
			if (facultyChanged) {
				FacultyFeignDTO oldFac = facultyClient.getFacultyById(oldFacultyId);
				notificationClient.sendNotification(oldFacultyId, saved.getCourseId(),
						"Course Removed: '" + saved.getTitle() + "'.", "COURSE_REASSIGNMENT", oldFac.getEmail());

				FacultyFeignDTO newFac = facultyClient.getFacultyById(saved.getFacultyId());
				notificationClient.sendNotification(saved.getFacultyId(), saved.getCourseId(),
						"New Assignment: '" + saved.getTitle() + "'.", "COURSE_ASSIGNMENT", newFac.getEmail());
			}
		} catch (Exception e) {
			log.error("[SYSTEM] Notification failed during course update");
		}

		return mapToCustomDto(saved);
	}

	@Override
	public List<CourseDTO> getCoursesByFacultyId(Long facultyId) {
		List<Course> courses = courseRepo.findByFacultyId(facultyId);
		if (courses.isEmpty()) throw new ResourceNotFoundException("No courses found.");
		return courses.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<CourseDTO> getCoursesByProgramId(Long programId) {
		List<Course> courses = courseRepo.findByProgram_ProgramId(programId);
		if (courses.isEmpty()) throw new ResourceNotFoundException("No courses found.");
		return courses.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public CourseDTO getCourseById(Long courseId) {
		Course course = courseRepo.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course missing"));
		return mapToCustomDto(course);
	}

	@Override
	public List<CourseDTO> getAllCourses() {
		List<Course> courses = courseRepo.findAll();
		if (courses.isEmpty()) throw new ResourceNotFoundException("No courses registered.");
		return courses.stream().map(this::mapToCustomDto).toList();
	}
}
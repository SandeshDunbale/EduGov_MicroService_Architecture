package com.project.edugov.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.FacultyClient;
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

	// Helper method
	private CourseDTO mapToCustomDto(Course c) {
		CourseDTO dto = modelMapper.map(c, CourseDTO.class);

		// 1. Map Admin Details
		try {
			UserFeignDTO admin = userClient.getUserById(c.getCreatedByAdminId());
			if (admin != null) {
				dto.setAdminId(admin.getUserId());
				dto.setAdminName(admin.getName());
			} else {
				dto.setAdminId(c.getCreatedByAdminId());
			}
		} catch (Exception e) {
			log.error("Remote Alert: Identity Service unavailable for Admin ID {}", c.getCreatedByAdminId());
			dto.setAdminId(c.getCreatedByAdminId());
			dto.setAdminName("Admin Service Unavailable");
		}

		// 2. Map Faculty Details
		try {
			FacultyFeignDTO faculty = facultyClient.getFacultyById(c.getFacultyId());
			if (faculty != null) {
				dto.setFacultyId(faculty.getFacultyId());
				dto.setFacultyName(faculty.getName());
				dto.setFacultyEmail(faculty.getEmail());
			} else {
				dto.setFacultyId(c.getFacultyId());
			}
		} catch (Exception e) {
			log.error("Remote Alert: Faculty Service unavailable for Faculty ID {}", c.getFacultyId());
			dto.setFacultyId(c.getFacultyId());
			dto.setFacultyName("Faculty Service Unavailable");
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
	public CourseDTO createCourse(Course course) {
		log.info("Service: Creating Course '{}'", course.getTitle());
		Long pId = (course.getProgram() != null) ? course.getProgram().getProgramId() : null;
		Long fId = course.getFacultyId();
		Long aId = course.getCreatedByAdminId();
		if (pId == null || fId == null || aId == null) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Program, Faculty, and Admin IDs are required.");
		}
		try {
			UserFeignDTO admin = userClient.getUserById(aId);
			if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
				throw new APIException(HttpStatus.FORBIDDEN,
						"Access Denied: Only University Admins can create courses.");
			}
		} catch (feign.FeignException e) {
			if (e.status() == 404 || e.status() == 403 || e.contentUTF8().toLowerCase().contains("not found")) {
				throw new ResourceNotFoundException("Admin not found with ID: " + aId);
			}
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service is unreachable.");
		}
		Program program = programRepo.findById(pId)
				.orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + pId));
		if (program.getStatus() != Status.ACTIVE) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Program is INACTIVE.");
		}
		try {
			if (facultyClient.getFacultyById(fId) == null) {
				throw new ResourceNotFoundException("Faculty not found with ID: " + fId);
			}
		} catch (feign.FeignException e) {
			if (e.status() == 404 || e.contentUTF8().toLowerCase().contains("not found")) {
				throw new ResourceNotFoundException("Faculty not found with ID: " + fId);
			}
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Registration Service is unreachable.");
		}
		if (courseRepo.existsByTitleIgnoreCase(course.getTitle())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Course title already exists.");
		}
		course.setProgram(program);
		return mapToCustomDto(courseRepo.save(course));
	}

	@Override
	public List<CourseDTO> getCoursesByFacultyId(Long facultyId) {
		log.info("Service: Fetching courses assigned to Faculty ID: {}", facultyId);
		try {
			if (facultyClient.getFacultyById(facultyId) == null) {
				throw new ResourceNotFoundException("Faculty not found with ID: " + facultyId);
			}
		} catch (feign.FeignException.NotFound e) {
			log.warn("Feign: Faculty ID {} not found in Registration Service", facultyId);
			throw new ResourceNotFoundException("Faculty not found with ID: " + facultyId);
		} catch (Exception e) {
			log.error("Feign: Connection error while checking Faculty ID {}", facultyId);
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Registration Service is currently unreachable.");
		}
		List<Course> courses = courseRepo.findByFacultyId(facultyId);
		if (courses.isEmpty()) {
			throw new ResourceNotFoundException("No courses are currently available for Faculty ID: " + facultyId);
		}
		return courses.stream().map(this::mapToCustomDto).collect(Collectors.toList());
	}

	@Override
	public List<CourseDTO> getCoursesByProgramId(Long programId) {
		log.info("Service: Fetching courses under Program ID: {}", programId);
		if (!programRepo.existsById(programId)) {
			throw new ResourceNotFoundException("Program not found with ID: " + programId);
		}
		List<Course> courses = courseRepo.findByProgram_ProgramId(programId);
		if (courses.isEmpty()) {
			throw new ResourceNotFoundException("No courses are currently registered under Program ID: " + programId);
		}
		return courses.stream().map(this::mapToCustomDto).collect(Collectors.toList());
	}

	@Override
	public CourseDTO getCourseById(Long courseId) {
		log.info("Service: Fetching course record for ID: {}", courseId);
		Course course = courseRepo.findById(courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
		return mapToCustomDto(course);
	}

	@Override
	public CourseDTO updateCourse(Long id, Course details) {
		log.info("Service: Validating update for Course ID: {}", id);
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
			log.info("Service: No changes detected for Program ID: {}", id);
			throw new APIException(HttpStatus.BAD_REQUEST, "No changes detected. Program is already up to date.");
		}
		log.info("Service: Update successful for ID: {}", id);
		return mapToCustomDto(courseRepo.save(existing));
	}

	@Override
	public List<CourseDTO> getAllCourses() {
		log.info("Service: Retrieving all academic course records");
		List<Course> courses = courseRepo.findAll();
		if (courses.isEmpty()) {
			throw new ResourceNotFoundException("No courses found in the database.");
		}
		return courses.stream().map(this::mapToCustomDto).collect(Collectors.toList());
	}
}

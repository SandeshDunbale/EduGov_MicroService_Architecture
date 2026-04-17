package com.project.edugov.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.FacultyClient; // Feign Client for Faculty
import com.project.edugov.client.UserClient; // Feign Client for Admin
import com.project.edugov.dto.CourseDTO;
import com.project.edugov.dto.FacultyFeignDTO; // Remote Faculty DTO
import com.project.edugov.dto.UserFeignDTO; // Remote Admin DTO
import com.project.edugov.exception.APIException;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Course;
import com.project.edugov.model.Program;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.repository.CourseRepository;
import com.project.edugov.repository.ProgramRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CourseServiceImpl implements CourseService {

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private ProgramRepository programRepo;

	@Autowired
	private UserClient userClient; // Replaces UserRepository

	@Autowired
	private FacultyClient facultyClient; // Replaces FacultyRepository

	@Autowired
	private ModelMapper modelMapper;

	/**
	 * Helper: Combines local Course data with data from remote services. Admin ->
	 * from UserClient Faculty -> from FacultyClient
	 */
	private CourseDTO mapToCustomDto(Course c) {
		CourseDTO dto = modelMapper.map(c, CourseDTO.class);

		// 1. Map Admin Info (From IDENTITY-SERVICE)
		try {
			UserFeignDTO admin = userClient.getUserById(c.getCreatedByAdminId());
			if (admin != null) {
				dto.setAdminId(admin.getUserId());
				dto.setAdminName(admin.getName());
			}
		} catch (Exception e) {
			log.error("Identity Service error for Admin ID: {}", c.getCreatedByAdminId());
			dto.setAdminName("Admin Service Unavailable");
		}

		// 2. Map Faculty Info (From FACULTY-SERVICE)
		try {
			FacultyFeignDTO faculty = facultyClient.getFacultyById(c.getFacultyId());
			if (faculty != null) {
				dto.setFacultyId(faculty.getFacultyId());
				dto.setFacultyName(faculty.getName());
				dto.setFacultyEmail(faculty.getEmail()); // Assumes Faculty Service provides this
			}
		} catch (Exception e) {
			log.error("Faculty Service error for Faculty ID: {}", c.getFacultyId());
			dto.setFacultyName("Faculty Service Unavailable");
		}

		// 3. Map Local Program details
		if (c.getProgram() != null) {
			dto.setProgramId(c.getProgram().getProgramId());
			dto.setProgramTitle(c.getProgram().getTitle());
			dto.setProgramStatus(c.getProgram().getStatus().toString());
		}
		return dto;
	}

	@Override
	public CourseDTO createCourse(Course course, Long pId, Long fId, Long aId) {
		// 1. Check Admin via UserClient
		UserFeignDTO admin = userClient.getUserById(aId);
		if (admin == null) {
			throw new ResourceNotFoundException("Admin not found with ID: " + aId);
		}
		if (!Role.UNIV_ADMIN.equals(admin.getRole())) {
			throw new APIException(HttpStatus.FORBIDDEN, "Access Denied: Only University Admins can create courses.");
		}

		// 2. Check local Program
		Program program = programRepo.findById(pId)
				.orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + pId));

		if (program.getStatus() != Status.ACTIVE) {
			throw new APIException(HttpStatus.BAD_REQUEST,
					"Cannot create course: Program '" + program.getTitle() + "' is INACTIVE.");
		}

		// 3. Check Faculty via FacultyClient
		FacultyFeignDTO faculty = facultyClient.getFacultyById(fId);
		if (faculty == null) {
			throw new ResourceNotFoundException("Faculty not found with ID: " + fId);
		}

		// 4. Title Check
		if (courseRepo.existsByTitleIgnoreCase(course.getTitle())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Course title already exists.");
		}

		// 5. Link and Save
		course.setProgram(program);
		course.setFacultyId(fId);
		course.setCreatedByAdminId(aId);

		log.info("Saving new course '{}' via Microservice logic", course.getTitle());
		return mapToCustomDto(courseRepo.save(course));
	}

	@Override
	public List<CourseDTO> getCoursesByFacultyId(Long facultyId) {
		log.info("Fetching courses for Faculty ID: {}", facultyId);
		// Verify Faculty first
		facultyClient.getFacultyById(facultyId);

		return courseRepo.findByFacultyId(facultyId).stream().map(this::mapToCustomDto).collect(Collectors.toList());
	}

	@Override
	public List<CourseDTO> getCoursesByProgramId(Long programId) {
		log.info("Fetching courses for Program ID: {}", programId);
		programRepo.findById(programId)
				.orElseThrow(() -> new ResourceNotFoundException("Program not found: " + programId));

		return courseRepo.findByProgram_ProgramId(programId).stream().map(this::mapToCustomDto)
				.collect(Collectors.toList());
	}

	@Override
	public CourseDTO getCourseById(Long courseId) {
		Course course = courseRepo.findById(courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
		return mapToCustomDto(course);
	}

	@Override
	public CourseDTO updateCourse(Long id, Course details) {
		Course existing = courseRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));

		// Logic for Title update
		if (details.getTitle() != null && !details.getTitle().equalsIgnoreCase(existing.getTitle())) {
			if (courseRepo.existsByTitleIgnoreCaseAndProgram_ProgramId(details.getTitle(),
					existing.getProgram().getProgramId())) {
				throw new APIException(HttpStatus.BAD_REQUEST, "Title already used in this program.");
			}
			existing.setTitle(details.getTitle());
		}

		if (details.getDescription() != null)
			existing.setDescription(details.getDescription());
		if (details.getStatus() != null)
			existing.setStatus(details.getStatus());

		return mapToCustomDto(courseRepo.save(existing));
	}

	@Override
	public List<CourseDTO> getAllCourses() {
		return courseRepo.findAll().stream().map(this::mapToCustomDto).collect(Collectors.toList());
	}
}
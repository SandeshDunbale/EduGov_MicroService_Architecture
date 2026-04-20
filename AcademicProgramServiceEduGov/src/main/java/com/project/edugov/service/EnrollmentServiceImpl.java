package com.project.edugov.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.FacultyClient; // New Client
import com.project.edugov.client.StudentClient;
import com.project.edugov.client.UserClient;    // Still used for Admin
import com.project.edugov.dto.EnrollmentResponseDTO;
import com.project.edugov.dto.FacultyFeignDTO; // New DTO
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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepo;
    
    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private StudentClient studentClient;

    @Autowired
    private FacultyClient facultyClient; // Updated

    @Autowired
    private UserClient userClient;

    @Autowired
    private ModelMapper modelMapper;

    private EnrollmentResponseDTO mapToCustomDto(Enrollment e) {
        EnrollmentResponseDTO dto = modelMapper.map(e, EnrollmentResponseDTO.class);
        dto.setEnrollmentDate(e.getDate());

        // 1. Fetch from Student Table
        try {
            StudentFeignDTO student = studentClient.getStudentById(e.getStudentId());
            if (student != null) {
                dto.setStudentName(student.getName());
                dto.setStudentEmail(student.getEmail());
            }
        } catch (Exception ex) {
            log.error("Student not found in Student table for ID: {}", e.getStudentId());
            dto.setStudentName("Student Not Found");
        }

        // 2. Fetch from Faculty Table
        if (e.getCourse() != null) {
            dto.setCourseId(e.getCourse().getCourseId());
            dto.setCourseTitle(e.getCourse().getTitle());
            
            Long fId = e.getCourse().getFacultyId();
            dto.setFacultyId(fId);

            if (fId != null) {
                try {
                    // Fetching specifically from the Faculty table/client
                    FacultyFeignDTO faculty = facultyClient.getFacultyById(fId);
                    if (faculty != null) {
                        dto.setFacultyName(faculty.getName());
                    }
                } catch (Exception ex) {
                    log.error("Faculty not found in Faculty table for ID: {}", fId);
                    dto.setFacultyName("Faculty Not Found");
                }
            }
        }

        // 3. Fetch Admin from User Table (Identity Service)
        if (e.getApprovedByAdminId() != null) {
            try {
                UserFeignDTO admin = userClient.getUserById(e.getApprovedByAdminId());
                if (admin != null) {
                    dto.setApprovedByAdminName(admin.getName());
                }
            } catch (Exception ex) {
                log.error("Admin not found in User table for ID: {}", e.getApprovedByAdminId());
            }
        }
        
        return dto;
    }

    @Override
    public EnrollmentResponseDTO applyForCourse(Long sId, Long cId) {
        // Validation: Verify student exists in student table
        if (studentClient.getStudentById(sId) == null) {
            throw new ResourceNotFoundException("Cannot apply: Student ID " + sId + " does not exist.");
        }

        Course course = courseRepo.findById(cId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));

        if (course.getStatus() != Status.ACTIVE) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Course is not active.");
        }

        if (enrollmentRepo.existsByStudentIdAndCourse_CourseId(sId, cId)) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Duplicate enrollment.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(sId);
        enrollment.setCourse(course);
        enrollment.setStatus(Status.PENDING);
        enrollment.setDate(LocalDateTime.now());

        return mapToCustomDto(enrollmentRepo.save(enrollment));
    }

    @Override
    public EnrollmentResponseDTO updateEnrollmentStatus(Long eId, Long aId, Status status) {
        Enrollment enrollment = enrollmentRepo.findById(eId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        // Admins are still verified against the User/Identity table
        UserFeignDTO admin = userClient.getUserById(aId);
        if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
            throw new APIException(HttpStatus.FORBIDDEN, "Only University Admins can update status.");
        }

        enrollment.setStatus(status);
        enrollment.setApprovedByAdminId(aId);
        return mapToCustomDto(enrollmentRepo.save(enrollment));
    }

    @Override
    public List<EnrollmentResponseDTO> getEnrollmentsByStatus(Status status) {
        return enrollmentRepo.findByStatus(status).stream()
                .map(this::mapToCustomDto).collect(Collectors.toList());
    }

    @Override
    public List<EnrollmentResponseDTO> getAllEnrollments() {
        return enrollmentRepo.findAll().stream()
                .map(this::mapToCustomDto).collect(Collectors.toList());
    }
}
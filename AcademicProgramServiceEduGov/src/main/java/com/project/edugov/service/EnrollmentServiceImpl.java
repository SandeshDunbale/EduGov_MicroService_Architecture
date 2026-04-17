package com.project.edugov.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.StudentClient; // For Student Info
import com.project.edugov.client.UserClient;    // For Admin Info
import com.project.edugov.dto.EnrollmentResponseDTO;
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
    private StudentClient studentClient; // Replaces StudentRepository

    @Autowired
    private UserClient userClient;       // Replaces UserRepository

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Helper: Stitches together local Enrollment data with remote Student/Admin info.
     */
    private EnrollmentResponseDTO mapToCustomDto(Enrollment e) {
        EnrollmentResponseDTO dto = modelMapper.map(e, EnrollmentResponseDTO.class);

        // 1. Fetch Student Info from STUDENT-SERVICE
        try {
            StudentFeignDTO student = studentClient.getStudentById(e.getStudentId());
            if (student != null) {
                dto.setStudentName(student.getName());
                dto.setStudentEmail(student.getEmail());
            }
        } catch (Exception ex) {
            log.error("Student Service unavailable for ID: {}", e.getStudentId());
            dto.setStudentName("Information Unavailable");
        }

        // 2. Map Local Course & Faculty Info
        if (e.getCourse() != null) {
            dto.setCourseId(e.getCourse().getCourseId());
            dto.setCourseTitle(e.getCourse().getTitle());
            // Faculty ID is stored in the course entity
            dto.setFacultyId(e.getCourse().getFacultyId()); 
        }

        // 3. Fetch Admin Info from IDENTITY-SERVICE (if approved)
        if (e.getApprovedByAdminId() != null) {
            try {
                UserFeignDTO admin = userClient.getUserById(e.getApprovedByAdminId());
                if (admin != null) {
                    dto.setApprovedByAdminId(admin.getUserId());
                    dto.setApprovedByAdminName(admin.getName());
                }
            } catch (Exception ex) {
                log.error("Identity Service unavailable for Admin ID: {}", e.getApprovedByAdminId());
            }
        }

        dto.setEnrollmentDate(e.getDate());
        return dto;
    }

    @Override
    public EnrollmentResponseDTO applyForCourse(Long sId, Long cId) {
        log.info("Process: New Enrollment Request. Student: {} -> Course: {}", sId, cId);

        // 1. Verify Student via StudentClient
        StudentFeignDTO student = studentClient.getStudentById(sId);
        if (student == null) {
            throw new ResourceNotFoundException("Enrollment failed: Student not found with ID: " + sId);
        }

        // 2. Verify Course locally
        Course course = courseRepo.findById(cId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + cId));

        // 3. Validation: Active Status
        if (course.getStatus() != Status.ACTIVE) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Cannot enroll: Course is currently " + course.getStatus());
        }
        
        if (course.getProgram() != null && course.getProgram().getStatus() != Status.ACTIVE) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Cannot enroll: Parent Program is INACTIVE.");
        }

        // 4. Duplicate Check
        if (enrollmentRepo.existsByStudentIdAndCourse_CourseId(sId, cId)) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Student is already enrolled in this course.");
        }

        // 5. Create Enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(sId);
        enrollment.setCourse(course);
        enrollment.setStatus(Status.PENDING);
        enrollment.setDate(LocalDateTime.now());

        log.info("Enrollment saved for course: {}", course.getTitle());
        return mapToCustomDto(enrollmentRepo.save(enrollment));
    }

    @Override
    public EnrollmentResponseDTO updateEnrollmentStatus(Long enrollmentId, Long adminId, Status newStatus) {
        log.info("Admin {} is updating Enrollment {} to {}", adminId, enrollmentId, newStatus);

        // 1. Verify Enrollment
        Enrollment enrollment = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        // 2. Verify Admin via UserClient
        UserFeignDTO admin = userClient.getUserById(adminId);
        if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
            throw new APIException(HttpStatus.FORBIDDEN, "Access Denied: Only University Admins can update status.");
        }

        // 3. Update Status and Approver
        enrollment.setStatus(newStatus);
        enrollment.setApprovedByAdminId(adminId);

        return mapToCustomDto(enrollmentRepo.save(enrollment));
    }

    @Override
    public List<EnrollmentResponseDTO> getEnrollmentsByStatus(Status status) {
        return enrollmentRepo.findByStatus(status).stream()
                .map(this::mapToCustomDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EnrollmentResponseDTO> getAllEnrollments() {
        return enrollmentRepo.findAll().stream()
                .map(this::mapToCustomDto)
                .collect(Collectors.toList());
    }
}
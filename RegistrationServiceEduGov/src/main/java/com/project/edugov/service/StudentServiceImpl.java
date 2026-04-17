package com.project.edugov.service;

import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.IdentityClient;
import com.project.edugov.client.NotificationClient;
import com.project.edugov.dto.StudentDTO;
import com.project.edugov.dto.StudentResponseDTO;
import com.project.edugov.dto.UserCreateRequest;
import com.project.edugov.dto.UserResponseDTO;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Status;
import com.project.edugov.model.Student;
import com.project.edugov.repository.StudentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepo;
    private final IdentityClient identityClient; // Correct variable name
    private final ModelMapper mapper;
 //   private final NotificationClient notificationClient;
    

    @Override
    public StudentResponseDTO registerStudent(StudentDTO dto) {
        log.info("SERVICE: Registering student profile for email: {}", dto.getEmail());

        // 1. Map DTO to Request
        UserCreateRequest iamRequest = new UserCreateRequest(
            dto.getEmail(), 
            dto.getPassword(), 
            dto.getName(), 
            "STUDENT",
            dto.getPhone()
                    
        );
        
        // FIXED: Changed identityService 
        UserResponseDTO iamUser = identityClient.registerUser(iamRequest);

        // 2. Map and Save
        Student student = mapper.map(dto, Student.class);
        student.setUserId(iamUser.getUserId());
        student.setStatus(Status.PENDING);
        
        Student savedStudent = studentRepo.save(student);
        return convertToResponseDTO(savedStudent, iamUser);
    }

//    @Override
//    public StudentResponseDTO approveStudent(Long id) {
//        Student student = studentRepo.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
//        
//        student.setStatus(Status.APPROVE);
//        
//       
//        identityClient.updateStatus(student.getUserId(), "APPROVE");
//        
//   /**   notificationClient.createNotification(
//                1L, 
//                student.getUserId(),
//                "Your registration has been approved!",
//                "SECURITY",
//                "System"
//        );**/
//        
//        return convertToResponseDTO(studentRepo.save(student), null);
//    }
    @Override
    public StudentResponseDTO approveStudent(Long id) {
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        log.info("Approving Student ID: {} with Identity User ID: {}", id, student.getUserId());
        
        // This sends "APPROVE" as a parameter, NOT as part of the URL path
        identityClient.updateStatus(student.getUserId(), "APPROVE"); 
        
        student.setStatus(Status.APPROVE);
        return convertToResponseDTO(studentRepo.save(student), null);
    }

    @Override
    public StudentResponseDTO declineStudent(Long id) {
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        student.setStatus(Status.REJECT);
        
        // FIXED: Changed iamClient to identityClient
        identityClient.updateStatus(student.getUserId(), "REJECT");
        
        return convertToResponseDTO(studentRepo.save(student), null);
    }

    // ... Other methods (get, update, delete) stay the same ...

    @Override
    public List<StudentResponseDTO> getStudentsByStatus(Status status) {
        return studentRepo.findByStatus(status).stream()
                .map(s -> convertToResponseDTO(s, null))
                .toList();
    }

    @Override
    public StudentResponseDTO updateStudent(Long id, StudentDTO dto) {
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        mapper.map(dto, student);
        return convertToResponseDTO(studentRepo.save(student), null);
    }

    @Override
    public Optional<StudentResponseDTO> getStudentById(Long id) {
        return studentRepo.findById(id).map(s -> convertToResponseDTO(s, null));
    }

    
    
    
    
    
    @Override
    @Transactional
    public String deleteStudent(Long id) {
        // 1. Find the student to get the userId
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        Long userIdToDelete = student.getUserId();

        try {
            // 2. Call the Identity Service to delete the User record
            identityClient.deleteUser(userIdToDelete);
            log.info("Successfully deleted User ID {} from Identity Service", userIdToDelete);
        } catch (Exception e) {
            log.error("Failed to delete user from Identity Service: {}", e.getMessage());
            // Decide: Do you want to stop the deletion if the Identity Service fails?
            // throw new RuntimeException("Could not delete user record, student deletion aborted.");
        }
        
        // 3. Delete from your local Student table
        studentRepo.delete(student);
        
        return "Student and associated User record deleted successfully.";
    }
//    @Override
//    @Transactional
//    public String deleteStudent(Long id) {
//        Student student = studentRepo.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
//        
//        studentRepo.delete(student);
//        
//        return "Student '" + student.getName() + "' (ID: " + id + ") deleted successfully.";
//    }

    private StudentResponseDTO convertToResponseDTO(Student student, UserResponseDTO iamUser) {
        StudentResponseDTO result = mapper.map(student, StudentResponseDTO.class);
        if (iamUser != null) {
            result.setEmail(iamUser.getEmail());
        }
        return result;
    }
}
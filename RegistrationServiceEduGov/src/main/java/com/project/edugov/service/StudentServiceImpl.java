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
            dto.getPhone(),
            dto.getDob()    
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
    UserResponseDTO identityData=identityClient.updateStatus(student.getUserId(), "REJECT");
        
   
       return convertToResponseDTO(studentRepo.save(student), identityData);
    }
//    
    
    
    
//    @Override
//    public StudentResponseDTO declineStudent(Long id) {
//        // 1. Find the student
//        Student student = studentRepo.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
//        
//        // 2. Update local status
//        student.setStatus(Status.REJECT);
//        
//        // 3. CAPTURE the response from Identity Service
//        // Ensure IdentityClient.updateStatus returns UserResponseDTO, not void!
//        UserResponseDTO identityData = identityClient.updateStatus(student.getUserId(), "REJECT");
//        
//        // 4. Save the student
//        Student savedStudent = studentRepo.save(student);
//        
//        // 5. Pass identityData instead of null to the converter
//        return convertToResponseDTO(savedStudent, identityData);
//    }
//    
    
    
    

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
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        // Call Identity Service first!
        identityClient.deleteUser(student.getUserId());
        
        // Then delete locally
        studentRepo.delete(student);
        return "Deleted successfully";
    }
    
    
    
    
    
    
//    
//    @Override
//    @Transactional
//    public String deleteStudent(Long id) {
//        // 1. Find the student to get the userId reference
//        Student student = studentRepo.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
//        
//        Long userIdToDelete = student.getUserId();
//
//        // 2. Call Identity Service via Feign to delete the User
//        try {
//            identityClient.deleteUser(userIdToDelete);
//            log.info("Successfully requested deletion of User ID: {}", userIdToDelete);
//        } catch (Exception e) {
//            // If Identity Service is down, decide if you want to fail the whole process
//            log.error("Failed to delete user from Identity Service: {}", e.getMessage());
//            throw new RuntimeException("External Service Error: Could not delete User credentials.");
//        }
//        
//        // 3. Finally, delete the student from your own database
//        studentRepo.delete(student);
//        
//        return "Student and associated User record have been permanently removed.";
//    }

    private StudentResponseDTO convertToResponseDTO(Student student, UserResponseDTO iamUser) {
        StudentResponseDTO result = mapper.map(student, StudentResponseDTO.class);
        if (iamUser != null) {
            result.setEmail(iamUser.getEmail());
        }
        return result;
    }


    
    
    
}
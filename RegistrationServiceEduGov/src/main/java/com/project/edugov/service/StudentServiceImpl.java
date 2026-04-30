package com.project.edugov.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
   private final NotificationClient notificationClient;
    

//  

   @Override
   @Transactional
   @CircuitBreaker(name = "identityService", fallbackMethod = "registerFallback")
   public StudentResponseDTO registerStudent(StudentDTO dto) {
       log.info("SERVICE: Registering student profile for email: {}", dto.getEmail());

       // 1. Map DTO to Request for the Identity Microservice
       UserCreateRequest iamRequest = new UserCreateRequest(
           dto.getEmail(), 
           dto.getPassword(), 
           dto.getName(), 
           "STUDENT",
           dto.getPhone(),
           dto.getDob()    
       );
       
       // 2. Call Identity Service to create the user credentials
       UserResponseDTO iamUser = identityClient.registerUser(iamRequest);

       // 3. Map DTO to local Student entity and link the User ID
       Student student = mapper.map(dto, Student.class);
       student.setUserId(iamUser.getUserId());
       student.setStatus(Status.PENDING);
       
       // 4. Save the student to your local database
       Student savedStudent = studentRepo.save(student);

       // 5. Send Welcome Notification / Email
       try {
           notificationClient.sendNotification(
               iamUser.getUserId(),               // userId
               savedStudent.getStudentId(),              // entityId (Student primary key)
               "Welcome to EduGov! Your registration was successful and is pending approval.", // message
               "WELCOME",                         // category
               dto.getEmail()                     // recipient email
           );
           log.info("Welcome email sent to: {}", dto.getEmail());
       } catch (Exception e) {
           // We catch the exception so that a mail server error 
           // doesn't stop a successful registration.
           log.error("Failed to send welcome email: {}", e.getMessage());
       }

       // 6. Return the combined data
       return convertToResponseDTO(savedStudent, iamUser);
   }
   
   
   
   
// --- FALLBACK ONLY FOR IDENTITY SERVICE ---
   public StudentResponseDTO registerFallback(StudentDTO dto, Throwable t) {
       log.error("CRITICAL: Identity Service unavailable for registration. Reason: {}", t.getMessage());
       // Since we can't create a student without an Identity ID, we throw an error to the user
       throw new RuntimeException("The Identity Service is currently down. Please try again later.");
   }

    @Override
    @Transactional
    @CircuitBreaker(name = "identityService", fallbackMethod = "statusUpdateFallback")
    public StudentResponseDTO approveStudent(Long id) {
        // 1. Fetch Student
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        // 2. Update Status locally
        student.setStatus(Status.APPROVE);
        student = studentRepo.save(student);

        // 3. Update Identity Service 
        // (Ensure identityData contains the email address)
        UserResponseDTO identityData = identityClient.updateStatus(student.getUserId(), "APPROVE");
        
        // 4. Trigger Notification with CORRECT parameters
        try {
            notificationClient.sendNotification(
                student.getUserId(),              // userId (First param in Client)
                id,                               // entityId (The Student ID)
                "Your registration has been approved!", // message
                "REGISTRATION",                   // category
                identityData.getEmail()           // FIX: Use real email from identityData
            );
        } catch (Exception e) {
            // We log the error but don't throw it, so the approval isn't cancelled 
            // just because an email failed to send.
            log.error("Notification failed for student: " + id + ". Error: " + e.getMessage());
        }
        
        return convertToResponseDTO(student, identityData);
    }
    
    
    
    
    
    
    
 
  
    
    
    
    
    
    @Override
    @Transactional
    @CircuitBreaker(name = "identityService", fallbackMethod = "statusUpdateFallback")// Added for data consistency
    public StudentResponseDTO declineStudent(Long id) {
        // 1. Fetch Student
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        // 2. Update Status locally
        student.setStatus(Status.REJECT);
        student = studentRepo.save(student);

        // 3. Update Identity Service
        UserResponseDTO identityData = identityClient.updateStatus(student.getUserId(), "REJECT");
        
        // 4. Trigger Notification for Rejection
        try {
            notificationClient.sendNotification(
                student.getUserId(),              // userId
                id,                               // entityId
                "We regret to inform you that your registration has been declined.", // message
                "REJECTION",                      // category
                identityData.getEmail()           // real email address
            );
        } catch (Exception e) {
            // Log the failure so the process doesn't stop
            log.error("Decline notification failed for student: " + id + ". Error: " + e.getMessage());
        }
        
        return convertToResponseDTO(student, identityData);
    }
    
 
    

    

    
    
    
    
    
//   @Override
    @Transactional 
    @CircuitBreaker(name = "identityService", fallbackMethod = "UpdateFallback")
    public StudentResponseDTO updateStudent(Long id, StudentDTO dto) {
        // 1. Find the student in the local database
        Student student = studentRepo.findById(id)
               .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        // 2. Map the new changes from the DTO to the Entity and save
        mapper.map(dto, student);
        Student saved = studentRepo.save(student);
        
        // 3. FETCH the identity data (crucial for getting the email)
        UserResponseDTO identityData = identityClient.getUserById(saved.getUserId());
        
        // 4. Trigger Notification for the Update
        try {
            notificationClient.sendNotification(
                saved.getUserId(),              // userId
                id,                             // entityId (Student ID)
                "Your profile information has been successfully updated.", // message
                "UPDATE_SUCCESS",               // category
                identityData.getEmail()         // The email fetched in Step 3
            );
        } catch (Exception e) {
            // Log the error but allow the method to return the DTO
            log.error("Update notification failed for student: " + id + ". Error: " + e.getMessage());
        }
        
        // 5. Pass the retrieved identityData to the converter
        return convertToResponseDTO(saved, identityData);
    }
    
    
    
    @Override
   public Optional<StudentResponseDTO> getStudentById(Long id) {  
    	return studentRepo.findById(id).map(s -> {      
    		UserResponseDTO identityData = identityClient.getUserById(s.getUserId());
           return convertToResponseDTO(s, identityData);
     });
    }   
    

    
    @Override
    public List<StudentResponseDTO> getStudentsByStatus(Status status) {
        return studentRepo.findByStatus(status).stream()
                .map(s -> {
                    // Fetch email/name for each student in the list
                    UserResponseDTO identityData = identityClient.getUserById(s.getUserId());
                    return convertToResponseDTO(s, identityData);
                })
                .toList();
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
  
    public StudentResponseDTO statusUpdateFallback(Long id, Throwable t) {
        log.error("FALLBACK: Could not update Identity status for ID: {}. Error: {}", id, t.getMessage());
        throw new RuntimeException("Communication with Identity Service failed. Status update rolled back.");
    }

    public StudentResponseDTO updateFallback(Long id, StudentDTO dto, Throwable t) {
        log.error("FALLBACK: Profile update failed for ID: {}. Error: {}", id, t.getMessage());
        throw new RuntimeException("Update failed because Identity Service is unreachable.");
    }
    
    
    
    
    


    private StudentResponseDTO convertToResponseDTO(Student student, UserResponseDTO iamUser) {
        StudentResponseDTO result = mapper.map(student, StudentResponseDTO.class);
        if (iamUser != null) {
            result.setEmail(iamUser.getEmail());
        }
        return result;
    }

    //Module 6 requirements
    @Override
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepo.findAll().stream()
                .map(student -> {
                    // Fetch email/name for each student from Identity Service
                    UserResponseDTO identityData = null;
                    try {
                        identityData = identityClient.getUserById(student.getUserId());
                    } catch (Exception e) {
                        log.warn("Could not fetch Identity data for User ID: {}", student.getUserId());
                    }
                    // Use your helper method to map it properly!
                    return convertToResponseDTO(student, identityData);
                })
                .collect(Collectors.toList());
    }
 
}
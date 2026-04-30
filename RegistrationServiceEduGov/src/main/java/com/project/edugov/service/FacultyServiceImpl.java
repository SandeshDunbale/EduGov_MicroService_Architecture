package com.project.edugov.service;

import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.IdentityClient;
import com.project.edugov.client.NotificationClient;
import com.project.edugov.dto.FacultyDTO;
import com.project.edugov.dto.FacultyResponseDTO;
import com.project.edugov.dto.StudentDTO;
import com.project.edugov.dto.StudentResponseDTO;
import com.project.edugov.dto.UserCreateRequest;
import com.project.edugov.dto.UserResponseDTO;
import com.project.edugov.model.Faculty;
import com.project.edugov.model.Status;
import com.project.edugov.repository.FacultyRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FacultyServiceImpl implements FacultyService {

    private final FacultyRepository facultyRepo;
    private final IdentityClient identityClient;
    private final ModelMapper mapper;
    private final NotificationClient notificationClient;
    
    @Override
    @Transactional
    @CircuitBreaker(name = "identityService", fallbackMethod = "registerFallback")
    public FacultyResponseDTO registerFaculty(FacultyDTO dto) {
        log.info("Processing microservice registration for faculty: {}", dto.getEmail());

        UserCreateRequest iamRequest = new UserCreateRequest(
                dto.getEmail(), 
                dto.getPassword(), 
                dto.getName(), 
                "FACULTY", 
                dto.getPhone(),
                dto.getDob()
        );
        
        // 1. Register in Identity Service
        UserResponseDTO iamUser = identityClient.registerUser(iamRequest);

        // 2. Persist Local Profile
        Faculty faculty = mapper.map(dto, Faculty.class);
        faculty.setUserId(iamUser.getUserId()); 
        faculty.setStatus(Status.PENDING);
        Faculty saved = facultyRepo.save(faculty);

        // 3. Send Welcome Notification
        try {
            notificationClient.sendNotification(
                iamUser.getUserId(),
                saved.getFacultyId(),
                "Welcome to EduGov! Your faculty registration is pending approval.",
                "WELCOME",
                dto.getEmail()
            );
        } catch (Exception e) {
            log.error("Welcome notification failed for faculty: {}", dto.getEmail());
        }

        return convertToResponse(saved, iamUser);
    }
    @Override
    @Transactional
    @CircuitBreaker(name = "identityService", fallbackMethod = "statusUpdateFallback")
    public FacultyResponseDTO approveFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty profile not found"));

        UserResponseDTO identityData = identityClient.updateStatus(faculty.getUserId(), "APPROVE");

        faculty.setStatus(Status.APPROVE);
        Faculty updated = facultyRepo.save(faculty);

        // Send Approval Notification
        try {
            notificationClient.sendNotification(
                faculty.getUserId(),
                id,
                "Congratulations! Your faculty account has been approved.",
                "APPROVAL",
                identityData.getEmail()
            );
        } catch (Exception e) {
            log.error("Approval notification failed for faculty ID: {}", id);
        }

        return convertToResponse(updated, identityData);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "identityService", fallbackMethod = "statusUpdateFallback")
    public FacultyResponseDTO declineFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        faculty.setStatus(Status.REJECT);
        UserResponseDTO identityData = identityClient.updateStatus(faculty.getUserId(), "REJECT");
        Faculty saved = facultyRepo.save(faculty);

        // Send Rejection Notification
        try {
            notificationClient.sendNotification(
                faculty.getUserId(),
                id,
                "We regret to inform you that your faculty registration has been declined.",
                "REJECTION",
                identityData.getEmail()
            );
        } catch (Exception e) {
            log.error("Decline notification failed for faculty ID: {}", id);
        }

        return convertToResponse(saved, identityData);
    }
    @Override
    public List<FacultyResponseDTO> getFacultyByStatus(Status status) {
        return facultyRepo.findByStatus(status).stream()
                .map(f -> {
                    // 1. Fetch identity data for this specific faculty's userId
                    UserResponseDTO identityData = identityClient.getUserById(f.getUserId());
                    
                    // 2. Pass the real data instead of 'null'
                    return convertToResponse(f, identityData);
                })
                .toList();
    }



    
    @Override
    public Optional<FacultyResponseDTO> getFacultyById(Long id) {
        return facultyRepo.findById(id).map(f -> {
            // 1. Fetch the user details from Identity Service using the userId stored in Faculty
            UserResponseDTO identityData = identityClient.getUserById(f.getUserId());
            
            // 2. Pass those details to the converter instead of 'null'
            return convertToResponse(f, identityData);
        });
    }

    
    @Override
    @Transactional
    @CircuitBreaker(name = "identityService", fallbackMethod = "updateFallback")
    public FacultyResponseDTO updateFaculty(Long id, FacultyDTO dto) {
        // 1. Fetch the existing faculty record
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));
        
        // 2. Update the local business fields
        faculty.setName(dto.getName());
        faculty.setPhone(dto.getPhone());
        faculty.setDepartment(dto.getDepartment());
        faculty.setDob(dto.getDob());
     
        // 3. Save the changes to the Faculty database
        Faculty updatedFaculty = facultyRepo.save(faculty);

        // 4. CROSS-SERVICE CALL: Fetch the email from Identity Service
        // This is the step that fixes the "null" email in the JSON response
        UserResponseDTO identityData = identityClient.getUserById(updatedFaculty.getUserId());

        // 5. Convert and return (Passing the real identityData instead of 'null')
        return convertToResponse(updatedFaculty, identityData);
    }
  
    @Transactional
    public String deleteFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));
        
        Long userId = faculty.getUserId(); 

        try {
            identityClient.deleteUser(userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete User from Identity Service. Faculty deletion aborted.");
        }
        
        facultyRepo.delete(faculty);
        return "Faculty with ID " + id + " deleted successfully!";
    }

    
    //fallbacks method 
    public FacultyResponseDTO statusUpdateFallback(Long id, Throwable t) {
        log.error("FALLBACK: Could not update Identity status for ID: {}. Error: {}", id, t.getMessage());
        throw new RuntimeException("Communication with Identity Service failed. Status update rolled back.");
    }

    public FacultyResponseDTO updateFallback(Long id, FacultyDTO dto, Throwable t) {
        log.error("FALLBACK: Profile update failed for ID: {}. Error: {}", id, t.getMessage());
        throw new RuntimeException("Update failed because Identity Service is unreachable.");
    }
    public FacultyResponseDTO registerFallback(FacultyDTO dto, Throwable t) {
        log.error("CRITICAL: Identity Service unavailable for registration. Reason: {}", t.getMessage());
        // Since we can't create a student without an Identity ID, we throw an error to the user
        throw new RuntimeException("The Identity Service is currently down. Please try again later.");
    }
    
    
    

    private FacultyResponseDTO convertToResponse(Faculty faculty, UserResponseDTO identityData) {
        FacultyResponseDTO resp = mapper.map(faculty, FacultyResponseDTO.class);
        
        
        resp.setPhone(faculty.getPhone());
        
        if (identityData != null) {
            resp.setEmail(identityData.getEmail());
            // Add this line to make sure DOB shows up too!
            resp.setDob(identityData.getDob()); 
        }
        return resp;
    }
}
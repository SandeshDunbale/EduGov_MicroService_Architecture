package com.project.edugov.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.IdentityClient;
import com.project.edugov.client.NotificationClient;
import com.project.edugov.dto.FacultyDTO;
import com.project.edugov.dto.FacultyResponseDTO;
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

    
    // 1. INJECT THE LOGGER
    private final AsyncAuditLogger auditLogger;

    private final NotificationClient notificationClient;
    @Override
    @Transactional
    @CircuitBreaker(name = "identityService", fallbackMethod = "registerFallback")
    public FacultyResponseDTO registerFaculty(FacultyDTO dto) {
        log.info("Processing microservice registration for faculty: {}", dto.getEmail());

        UserCreateRequest iamRequest = new UserCreateRequest(
                dto.getEmail(), dto.getPassword(), dto.getName(), "FACULTY", dto.getPhone(), dto.getDob() 
        );
        // 1. Register in Identity Service
        UserResponseDTO iamUser = identityClient.registerUser(iamRequest);
        Faculty faculty = mapper.map(dto, Faculty.class);
//        faculty.setUserId(iamUser.getUserId()); 
//        faculty.setStatus(Status.PENDING);
        faculty.setName(dto.getName()); 
        faculty.setPhone(dto.getPhone()); // Ensure DTO has getPhone()
        faculty.setDob(dto.getDob());
        faculty.setAddress(dto.getAddress());
        faculty.setEmail(dto.getEmail());
        faculty.setDepartment(dto.getDepartment());
        faculty.setUserId(iamUser.getUserId());
        faculty.setStatus(Status.PENDING);
        Faculty saved = facultyRepo.save(faculty);
        // 2. LOG REGISTRATION
        auditLogger.fireAndForgetLog(iamUser.getUserId(), "REGISTER_FACULTY", "Email: " + dto.getEmail());
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
        // 3. LOG APPROVAL
        // Note: If you eventually add 'adminId' to this method's parameters, use adminId instead of faculty.getUserId()!
        auditLogger.fireAndForgetLog(faculty.getUserId(), "APPROVE_FACULTY", "Faculty ID: " + id);

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
                    UserResponseDTO identityData = identityClient.getUserById(f.getUserId());
                    return convertToResponse(f, identityData);
                })
                .toList();
    }
    @Override
    public Optional<FacultyResponseDTO> getFacultyById(Long id) {
        return facultyRepo.findById(id).map(f -> {
            UserResponseDTO identityData = identityClient.getUserById(f.getUserId());
            return convertToResponse(f, identityData);
        });
    }

//    @Override
//    public Optional<FacultyResponseDTO> getFacultyByUserId(Long userId) {
//        
//        // 1. Ask the repository to find the Faculty row where user_id matches
//        Optional<Faculty> facultyOptional = facultyRepo.findByUserId(userId);
//        
//        // 2. If it finds one, use ModelMapper to convert it to a DTO and return it
//        return facultyOptional.map(faculty -> mapper.map(faculty, FacultyResponseDTO.class));
//    }
//    @Override
//    @Transactional
//    public FacultyResponseDTO updateFaculty(Long id, FacultyDTO dto) {
//        Faculty faculty = facultyRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Faculty not found"));
//        
//        faculty.setName(dto.getName());
//        faculty.setPhone(dto.getPhone());
//        faculty.setDepartment(dto.getDepartment());
//        faculty.setDob(dto.getDob());
//        
//        Faculty updated = facultyRepo.save(faculty);
//        // Fixed: You were passing 'saved' and 'iamUser' which didn't exist here
//        return convertToResponse(updated, null); 
//    }


    @Override
    @Transactional
    @CircuitBreaker(name = "identityService", fallbackMethod = "updateFallback")
    public FacultyResponseDTO updateFaculty(Long id, FacultyDTO dto) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));
        faculty.setName(dto.getName());
        // 2. Update the local business fields
        faculty.setPhone(dto.getPhone());
        faculty.setDepartment(dto.getDepartment());

        faculty.setDob(dto.getDob());
        // 3. Save the changes to the Faculty database

        Faculty updatedFaculty = facultyRepo.save(faculty);
        UserResponseDTO identityData = identityClient.getUserById(updatedFaculty.getUserId());

        // 4. LOG UPDATE
        auditLogger.fireAndForgetLog(faculty.getUserId(), "UPDATE_FACULTY", "Faculty ID: " + id);

        return convertToResponse(updatedFaculty, identityData);
    }

//    @Override
//    @Transactional
//    public FacultyResponseDTO declineFaculty(Long id) {
//        Faculty faculty = facultyRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Faculty not found"));
//        
//        faculty.setStatus(Status.REJECT);
//        UserResponseDTO identityData = identityClient.updateStatus(faculty.getUserId(), "REJECT");
//        
//        // 5. LOG REJECTION
//        auditLogger.fireAndForgetLog(faculty.getUserId(), "DECLINE_FACULTY", "Faculty ID: " + id);
//
//        return convertToResponse(facultyRepo.save(faculty), identityData);
//    }

    @Override
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
        // 6. LOG DELETION
        auditLogger.fireAndForgetLog(userId, "DELETE_FACULTY", "Faculty ID: " + id);
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
 // Add this implementation in FacultyServiceImpl.java
    @Override
    public List<FacultyResponseDTO> getAllFaculties() {
        return facultyRepo.findAll()
                .stream()
                .map(faculty -> {
                    FacultyResponseDTO dto = new FacultyResponseDTO();
                    dto.setUserId(faculty.getUserId());
                    dto.setName(faculty.getName());
                    dto.setEmail(faculty.getEmail());
                    dto.setStatus(faculty.getStatus()); 
                    dto.setDepartment(faculty.getDepartment());
                          dto.setPhone(faculty.getPhone()) ;
                              dto.setAddress(faculty.getAddress());
                              dto.setDob(faculty.getDob());
                              // Ensure this is PENDING/APPROVED/REJECTED
                    dto.setFacultyId(faculty.getFacultyId());
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<FacultyResponseDTO> getFacultyByUserId(Long userId) {
        return facultyRepo.findByUserId(userId)
                .map((com.project.edugov.model.Faculty faculty) -> { // Use full path if needed
                    FacultyResponseDTO dto = new FacultyResponseDTO();
                    dto.setFacultyId(faculty.getFacultyId());
                    dto.setUserId(faculty.getUserId());
                    dto.setName(faculty.getName());
                    dto.setEmail(faculty.getEmail());
                    dto.setPhone(faculty.getPhone());
                    dto.setDob(faculty.getDob());
                    dto.setAddress(faculty.getAddress());
                    dto.setDepartment(faculty.getDepartment());
                    dto.setStatus(faculty.getStatus());
                    return dto;
                });
    }

    private FacultyResponseDTO convertToResponse(Faculty faculty, UserResponseDTO identityData) {
        FacultyResponseDTO resp = mapper.map(faculty, FacultyResponseDTO.class);
        resp.setFacultyId(faculty.getFacultyId());
        resp.setUserId(faculty.getUserId());
        resp.setName(faculty.getName());
        resp.setPhone(faculty.getPhone());
        resp.setDepartment(faculty.getDepartment());
        resp.setDob(faculty.getDob());
        resp.setAddress(faculty.getAddress());
        resp.setStatus(faculty.getStatus());
        if (identityData != null) {
            resp.setEmail(identityData.getEmail());
            resp.setDob(identityData.getDob()); 
        }
        return resp;
    }
}
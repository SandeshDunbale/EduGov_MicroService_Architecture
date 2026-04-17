package com.project.edugov.service;

import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.IdentityClient;
//import com.project.edugov.client.NotificationClient;
import com.project.edugov.dto.FacultyDTO;
import com.project.edugov.dto.FacultyResponseDTO;
import com.project.edugov.dto.UserCreateRequest;
import com.project.edugov.dto.UserResponseDTO;
import com.project.edugov.model.Faculty;
import com.project.edugov.model.Status;
import com.project.edugov.repository.FacultyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacultyServiceImpl implements FacultyService {

    private final FacultyRepository facultyRepo;
    private final IdentityClient identityClient;
  //  private final NotificationClient notificationClient;
    private final ModelMapper mapper;

    @Override
    @Transactional
    public FacultyResponseDTO registerFaculty(FacultyDTO dto) {
        log.info("Processing microservice registration for faculty: {}", dto.getEmail());

        // 1. COORDINATE WITH IDENTITY SERVICE
        // We call the external service to create the login credentials.
        // It returns a UserResponseDTO containing the numeric ID.
        UserCreateRequest iamRequest = new UserCreateRequest(
                dto.getEmail(),
                dto.getPassword(),
                dto.getName(),
                "FACULTY"
        );
        UserResponseDTO iamUser = identityClient.registerUser(iamRequest);

        // 2. PERSIST LOCAL PROFILE
        // We save the faculty details in OUR database.
        // We store the ID from the Identity service as a reference (soft link).
        Faculty faculty = mapper.map(dto, Faculty.class);
        faculty.setUserId(iamUser.getUserId()); 
        faculty.setStatus(Status.PENDING);
        
        Faculty saved = facultyRepo.save(faculty);

        // 3. TRIGGER NOTIFICATION
        // Notification is 'fire and forget'—we don't want to crash the 
        // whole process if the email service is temporarily down.
      /** try {
            notificationClient.createNotification(
                1L, 
                saved.getUserId(), 
                "Application Received", 
                "INFO", 
                "System"
            );
        } catch (Exception e) {
            log.warn("Notification service unreachable: {}", e.getMessage());
        }**/

        return convertToResponse(saved, iamUser.getEmail());
    }

    @Override
    @Transactional
    public FacultyResponseDTO approveFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty profile not found"));

        // 1. Update Identity Service first
        identityClient.updateStatus(faculty.getUserId(), "APPROVE");

        // 2. Update local state
        faculty.setStatus(Status.APPROVE);
        Faculty updated = facultyRepo.save(faculty);

        // 3. Notify the Faculty member
     /**  notificationClient.createNotification(
            1L, 
            faculty.getUserId(), 
            "Congratulations! Your Faculty profile is approved.", 
            "SUCCESS", 
            "Admin"
        );
**/
        return convertToResponse(updated, null);
    }

    
    
    @Override
    public List<FacultyResponseDTO> getFacultyByStatus(Status status) {
        return facultyRepo.findByStatus(status).stream()
                .map(f -> convertToResponse(f, null))
                .toList();
    }

    @Override
    public Optional<FacultyResponseDTO> getFacultyById(Long id) {
        return facultyRepo.findById(id).map(f -> convertToResponse(f, null));
    }

    @Override
    @Transactional
    public FacultyResponseDTO updateFaculty(Long id, FacultyDTO dto) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        // Update only local profile fields
        faculty.setName(dto.getName());
        faculty.setPhone(dto.getPhone());
        faculty.setDepartment(dto.getDepartment());
        faculty.setDob(dto.getDob());
        
        return convertToResponse(facultyRepo.save(faculty), null);
    }


   
    @Override
    @Transactional
    public String deleteFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));
        
        facultyRepo.delete(faculty);
        
        return "Faculty with ID " + id + " deleted successfully!";
    }

    @Override
    @Transactional
    public FacultyResponseDTO declineFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        faculty.setStatus(Status.REJECT);
        identityClient.updateStatus(faculty.getUserId(), "REJECT");
        
        return convertToResponse(facultyRepo.save(faculty), null);
    }

    /**
     * Helper to map Entity to DTO and manually inject the email 
     * since the email isn't stored in the local faculty table.
     */
    private FacultyResponseDTO convertToResponse(Faculty faculty, String email) {
        FacultyResponseDTO resp = mapper.map(faculty, FacultyResponseDTO.class);
        if (email != null) {
            resp.setEmail(email);
        }
        return resp;
    }
}
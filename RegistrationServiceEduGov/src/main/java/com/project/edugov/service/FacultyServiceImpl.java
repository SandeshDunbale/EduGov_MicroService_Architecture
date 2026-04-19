package com.project.edugov.service;

import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.edugov.client.IdentityClient;
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
    private final ModelMapper mapper;

    @Override
    @Transactional
    public FacultyResponseDTO registerFaculty(FacultyDTO dto) {
        log.info("Processing microservice registration for faculty: {}", dto.getEmail());

        UserCreateRequest iamRequest = new UserCreateRequest(
        		dto.getEmail(),    // 1. Email
        	    dto.getPassword(), // 2. Password
        	    dto.getName(),     // 3. Name
        	    "FACULTY",         // 4. Role
        	    dto.getPhone(),
        	    dto.getDob()// 5. Phone
        );
        
        // 1. Capture the DTO from Identity Service
        UserResponseDTO iamUser = identityClient.registerUser(iamRequest);

        // 2. Persist Local Profile
        Faculty faculty = mapper.map(dto, Faculty.class);
        faculty.setUserId(iamUser.getUserId()); 
        faculty.setStatus(Status.PENDING);
        Faculty saved = facultyRepo.save(faculty);

        // 3. Pass the WHOLE iamUser object to the helper
        return convertToResponse(saved, iamUser);
    }

    @Override
    @Transactional
    public FacultyResponseDTO approveFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty profile not found"));

        // Capture Identity data to get the email for Postman
        UserResponseDTO identityData = identityClient.updateStatus(faculty.getUserId(), "APPROVE");

        faculty.setStatus(Status.APPROVE);
        Faculty updated = facultyRepo.save(faculty);

        return convertToResponse(updated, identityData);
    }

    @Override
    public List<FacultyResponseDTO> getFacultyByStatus(Status status) {
        return facultyRepo.findByStatus(status).stream()
                .map(f -> convertToResponse(f, null)) // Passing null is fine now
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
        
        faculty.setName(dto.getName());
        faculty.setPhone(dto.getPhone());
        faculty.setDepartment(dto.getDepartment());
        faculty.setDob(dto.getDob());
        
        Faculty updated = facultyRepo.save(faculty);
        // Fixed: You were passing 'saved' and 'iamUser' which didn't exist here
        return convertToResponse(updated, null); 
    }

    @Override
    @Transactional
    public FacultyResponseDTO declineFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        faculty.setStatus(Status.REJECT);
        UserResponseDTO identityData = identityClient.updateStatus(faculty.getUserId(), "REJECT");
        
        return convertToResponse(facultyRepo.save(faculty), identityData);
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

    /**
     * Helper Method - This was the source of the compilation error.
     * It now consistently takes UserResponseDTO.
     */
    private FacultyResponseDTO convertToResponse(Faculty faculty, UserResponseDTO identityData) {
        FacultyResponseDTO resp = mapper.map(faculty, FacultyResponseDTO.class);
        
        if (identityData != null) {
            resp.setEmail(identityData.getEmail());
            // Add this line to make sure DOB shows up too!
            resp.setDob(identityData.getDob()); 
        }
        return resp;
    }
}
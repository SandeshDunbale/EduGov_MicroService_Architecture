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
    
    // 1. INJECT THE LOGGER
    private final AsyncAuditLogger auditLogger;

    @Override
    @Transactional
    public FacultyResponseDTO registerFaculty(FacultyDTO dto) {
        log.info("Processing microservice registration for faculty: {}", dto.getEmail());

        UserCreateRequest iamRequest = new UserCreateRequest(
                dto.getEmail(), dto.getPassword(), dto.getName(), "FACULTY", dto.getPhone(), dto.getDob()
        );
        
        UserResponseDTO iamUser = identityClient.registerUser(iamRequest);

        Faculty faculty = mapper.map(dto, Faculty.class);
        faculty.setUserId(iamUser.getUserId()); 
        faculty.setStatus(Status.PENDING);
        Faculty saved = facultyRepo.save(faculty);

        // 2. LOG REGISTRATION
        auditLogger.fireAndForgetLog(iamUser.getUserId(), "REGISTER_FACULTY", "Email: " + dto.getEmail());

        return convertToResponse(saved, iamUser);
    }

    @Override
    @Transactional
    public FacultyResponseDTO approveFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty profile not found"));

        UserResponseDTO identityData = identityClient.updateStatus(faculty.getUserId(), "APPROVE");

        faculty.setStatus(Status.APPROVE);
        Faculty updated = facultyRepo.save(faculty);

        // 3. LOG APPROVAL
        // Note: If you eventually add 'adminId' to this method's parameters, use adminId instead of faculty.getUserId()!
        auditLogger.fireAndForgetLog(faculty.getUserId(), "APPROVE_FACULTY", "Faculty ID: " + id);

        return convertToResponse(updated, identityData);
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

    
    @Override
    public Optional<FacultyResponseDTO> getFacultyByUserId(Long userId) {
        
        // 1. Ask the repository to find the Faculty row where user_id matches
        Optional<Faculty> facultyOptional = facultyRepo.findByUserId(userId);
        
        // 2. If it finds one, use ModelMapper to convert it to a DTO and return it
        return facultyOptional.map(faculty -> mapper.map(faculty, FacultyResponseDTO.class));
    }
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
    public FacultyResponseDTO updateFaculty(Long id, FacultyDTO dto) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + id));
        
        faculty.setName(dto.getName());
        faculty.setPhone(dto.getPhone());
        faculty.setDepartment(dto.getDepartment());
        faculty.setDob(dto.getDob());
     
        Faculty updatedFaculty = facultyRepo.save(faculty);
        UserResponseDTO identityData = identityClient.getUserById(updatedFaculty.getUserId());

        // 4. LOG UPDATE
        auditLogger.fireAndForgetLog(faculty.getUserId(), "UPDATE_FACULTY", "Faculty ID: " + id);

        return convertToResponse(updatedFaculty, identityData);
    }

    @Override
    @Transactional
    public FacultyResponseDTO declineFaculty(Long id) {
        Faculty faculty = facultyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        faculty.setStatus(Status.REJECT);
        UserResponseDTO identityData = identityClient.updateStatus(faculty.getUserId(), "REJECT");
        
        // 5. LOG REJECTION
        auditLogger.fireAndForgetLog(faculty.getUserId(), "DECLINE_FACULTY", "Faculty ID: " + id);

        return convertToResponse(facultyRepo.save(faculty), identityData);
    }

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

    private FacultyResponseDTO convertToResponse(Faculty faculty, UserResponseDTO identityData) {
        FacultyResponseDTO resp = mapper.map(faculty, FacultyResponseDTO.class);
        resp.setPhone(faculty.getPhone());
        
        if (identityData != null) {
            resp.setEmail(identityData.getEmail());
            resp.setDob(identityData.getDob()); 
        }
        return resp;
    }
}
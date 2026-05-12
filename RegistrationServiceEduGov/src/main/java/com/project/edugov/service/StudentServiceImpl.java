package com.project.edugov.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.IdentityClient;
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
    private final IdentityClient identityClient; 
    private final ModelMapper mapper;
    

    // 1. INJECT THE LOGGER
    private final AsyncAuditLogger auditLogger;

 // 🟢 NEW: Implementation for fetching by User ID
    @Override
    public Optional<StudentResponseDTO> getStudentByUserId(Long userId) {
        return studentRepo.findByUserId(userId)
                .map(student -> mapper.map(student, StudentResponseDTO.class));
    }
    

    @Override
    public StudentResponseDTO registerStudent(StudentDTO dto) {
        log.info("SERVICE: Registering student profile for email: {}", dto.getEmail());

        UserCreateRequest iamRequest = new UserCreateRequest(
            dto.getEmail(), dto.getPassword(), dto.getName(), "STUDENT", dto.getPhone(), dto.getDob()   
        );
        
        UserResponseDTO iamUser = identityClient.registerUser(iamRequest);

        Student student = mapper.map(dto, Student.class);
        student.setUserId(iamUser.getUserId());
        student.setStatus(Status.PENDING);
        
        Student savedStudent = studentRepo.save(student);
        
        // 2. LOG REGISTRATION
        auditLogger.fireAndForgetLog(iamUser.getUserId(), "REGISTER_STUDENT", "Email: " + dto.getEmail());
        
        return convertToResponseDTO(savedStudent, iamUser);
    }

    @Override
    public StudentResponseDTO approveStudent(Long id) {
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        log.info("Approving Student ID: {} with Identity User ID: {}", id, student.getUserId());
        
        UserResponseDTO identityData = identityClient.updateStatus(student.getUserId(), "APPROVE"); 
        student.setStatus(Status.APPROVE);
        
        // 3. LOG APPROVAL
        auditLogger.fireAndForgetLog(student.getUserId(), "APPROVE_STUDENT", "Student ID: " + id);
        
        return convertToResponseDTO(studentRepo.save(student), identityData);
    }

    @Override
    public StudentResponseDTO declineStudent(Long id) {
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        student.setStatus(Status.REJECT);
        UserResponseDTO identityData = identityClient.updateStatus(student.getUserId(), "REJECT");
        
        // 4. LOG REJECTION
        auditLogger.fireAndForgetLog(student.getUserId(), "DECLINE_STUDENT", "Student ID: " + id);
        
        return convertToResponseDTO(studentRepo.save(student), identityData);
    }

    @Override
    public StudentResponseDTO updateStudent(Long id, StudentDTO dto) {
        Student student = studentRepo.findById(id)
               .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        mapper.map(dto, student);
        Student saved = studentRepo.save(student);
        
        UserResponseDTO identityData = identityClient.getUserById(saved.getUserId());
        
        // 5. LOG UPDATE
        auditLogger.fireAndForgetLog(student.getUserId(), "UPDATE_STUDENT", "Student ID: " + id);
        
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
        
        Long userId = student.getUserId();
        
        identityClient.deleteUser(userId);
        studentRepo.delete(student);
        
        // 6. LOG DELETION
        auditLogger.fireAndForgetLog(userId, "DELETE_STUDENT", "Student ID: " + id);
        
        return "Deleted successfully";
    }

    private StudentResponseDTO convertToResponseDTO(Student student, UserResponseDTO iamUser) {
        StudentResponseDTO result = mapper.map(student, StudentResponseDTO.class);
        if (iamUser != null) {
            result.setEmail(iamUser.getEmail());
        }
        return result;
    }

    @Override
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepo.findAll().stream()
                .map(student -> {
                    UserResponseDTO identityData = null;
                    try {
                        identityData = identityClient.getUserById(student.getUserId());
                    } catch (Exception e) {
                        log.warn("Could not fetch Identity data for User ID: {}", student.getUserId());
                    }
                    return convertToResponseDTO(student, identityData);
                })
                .collect(Collectors.toList());
    }
}
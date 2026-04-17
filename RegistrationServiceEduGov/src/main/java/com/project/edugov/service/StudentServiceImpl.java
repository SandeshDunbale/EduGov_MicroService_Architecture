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
            "STUDENT"
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

    @Override
    public StudentResponseDTO approveStudent(Long id) {
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        student.setStatus(Status.APPROVED);
        
       
        identityClient.updateStatus(student.getUserId(), "APPROVED");
        
   /**   notificationClient.createNotification(
                1L, 
                student.getUserId(),
                "Your registration has been approved!",
                "SECURITY",
                "System"
        );**/
        
        return convertToResponseDTO(studentRepo.save(student), null);
    }

    @Override
    public StudentResponseDTO declineStudent(Long id) {
        Student student = studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found ID: " + id));
        
        student.setStatus(Status.REJECTED);
        
        // FIXED: Changed iamClient to identityClient
        identityClient.updateStatus(student.getUserId(), "REJECTED");
        
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
    public void deleteStudent(Long id) {
        if (!studentRepo.existsById(id)) throw new ResourceNotFoundException("ID not found: " + id);
        studentRepo.deleteById(id);
    }

    private StudentResponseDTO convertToResponseDTO(Student student, UserResponseDTO iamUser) {
        StudentResponseDTO result = mapper.map(student, StudentResponseDTO.class);
        if (iamUser != null) {
            result.setEmail(iamUser.getEmail());
        }
        return result;
    }
}
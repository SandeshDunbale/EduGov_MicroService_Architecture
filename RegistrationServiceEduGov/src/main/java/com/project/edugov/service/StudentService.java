package com.project.edugov.service;

import com.project.edugov.dto.StudentDTO;
import com.project.edugov.dto.StudentResponseDTO;
import com.project.edugov.model.Status;
import java.util.List;
import java.util.Optional;

public interface StudentService {
    StudentResponseDTO registerStudent(StudentDTO dto);
    
    List<StudentResponseDTO> getStudentsByStatus(Status status);
    
    StudentResponseDTO approveStudent(Long id);
    
    StudentResponseDTO declineStudent(Long id);
    
    StudentResponseDTO updateStudent(Long id, StudentDTO dto);
    
    Optional<StudentResponseDTO> getStudentById(Long id);
    //StudentResponseDTO getStudentById(Long id);
    
    String deleteStudent(Long id);
}
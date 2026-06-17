package com.project.edugov.service;

import com.project.edugov.dto.FacultyDTO;
import com.project.edugov.dto.FacultyResponseDTO;
import com.project.edugov.model.Status;
import java.util.List;
import java.util.Optional;

public interface FacultyService {

    
    FacultyResponseDTO registerFaculty(FacultyDTO dto);

  
    FacultyResponseDTO approveFaculty(Long facultyId);

   
    FacultyResponseDTO declineFaculty(Long facultyId);

    Optional<FacultyResponseDTO> getFacultyById(Long facultyId);

  
    List<FacultyResponseDTO> getFacultyByStatus(Status status);

    
    FacultyResponseDTO updateFaculty(Long facultyId, FacultyDTO dto);
    Optional<FacultyResponseDTO> getFacultyByUserId(Long userId);


    String deleteFaculty(Long id);
}

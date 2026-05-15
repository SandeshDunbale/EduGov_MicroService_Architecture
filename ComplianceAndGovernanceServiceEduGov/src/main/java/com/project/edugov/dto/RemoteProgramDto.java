package com.project.edugov.dto;

// 1. Add this import!
import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteProgramDto {
    
    // 2. Add this annotation right above the variable!
    @JsonProperty("programId")
    private Long programID;

    public Long getProgramID() {
        return programID;
    }

    public void setProgramID(Long programID) {
        this.programID = programID;
    }
}
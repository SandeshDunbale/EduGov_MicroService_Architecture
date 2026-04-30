package com.project.edugov.dto;
 
import lombok.Data;
 
import java.time.LocalDate;
 
@Data
public class ReportDTO {
 
    private Long reportId;
    private String scope;
    private String metrics;
    private LocalDate generatedDate;
}



/*
 * package com.project.edugov.dto;
 * 
 * import lombok.Data; import java.time.LocalDate; import java.util.Map; // Add
 * this import
 * 
 * @Data public class ReportDTO { private Long reportId; private String scope;
 * private Object metrics; // Changed from String to Object private LocalDate
 * generatedDate; }
 */
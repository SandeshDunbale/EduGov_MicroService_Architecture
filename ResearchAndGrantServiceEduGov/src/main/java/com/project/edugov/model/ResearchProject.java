package com.project.edugov.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "ResearchProject")
public class ResearchProject {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ProjectId")
	private Long projectId;

	@Column(name = "Project_Title", nullable = false, length = 255)
	@NotBlank(message = "Project name is required")
	private String title;

	@Size(max = 4000)
	@Column(name = "Project_Desp", nullable = false, length = 4000)
	@NotBlank(message = "Project description is required")
	private String description;

	// CHANGED: No longer a ManyToOne entity mapping, just a Long ID reference
	@Column(name = "FacultyId", nullable = false)
	private Long facultyId;

	@NotNull
	@Column(name = "StartDate", nullable = false)
	private LocalDate startDate;

	@NotNull
	@Column(name = "EndDate", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", length = 32)
	private ProjectStatus status = ProjectStatus.DRAFT;

	@AssertTrue(message = "EndDate must be the same or after StartDate")
	public boolean isDateRangeValid() {
		return startDate == null || endDate == null || !endDate.isBefore(startDate);
	}
}
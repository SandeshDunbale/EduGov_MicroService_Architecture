package com.project.edugov.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "GrantApplication")
public class GrantApplication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ApplicationId")
	private Long applicationID;

	// CHANGED: Replaced Faculty entity mapping with a simple ID
	@Column(name = "FacultyId", nullable = false)
	private Long facultyId;

	// KEPT: This stays because ResearchProject belongs to YOUR microservice!
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ProjectId")
	private ResearchProject project;

	@PastOrPresent(message = "SubmittedDate cannot be in the future")
	@Column(name = "SubmittedDate", nullable = false)
	private LocalDate submittedDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", length = 32)
	private GrantApplicationStatus status = GrantApplicationStatus.UNDER_REVIEW;

	@Column(name = "Requested_Amount", nullable = false)
	@NotNull(message = "Requested amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be greater than 0")
	@Positive(message = "Amount must be greater than zero")
	private BigDecimal requestedAmount;
}
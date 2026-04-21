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
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "Grants")
public class Grant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "GrantID", nullable = false, updatable = false)
	private Long grantId;

	// KEPT: Stays because it's inside your module
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ProjectId")
	private ResearchProject project;

	// CHANGED: Replaced Faculty entity with an ID
	@Column(name = "facultyId", nullable = false)
	private Long facultyId;

	@NotNull(message = "Grant amount is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
	@Digits(integer = 18, fraction = 2)
	@Column(name = "Amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Column(name = "Date", nullable = false)
	private LocalDate date;

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", length = 32)
	private GrantStatus status = GrantStatus.UNDER_REVIEW;

	// CHANGED: Replaced User entity with an ID representing the Program Manager
	@Column(name = "approved_by_ProgramManager_id")
	private Long approvedByUserId; 
}
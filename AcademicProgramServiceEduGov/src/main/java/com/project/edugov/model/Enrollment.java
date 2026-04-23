package com.project.edugov.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "enrollments")
@Data
public class Enrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long enrollmentId;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", referencedColumnName = "courseId", nullable = false)
	private Course course;

	@CreationTimestamp
	@Column(name = "enrollment_date", updatable = false)
	private LocalDateTime date;

	@Enumerated(EnumType.STRING)
	private Status status = Status.PENDING;

	@Column(name = "approved_by_admin_id")
	private Long approvedByAdminId;
}
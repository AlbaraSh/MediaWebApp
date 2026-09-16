package com.mediawebapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lookup row in {@code genres}. Names are stored in canonical Title Case.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "genres")
public class Genre {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;
}

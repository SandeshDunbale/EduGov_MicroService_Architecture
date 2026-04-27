package com.project.edugov.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.edugov.dto.GrantApplicationDTO;
import com.project.edugov.dto.GrantResponseDTO;
import com.project.edugov.model.Grant;
import com.project.edugov.model.GrantApplication;

@Configuration
public class ModelMapperConfig {

	@Bean
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		
		// Configuration: STRICT mapping ensures it only maps fields with EXACT matching names.
		// This prevents accidental data leaks if fields have similar but not exact names.
		modelMapper.getConfiguration()
				   .setMatchingStrategy(MatchingStrategies.STRICT)
				   .setFieldMatchingEnabled(true)
				   .setSkipNullEnabled(true); // Don't overwrite existing values with nulls
		

		// Map Project Title to GrantResponseDTO
		modelMapper.typeMap(Grant.class, GrantResponseDTO.class).addMappings(mapper -> {
			// WE KEPT THIS: Because ResearchProject is still inside your microservice
			mapper.map(src -> src.getProject().getTitle(), GrantResponseDTO::setProjectTitle);
			
			// WE DELETED getApprovedByRole(): Because the User role comes from Feign now!
		});

		// Map Project details to GrantApplicationDTO
		// Map Project Title AND ID to GrantResponseDTO
		modelMapper.typeMap(Grant.class, GrantResponseDTO.class).addMappings(mapper -> {
		    mapper.map(src -> src.getProject().getProjectId(), GrantResponseDTO::setProjectId); // ADD THIS LINE
		    mapper.map(src -> src.getProject().getTitle(), GrantResponseDTO::setProjectTitle);
		});
		
		return modelMapper;
	}
}
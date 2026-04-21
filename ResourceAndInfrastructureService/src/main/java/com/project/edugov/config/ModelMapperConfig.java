package com.project.edugov.config;

import com.project.edugov.dto.*;
import com.project.edugov.model.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mm = new ModelMapper();

        // FIX FOR THE AMBIGUITY ERROR
        // This explicitly tells ModelMapper NOT to guess which ID goes into resourceId
        mm.addMappings(new PropertyMap<ResourceRequest, ResourceRequestResponse>() {
            @Override
            protected void configure() {
                // Skip the automatic mapping for this field to prevent the error
                skip(destination.getResourceId());
            }
        });

        // 1. Mapping for INFRASTRUCTURE requests
        mm.typeMap(ResourceRequest.class, InfrastructureRequestResponse.class)
          .setConverter(ctx -> {
              ResourceRequest src = ctx.getSource();
              Infrastructure infra = src.getInfrastructure();
              
              return InfrastructureRequestResponse.builder()
                  .requestId(src.getRequestId())
                  .requesterUserId(src.getRequesterUserId())
                  .itemType(src.getItemType()) 
                  .status(src.getStatus())
                  .infraId(infra != null ? infra.getInfraId() : null)
                  .infraCapacity(infra != null ? infra.getCapacity() : null)
                  .approvedByUserId(src.getApprovedByUserId())
                  .createdAt(src.getCreatedAt())
                  .updatedAt(src.getUpdatedAt())
                  .decisionAt(src.getDecisionAt())
                  .build();
          });

        // 2. Mapping for RESOURCE requests
        mm.typeMap(ResourceRequest.class, ResourceRequestResponse.class)
          .setConverter(ctx -> {
              ResourceRequest src = ctx.getSource();
              Resource res = src.getResource();
              
              return ResourceRequestResponse.builder()
                  .requestId(src.getRequestId())
                  .requesterUserId(src.getRequesterUserId())
                  .itemType(src.getItemType())
                  .status(src.getStatus())
                  // Manually mapping the ID here solves the confusion
                  .resourceId(res != null ? res.getResourceId() : null)
                  .quantity(src.getQuantity())
                  .createdAt(src.getCreatedAt())
                  .build();
          });

        return mm;
    }
}
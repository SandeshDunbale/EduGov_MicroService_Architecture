package com.project.edugov.config;

import java.util.Map;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.edugov.dto.InfrastructureRequestResponse;
import com.project.edugov.dto.ResourceRequestResponse;
import com.project.edugov.feign.ProgramClient;
import com.project.edugov.model.Infrastructure;
import com.project.edugov.model.RequestItemType;
import com.project.edugov.model.Resource;
import com.project.edugov.model.ResourceRequest;

@Configuration
public class ModelMapperConfig {

	@Bean
	public ModelMapper modelMapper(ProgramClient programClient) {

	    ModelMapper mm = new ModelMapper();

	    mm.addMappings(new PropertyMap<ResourceRequest, ResourceRequestResponse>() {
	        @Override
	        protected void configure() {
	            skip(destination.getResourceId());
	        }
	    });

	    // ✅ INFRA MAPPING
	    mm.typeMap(ResourceRequest.class, InfrastructureRequestResponse.class)
	      .setConverter(ctx -> {

	          ResourceRequest src = ctx.getSource();
	          Infrastructure infra = src.getInfrastructure();

	          String programName = "Unknown Program";

	          if (infra != null) {
	              try {
	                  programName = programClient
	                          .getProgramById(infra.getProgramId())
	                          .title();
	              } catch (Exception e) {
	                  // fallback
	              }
	          }

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
	                  .reason(src.getReason())

	                  // ✅ FIXED
	                  .infrastructureType(infra != null ? infra.getType().name() : null)
	                  .programName(programName)
	                  .location(infra != null ? infra.getLocation() : null)

	                  .build();
	      });

	    // ✅ RESOURCE MAPPING
	    mm.typeMap(ResourceRequest.class, ResourceRequestResponse.class)
	      .setConverter(ctx -> {

	          ResourceRequest src = ctx.getSource();
	          Resource res = src.getResource();

	          String programName = "Unknown Program";

	          if (res != null) {
	              try {
	                  programName = programClient
	                          .getProgramById(res.getProgramId())
	                          .title();
	              } catch (Exception e) {
	                  // fallback
	              }
	          }

	          return ResourceRequestResponse.builder()
	                  .requestId(src.getRequestId())
	                  .requesterUserId(src.getRequesterUserId())
	                  .itemType(src.getItemType())
	                  .status(src.getStatus())
	                  .resourceId(res != null ? res.getResourceId() : null)
	                  .quantity(src.getQuantity())
	                  .createdAt(src.getCreatedAt())
	                  .reason(src.getReason())

	                  // ✅ FIXED
	                  .resourceType(res != null ? res.getType().name() : null)
	                  .programName(programName)

	                  .build();
	      });

	    return mm;
	}
	
	private Object mapWithProgram(ResourceRequest req, Map<Long, String> programMap) {

	    if (req.getItemType() == RequestItemType.RESOURCE) {

	        Resource res = req.getResource();

	        return ResourceRequestResponse.builder()
	                .requestId(req.getRequestId())
	                .requesterUserId(req.getRequesterUserId())
	                .itemType(req.getItemType())
	                .status(req.getStatus())
	                .resourceId(res != null ? res.getResourceId() : null)
	                .quantity(req.getQuantity())
	                .createdAt(req.getCreatedAt())
	                .reason(req.getReason())
	                .resourceType(res != null ? res.getType().name() : null)

	                // ✅ ONLY MAP LOOKUP (NO FEIGN HERE)
	                .programName(programMap.getOrDefault(
	                        res != null ? res.getProgramId() : null,
	                        "Unknown Program"
	                ))

	                .build();

	    } else {

	        Infrastructure infra = req.getInfrastructure();

	        return InfrastructureRequestResponse.builder()
	                .requestId(req.getRequestId())
	                .requesterUserId(req.getRequesterUserId())
	                .itemType(req.getItemType())
	                .status(req.getStatus())
	                .infraId(infra != null ? infra.getInfraId() : null)
	                .infraCapacity(infra != null ? infra.getCapacity() : null)
	                .createdAt(req.getCreatedAt())
	                .reason(req.getReason())
	                .infrastructureType(infra != null ? infra.getType().name() : null)

	                // ✅ OPTIMIZED
	                .programName(programMap.getOrDefault(
	                        infra != null ? infra.getProgramId() : null,
	                        "Unknown Program"
	                ))

	                .build();
	    }
	}
}
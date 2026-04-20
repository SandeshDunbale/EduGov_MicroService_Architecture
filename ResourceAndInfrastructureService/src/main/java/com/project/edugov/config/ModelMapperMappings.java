package com.project.edugov.config;

import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import com.project.edugov.dto.*;
import com.project.edugov.model.*;

@Configuration
public class ModelMapperMappings {

    private final ModelMapper mm;

    public ModelMapperMappings(ModelMapper mm) {
        this.mm = mm;
    }

    @PostConstruct
    public void setup() {

        // =================================================
        // Resource → ResourceResponse
        // =================================================
        mm.typeMap(Resource.class, ResourceResponse.class)
          .setConverter(ctx -> {
              Resource r = ctx.getSource();
              return new ResourceResponse(
                      r.getResourceId(),
                      r.getProgramId(),     // ✅ ID only
                      r.getType(),
                      r.getQuantity(),
                      r.getStatus()
              );
          });

        // =================================================
        // Infrastructure → InfrastructureResponse
        // =================================================
        mm.typeMap(Infrastructure.class, InfrastructureResponse.class)
          .setConverter(ctx -> {
              Infrastructure i = ctx.getSource();
              return new InfrastructureResponse(
                      i.getInfraId(),
                      i.getProgramId(),    // ✅ ID only
                      i.getType(),
                      i.getLocation(),
                      i.getCapacity(),
                      i.getStatus()
              );
          });

        // =================================================
        // ResourceRequest → ResourceRequestResponse
        // =================================================
        mm.typeMap(ResourceRequest.class, ResourceRequestResponse.class)
          .setConverter(ctx -> {
              ResourceRequest rr = ctx.getSource();
              return new ResourceRequestResponse(
                      rr.getRequestId(),
                      rr.getRequesterUserId(),               // ✅ ID only
                      rr.getResource() != null
                              ? rr.getResource().getResourceId()
                              : null,
                      rr.getInfrastructure() != null
                              ? rr.getInfrastructure().getInfraId()
                              : null,
                      rr.getItemType(),
                      rr.getQuantity(),
                      rr.getStatus(),
                      rr.getApprovedByUserId(),              // ✅ ID only
                      rr.getCreatedAt(),
                      rr.getUpdatedAt(),
                      rr.getDecisionAt()
              );
          });
    }
}
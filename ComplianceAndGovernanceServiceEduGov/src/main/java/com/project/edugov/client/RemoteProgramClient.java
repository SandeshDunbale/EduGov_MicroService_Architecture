package com.project.edugov.client;

import com.project.edugov.dto.RemoteProgramDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
    name = "ACADEMICPROGRAMSERVICEEDUGOV",
    contextId = "remoteProgramClient",
    path = "/programs"
)
public interface RemoteProgramClient {

    @GetMapping("/status/{status}")
    List<RemoteProgramDto> getByStatus(
        @PathVariable("status") String status
    );
}

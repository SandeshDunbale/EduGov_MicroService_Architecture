package com.project.edugov.filter;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouteValidator {


//    public static final List<String> openApiEndpoints = List.of(
//            "/api/auth/login",
//            "/api/auth/recoverEmail",
//            "/api/auth/resetPassword",
//            "/api/identity/register",
//            "/eureka"
//    );

	public static final List<String> openApiEndpoints = List.of(
		    "/api/auth/login",
		    "/api/auth/recoverEmail",
		    "/api/auth/resetPassword",
		    "/eureka",
		    "/api/identity/register",
		    "/documents/student/upload",  // ADD THIS LINE
		    "/documents/faculty/upload"   // ADD THIS LINE
		);


    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
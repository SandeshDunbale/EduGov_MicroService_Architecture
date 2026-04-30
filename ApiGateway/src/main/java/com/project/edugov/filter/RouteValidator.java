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
		    "/api/users/recoverEmail", // <-- FIXED PATH
		    "/api/auth/resetPassword",
		    "/eureka",
		    "/students/register", // <- MUST BE HERE
		    "/faculty/register",
		    "/api/identity/register",
		    "/documents/student/upload", 
		    "/documents/faculty/upload" 
		);
 
 
    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
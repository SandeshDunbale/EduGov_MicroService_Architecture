/*
 * package com.project.edugov.controller;
 * 
 * import com.project.edugov.config.JwtUtil; import
 * lombok.RequiredArgsConstructor; import
 * org.springframework.web.bind.annotation.*;
 * 
 * @RestController
 * 
 * @RequestMapping("/auth")
 * 
 * @RequiredArgsConstructor public class AuthController {
 * 
 * private final JwtUtil jwtUtil;
 * 
 * @PostMapping("/login") public String login(@RequestParam String username) {
 * return jwtUtil.generateToken(username); } }
 */
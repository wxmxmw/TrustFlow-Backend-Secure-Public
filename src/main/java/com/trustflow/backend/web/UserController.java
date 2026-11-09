package com.trustflow.backend.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Map<String, Object> res = new HashMap<>();
        res.put("message", "not_implemented");
        return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
    }
}


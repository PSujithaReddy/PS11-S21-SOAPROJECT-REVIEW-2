package com.example.authservice;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    public static final String SECRET = "SmartUrbanMobilitySecretKey2026ProjectSecureKey!";

    static class LoginRequest { public String username; public String password; }

    private Key getSignKey() { return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)); }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        Map<String, String> response = new HashMap<>();
        // Hardcoded admin credentials for the review demo
        if ("admin".equals(req.username) && "admin".equals(req.password)) {
            String jwt = Jwts.builder().setSubject(req.username)
                    .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
            response.put("token", "Bearer " + jwt);
        } else {
            response.put("error", "Invalid credentials");
        }
        return response;
    }
}

package com.sagarpandey.activity_tracker.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtIssuerDebugFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtIssuerDebugFilter.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String configuredIssuer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only debug the specific API endpoint
        if ("/api/v1/goals/tree".equals(path)) {
            log.info("=== JWT Issuer Debug for /api/v1/goals/tree ===");
            log.info("Configured issuer-uri: {}", configuredIssuer);
            
            // Extract Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.info("JWT token received: {}...", token.substring(0, Math.min(50, token.length())));
                
                // Try to decode the JWT payload (without verification)
                try {
                    String[] parts = token.split("\\.");
                    if (parts.length == 3) {
                        String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
                        log.info("JWT payload: {}", payload);
                        
                        // Extract iss claim from payload
                        if (payload.contains("\"iss\"")) {
                            String issStart = "\"iss\":\"";
                            int issIndex = payload.indexOf(issStart);
                            if (issIndex != -1) {
                                int start = issIndex + issStart.length();
                                int end = payload.indexOf("\"", start);
                                String jwtIss = payload.substring(start, end);
                                log.info("JWT iss claim: {}", jwtIss);
                                log.info("Issuer match: {}", jwtIss.equals(configuredIssuer));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error decoding JWT: {}", e.getMessage());
                }
            } else {
                log.info("No Authorization header found");
            }
            log.info("=== End JWT Issuer Debug ===");
        }
        
        filterChain.doFilter(request, response);
    }
}

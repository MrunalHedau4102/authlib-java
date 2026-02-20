package dev.authlib.examples;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.authlib.services.AuthService;
import dev.authlib.services.UserService;
import dev.authlib.models.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot REST Controller demonstrating AuthLib usage
 * 
 * Example endpoints:
 * POST   /api/auth/register    - Register new user
 * POST   /api/auth/login       - Login user
 * POST   /api/auth/refresh     - Refresh access token
 * POST   /api/auth/logout      - Logout user (blacklist token)
 * GET    /api/users/:id        - Get user by ID
 * GET    /api/users/email/:email - Get user by email
 * PUT    /api/users/:id        - Update user
 * POST   /api/users/:id/activate - Activate user
 * POST   /api/users/:id/deactivate - Deactivate user
 * POST   /api/users/:id/verify - Verify user
 * GET    /api/health           - Health check
 */
@RestController
@RequestMapping("/api")
public class AuthLibController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    // ========================================================================
    // Auth Endpoints
    // ========================================================================

    /**
     * Register a new user
     * 
     * Example request:
     * {
     *   "email": "user@example.com",
     *   "password": "SecurePass123!",
     *   "firstName": "John",
     *   "lastName": "Doe"
     * }
     */
    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody AuthService.RegisterRequest request) {
        
        try {
            var result = authService.register(request);

            if (!result.success) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", result.message != null ? result.message : "Registration failed"
                ));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "User registered successfully",
                "accessToken", result.accessToken,
                "refreshToken", result.refreshToken,
                "user", Map.of(
                    "id", result.user.getId(),
                    "email", result.user.getEmail(),
                    "firstName", result.user.getFirstName(),
                    "lastName", result.user.getLastName()
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Login user
     * 
     * Example request:
     * {
     *   "email": "user@example.com",
     *   "password": "SecurePass123!"
     * }
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody AuthService.LoginRequest request) {
        
        try {
            var result = authService.login(request);

            if (!result.success) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", result.message != null ? result.message : "Login failed"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful",
                "accessToken", result.accessToken,
                "refreshToken", result.refreshToken,
                "expiresIn", 3600,
                "user", Map.of(
                    "id", result.user.getId(),
                    "email", result.user.getEmail(),
                    "firstName", result.user.getFirstName(),
                    "lastName", result.user.getLastName()
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * Refresh access token
     * 
     * Example request:
     * {
     *   "refreshToken": "eyJhbGc..."
     * }
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestBody Map<String, String> request) {
        
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Refresh token is required"
                ));
            }

            var result = authService.refreshAccessToken(refreshToken);

            if (!result.success) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", result.message != null ? result.message : "Token refresh failed"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Token refreshed successfully",
                "accessToken", result.accessToken,
                "expiresIn", 3600
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * Logout user (blacklist refresh token)
     * 
     * Example request:
     * {
     *   "refreshToken": "eyJhbGc..."
     * }
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestBody Map<String, String> request) {
        
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Refresh token is required"
                ));
            }

            var result = authService.logout(refreshToken);

            if (!result.success) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", result.message != null ? result.message : "Logout failed"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    // ========================================================================
    // User Endpoints
    // ========================================================================

    /**
     * Get user by ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String id) {
        try {
            User user = userService.getUserById(id);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "isActive", user.isActive(),
                    "isVerified", user.isVerified(),
                    "createdAt", user.getCreatedAt(),
                    "lastLogin", user.getLastLogin()
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * Get user by email
     */
    @GetMapping("/users/email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        try {
            User user = userService.getUserByEmail(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "isActive", user.isActive(),
                    "isVerified", user.isVerified()
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * Update user
     * 
     * Example request:
     * {
     *   "firstName": "Jane",
     *   "lastName": "Smith"
     * }
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String id,
            @RequestBody Map<String, String> updates) {
        
        try {
            User user = userService.getUserById(id);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            // Update fields
            if (updates.containsKey("firstName")) {
                user.setFirstName(updates.get("firstName"));
            }
            if (updates.containsKey("lastName")) {
                user.setLastName(updates.get("lastName"));
            }

            // In a real application, you would persist changes:
            // userService.saveUser(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User updated successfully",
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName()
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * Activate user account
     */
    @PostMapping("/users/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable String id) {
        try {
            boolean success = userService.activateUser(id);

            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User activated successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * Deactivate user account
     */
    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable String id) {
        try {
            boolean success = userService.deactivateUser(id);

            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deactivated successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * Verify user email
     */
    @PostMapping("/users/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyUser(@PathVariable String id) {
        try {
            boolean success = userService.verifyUser(id);

            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User verified successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    // ========================================================================
    // Health Check
    // ========================================================================

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "timestamp", new java.util.Date(),
            "version", "1.0.0"
        ));
    }
}

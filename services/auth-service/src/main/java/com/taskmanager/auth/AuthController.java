package com.taskmanager.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserAccountRepository users;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final SecretKey key;
  private final long ttlSeconds;

  public AuthController(UserAccountRepository users,
      @Value("${JWT_SECRET}") String secret,
      @Value("${JWT_TTL_SECONDS:3600}") long ttlSeconds) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
    this.users=users; this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.ttlSeconds=ttlSeconds;
  }

  public record RegisterRequest(String username, String email, String password) {}
  public record LoginRequest(String email, String password) {}
  public record AuthResponse(String token, String tokenType, long expiresIn, UserView user) {}
  public record UserView(String id, String username, String email) {}

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@RequestBody RegisterRequest request) {
    if (request.username()==null || request.username().trim().length()<3 || request.username().length()>80)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Username must be 3-80 characters");
    if (request.email()==null || !request.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid email is required");
    if (request.password()==null || request.password().length()<8 || request.password().length()>72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be 8-72 characters");
    if (users.existsByEmailIgnoreCase(request.email().trim()))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");
    UserAccount saved=users.save(new UserAccount(request.username().trim(),request.email().trim(),encoder.encode(request.password())));
    return response(saved);
  }

  @PostMapping("/login")
  public AuthResponse login(@RequestBody LoginRequest request) {
    if (request.email()==null || request.password()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email and password are required");
    UserAccount user=users.findByEmailIgnoreCase(request.email().trim())
      .filter(u -> encoder.matches(request.password(),u.getPasswordHash()))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password"));
    return response(user);
  }

  @GetMapping("/me")
  public UserView me(@RequestHeader(value="Authorization", required=false) String authorization) {
    String subject=subject(authorization);
    UserAccount user=users.findByEmailIgnoreCase(subject).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account no longer exists"));
    return new UserView(user.getId(),user.getUsername(),user.getEmail());
  }

  private AuthResponse response(UserAccount user) {
    Instant now=Instant.now();
    String token=Jwts.builder().subject(user.getEmail()).issuedAt(java.util.Date.from(now))
      .expiration(java.util.Date.from(now.plusSeconds(ttlSeconds))).signWith(key).compact();
    return new AuthResponse(token,"Bearer",ttlSeconds,new UserView(user.getId(),user.getUsername(),user.getEmail()));
  }
  private String subject(String authorization) {
    try {
      if (authorization==null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
      return Jwts.parser().verifyWith(key).build().parseSignedClaims(authorization.substring(7)).getPayload().getSubject();
    } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid or expired token"); }
  }
}
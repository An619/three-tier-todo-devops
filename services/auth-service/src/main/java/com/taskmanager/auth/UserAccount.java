package com.taskmanager.auth;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="user_accounts", uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class UserAccount {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
  @Column(nullable=false, unique=true, length=254) private String email;
  @Column(nullable=false, length=80) private String username;
  @Column(nullable=false, length=100) private String passwordHash;
  @Column(nullable=false) private Instant createdAt=Instant.now();
  protected UserAccount() {}
  public UserAccount(String username, String email, String passwordHash) { this.username=username; this.email=email.toLowerCase(java.util.Locale.ROOT); this.passwordHash=passwordHash; }
  public String getId(){return id;} public String getEmail(){return email;} public String getUsername(){return username;}
  public String getPasswordHash(){return passwordHash;}
}
package com.taskmanager.auth;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
class AuthControllerValidationTest {
  @Test void passwordsAreStoredAsHashesNotPlainText() {
    BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    String raw="CorrectHorseBattery42!";
    String hash=encoder.encode(raw);
    assertNotEquals(raw,hash);
    assertTrue(encoder.matches(raw,hash));
    assertFalse(encoder.matches("wrong-password",hash));
  }
}
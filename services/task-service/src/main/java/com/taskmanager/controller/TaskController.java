package com.taskmanager.controller;
import com.taskmanager.dto.TaskDTO;
import com.taskmanager.service.TaskService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
  private final TaskService taskService;
  private final SecretKey key;
  public TaskController(TaskService taskService,@Value("${JWT_SECRET}") String secret){
    if(secret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
    this.taskService=taskService;this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
  @GetMapping public ResponseEntity<List<TaskDTO>> getAllTasks(@RequestHeader(value="Authorization",required=false) String auth){return ResponseEntity.ok(taskService.getTasksForOwner(subject(auth)));}
  @GetMapping("/{id}") public ResponseEntity<TaskDTO> getTaskById(@PathVariable String id,@RequestHeader(value="Authorization",required=false) String auth){return taskService.getTaskForOwner(id,subject(auth)).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());}
  @PostMapping public ResponseEntity<TaskDTO> createTask(@RequestBody TaskDTO dto,@RequestHeader(value="Authorization",required=false) String auth){return new ResponseEntity<>(taskService.createTaskForOwner(dto,subject(auth)),HttpStatus.CREATED);}
  @PutMapping("/{id}") public ResponseEntity<TaskDTO> updateTask(@PathVariable String id,@RequestBody TaskDTO dto,@RequestHeader(value="Authorization",required=false) String auth){return taskService.updateTaskForOwner(id,dto,subject(auth)).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());}
  @DeleteMapping("/{id}") public ResponseEntity<Void> deleteTask(@PathVariable String id,@RequestHeader(value="Authorization",required=false) String auth){return taskService.deleteTaskForOwner(id,subject(auth))?ResponseEntity.noContent().build():ResponseEntity.notFound().build();}
  private String subject(String auth){
    try{if(auth==null||!auth.startsWith("Bearer "))throw new IllegalArgumentException();return Jwts.parser().verifyWith(key).build().parseSignedClaims(auth.substring(7)).getPayload().getSubject();}
    catch(Exception ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"A valid bearer token is required");}
  }
}
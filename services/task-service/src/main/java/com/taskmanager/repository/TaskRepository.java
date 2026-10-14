package com.taskmanager.repository;
import com.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface TaskRepository extends JpaRepository<Task,String> {
  List<Task> findByStatus(Task.Status status);
  List<Task> findByPriority(Task.Priority priority);
  List<Task> findByCategory(Task.Category category);
  List<Task> findByTitleContainingIgnoreCase(String title);
  List<Task> findAllByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
  Optional<Task> findByIdAndOwnerEmail(String id,String ownerEmail);
  boolean existsByIdAndOwnerEmail(String id,String ownerEmail);
  void deleteByIdAndOwnerEmail(String id,String ownerEmail);
}
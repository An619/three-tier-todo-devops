package com.taskmanager.service;
import com.taskmanager.dto.TaskDTO;
import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class TaskService {
  private final TaskRepository taskRepository;
  public TaskService(TaskRepository taskRepository){this.taskRepository=taskRepository;}

  public List<TaskDTO> getAllTasks(){return taskRepository.findAll().stream().map(TaskDTO::new).collect(Collectors.toList());}
  public Optional<TaskDTO> getTaskById(String id){return taskRepository.findById(id).map(TaskDTO::new);}
  public TaskDTO createTask(TaskDTO dto){Task task=convertDTOToEntity(dto);return new TaskDTO(taskRepository.save(task));}
  public Optional<TaskDTO> updateTask(String id,TaskDTO dto){return taskRepository.findById(id).map(existing->{updateTaskFromDTO(existing,dto);return new TaskDTO(taskRepository.save(existing));});}
  public boolean deleteTask(String id){if(taskRepository.existsById(id)){taskRepository.deleteById(id);return true;}return false;}

  public List<TaskDTO> getTasksForOwner(String owner){return taskRepository.findAllByOwnerEmailOrderByCreatedAtDesc(owner).stream().map(TaskDTO::new).collect(Collectors.toList());}
  public Optional<TaskDTO> getTaskForOwner(String id,String owner){return taskRepository.findByIdAndOwnerEmail(id,owner).map(TaskDTO::new);}
  public TaskDTO createTaskForOwner(TaskDTO dto,String owner){Task task=convertDTOToEntity(dto);task.setOwnerEmail(owner);return new TaskDTO(taskRepository.save(task));}
  public Optional<TaskDTO> updateTaskForOwner(String id,TaskDTO dto,String owner){return taskRepository.findByIdAndOwnerEmail(id,owner).map(existing->{updateTaskFromDTO(existing,dto);return new TaskDTO(taskRepository.save(existing));});}
  public boolean deleteTaskForOwner(String id,String owner){if(!taskRepository.existsByIdAndOwnerEmail(id,owner))return false;taskRepository.deleteByIdAndOwnerEmail(id,owner);return true;}

  private Task convertDTOToEntity(TaskDTO dto){
    Task task=new Task();task.setTitle(dto.getTitle());task.setDescription(dto.getDescription());
    if(dto.getPriority()!=null)task.setPriority(Task.Priority.valueOf(dto.getPriority().toUpperCase()));
    if(dto.getStatus()!=null)task.setStatus(Task.Status.valueOf(dto.getStatus().toUpperCase()));
    if(dto.getCategory()!=null)task.setCategory(Task.Category.valueOf(dto.getCategory().toUpperCase()));
    task.setDueDate(dto.getDueDate());return task;
  }
  private void updateTaskFromDTO(Task task,TaskDTO dto){
    if(dto.getTitle()!=null)task.setTitle(dto.getTitle());
    if(dto.getDescription()!=null)task.setDescription(dto.getDescription());
    if(dto.getPriority()!=null)task.setPriority(Task.Priority.valueOf(dto.getPriority().toUpperCase()));
    if(dto.getStatus()!=null)task.setStatus(Task.Status.valueOf(dto.getStatus().toUpperCase()));
    if(dto.getCategory()!=null)task.setCategory(Task.Category.valueOf(dto.getCategory().toUpperCase()));
    task.setDueDate(dto.getDueDate());
  }
}
package com.taskmanager.service;

import com.taskmanager.dto.TaskDTO;
import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getAllTasksMapsEntitiesToDtos() {
        Task task = new Task();
        task.setId("task-1");
        task.setTitle("Write CI pipeline");
        task.setDescription("Add automated checks");
        task.setPriority(Task.Priority.HIGH);
        task.setStatus(Task.Status.INPROGRESS);
        task.setCategory(Task.Category.STUDY);

        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<TaskDTO> result = taskService.getAllTasks();

        assertEquals(1, result.size());
        assertEquals("task-1", result.get(0).getId());
        assertEquals("Write CI pipeline", result.get(0).getTitle());
        assertEquals("high", result.get(0).getPriority());
        assertEquals("inprogress", result.get(0).getStatus());
        assertEquals("study", result.get(0).getCategory());
        verify(taskRepository).findAll();
    }

    @Test
    void createTaskConvertsRequestAndReturnsSavedTask() {
        TaskDTO request = new TaskDTO();
        request.setTitle("Add tests");
        request.setDescription("Test service behavior");
        request.setPriority("high");
        request.setStatus("todo");
        request.setCategory("work");

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId("generated-task-id");
            return saved;
        });

        TaskDTO result = taskService.createTask(request);

        assertEquals("generated-task-id", result.getId());
        assertEquals("Add tests", result.getTitle());
        assertEquals("high", result.getPriority());
        assertEquals("todo", result.getStatus());
        assertEquals("work", result.getCategory());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void updateTaskReturnsEmptyWhenTaskDoesNotExist() {
        when(taskRepository.findById("missing")).thenReturn(Optional.empty());

        assertTrue(taskService.updateTask("missing", new TaskDTO()).isEmpty());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTaskDeletesExistingTask() {
        when(taskRepository.existsById("task-1")).thenReturn(true);

        assertTrue(taskService.deleteTask("task-1"));

        verify(taskRepository).deleteById("task-1");
    }

    @Test
    void deleteTaskReturnsFalseWhenTaskDoesNotExist() {
        when(taskRepository.existsById("missing")).thenReturn(false);

        assertFalse(taskService.deleteTask("missing"));

        verify(taskRepository, never()).deleteById(any());
    }
}

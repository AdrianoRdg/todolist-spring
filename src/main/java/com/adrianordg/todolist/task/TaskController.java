package com.adrianordg.todolist.task;

import com.adrianordg.todolist.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final ITaskRepository taskRepository;

    @Autowired
    public TaskController(ITaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @PostMapping()
    public ResponseEntity<?> create(@RequestBody TaskModel task, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        task.setUserId(userId);

        LocalDateTime currDate = LocalDateTime.now();

        if (currDate.isAfter(task.getStartAt()) || currDate.isAfter(task.getEndAt())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The start date / end date must be greater than the current date");
        }

        if (task.getStartAt().isAfter(task.getEndAt())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The start date must be greater than the end date");
        }

        TaskModel createdTask = this.taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @GetMapping()
    public List<TaskModel> list(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return this.taskRepository.findByUserId(userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@RequestBody TaskModel task, @PathVariable UUID id, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");

        Optional<TaskModel> foundTask = this.taskRepository.findById(id);

        if (foundTask.isPresent()) {
            TaskModel existingTask = foundTask.get();

            if (!existingTask.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User has no permission to update this task");
            }

            Utils.copyNonNullProperties(task, existingTask);
            TaskModel updatedTask = this.taskRepository.save(existingTask);

            return ResponseEntity.status(HttpStatus.OK).body(updatedTask);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task with id " + id + " not found");
        }
    }
}

package com.education.backend;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    
    private final CourseRepository repository;

    public CourseController(CourseRepository repository) {
        this.repository = repository;
    }

    // Получение списка курсов с фильтрацией (п. 1.5)
    @GetMapping
    public List<Course> getAllCourses(@RequestParam(required = false) String search) {
        if (search != null && !search.isEmpty()) {
            return repository.findByTitleContainingIgnoreCase(search);
        }
        return repository.findAll();
    }
    
    // Получение одного курса по ID
    @GetMapping("/{id}")
    public Course getCourseById(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }

    // CRUD операции (требование п. 1.4)
    @PostMapping
    public Course createCourse(@RequestBody Course course) {
        return repository.save(course);
    }

    @DeleteMapping("/{id}")
    public void deleteCourse(@PathVariable Long id) {
        repository.deleteById(id);
    }
}

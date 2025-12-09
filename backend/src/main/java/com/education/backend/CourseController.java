package com.education.backend;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository repository;

    public CourseController(CourseRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Course> getAllCourses(
        @RequestParam(required = false) String search
    ) {
        if (search != null && !search.isEmpty()) {
            return repository.searchCourses(search);
        }
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Course getCourseById(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }

    @PostMapping
    public Course createCourse(@RequestBody Course course) {
        return repository.save(course);
    }

    @DeleteMapping("/{id}")
    public void deleteCourse(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PutMapping("/{id}")
    public Course updateCourse(
        @PathVariable Long id,
        @RequestBody Course course
    ) {
        Course existingCourse = repository.findById(id).orElse(null);
        if (existingCourse == null) {
            return null;
        }
        // Обновляем поля курса
        existingCourse.setTitle(course.getTitle());
        existingCourse.setDescription(course.getDescription());
        existingCourse.setDetailedDescription(course.getDetailedDescription());
        existingCourse.setCategory(course.getCategory());
        existingCourse.setCurriculum(course.getCurriculum());
        // authorId не обновляем, т.к. создатель курса должен остаться прежним
        return repository.save(existingCourse);
    }
}

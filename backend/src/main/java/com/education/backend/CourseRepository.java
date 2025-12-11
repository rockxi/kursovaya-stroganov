package com.education.backend;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitleContainingIgnoreCase(String keyword);

    @Query(
        "SELECT c FROM Course c WHERE " +
            "LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))"
    )
    List<Course> searchCourses(@Param("search") String search);
}

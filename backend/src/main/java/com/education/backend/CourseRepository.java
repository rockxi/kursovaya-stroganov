package com.education.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    // Поиск по ключевым словам (требование п. 1.5)
    List<Course> findByTitleContainingIgnoreCase(String keyword);
    
    // Сортировка реализуется через Sort параметр в контроллере (требование п. 1.6)
}

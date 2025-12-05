package com.education.backend;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "courses")
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String category;
    
    @Column(name = "author_id")
    private Long authorId;
    
    @Column(name = "detailed_description", columnDefinition = "TEXT")
    private String detailedDescription;
    
    @Column(name = "curriculum", columnDefinition = "TEXT")
    private String curriculum;
}

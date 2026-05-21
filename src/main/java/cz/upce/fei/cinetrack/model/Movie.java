package cz.upce.fei.cinetrack.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                  // Автоматично робить getters, setters, toString
@NoArgsConstructor    // Порожній конструктор
@AllArgsConstructor   // Конструктор з усіма полями
public class Movie {
    private Long id;
    private String title;
    private String genre;
    private int year;
    private double rating;
    private String description; // Короткий опис фільму
}
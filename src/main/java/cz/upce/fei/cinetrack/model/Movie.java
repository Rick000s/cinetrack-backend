package cz.upce.fei.cinetrack.model;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Movie {
    private Long id;
    private String title;
    private String posterUrl;
    private String genre;
    private int year;
    private double rating;
    private String description;
    private String trailerUrl;
}
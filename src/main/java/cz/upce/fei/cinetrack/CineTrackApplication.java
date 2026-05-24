package cz.upce.fei.cinetrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class CineTrackApplication {

    public static void main(String[] args) {
        configureDatabaseFromUrl();
        SpringApplication.run(CineTrackApplication.class, args);
    }

    private static void configureDatabaseFromUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()
                || System.getenv("JDBC_DATABASE_URL") != null
                || System.getenv("SPRING_DATASOURCE_URL") != null) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isBlank()) {
            return;
        }

        String[] credentials = userInfo.split(":", 2);
        String username = decode(credentials[0]);
        String password = credentials.length > 1 ? decode(credentials[1]) : "";
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();

        System.setProperty("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath());
        System.setProperty("spring.datasource.username", username);
        System.setProperty("spring.datasource.password", password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

}

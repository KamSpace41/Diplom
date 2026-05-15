package cloudestorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"controller", "service", "repository", "config", "security", "model", "cloudestorage"})
@EnableJpaRepositories(basePackages = "repository")
@EntityScan(basePackages = "model")
public class CloudStorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudStorageApplication.class, args);
    }
}
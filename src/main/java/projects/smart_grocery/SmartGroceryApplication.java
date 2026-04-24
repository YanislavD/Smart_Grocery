package projects.smart_grocery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import projects.smart_grocery.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SmartGroceryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartGroceryApplication.class, args);
    }

}

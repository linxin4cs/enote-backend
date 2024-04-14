package sit.zlx.enotebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class EnoteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnoteBackendApplication.class, args);
    }

}

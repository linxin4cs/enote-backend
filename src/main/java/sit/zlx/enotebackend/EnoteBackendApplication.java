package sit.zlx.enotebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("sit.zlx.enotebackend.mapper")
public class EnoteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnoteBackendApplication.class, args);
    }

}

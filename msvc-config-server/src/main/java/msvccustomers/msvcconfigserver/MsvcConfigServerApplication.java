package msvccustomers.msvcconfigserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class MsvcConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcConfigServerApplication.class, args);
    }

}

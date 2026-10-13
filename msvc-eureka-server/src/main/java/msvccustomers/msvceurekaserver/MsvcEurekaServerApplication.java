package msvccustomers.msvceurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class MsvcEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcEurekaServerApplication.class, args);
    }

}

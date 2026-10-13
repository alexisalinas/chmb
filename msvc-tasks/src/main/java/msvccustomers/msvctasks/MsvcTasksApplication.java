package msvccustomers.msvctasks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient

public class MsvcTasksApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcTasksApplication.class, args);
    }

}

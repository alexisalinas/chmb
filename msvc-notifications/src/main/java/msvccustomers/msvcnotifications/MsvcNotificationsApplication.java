package msvccustomers.msvcnotifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsvcNotificationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcNotificationsApplication.class, args);
    }

}

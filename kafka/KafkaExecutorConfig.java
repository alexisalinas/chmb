package com.banamex.cops.update_offer.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/** Ejecutor con cola acotada: si se llena, se rechaza (y se cuenta) en vez de acumular tareas sin límite. */
@Configuration
public class KafkaExecutorConfig {

    @Bean(name = "fulFillKafkaExecutor")
    public ThreadPoolTaskExecutor fulFillKafkaExecutor(OfferKafkaProps props) {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setThreadNamePrefix("fulfill-kafka-");
        e.setCorePoolSize(props.getExecutorCoreSize());
        e.setMaxPoolSize(props.getExecutorMaxSize());
        e.setQueueCapacity(props.getExecutorQueueCapacity());
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        e.setWaitForTasksToCompleteOnShutdown(true);
        e.setAwaitTerminationSeconds(5);
        return e;
    }
}

package com.plataformas.hilos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "transferExecutor")
    public ThreadPoolTaskExecutor transferExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Pool realista para alta concurrencia con backpressure
        // Evita crear 10k hilos nativos; se encolan tareas excedentes
        executor.setCorePoolSize(1);       // ajusta según # de CPUs
        executor.setMaxPoolSize(10);       // permite bursts controlados
        executor.setQueueCapacity(20000);   // capacidad para oleadas grandes
        executor.setThreadNamePrefix("Transfer-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // En caso de saturación, ejecutar en el hilo del llamador para evitar rechazos (500)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

package com.parking.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsConfig {

    @Bean
    @ConditionalOnProperty(name = "parking.sqs.enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner ensureSqsQueueExists(
            SqsAsyncClient sqsAsyncClient,
            @Value("${parking.sqs.relocation-queue}") String queueName) {
        return args -> sqsAsyncClient.createQueue(r -> r.queueName(queueName)).join();
    }
}

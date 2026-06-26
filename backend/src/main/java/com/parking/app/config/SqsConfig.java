package com.parking.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;

@Configuration
public class SqsConfig {

    /**
     * Creates the DLQ then the main queue with a redrive policy before any
     * SmartLifecycle bean starts (phase Integer.MIN_VALUE). ApplicationRunner
     * would run too late — the SQS listener container starts during context refresh.
     */
    @Bean
    @ConditionalOnProperty(name = "parking.sqs.enabled", havingValue = "true", matchIfMissing = true)
    public SmartLifecycle sqsQueueCreator(
            SqsAsyncClient sqsAsyncClient,
            @Value("${parking.sqs.relocation-queue}") String queueName,
            @Value("${parking.sqs.max-receive-count:3}") int maxReceiveCount) {
        return new SmartLifecycle() {
            private volatile boolean running = false;

            @Override
            public void start() {
                String dlqName = queueName + "-dlq";

                String dlqUrl = sqsAsyncClient
                        .createQueue(r -> r.queueName(dlqName))
                        .join().queueUrl();

                String dlqArn = sqsAsyncClient
                        .getQueueAttributes(r -> r
                                .queueUrl(dlqUrl)
                                .attributeNames(QueueAttributeName.QUEUE_ARN))
                        .join()
                        .attributes()
                        .get(QueueAttributeName.QUEUE_ARN);

                sqsAsyncClient.createQueue(r -> r
                        .queueName(queueName)
                        .attributes(Map.of(
                                QueueAttributeName.REDRIVE_POLICY,
                                "{\"deadLetterTargetArn\":\"" + dlqArn
                                + "\",\"maxReceiveCount\":\"" + maxReceiveCount + "\"}"
                        ))).join();

                running = true;
            }

            @Override public void stop() { running = false; }
            @Override public boolean isRunning() { return running; }
            @Override public int getPhase() { return Integer.MIN_VALUE; }
        };
    }
}

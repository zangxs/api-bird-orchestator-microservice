package com.brayanpv.app.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.port}")
    private int port;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue}")
    private String queue;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    @Value("${rabbitmq.result-queue}")
    private String resultQueue;

    @Value("${rabbitmq.result-routing-key}")
    private String resultRoutingKey;

    @Value("${rabbitmq.classification-queue}")
    private String classificationQueue;

    @Value("${rabbitmq.classification-routing-key}")
    private String classificationRoutingKey;

    @Value("${rabbitmq.classification-result-queue}")
    private String classificationResultQueue;

    @Value("${rabbitmq.result-classification-routing-key}")
    private String resultClassificationRoutingKey;


    @Bean
    public com.rabbitmq.client.ConnectionFactory nativeConnectionFactory() {
        com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.useNio();
        return factory;
    }

    @Bean(destroyMethod = "close")
    public Sender sender(com.rabbitmq.client.ConnectionFactory connectionFactory) {
        return RabbitFlux.createSender(
                new SenderOptions().connectionFactory(connectionFactory)
        );
    }

    @Bean(destroyMethod = "close")
    public Receiver receiver(com.rabbitmq.client.ConnectionFactory connectionFactory) {
        return RabbitFlux.createReceiver(
                new ReceiverOptions().connectionFactory(connectionFactory)
        );
    }

    @Bean
    public Mono<Void> topology(Sender sender) {
        return sender.declareExchange(ExchangeSpecification.exchange(exchange)
                        .type("direct")
                        .durable(true))
                .then(sender.declareQueue(QueueSpecification.queue(queue).durable(true)))
                .then(sender.declareQueue(QueueSpecification.queue(resultQueue).durable(true)))
                .then(sender.declareQueue(QueueSpecification.queue(classificationQueue).durable(true)))
                .then(sender.declareQueue(QueueSpecification.queue(classificationResultQueue).durable(true)))
                .then(sender.bind(BindingSpecification.binding(exchange, routingKey, queue)))
                .then(sender.bind(BindingSpecification.binding(exchange, resultRoutingKey, resultQueue)))
                .then(sender.bind(BindingSpecification.binding(exchange, classificationRoutingKey, classificationQueue)))
                .then(sender.bind(BindingSpecification.binding(exchange, resultClassificationRoutingKey, classificationResultQueue)))
                .then()
                .cache(); // se ejecuta una sola vez, resultado cacheado
    }

}

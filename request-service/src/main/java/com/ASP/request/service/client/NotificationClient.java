package com.ASP.request.service.client;


import com.ASP.request.service.DTO.EmailNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NotificationClient {
    @Autowired
    private WebClient.Builder webClientBuilder;

    private Logger logger = LoggerFactory.getLogger(NotificationClient.class);

    public Mono<String> getEmailByName(String name) {
        String url = "http://localhost:3001/auth/email/" + name;
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> logger.error("Email fetch Failed!", e))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Boolean> sendEmail(String to, String subject, String body) {
        String url = "http://localhost:3010/notify/send";
        EmailNotification emailNotification = new EmailNotification(to, subject, body);
        return webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(emailNotification)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorResume(e -> {
                    logger.error("Email send Failed!", e);
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> sendEmailByName(String name, String subject, String body) {
        return getEmailByName(name)
                .flatMap(email -> {
                    if (email == null || email.isEmpty()) {
                        logger.error("Email not found for name: {}", name);
                        return Mono.just(false);
                    }
                    return sendEmail(email, subject, body);
                })
                .onErrorResume(e -> {
                    logger.error("Failed to send email by name!", e);
                    return Mono.just(false);
                });
    }
}

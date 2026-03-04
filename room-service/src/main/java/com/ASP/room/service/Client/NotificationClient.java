package com.ASP.room.service.Client;

import com.ASP.room.service.DTO.EmailNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NotificationClient {
    @Autowired
    private WebClient webClient;

    private Logger logger = LoggerFactory.getLogger(NotificationClient.class);

    public Mono<String> getEmailByName(String name) {
        String url = "/auth/email/" + name;
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> logger.error("Email fetch Failed!", e));
    }

    public Mono<String> sendEmail(String to, String subject, String body) {
        String url = "/notidf/email";
        EmailNotification emailNotification = new EmailNotification(to, subject, body);
        return webClient.post()
                .uri(url)
                .bodyValue(emailNotification)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> logger.error("Email send Failed!", e));
    }

    public Mono<String> sendNotificationByName(String name, String subject, String body) {
        return getEmailByName(name)
                .flatMap(email -> sendEmail(email, subject, body))
                .doOnError(e -> logger.error("Notification send failed!", e));
    }
}

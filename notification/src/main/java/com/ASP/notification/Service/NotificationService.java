package com.ASP.notification.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${app.mail.from}")
    private String from;

    public Mono<String> sendMail(String to, String subject, String content) {
        String url = "http://localhost:3001/auth/byEmail?email=" + to;
        WebClient webClient = webClientBuilder.build();
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Boolean.class)
                .flatMap(emailExists -> {
                    if (!Boolean.TRUE.equals(emailExists)) {
                        return Mono.just("the email does not exist");
                    }
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(from);
                    message.setTo(to);
                    message.setSubject(subject);
                    message.setText(content);
                    mailSender.send(message); // This is still synchronous, but safe if mailSender is thread-safe
                    return Mono.just("success");
                })
                .doOnError(error -> {
                    // Log or handle error
                });
    }
}

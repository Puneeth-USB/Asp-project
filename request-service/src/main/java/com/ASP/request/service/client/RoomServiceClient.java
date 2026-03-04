package com.ASP.request.service.client;


import com.ASP.request.service.DTO.MaintenanceBlockRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RoomServiceClient {
    private final WebClient webClient;
    private final String baseUrl;

    public RoomServiceClient(WebClient.Builder webClientBuilder,
                             @Value("${room.service.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<Void> blockRoomOrFacility(Long roomId, MaintenanceBlockRequestDTO dto) {
        String url = "/rooms/" + roomId + "/maintenance/block";
        return webClient.post()
                .uri(url)
                .bodyValue(dto)
                .retrieve()
                .onStatus(status -> status.isError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("Room service error: " + body))))
                .toBodilessEntity()
                .then();
    }

    public Mono<Void> unblockRoomOrFacility(Long roomId, MaintenanceBlockRequestDTO dto) {
        String url = "/rooms/" + roomId + "/maintenance/unblock";
        return webClient.post()
                .uri(url)
                .bodyValue(dto)
                .retrieve()
                .onStatus(status -> status.isError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("Room service error: " + body))))
                .toBodilessEntity()
                .then();
    }
}

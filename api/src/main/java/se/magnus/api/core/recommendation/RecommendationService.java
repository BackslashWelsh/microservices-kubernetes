package se.magnus.api.core.recommendation;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationService {

    @GetMapping(
            value = "/recommendation",
            produces = "application/json")
    Flux<Recommendation> getRecommendations(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "productId") int productId);

    Mono<Recommendation> createRecommendation(@RequestBody Recommendation body);

    Mono<Void> deleteRecommendations(@RequestParam(value = "productId") int productId);
}

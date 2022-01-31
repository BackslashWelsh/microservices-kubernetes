package se.magnus.api.core.review;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewService {

    @GetMapping(
            value = "/review",
            produces = "application/json")
    Flux<Review> getReviews(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "productId") int productId);

    Mono<Review> createReview(@RequestBody Review body);

    Mono<Void> deleteReviews(@RequestParam(value = "productId") int productId);
}

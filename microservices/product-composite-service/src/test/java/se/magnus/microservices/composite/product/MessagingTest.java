package se.magnus.microservices.composite.product;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;
import static se.magnus.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;

@Slf4j
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        classes = {TestSecurityConfig.class},
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                "spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class})
public class MessagingTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private OutputDestination target;

    @BeforeEach
    void setUp() {
        purgeMessages("products");
        purgeMessages("recommendations");
        purgeMessages("reviews");
    }

    @Test
    void createCompositeProductWithoutRecommendationAndReview() {
        ProductAggregate composite = new ProductAggregate(1, "name",
                1, null, null, null);

        postAndVerifyProduct(composite, ACCEPTED);

        final List<String> productsMessages = getMessages("products");
        final List<String> recommendationsMessages = getMessages("recommendations");
        final List<String> reviewsMessages = getMessages("reviews");

        // Assert one expected new product event queued up
        assertEquals(1, productsMessages.size());

        Event<Integer, Product> expectedProductEvent =
                new Event<>(CREATE, composite.getProductId(),
                        new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
        assertThat(productsMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

        assertEquals(0, recommendationsMessages.size());
        assertEquals(0, reviewsMessages.size());
    }

    @Test
    void createCompositeProduct() {
        ProductAggregate composite = new ProductAggregate(1, "name", 1,
                Collections.singletonList(new RecommendationSummary(1, "a", 1, "c")),
                Collections.singletonList(new ReviewSummary(1, "a", "s", "c")), null);
        postAndVerifyProduct(composite, ACCEPTED);

        final List<String> productsMessages = getMessages("products");
        final List<String> recommendationsMessages = getMessages("recommendations");
        final List<String> reviewsMessages = getMessages("reviews");

        assertEquals(1, productsMessages.size());

        Event<Integer, Product> expectedProductEvent =
                new Event<>(CREATE, composite.getProductId(),
                        new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
        assertThat(productsMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

        assertEquals(1, recommendationsMessages.size());

        RecommendationSummary rec = composite.getRecommendations().get(0);
        Event<Integer, Recommendation> expectedRecommendationEvent =
                new Event<>(CREATE, composite.getProductId(),
                        new Recommendation(composite.getProductId(), rec.getRecommendationId(),
                                rec.getAuthor(), rec.getRate(), rec.getContent(), null));
        assertThat(recommendationsMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

        assertEquals(1, reviewsMessages.size());

        ReviewSummary rev = composite.getReviews().get(0);
        Event<Integer, Review> expectedReviewEvent =
                new Event<>(CREATE, composite.getProductId(),
                        new Review(composite.getProductId(), rev.getReviewId(),
                                rev.getAuthor(), rev.getSubject(), rev.getContent(), null));
        assertThat(reviewsMessages.get(0), is(sameEventExceptCreatedAt(expectedReviewEvent)));
    }

    @Test
    void deleteProduct() {
        deleteAndVerifyProduct(1, ACCEPTED);

        final List<String> productsMessages = getMessages("products");
        final List<String> recommendationsMessages = getMessages("recommendations");
        final List<String> reviewsMessages = getMessages("reviews");

        // Assert one delete product event queued up
        assertEquals(1, productsMessages.size());

        Event<Integer, Product> expectedProductEvent = new Event<>(DELETE, 1, null);
        assertThat(productsMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

        assertEquals(1, recommendationsMessages.size());

        Event<Integer, Product> expectedRecommendationEvent = new Event<>(DELETE, 1, null);
        assertThat(recommendationsMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

        assertEquals(1, reviewsMessages.size());

        Event<Integer, Product> expectedReviewEvent = new Event<>(DELETE, 1, null);
        assertThat(reviewsMessages.get(0), is(sameEventExceptCreatedAt(expectedReviewEvent)));
    }


    private void purgeMessages(String bindingName) {
        getMessages(bindingName);
    }

    private List<String> getMessages(String bindingName) {
        List<String> messages = new ArrayList<>();

        boolean anyMoreMessages = true;

        while (anyMoreMessages) {
            Message<byte[]> message = getMessage(bindingName);

            if (message == null) {
                anyMoreMessages = false;

            } else {
                messages.add(new String(message.getPayload()));
            }
        }
        return messages;
    }

    private Message<byte[]> getMessage(String bindingName) {
        try {
            return target.receive(0, bindingName);
        } catch (NullPointerException npe) {
            // If the messageQueues member variable in the target object contains no queues when the receive method is called, it will cause a NPE to be thrown.
            // So we catch the NPE here and return null to indicate that no messages were found.
            log.error("getMessage() receive a NPE with binding = {}", bindingName);
            return null;
        }
    }


    private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) {
        client.post()
                .uri("/product-composite")
                .accept(APPLICATION_JSON)
                .body(just(compositeProduct), ProductAggregate.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        client.delete()
                .uri("/product-composite/" + productId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }


}

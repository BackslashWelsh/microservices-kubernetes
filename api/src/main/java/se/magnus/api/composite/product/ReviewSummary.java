package se.magnus.api.composite.product;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class ReviewSummary {

    int reviewId;
    String author;
    String subject;
    String content;
}

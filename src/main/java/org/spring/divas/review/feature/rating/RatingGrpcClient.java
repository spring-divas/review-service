package org.spring.divas.review.feature.rating;

import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import org.spring.divas.rating.v1.DishRatingUpdateRequest;
import org.spring.divas.rating.v1.RatingUpdateServiceGrpc;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingGrpcClient {

    private final RatingUpdateServiceGrpc.RatingUpdateServiceBlockingStub ratingStub;

    public void updateDishRating(Long dishId, float rating, int reviewCount) {
        Empty response = ratingStub.updateDishRating(
                DishRatingUpdateRequest.newBuilder()
                        .setId(dishId)
                        .setRating(rating)
                        .setReviewCount(reviewCount)
                        .build()
        );
    }
}
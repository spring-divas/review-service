package org.spring.divas.review.feature.rating;

import org.spring.divas.rating.v1.RatingUpdateServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class RatingGrpcClientConfig {

    @Bean
    public RatingUpdateServiceGrpc.RatingUpdateServiceBlockingStub ratingUpdateServiceBlockingStub(GrpcChannelFactory channels) {
        return RatingUpdateServiceGrpc.newBlockingStub(
                channels.createChannel("rating")
        );
    }
}
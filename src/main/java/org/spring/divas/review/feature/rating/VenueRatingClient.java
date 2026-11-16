package org.spring.divas.review.feature.rating;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.spring.divas.rating.v1.RatingSyncResponse;
import org.spring.divas.rating.v1.RatingSyncServiceGrpc;
import org.spring.divas.rating.v1.RatingUpdate;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Component
public class VenueRatingClient {

  private final RatingSyncServiceGrpc.RatingSyncServiceStub stub;

  public VenueRatingClient(GrpcChannelFactory channelFactory) {
    this.stub = RatingSyncServiceGrpc.newStub(channelFactory.createChannel("venue"));
  }

  public CompletableFuture<Integer> syncRatings(List<RatingUpdate> updates) {
    CompletableFuture<Integer> updatedCount = new CompletableFuture<>();

    StreamObserver<RatingUpdate> stream = stub.syncRatings(new StreamObserver<>() {
      @Override
      public void onNext(RatingSyncResponse response) {
        updatedCount.complete(response.getUpdatedCount());
      }

      @Override
      public void onError(Throwable throwable) {
        updatedCount.completeExceptionally(throwable);
      }

      @Override
      public void onCompleted() {
        updatedCount.completeExceptionally(
            new IllegalStateException("venue-service closed the stream without a response"));
      }
    });

    updates.forEach(stream::onNext);
    stream.onCompleted();

    return updatedCount;
  }
}

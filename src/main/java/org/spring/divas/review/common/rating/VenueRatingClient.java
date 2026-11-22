package org.spring.divas.review.common.rating;

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

  public CompletableFuture<Integer> syncDishRatings(List<RatingUpdate> updates) {
    CompletableFuture<Integer> updatedCount = new CompletableFuture<>();

    StreamObserver<RatingUpdate> stream = stub.syncDishRatings(new StreamObserver<>() {
      private RatingSyncResponse response;

      @Override
      public void onNext(RatingSyncResponse response) {
        this.response = response;
      }

      @Override
      public void onError(Throwable throwable) {
        updatedCount.completeExceptionally(throwable);
      }

      @Override
      public void onCompleted() {
        if (response == null) {
          updatedCount.completeExceptionally(
              new IllegalStateException("venue-service closed the stream without a response"));
          return;
        }
        updatedCount.complete(response.getUpdatedCount());
      }
    });

    updates.forEach(stream::onNext);
    stream.onCompleted();

    return updatedCount;
  }

  public CompletableFuture<Integer> syncVenueRatings(List<RatingUpdate> updates) {
    List<CompletableFuture<Integer>> futures = updates.stream()
        .map(update -> {
          CompletableFuture<Integer> future = new CompletableFuture<>();

          stub.updateVenueRating(update, new StreamObserver<>() {
            @Override
            public void onNext(RatingSyncResponse response) {
              future.complete(response.getUpdatedCount());
            }

            @Override
            public void onError(Throwable throwable) {
              future.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
              if (!future.isDone()) {
                future.completeExceptionally(
                  new IllegalStateException("venue-service completed without a response"));
              }
            }
          });

          return future;
        })
        .toList();

    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
            .thenApply(_ -> futures.stream()
                    .mapToInt(CompletableFuture::join)
                    .sum());
  }
}

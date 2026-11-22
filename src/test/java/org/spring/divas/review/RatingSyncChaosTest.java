package org.spring.divas.review;

import static org.assertj.core.api.Assertions.assertThat;

import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultException;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeySettings;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.rating.v1.RatingSyncResponse;
import org.spring.divas.rating.v1.RatingSyncServiceGrpc;
import org.spring.divas.rating.v1.RatingUpdate;
import org.spring.divas.review.feature.dish.DishReview;
import org.spring.divas.review.feature.dish.DishReviewRepository;
import org.spring.divas.review.feature.rating.RatingChangedEvent;
import org.spring.divas.review.feature.rating.RatingSyncJob;
import org.spring.divas.review.feature.rating.ReviewTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
    "chaos.monkey.watcher.service=false",
    "chaos.monkey.watcher.rest-controller=false",
    "chaos.monkey.watcher.beans=venueRatingClient",
    "rating.sync.interval=1h"
})
@ActiveProfiles("chaos-monkey")
class RatingSyncChaosTest {

  private static final long DISH_ID = 7L;

  private static final List<RatingUpdate> SYNCED = new CopyOnWriteArrayList<>();

  private static final Server VENUE_SERVICE = startVenueService();

  @Autowired
  private RatingSyncJob ratingSyncJob;

  @Autowired
  private DishReviewRepository dishReviewRepository;

  @Autowired
  private ChaosMonkeySettings chaosMonkey;

  @DynamicPropertySource
  static void venueTarget(DynamicPropertyRegistry registry) {
    registry.add("spring.grpc.client.channel.venue.target",
        () -> "static://localhost:" + VENUE_SERVICE.getPort());
  }

  @AfterAll
  static void stopVenueService() {
    VENUE_SERVICE.shutdownNow();
  }

  @BeforeEach
  void setUp() {
    SYNCED.clear();
    dishReviewRepository.deleteAll();

    AssaultProperties assaults = chaosMonkey.getAssaultProperties();
    assaults.setLevel(1);
    assaults.setDeterministic(true);
    assaults.setLatencyActive(false);
    assaults.setExceptionsActive(true);
    assaults.setException(new AssaultException());
  }

  @AfterEach
  void stopChaos() {
    chaosMonkey.getChaosMonkeyProperties().setEnabled(false);
  }

  @Test
  void ratingChangesSurviveVenueOutageAndAreSyncedAfterRecovery() {
    dishReviewRepository.save(review(1L, 4));
    dishReviewRepository.save(review(2L, 2));
    ratingSyncJob.onRatingChanged(new RatingChangedEvent(ReviewTarget.DISH, DISH_ID));

    chaosMonkey.getChaosMonkeyProperties().setEnabled(true);
    ratingSyncJob.syncChangedRatings();
    ratingSyncJob.syncChangedRatings();

    assertThat(SYNCED).isEmpty();

    chaosMonkey.getChaosMonkeyProperties().setEnabled(false);
    ratingSyncJob.syncChangedRatings();

    assertThat(SYNCED).singleElement().satisfies(update -> {
      assertThat(update.getTargetId()).isEqualTo(DISH_ID);
      assertThat(update.getAverageRating()).isEqualTo(3.0);
      assertThat(update.getReviewCount()).isEqualTo(2);
    });

    ratingSyncJob.syncChangedRatings();

    assertThat(SYNCED).hasSize(1);
  }

  private static DishReview review(long userId, int rating) {
    return DishReview.builder()
        .userId(userId)
        .dishId(DISH_ID)
        .rating((short) rating)
        .build();
  }

  private static Server startVenueService() {
    try {
      return ServerBuilder.forPort(0)
          .addService(new RecordingVenueService())
          .build()
          .start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final class RecordingVenueService extends RatingSyncServiceGrpc.RatingSyncServiceImplBase {

    @Override
    public StreamObserver<RatingUpdate> syncDishRatings(StreamObserver<RatingSyncResponse> responseObserver) {
      return new StreamObserver<>() {
        private int count;

        @Override
        public void onNext(RatingUpdate update) {
          SYNCED.add(update);
          count++;
        }

        @Override
        public void onError(Throwable throwable) {
          responseObserver.onError(throwable);
        }

        @Override
        public void onCompleted() {
          responseObserver.onNext(RatingSyncResponse.newBuilder().setUpdatedCount(count).build());
          responseObserver.onCompleted();
        }
      };
    }

    @Override
    public void updateVenueRating(RatingUpdate update, StreamObserver<RatingSyncResponse> responseObserver) {
      SYNCED.add(update);
      responseObserver.onNext(RatingSyncResponse.newBuilder().setUpdatedCount(1).build());
      responseObserver.onCompleted();
    }
  }
}

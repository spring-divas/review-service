package org.spring.divas.review.common.rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.rating.v1.RatingUpdate;
import org.spring.divas.review.feature.dish.DishReviewRepository;
import org.spring.divas.review.feature.venue.VenueReviewRepository;

@ExtendWith(MockitoExtension.class)
class RatingSyncJobTest {

  @Mock private DishReviewRepository dishReviewRepository;

  @Mock private VenueReviewRepository venueReviewRepository;

  @Mock private VenueRatingClient venueClient;

  @Captor private ArgumentCaptor<List<RatingUpdate>> dishUpdates;

  @Captor private ArgumentCaptor<List<RatingUpdate>> venueUpdates;

  private RatingSyncJob job;

  @BeforeEach
  void setUp() {
    job = new RatingSyncJob(dishReviewRepository, venueReviewRepository, venueClient);
  }

  @Test
  void skipsSyncWhenNothingChanged() {
    job.syncChangedRatings();

    verifyNoInteractions(venueClient, dishReviewRepository, venueReviewRepository);
  }

  @Test
  void sendsCurrentStatsForChangedDishesAndVenues() {
    when(dishReviewRepository.getRatingStats(10L)).thenReturn(new RatingStats(10L, 4.5, 2L));
    when(venueReviewRepository.getRatingStats(20L)).thenReturn(new RatingStats(20L, 3.0, 1L));
    when(venueClient.syncDishRatings(anyList())).thenReturn(CompletableFuture.completedFuture(1));
    when(venueClient.syncVenueRatings(anyList())).thenReturn(CompletableFuture.completedFuture(1));

    job.onRatingChanged(new RatingChangedEvent(ReviewTarget.DISH, 10L));
    job.onRatingChanged(new RatingChangedEvent(ReviewTarget.VENUE, 20L));
    job.syncChangedRatings();

    verify(venueClient).syncDishRatings(dishUpdates.capture());
    verify(venueClient).syncVenueRatings(venueUpdates.capture());
    assertThat(dishUpdates.getValue())
        .containsExactly(
            RatingUpdate.newBuilder()
                .setTargetId(10L)
                .setAverageRating(4.5)
                .setReviewCount(2)
                .build());
    assertThat(venueUpdates.getValue())
        .containsExactly(
            RatingUpdate.newBuilder()
                .setTargetId(20L)
                .setAverageRating(3.0)
                .setReviewCount(1)
                .build());
  }

  @Test
  void sendsRepeatedChangesOfSameTargetOnce() {
    when(dishReviewRepository.getRatingStats(10L)).thenReturn(new RatingStats(10L, 4.0, 3L));
    when(venueClient.syncDishRatings(anyList())).thenReturn(CompletableFuture.completedFuture(1));
    when(venueClient.syncVenueRatings(anyList())).thenReturn(CompletableFuture.completedFuture(0));

    job.onRatingChanged(new RatingChangedEvent(ReviewTarget.DISH, 10L));
    job.onRatingChanged(new RatingChangedEvent(ReviewTarget.DISH, 10L));
    job.syncChangedRatings();

    verify(venueClient).syncDishRatings(dishUpdates.capture());
    assertThat(dishUpdates.getValue()).hasSize(1);
  }

  @Test
  void doesNotResendAfterSuccessfulSync() {
    when(dishReviewRepository.getRatingStats(10L)).thenReturn(new RatingStats(10L, 4.0, 3L));
    when(venueClient.syncDishRatings(anyList())).thenReturn(CompletableFuture.completedFuture(1));
    when(venueClient.syncVenueRatings(anyList())).thenReturn(CompletableFuture.completedFuture(0));

    job.onRatingChanged(new RatingChangedEvent(ReviewTarget.DISH, 10L));
    job.syncChangedRatings();
    job.syncChangedRatings();

    verify(venueClient, times(1)).syncDishRatings(any());
  }

  @Test
  void retriesFailedChangesOnNextRun() {
    when(dishReviewRepository.getRatingStats(10L)).thenReturn(new RatingStats(10L, 4.0, 3L));
    when(venueClient.syncDishRatings(anyList()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("unavailable")))
        .thenReturn(CompletableFuture.completedFuture(1));
    when(venueClient.syncVenueRatings(anyList())).thenReturn(CompletableFuture.completedFuture(0));

    job.onRatingChanged(new RatingChangedEvent(ReviewTarget.DISH, 10L));
    job.syncChangedRatings();
    job.syncChangedRatings();

    verify(venueClient, times(2)).syncDishRatings(dishUpdates.capture());
    assertThat(dishUpdates.getAllValues().get(1)).hasSize(1);
  }
}

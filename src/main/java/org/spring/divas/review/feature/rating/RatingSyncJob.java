package org.spring.divas.review.feature.rating;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.divas.rating.v1.RatingUpdate;
import org.spring.divas.review.feature.dish.DishReviewRepository;
import org.spring.divas.review.feature.venue.VenueReviewRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatingSyncJob {

  private final Set<RatingChangedEvent> changes = ConcurrentHashMap.newKeySet();

  private final DishReviewRepository dishReviewRepository;

  private final VenueReviewRepository venueReviewRepository;

  private final VenueRatingClient venueClient;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRatingChanged(RatingChangedEvent event) {
    changes.add(event);
  }

  @Scheduled(fixedDelayString = "${rating.sync.interval:30s}")
  public void syncChangedRatings() {
    if (changes.isEmpty()) {
      return;
    }
    List<RatingChangedEvent> pending = List.copyOf(changes);
    pending.forEach(changes::remove);

    try {
      List<RatingUpdate> dishUpdates = pending.stream()
              .filter(u -> u.target() == ReviewTarget.DISH)
              .map(this::toUpdate)
              .toList();

      List<RatingUpdate> venueUpdates = pending.stream()
              .filter(u -> u.target() == ReviewTarget.VENUE)
              .map(this::toUpdate)
              .toList();

      int updatedDishesCount = venueClient.syncDishRatings(dishUpdates).join();
      int updatedVenuesCount = venueClient.syncVenueRatings(venueUpdates).join();
      int updatedCount = updatedDishesCount + updatedVenuesCount;
      log.info("Synced {} ratings to venue-service, {} updated", pending.size(), updatedCount);
    } catch (RuntimeException ex) {
      log.warn("Rating sync failed, {} ratings will be retried next run", pending.size(), ex);
      changes.addAll(pending);
    }
  }

  private RatingUpdate toUpdate(RatingChangedEvent change) {
    RatingStats stats = switch (change.target()) {
      case DISH -> dishReviewRepository.getRatingStats(change.targetId());
      case VENUE -> venueReviewRepository.getRatingStats(change.targetId());
    };
    return RatingUpdate.newBuilder()
        .setTargetId(stats.targetId())
        .setAverageRating(stats.averageRating())
        .setReviewCount(stats.reviewCount().intValue())
        .build();
  }
}

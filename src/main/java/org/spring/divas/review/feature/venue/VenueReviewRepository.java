package org.spring.divas.review.feature.venue;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueReviewRepository extends JpaRepository<VenueReview, Long> {

  List<VenueReview> findAllByVenueId(Long venueId);

  boolean existsByUserIdAndVenueId(Long userId, Long venueId);
}

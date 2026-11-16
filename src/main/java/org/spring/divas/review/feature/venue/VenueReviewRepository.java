package org.spring.divas.review.feature.venue;

import java.util.List;
import org.spring.divas.review.feature.rating.RatingStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueReviewRepository extends JpaRepository<VenueReview, Long> {

  List<VenueReview> findAllByVenueId(Long venueId);

  boolean existsByUserIdAndVenueId(Long userId, Long venueId);

  @Query(
      """
      select :venueId, coalesce(avg(r.rating), 0.0), count(r)
      from VenueReview r
      where r.venueId = :venueId
      """
  )
  RatingStats getRatingStats(@Param("venueId") Long venueId);
}

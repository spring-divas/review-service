package org.spring.divas.review.feature.venue;

import java.util.List;
import org.spring.divas.review.feature.venue.dto.VenueReviewCreateParams;
import org.spring.divas.review.feature.venue.dto.VenueReviewDeleteParams;
import org.spring.divas.review.feature.venue.dto.VenueReviewDto;
import org.spring.divas.review.feature.venue.dto.VenueReviewUpdateParams;

public interface VenueReviewService {

  VenueReviewDto create(VenueReviewCreateParams params);

  VenueReviewDto getById(Long id);

  List<VenueReviewDto> getAll();

  VenueReviewDto update(VenueReviewUpdateParams params);

  void delete(VenueReviewDeleteParams params);
}

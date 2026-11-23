package org.spring.divas.review.feature.venue;

import java.util.List;

public interface VenueReviewService {

  VenueReviewResponseDto create(VenueReviewRequestDto dto, Long userId);

  VenueReviewResponseDto getById(Long id);

  List<VenueReviewResponseDto> getAll();

  VenueReviewResponseDto update(Long id, VenueReviewRequestDto dto, Long userId);

  void delete(Long id, Long userId);
}

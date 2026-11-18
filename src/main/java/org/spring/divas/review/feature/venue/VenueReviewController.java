package org.spring.divas.review.feature.venue;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.spring.divas.review.feature.venue.dto.VenueReviewCreateParams;
import org.spring.divas.review.feature.venue.dto.VenueReviewCreateRequest;
import org.spring.divas.review.feature.venue.dto.VenueReviewDeleteParams;
import org.spring.divas.review.feature.venue.dto.VenueReviewDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/review/venue")
@AllArgsConstructor
public class VenueReviewController {

  private static final Long MOCK_USER_ID = 1L; // TODO: remove after auth implementation

  private final VenueReviewService venueReviewService;

  @GetMapping
  public List<VenueReviewDto> getAll() {
    return venueReviewService.getAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public VenueReviewDto create(@RequestBody @Valid VenueReviewCreateRequest request) {
    VenueReviewCreateParams params = VenueReviewCreateParams.of(request, request.userId());
    return venueReviewService.create(params);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    VenueReviewDeleteParams params = new VenueReviewDeleteParams(MOCK_USER_ID, id);
    venueReviewService.delete(params);
  }
}

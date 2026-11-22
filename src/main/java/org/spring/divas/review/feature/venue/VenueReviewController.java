package org.spring.divas.review.feature.venue;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
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
  public List<VenueReviewResponseDto> getAll() {
    return venueReviewService.getAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public VenueReviewResponseDto create(@RequestBody @Valid VenueReviewRequestDto dto) {
    return venueReviewService.create(dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    venueReviewService.delete(id, MOCK_USER_ID);
  }
}

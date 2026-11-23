package org.spring.divas.review.feature.venue;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.spring.divas.review.common.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

  private final VenueReviewService venueReviewService;

  @GetMapping
  public List<VenueReviewResponseDto> getAll() {
    return venueReviewService.getAll();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public VenueReviewResponseDto create(@RequestBody @Valid VenueReviewRequestDto dto,
      @AuthenticationPrincipal Jwt jwt) {
    return venueReviewService.create(dto, CurrentUser.id(jwt));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    venueReviewService.delete(id, CurrentUser.id(jwt));
  }
}

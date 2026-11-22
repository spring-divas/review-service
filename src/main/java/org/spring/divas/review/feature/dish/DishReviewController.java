package org.spring.divas.review.feature.dish;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
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
@RequestMapping("/review/dish")
@AllArgsConstructor
public class DishReviewController {

  private static final Long MOCK_USER_ID = 1L;

  private final DishReviewService dishReviewService;

  @GetMapping
  public List<DishReviewResponseDto> getAll() {
    return dishReviewService.getAll();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public DishReviewResponseDto create(@RequestBody @Valid DishReviewRequestDto dto,
      @AuthenticationPrincipal Jwt jwt) {
    if (jwt != null) {
      dto = new DishReviewRequestDto(dto.content(), dto.rating(), dto.dishId(),
          Long.valueOf(jwt.getSubject()));
    }
    return dishReviewService.create(dto);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    Long userId = jwt != null ? Long.valueOf(jwt.getSubject()) : MOCK_USER_ID;
    dishReviewService.delete(id, userId);
  }
}

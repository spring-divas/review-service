package org.spring.divas.review.feature.dish;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.spring.divas.review.feature.dish.dto.DishReviewCreateParams;
import org.spring.divas.review.feature.dish.dto.DishReviewCreateRequest;
import org.spring.divas.review.feature.dish.dto.DishReviewDeleteParams;
import org.spring.divas.review.feature.dish.dto.DishReviewDto;
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
@RequestMapping("/review/dish")
@AllArgsConstructor
public class DishReviewController {

  private static final Long MOCK_USER_ID = 1L; // // todo: remove after auth implementation

  private final DishReviewService dishReviewService;

  @GetMapping
  public List<DishReviewDto> getAll() {
    return dishReviewService.getAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DishReviewDto create(@RequestBody @Valid DishReviewCreateRequest request) {
    DishReviewCreateParams params = DishReviewCreateParams.of(
        request, MOCK_USER_ID
    );
    return dishReviewService.create(params);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    DishReviewDeleteParams params = new DishReviewDeleteParams(id, MOCK_USER_ID);
    dishReviewService.delete(params);
  }
}

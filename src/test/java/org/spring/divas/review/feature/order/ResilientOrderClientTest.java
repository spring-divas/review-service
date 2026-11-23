package org.spring.divas.review.feature.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResilientOrderClientTest {

  @Mock private OrderClient orderClient;

  @InjectMocks private ResilientOrderClient resilientOrderClient;

  @Test
  void checksDishOrderViaOrderClient() {
    when(orderClient.hasUserOrderedDish(10L)).thenReturn(true);

    assertThat(resilientOrderClient.hasUserOrderedDish(1L, 10L)).isTrue();
  }

  @Test
  void checksVenueVisitViaOrderClient() {
    when(orderClient.hasUserBeenToVenue(20L)).thenReturn(false);

    assertThat(resilientOrderClient.hasUserBeenToVenue(1L, 20L)).isFalse();
  }
}

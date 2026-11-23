package org.spring.divas.review;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestJwt {

  public static final String TOKEN_VALUE = "test-token";

  private TestJwt() {
  }

  public static RequestPostProcessor customer(long userId) {
    return jwt()
        .jwt(token -> token.tokenValue(TOKEN_VALUE)
            .subject(String.valueOf(userId))
            .claim("roles", List.of("CUSTOMER")))
        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }
}

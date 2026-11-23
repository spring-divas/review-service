package org.spring.divas.review.common.auth;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resolves the id of the authenticated user from the JWT issued by user-service.
 */
public final class CurrentUser {

  private CurrentUser() {
  }

  public static Long id(Jwt jwt) {
    if (jwt == null || jwt.getSubject() == null) {
      throw new AuthenticationCredentialsNotFoundException("Authentication is required");
    }
    return Long.valueOf(jwt.getSubject());
  }
}

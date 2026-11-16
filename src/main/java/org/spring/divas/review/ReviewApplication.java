package org.spring.divas.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReviewApplication {

  static void main(String[] args) {
    SpringApplication.run(ReviewApplication.class, args);
  }

}

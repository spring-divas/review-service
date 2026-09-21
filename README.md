# Review Service
[![CI](https://github.com/spring-divas/review-service/actions/workflows/ci.yml/badge.svg)](https://github.com/spring-divas/review-service/actions/workflows/ci.yml)

## Overview
This service is for receiving user's reviews on dishes and venues.

## Setup

1. Fill .env.example
```bash
cp .env.example .env
```
2. Run with Docker Compose

```bash
docker compose up -d --build     # build the image and start the compose
```
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

## Kubernetes
1. Create the secret from the example
```bash
cp k8s/review-secret.yaml.example k8s/review-secret.yaml
```
2. Build the image and make it available to the cluster
```bash
docker build -t review-service:latest .
minikube image load review-service:latest
```
3. Apply the manifests
```bash
kubectl apply -f k8s/review-secret.yaml
kubectl apply -f k8s/review-configmap.yaml
kubectl apply -f k8s/review-postgres.yaml
kubectl apply -f k8s/review-service.yaml
```
4. Reach the API
```bash
kubectl port-forward svc/review-service 8080:8080
```

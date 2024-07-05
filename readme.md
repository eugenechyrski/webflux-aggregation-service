# Aggregation Service

## Overview

The Aggregation Service is a Java-based web application built with Spring WebFlux. It aggregates pricing, tracking, and
shipment information from multiple APIs into a single endpoint response.

## Features

- Aggregates data from multiple external APIs
- Uses reactive programming with Spring WebFlux
- Error handling and fallback mechanisms
- Unit and integration tests included

## Technology Stack

- Java
- Spring Boot
- Spring WebFlux
- Maven
- Docker
- JUnit
- Mockito

## Prerequisites

To run this project, make sure you have the following installed:

- JDK 17 or higher
- Docker
- Docker Compose (optional)
- k6 (optional)

## Design

- Spring WebFlux allows handling concurrent requests efficiently using non-blocking I/O.
  This is crucial for an aggregation service that needs to interact with multiple external
  APIs simultaneously without blocking threads.

- Sinks (Sinks.Many) are used to collect requests and batch them before making external API calls.
  This helps in optimizing network usage by sending multiple requests in a single batch,
  thus reducing overhead and improving efficiency.

- Environment variable SPRING_PROFILES_ACTIVE allow configuring the service
  dynamically without changing the code. This flexibility is essential for deploying the service across
  different environments (development, testing, production) with varying configurations.

## Build and Run

### Building the Project

```sh
mvn clean install -DskipTests
```

```sh
docker build -t aggregation-service .
```

## Running the Application

```sh
docker run -p 8080:8080  -e SERVICES_HOST=http://127.0.0.1:8080  aggregation-service 
```

## Usage

The Aggregation Service exposes the following HTTP endpoint:

- **GET `/aggregation`**: Accepts query parameters `pricing`, `track`, and `shipments` to retrieve aggregated data from
  respective APIs.

## End to end load testing

 ```sh
docker compose up
```

 ```sh
K6_WEB_DASHBOARD=true k6 run --vus 10 --duration 30s ./load-tests/test.js
```

Dashboard is available under http://127.0.0.1:5665
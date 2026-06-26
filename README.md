# Spring Boot OpenTelemetry Demo

This repository is a small microservices demo with three Spring Boot services:

- `storefront-service` on port `8080`
- `order-service` on port `8081`
- `inventory-service` on port `8082`

The request path is:

```text
storefront-service -> order-service -> inventory-service
```

Each service exposes Prometheus metrics at `/actuator/prometheus` and exports OpenTelemetry traces to the collector over OTLP HTTP.

* Note: OTLP stands for OpenTelemetry Protocol. It's the standard data transmission protocol used by OpenTelemetry to send telemetry data (traces, metrics, and logs) from instrumented applications to a backend or collector.

## Stack

- Spring Boot `4.0.2`
- Micrometer metrics and tracing
- OpenTelemetry OTLP exporter
- OpenTelemetry Collector
- Prometheus
- Jaeger
- Grafana

## Run Locally

Build the service jars:

```bash
mvn clean package
```

Start the full stack (everything in Docker, including the Spring boot services):

```bash
docker compose up --build
```

Generate traffic:

```bash
curl http://localhost:8080/api/storefront/orders/1001
curl http://localhost:8080/api/storefront/orders/1002
curl http://localhost:8080/api/storefront/orders/1003
```

Open the tools:

- Storefront API: <http://localhost:8080/api/storefront/orders/1001>
- Prometheus: <http://localhost:9090>
- Jaeger: <http://localhost:16686>
- Grafana: <http://localhost:3000>

Grafana anonymous admin access is enabled for the demo. The `Spring Boot Observability Demo` dashboard and Prometheus/Jaeger data sources are provisioned automatically.

## Useful Prometheus Queries

```promql
sum by (application, uri) (rate(http_server_requests_seconds_count[1m]))
demo_storefront_order_pages_total
demo_orders_requests_total
demo_inventory_lookups_total
jvm_memory_used_bytes
process_cpu_usage
```

## Run the Spring Boot Services outside of Docker

Start the collector, Prometheus, Jaeger, and Grafana:

```bash
docker compose up otel-collector prometheus jaeger grafana
```

Then run the services in separate terminals:

```bash
mvn -pl inventory-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl storefront-service spring-boot:run
```

The default local service URLs and OTLP endpoint are already set in each `application.yml`.

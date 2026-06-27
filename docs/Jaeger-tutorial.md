# Jaeger Tutorial

Jaeger is an open source distributed tracing system. It helps you see how one request moves through multiple services, how long each step takes, and where failures or latency are introduced.

In this demo, Jaeger is used to visualize requests that travel through the Spring Boot microservices:

```text
storefront-service -> order-service -> inventory-service
```

## Why Jaeger Matters

In a monolith, a request usually stays inside one application process. In microservices, one user action may cross many services, network calls, databases, queues, and external APIs.

Logs can tell you what happened inside one service. Metrics can tell you that something is slow or broken. Traces connect the whole request path so you can answer questions like:

- Which services handled this request?
- How long did each service spend processing it?
- Which downstream call caused the delay?
- Did the same trace produce errors in multiple services?
- Are trace IDs visible in logs for easier debugging?

## Core Concepts

### Trace

A trace represents one complete request or workflow across the system.

For example, when you call:

```bash
curl http://localhost:8080/api/storefront/orders/1001
```

Jaeger can show a single trace that includes work done by:

- `storefront-service`
- `order-service`
- `inventory-service`

### Span

A span is one timed operation inside a trace.

Examples:

- Handling an HTTP request in `storefront-service`
- Calling `order-service`
- Handling the request in `order-service`
- Calling `inventory-service`
- Handling the inventory lookup

Each span has a start time, duration, service name, operation name, and optional tags or events.

### Parent and Child Spans

Spans are arranged as a tree.

If `storefront-service` receives the original request, its server span becomes the parent. The HTTP call from `storefront-service` to `order-service` becomes a child span. The request inside `order-service` then has related child spans, and so on.

This parent-child structure is what lets Jaeger reconstruct the full journey of a request.

### Trace Context

Trace context is metadata passed between services, usually through HTTP headers.

The most common header is:

```text
traceparent
```

It carries the trace ID and span information so downstream services know they are part of the same trace.

## How This Demo Uses Jaeger

This project does not send traces directly from Spring Boot to Jaeger. Instead, it uses OpenTelemetry Collector in the middle:

```text
Spring Boot services
  -> OpenTelemetry Collector
  -> Jaeger
```

This is a common production-style pattern because the collector can receive telemetry from many applications, process it, and export it to different backends.

The main files are:

- `docker-compose.yml`
- `observability/otel-collector/config.yml`
- `storefront-service/src/main/resources/application.yml`
- `order-service/src/main/resources/application.yml`
- `inventory-service/src/main/resources/application.yml`

## Running the Demo

Build the Spring Boot services:

```bash
mvn clean package
```

Start the full observability stack:

```bash
docker compose up --build
```

The important ports are:

- Storefront service: <http://localhost:8080>
- Jaeger UI: <http://localhost:16686>
- Prometheus: <http://localhost:9090>
- Grafana: <http://localhost:3000>
- OpenTelemetry Collector OTLP HTTP: `localhost:4318`
- OpenTelemetry Collector OTLP gRPC: `localhost:4317`

## Generating Traces

Jaeger needs traffic before it can show useful traces.

Run these commands a few times:

```bash
curl http://localhost:8080/api/storefront/orders/1001
curl http://localhost:8080/api/storefront/orders/1002
curl http://localhost:8080/api/storefront/orders/1003
```

Each request should call:

```text
storefront-service -> order-service -> inventory-service
```

That gives Jaeger a multi-service trace to display.

## Using the Jaeger UI

Open:

```text
http://localhost:16686
```

In the search panel:

1. Select a service, such as `storefront-service`.
2. Choose a recent time range, such as the last 5 or 15 minutes.
3. Click **Find Traces**.
4. Select one of the traces in the result list.

You should see a waterfall timeline showing spans from the services involved in the request.

## Reading a Trace

When you open a trace, focus on these areas:

### Service Names

The trace should include spans from the services that handled the request.

For this demo, expect to see:

```text
storefront-service
order-service
inventory-service
```

### Duration

The total trace duration tells you how long the whole request took.

Each span duration tells you how long a specific operation took.

If the full request is slow, look for the longest span. That span is often the best starting point for investigation.

### Timeline

Jaeger displays spans as horizontal bars.

The left-to-right position shows when each operation happened. The width shows how long it took.

Nested bars show parent-child relationships between operations.

### Tags

Tags add metadata to spans.

Common HTTP tags include:

- HTTP method
- HTTP route
- HTTP status code
- Service name
- Error status

Tags help you filter and understand traces.

### Errors

Failed spans are usually highlighted in the trace view.

If one service returns an error, Jaeger helps you see whether the error started there or came from a downstream dependency.

## Example Debugging Workflow

Suppose users report that order pages are slow.

You can investigate like this:

1. Generate or find a slow request in Jaeger.
2. Open the trace.
3. Look for the longest span.
4. Check whether the slow span is in `storefront-service`, `order-service`, or `inventory-service`.
5. Open the span details.
6. Check route, status code, and timing metadata.
7. Use the trace ID to correlate with logs if needed.

This turns a vague report like "orders are slow" into a focused question like "the inventory lookup is taking most of the request time."

## Jaeger vs Prometheus

Jaeger and Prometheus solve related but different observability problems.

Prometheus is for metrics:

- Request rate
- Error rate
- Latency percentiles
- CPU and memory
- JVM metrics
- Service health over time

Jaeger is for traces:

- One request across many services
- Per-operation timing
- Causal relationships
- Downstream dependency behavior
- Request-level debugging

A healthy observability setup usually uses both.

Prometheus can tell you that latency increased. Jaeger can help explain why.

## Jaeger vs Logs

Logs are event records from applications.

Jaeger traces are structured request paths.

Logs are best for detailed application-specific facts, such as:

- Validation failed for field `sku`
- User `user-42` submitted an order
- Inventory lookup returned zero quantity

Traces are best for request flow and timing.

When logs include trace IDs, you can jump from a Jaeger trace to the exact logs for that request.

This project configures log patterns with trace and span fields:

```text
traceId=%X{traceId:-},spanId=%X{spanId:-}
```

## OpenTelemetry Collector Configuration

The collector config lives at:

```text
observability/otel-collector/config.yml
```

It accepts OTLP data over HTTP and gRPC:

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
```

It exports traces to Jaeger:

```yaml
exporters:
  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true
```

And its trace pipeline connects the receiver to the Jaeger exporter:

```yaml
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/jaeger, debug]
```

## Spring Boot Configuration

Each service has tracing enabled in its `application.yml`.

Example:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:http://localhost:4318/v1/traces}
```

The sampling probability is set to `1.0`, which means every request is traced.

That is useful for demos and local development. In production, you may choose a lower sampling rate to reduce cost and storage usage.

## Common Problems

### No Traces Appear

Check these things:

- Did you generate traffic with `curl`?
- Is Jaeger running at <http://localhost:16686>?
- Is the OpenTelemetry Collector running?
- Are the services configured with `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`?
- Did you search the right service and time range in Jaeger?

### Only One Service Appears in a Trace

This usually means trace context is not propagating between services.

Check that HTTP client instrumentation is active and that downstream services receive trace headers.

### Collector Cannot Export to Jaeger

Check the collector logs:

```bash
docker compose logs otel-collector
```

Also check that Jaeger has OTLP enabled:

```yaml
environment:
  COLLECTOR_OTLP_ENABLED: "true"
```

### Too Many Traces

For local demos, tracing every request is fine.

For production, reduce the sampling probability:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1
```

That would sample about 10 percent of requests.

## Practice Exercise

1. Start the stack with `docker compose up --build`.
2. Run several storefront requests.
3. Open Jaeger at <http://localhost:16686>.
4. Search for traces from `storefront-service`.
5. Open a trace and identify spans from all three services.
6. Find the slowest span.
7. Compare the trace timing with Prometheus request metrics.

## Summary

Jaeger helps you understand individual requests in a microservices system.

In this demo:

- Spring Boot services create telemetry.
- OpenTelemetry carries trace data.
- OpenTelemetry Collector receives and exports telemetry.
- Jaeger stores and visualizes traces.
- Prometheus and Grafana complement Jaeger with metrics.

Together, these tools give you a practical observability workflow for Spring Boot microservices.

## Reference: 

[Spring Boot Micrometer OTLP Registry](https://docs.micrometer.io/micrometer/reference/implementations/otlp.html)

## Standard Configuration Examples

1. Inside application.properties:

```properties
# The target OTLP receiver endpoint (Default: http://localhost:4318/v1/metrics)
management.otlp.metrics.export.url=http://your-otel-collector:4318/v1/metrics

# Optional: Adjust the metric shipping frequency (e.g., every 30 seconds)
management.otlp.metrics.export.step=30s
```

2. Inside application.yml:

```yaml
management:
  otlp:
    metrics:
      export:
        url: "http://your-otel-collector:4318/v1/metrics"
        step: 30s

```

---

## Important Context & Ecosystem DetailsDefault Fallback: 

* If you do not specify this property, Spring Boot implicitly defaults the destination path to http://localhost:4318/v1/metrics.

* The Required Dependency: To make this configuration work, ensure your project contains the micrometer-registry-otlp dependency. This comes bundled automatically if you utilize the native spring-boot-starter-opentelemetry starter.

* Naming Inconsistency Notice: Be aware that the telemetry landscape in Spring Boot has separate configuration branches due to underlying library integrations. Metrics rely on Micrometer (prefixed with management.otlp), whereas traces use the native OpenTelemetry SDK (prefixed with management.opentelemetry):

| Telemetry Type | Configuration Key | Default Path |
|----------------|-------------------|--------------|
| Metrics | management.otlp.metrics.export.url | /v1/metrics |
| Traces | management.opentelemetry.tracing.export.otlp.endpoint | /v1/traces |





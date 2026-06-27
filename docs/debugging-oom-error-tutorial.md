Here's a systematic approach to debugging an OOM error in your Spring Boot service on EKS:

## 1. Confirm & Capture the OOM Event

**Check pod events and logs:**
```bash
kubectl describe pod <pod-name> -n <namespace>
# Look for: OOMKilled in "Last State" and "Exit Code: 137"

kubectl logs <pod-name> -n <namespace> --previous
# --previous gets logs from the crashed container
```

**Check recent pod restarts:**
```bash
kubectl get pods -n <namespace>
# RESTARTS column will be elevated
```

---

## 2. Identify *Which* Memory Is Exhausted

OOM in a JVM pod can mean two different things:

| Scenario | Who kills the process | Signal |
|---|---|---|
| **Container OOM** | Linux kernel / cgroup | `Exit Code 137`, pod status `OOMKilled` |
| **JVM heap exhaustion** | JVM itself | `java.lang.OutOfMemoryError` in logs, graceful exit |

- **Container OOM** → your `-Xmx` is set too close to (or over) the container `resources.limits.memory`
- **JVM heap OOM** → a heap leak or undersized heap

---

## 3. Understand JVM Memory vs. Container Limit

The JVM uses more than just heap. Total JVM footprint ≈:

```
Heap (-Xmx)
+ Metaspace
+ Thread stacks (~512KB × thread count)
+ Direct/off-heap buffers (Netty, NIO)
+ Code cache (JIT)
+ JVM overhead (~50–100MB)
```

**Rule of thumb:** set `resources.limits.memory` to at least **1.5–2× your `-Xmx`** to leave room for non-heap.

Check your current config:
```bash
kubectl get deployment <name> -n <namespace> -o yaml | grep -A 10 resources
```

---

## 4. Get a Heap Dump

The most valuable artifact. Add these JVM flags to your deployment:

```yaml
env:
  - name: JAVA_OPTS
    value: >-
      -Xmx512m
      -XX:+HeapDumpOnOutOfMemoryError
      -XX:HeapDumpPath=/tmp/heapdump.hprof
      -XX:+ExitOnOutOfMemoryError
```

Then copy the dump out before the pod restarts (or mount an EFS/EBS volume at `/tmp` to persist it):
```bash
kubectl cp <pod-name>:/tmp/heapdump.hprof ./heapdump.hprof -n <namespace>
```

Analyze with **Eclipse MAT** or **VisualVM** — look for the dominator tree to find what's holding memory.

---

## 5. Enable GC Logging

Add to `JAVA_OPTS`:
```
-Xlog:gc*:file=/tmp/gc.log:time,uptime:filecount=5,filesize=20m
```

Look for: frequent full GCs, GC overhead limit, heap never releasing — these indicate a leak vs. just undersizing.

---

## 6. Common Spring Boot OOM Causes

| Root Cause | What to Look For |
|---|---|
| **Connection pool leak** (HikariCP) | Connections growing, never released |
| **Cache unbounded growth** | `@Cacheable` with no eviction policy / max size |
| **Large request/response buffering** | File uploads, streaming held in heap |
| **Metaspace exhaustion** | Dynamic class generation (Groovy, reflection, proxies) — add `-XX:MaxMetaspaceSize=256m` |
| **Thread leak** | Too many `@Async` threads or ExecutorService not shut down |
| **Netty direct buffer** (WebFlux) | Off-heap, not covered by `-Xmx` — set `-XX:MaxDirectMemorySize` |

---

## 7. Right-size and Add Guardrails

```yaml
resources:
  requests:
    memory: "768Mi"
  limits:
    memory: "1Gi"

env:
  - name: JAVA_OPTS
    value: "-Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:+ExitOnOutOfMemoryError"
```

Use `-XX:+ExitOnOutOfMemoryError` so the JVM exits cleanly and Kubernetes restarts the pod predictably rather than letting it thrash.

---

## 8. Ongoing Observability

- **Actuator metrics** → expose `jvm.memory.used`, `jvm.gc.pause` to Prometheus/CloudWatch via your existing OTLP pipeline
- **HPA** → scale out under load rather than letting one pod absorb all pressure
- Set up a **CloudWatch alarm** on pod restart count or container OOM metric from Container Insights

The fastest path to root cause is: logs (`--previous`) → confirm OOM type → heap dump → MAT analysis.

---

## [Reference book: JVM Performance Engineering](https://www.scribd.com/document/749586101/JVM-performance-engineering-2024-4)

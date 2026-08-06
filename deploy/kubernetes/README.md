# Kubernetes deployment

The base deployment intentionally does not contain a ConfigMap or Secret with deployable values.

1. Copy `examples/configmap.example.yaml` into the environment repository and replace every example value.
2. Provision `payroll-orchestrator-secrets` from an enterprise secret manager. The Azure overlay contains a Key Vault CSI example.
3. Replace the image with an immutable digest produced by the release pipeline.
4. Keep `APP_PAYMENT_EXECUTION_ENABLED=false` until the bank connector certification gate is signed.
5. Execute `scripts/preflight-production.*` against the exact deployment environment before rollout.
6. Apply with `kubectl apply -k deploy/kubernetes/base` or the Azure overlay after the external ConfigMap and Secret exist.

The deployment runs as UID/GID 10001, drops Linux capabilities, uses a read-only root filesystem, exposes separate startup/liveness/readiness probes, and performs rolling updates with zero planned unavailability.

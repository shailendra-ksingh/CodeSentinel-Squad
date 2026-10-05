# CodeSentinel Squad — 3-Minute Evaluator Walkthrough

## 1. Build

```text
mvn clean test
```

Expected: all automated tests pass without an API key.

## 2. Run the strongest demo

Run one realistic polyglot change set:

```text
java -jar target/codesentinel.jar \
  tests/samples/SampleVulnerableService.java \
  tests/samples/polyglot/order_api.ts \
  tests/samples/polyglot/risk_service.py \
  tests/samples/polyglot/schema.sql \
  tests/samples/polyglot/deployment.yaml
```

The output demonstrates both file-level and system-level review:

```text
output/<file>/review.md
output/project/project-review.md
```

## 3. Look for these six proof points

1. **Polyglot detection** — the same CLI accepts Java, TypeScript, Python, SQL and YAML.
2. **Evidence first** — deterministic analysis creates findings before AI is called.
3. **Selective intelligence** — only relevant specialists are dispatched.
4. **Self-correction** — generated Java tests go through the Critic and can be revised.
5. **Persistent learning** — recurring findings appear on later reviews.
6. **Cross-language reasoning** — the Project Integration Agent reviews boundaries between APIs, services, schemas and configuration.

## Why it matters

A basic AI review looks like:

```text
source → one large prompt → one answer
```

CodeSentinel demonstrates:

```text
polyglot source
  → language detection
  → deterministic evidence
  → targeted specialist agents
  → critic/reflection
  → memory
  → cross-file / cross-language integration review
  → transparent quality gate
  → human decision
```

The score is intentionally **not** an autonomous approval. CodeSentinel accelerates expert review while keeping evidence, uncertainty and human accountability visible.

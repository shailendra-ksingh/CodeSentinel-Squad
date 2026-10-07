# CodeSentinel Squad - Quick Walkthrough

This is the shortest path to build CodeSentinel and see its main capabilities.

The recommended demonstration uses the supplied five-file polyglot sample and does not require an API key.

---

## 1. Build

Run:

```text
mvn clean package
```

Expected result:

- All automated tests pass.
- `target/codesentinel.jar` is created.

The test suite does not require an API key or network connection.

---

## 2. Run the polyglot sample

Run the five supplied files together:

```text
java -jar target/codesentinel.jar \
  tests/samples/polyglot/OrderController.java \
  tests/samples/polyglot/order_api.ts \
  tests/samples/polyglot/risk_service.py \
  tests/samples/polyglot/schema.sql \
  tests/samples/polyglot/deployment.yaml
```

Without `ANTHROPIC_API_KEY`, CodeSentinel runs in **Safe Mode**.

The review produces:

```text
output/<file>/review.md
output/project/project-review.md
```

Open the project report:

```text
output/project/project-review.md
```

This is the main report for the evaluator.

---

## 3. What the sample demonstrates

The sample represents a small order-processing flow:

```text
TypeScript Client
       |
       | POST /api/orders
       v
Java OrderController
       |
       | customerId
       v
Python Risk Service
       |
       | customer_id
       v
SQL customer / orders tables
       |
       v
Kubernetes configuration
```

The files use five different technologies:

```text
Java
TypeScript
Python
SQL
YAML
```

The sample contains one deliberate API contract mismatch.

### TypeScript client expects

```text
id
total
customerId
```

### Java API returns

```text
orderId
amount
```

The files are individually valid, but the response contracts do not match.

The project-level deterministic check reports this as:

```text
Cross-File Contract Mismatch
```

This is the key demonstration.

The problem exists **between the files**, not simply inside one file.

---

## 4. Expected project result

The supplied Safe Mode example produces a project report showing:

```text
Files reviewed:       5
Critical findings:    0
Major findings:       0
Minor findings:       2
Recurring findings:   0
Review score:         96/100
Cross-file risks:     1
```

The generated example is also available at:

```text
docs/sample-output/Polyglot-Project-safe-mode.md
```

---

## 5. Three-minute demonstration

### Step 1 - Show Safe Mode

Run the sample without `ANTHROPIC_API_KEY`.

The console shows:

```text
SAFE MODE: ANTHROPIC_API_KEY not configured
```

This demonstrates that deterministic review does not depend on an AI service.

### Step 2 - Show file-level evidence

Open one of:

```text
output/OrderController/review.md
output/order_api/review.md
output/risk_service/review.md
```

Show the deterministic findings.

### Step 3 - Show the project report

Open:

```text
output/project/project-review.md
```

Show:

```text
Review score: 96/100
```

and:

```text
Cross-File Contract Mismatch
```

### Step 4 - Show the actual cross-file problem

Point out:

```text
TypeScript:
id
total
customerId
```

versus:

```text
Java:
orderId
amount
```

Explain:

> The individual files can look valid, but their contracts do not agree. CodeSentinel checks the relationship between them.

### Step 5 - Show the quality gate

The report provides:

```text
PASS WITH REVIEW
```

The score is an indicative review signal, not an automatic approval.

---

## 6. Where AI fits

Safe Mode demonstrates the deterministic part of the workflow without sending source code to an external AI service.

When `ANTHROPIC_API_KEY` is configured, CodeSentinel can additionally use its specialist agents.

The broader workflow is:

```text
Evidence
   ↓
Route
   ↓
Specialize
   ↓
Critique / Revision
   ↓
Remember
   ↓
Integrate
   ↓
Quality Gate
   ↓
Human Decision
```

The important design choice is:

> **AI is not the evidence. AI reasons over the evidence.**

Deterministic findings remain visible in the review.

---

## 7. Key capabilities to look for

### 1. Polyglot review

The same CLI accepts Java, TypeScript, Python, SQL and YAML.

### 2. Evidence first

Deterministic analysis creates findings before AI reasoning is used.

### 3. Selective agents

In AI mode, specialist agents are selected based on the evidence rather than running every agent for every file.

### 4. Critic and revision

Generated test output can be checked and revised before becoming part of the review.

### 5. Review memory

Previous findings can be remembered and recurring issues can be surfaced in later reviews.

### 6. Cross-language review

The Project Integration Agent looks at relationships between APIs, services, schemas and configuration.

### 7. Safe Mode

The deterministic review works without an AI API key.

### 8. Human quality gate

The final score helps the reviewer understand the result but does not replace developer judgement.

---

## 8. Run the test suite

Run:

```text
mvn clean test
```

The current test suite contains:

```text
61 tests
0 failures
0 errors
0 skipped
BUILD SUCCESS
```

No API key or network connection is required.

---

## 9. Final takeaway

A traditional file review asks:

> **What is wrong in this file?**

CodeSentinel also asks:

> **What happens when this file interacts with the rest of the system?**

The project combines:

```text
Deterministic Evidence
        +
Selective AI Reasoning
        +
Critique
        +
Review Memory
        +
Cross-File Analysis
        +
Quality Gate
        +
Human Decision
```

The goal is simple:

**make the first code review faster and more structured without hiding the evidence, uncertainty or need for human judgement.**

# CodeSentinel Squad - Sequence Diagrams

This document shows the main runtime interactions inside CodeSentinel.

The diagrams focus on three common scenarios:

1. Single-file review
2. AI review with secret protection
3. Multi-file project review

---

## 1. Single-file review

This flow shows how CodeSentinel processes one source file from input to the final report.

```text
User
  ↓
Main
  ↓
LanguageDetector
  ↓
StaticAnalyzer
  ↓
CoordinatorAgent
  ↓
Specialist Agents
  ↓
CriticAgent
  ↓
MemoryStore
  ↓
ReportGenerator
  ↓
User
````

### Flow

* The user provides a source file.
* `LanguageDetector` identifies the file type.
* `StaticAnalyzer` collects deterministic findings.
* `CoordinatorAgent` decides which specialist agents are relevant.
* Specialist agents analyze the findings.
* `CriticAgent` checks generated test recommendations where applicable.
* `MemoryStore` records lightweight recurring finding information.
* `ReportGenerator` creates the final review report.

---

## 2. AI review with secret protection

This flow shows how source code is protected before it is sent to the AI service.

```text
Source
  ↓
SecretRedactor
  ↓
Coordinator
  ↓
ClaudeClient
  ↓
Anthropic API
  ↓
Finding / Recommendation
```

### Flow

* Source code is first passed through `SecretRedactor`.
* Credential-like values are replaced with `[REDACTED]`.
* The coordinator prepares the relevant review request.
* `ClaudeClient` sends the redacted content to the Anthropic API.
* The AI response is returned as a finding or recommendation.

The intention is simple:

> **The AI can review the code without receiving the original secret-like values.**

---

## 3. Multi-file project review

This flow shows how CodeSentinel moves from individual file analysis to project-level reasoning.

```text
Project
  ↓
File Analysis
  ↓
Coordinator
  ↓
ProjectIntegrationAgent
  ↓
Cross-file Findings
  ↓
Quality Gate
  ↓
Project Report
```

### Flow

* The user provides multiple files or a project directory.
* Each supported file is analyzed individually.
* `CoordinatorAgent` coordinates the available findings.
* `ProjectIntegrationAgent` reviews relationships between files.
* Cross-file risks and integration issues are identified.
* `Quality Gate` combines the available results.
* A consolidated project report is generated.

---

## 4. Overall sequence

The three flows can be summarized as:

```text
Single File
    │
    ├── Detect
    ├── Analyze
    ├── Route
    ├── Specialize
    └── Report
             │
             ▼
       Multi-file Review
             │
             ├── Cross-file analysis
             ├── Integration checks
             └── Project report
             
AI path
    │
    ├── Redact secrets
    ├── Prepare context
    ├── ClaudeClient
    └── AI findings
```

The key design principle is:

> **Deterministic evidence comes first, focused AI reasoning comes next, and the final result remains subject to human review.**


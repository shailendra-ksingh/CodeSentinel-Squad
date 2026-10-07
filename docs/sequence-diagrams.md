# CodeSentinel Squad - Sequence Diagrams

This document shows the main runtime interactions inside CodeSentinel.

The diagrams cover three common scenarios:

1. Single-file review
2. AI review with secret protection
3. Multi-file project review

---

## 1. Single-file review

This sequence shows how one source file moves through the CodeSentinel review process.

```text
User          Main        LanguageDetector    StaticAnalyzer
 |              |                |                   |
 |-- review --->|                |                   |
 |              |-- detect ----->|                   |
 |              |<-- language ---|                   |
 |              |---------------------- analyze ---->|
 |              |<-------------------- findings -----|
 |              |                                    |
 |              |------ route to CoordinatorAgent --->
 |              |                                    |
 |              |<----- specialist results -----------|
 |              |                                    |
 |              |---------- CriticAgent ------------->|
 |              |<--------- critic result ------------|
 |              |                                    |
 |              |---------- MemoryStore ------------>|
 |              |<--------- memory status ------------|
 |              |                                    |
 |              |---------- ReportGenerator -------->|
 |              |<----------- final report -----------|
 |<------------- report ------------------------------|
```

### Flow

- The user provides a source file.
- `LanguageDetector` identifies the file type.
- `StaticAnalyzer` collects deterministic findings.
- `CoordinatorAgent` decides which specialist agents are relevant.
- Specialist agents analyze the relevant findings.
- `CriticAgent` checks generated test recommendations where applicable.
- `MemoryStore` records lightweight recurring finding information.
- `ReportGenerator` creates the final review report.

---

## 2. AI review with secret protection

This sequence shows how source code is protected before it is sent to the AI service.

```text
Source        SecretRedactor    Coordinator    ClaudeClient    Anthropic API
  |                 |                |               |                |
  |-- source ------>|                |               |                |
  |                 |                |               |                |
  |<-- redacted ----|                |               |                |
  |                                  |               |                |
  |---------------------- review ------------------->|                |
  |                                  |               |                |
  |                                  |-- prompt ---->|                |
  |                                  |               |-- request ---->|
  |                                  |               |                |
  |                                  |               |<-- response ---|
  |                                  |<-- AI result --|                |
  |                                  |               |                |
  |<------------- finding / recommendation -----------|                |
```

### Flow

- Source code is first passed through `SecretRedactor`.
- Credential-like values are replaced with `[REDACTED]`.
- The coordinator prepares the relevant review request.
- `ClaudeClient` sends the redacted content to the Anthropic API.
- The AI response is returned as a finding or recommendation.

For example:

```text
Before:
apiKey = "real-secret-value"

After:
apiKey = "[REDACTED]"
```

The intention is simple:

> **The AI can review the code without receiving the original secret-like value.**

---

## 3. Multi-file project review

This sequence shows how CodeSentinel moves from individual file analysis to project-level reasoning.

```text
User       File Analysis    Coordinator    ProjectIntegrationAgent    Quality Gate    Report
 |              |                |                    |                    |            |
 |-- files ---->|                |                    |                    |            |
 |              |-- analyze --->|                    |                    |            |
 |              |<-- findings --|                    |                    |            |
 |              |                |                    |                    |            |
 |              |---------------- project review --->|                    |            |
 |              |                |                    |                    |            |
 |              |                |<-- cross-file findings ---------------|            |
 |              |                |                    |                    |            |
 |              |                |------------------------ results ------>|            |
 |              |                |                    |                    |            |
 |              |                |                    |                    |-- report ->|
 |              |                |                    |                    |            |
 |<----------------------------------------------------------------------- report -----|
```

### Flow

- The user provides multiple files or a project directory.
- Each supported file is analyzed individually.
- `CoordinatorAgent` coordinates the available findings.
- `ProjectIntegrationAgent` reviews relationships between files.
- Cross-file risks and integration issues are identified.
- `Quality Gate` combines the available results.
- A consolidated project report is generated.

The important difference is:

```text
Single-file review
        ↓
"Is this file okay?"

Multi-file review
        ↓
"Do these files work correctly together?"
```

---

## 4. Critic and revision flow

For generated tests, CodeSentinel adds a second level of review.

```text
Finding       Test Agent       Local Checks       Critic Agent
   |              |                 |                  |
   |-- test ----->|                 |                  |
   |              |-- candidate --->|                  |
   |              |                 |-- check -------->|
   |              |                 |<-- result -------|
   |              |----------------------------------->|
   |              |                                    |
   |              |<----------- critic result ---------|
   |              |                                    |
   |              |---- revise if needed ------------>|
   |              |                                    |
   |<------------- final test -------------------------|
```

The basic idea is:

```text
Generate
   ↓
Local checks
   ↓
Critic
   ↓
One revision if needed
   ↓
Final result
```

The Critic does not replace the original test generation. It provides a second check before the generated test is included in the review result.

---

## 5. Overall review sequence

The complete CodeSentinel approach can be summarized as:

```text
Input
  │
  ▼
Evidence
  │
  ▼
Coordinator
  │
  ├──────────────┬──────────────┐
  ▼              ▼              ▼
Security        Test       Architecture
Agent           Agent          Agent
                  │
                  ▼
                Critic
                  │
                  ▼
              One revision
                  │
                  ▼
               Memory
                  │
                  ▼
       Project Integration
          (2+ files)
                  │
                  ▼
             Quality Gate
                  │
                  ▼
           Final Report
                  │
                  ▼
          Human Decision
```

### Key design principle

> **Deterministic evidence comes first, focused AI reasoning comes next, and the final result remains subject to human review.**

The overall CodeSentinel flow is:

**Evidence → Route → Specialize → Critique → Remember → Integrate → Quality Gate → Human Decision**
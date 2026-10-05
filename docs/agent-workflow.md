# CodeSentinel Squad - Agent Workflow

CodeSentinel is designed around a simple idea:

> **Don't ask AI to review everything. First find the evidence, then bring in the right specialist.**

The workflow moves from deterministic checks to focused AI reasoning and ends with a human decision.

## 1. The workflow at a glance

```text
┌──────────────┐     ┌──────────────────┐     ┌────────────────┐
│    INPUT     │ ──> │  FIND EVIDENCE   │ ──> │   COORDINATOR  │
│ 1 or many    │     │ Language + rules │     │ Route relevant │
│    files     │     │     first        │     │     agents     │
└──────────────┘     └──────────────────┘     └───────┬────────┘
                                                       │
                          ┌────────────────────────────┼─────────────────────────┐
                          │                            │                         │
                          ▼                            ▼                         ▼
                   ┌─────────────┐             ┌─────────────┐          ┌─────────────┐
                   │  SECURITY   │             │    TEST     │          │ DOCS / ARCH │
                   │    AGENT    │             │    AGENT    │          │    AGENTS   │
                   └──────┬──────┘             └──────┬──────┘          └──────┬──────┘
                          │                            │                         │
                          │                            ▼                         │
                          │                    ┌─────────────┐                  │
                          │                    │   CRITIC    │                  │
                          │                    │ Check +     │                  │
                          │                    │ one revision│                  │
                          │                    └──────┬──────┘                  │
                          │                           │                         │
                          └───────────────────────────┼─────────────────────────┘
                                                      ▼
                                             ┌────────────────┐
                                             │     MEMORY     │
                                             │ Recurring      │
                                             │ findings       │
                                             └───────┬────────┘
                                                     │
                              ┌──────────────────────┴─────────────────────┐
                              │                                            │
                         One file                                    2+ files
                              │                                            │
                              │                                            ▼
                              │                              ┌────────────────────────┐
                              │                              │ PROJECT INTEGRATION    │
                              │                              │ Cross-file / polyglot  │
                              │                              │ reasoning              │
                              │                              └───────────┬────────────┘
                              │                                          │
                              └──────────────────────┬───────────────────┘
                                                     ▼
                                           ┌──────────────────┐
                                           │   QUALITY GATE   │
                                           │ Score + actions  │
                                           └────────┬─────────┘
                                                    ▼
                                           ┌──────────────────┐
                                           │  HUMAN DECISION  │
                                           │ Approve / Fix /  │
                                           │      Rerun       │
                                           └──────────────────┘
````

### The flow in one line

**Evidence → Route → Specialize → Critique → Remember → Integrate → Decide**

---

## 2. What happens at each step

### Step 1 — Input

The developer provides one file, several files or a project directory.

CodeSentinel identifies the supported file types and starts the review locally.

Supported languages and assets include:

* Java
* Python
* JavaScript
* TypeScript
* SQL
* YAML
* JSON

---

### Step 2 — Find evidence first

Before calling an AI model, CodeSentinel runs deterministic checks.

For Java, this includes signals such as:

* hard-coded secrets
* SQL concatenation
* resource leaks
* broad exception handling
* null-safety issues
* transaction-related issues
* architecture and maintainability signals

For other supported languages, `PolyglotAnalyzer` provides lightweight language-specific checks.

The result is a set of `Finding` objects.

**This evidence drives the next step.**

---

### Step 3 — Coordinator decides what is needed

`CoordinatorAgent` looks at the findings and decides which specialist agents are relevant.

For example:

```text
Security finding
      ↓
Security Agent

Testable finding
      ↓
Test Generation Agent

Documentation finding
      ↓
Documentation Agent

Architecture finding
      ↓
Architecture Agent
```

If there is no relevant evidence, the specialist is skipped.

This keeps the review focused instead of sending every file to every agent.

---

### Step 4 — Specialist agents investigate

Each specialist has a focused responsibility.

| Agent                     | Main responsibility                            |
| ------------------------- | ---------------------------------------------- |
| Security Agent            | Security risks and possible fixes              |
| Test Generation Agent     | Generate useful tests for testable findings    |
| Documentation Agent       | Improve Javadoc and maintainability            |
| Architecture Agent        | Review design and responsibility boundaries    |
| Critic Agent              | Check generated tests before they are accepted |
| Project Integration Agent | Review multiple files as one system            |

The agents are intentionally focused rather than using one large general-purpose prompt.

---

### Step 5 — Generated tests are challenged

The Test Generation Agent does not get the final word.

The generated test first goes through local structural checks. `CriticAgent` then reviews it for issues such as:

* missing assertions
* incorrect test structure
* weak coverage of the reported problem
* obvious semantic problems

If needed, one revision is allowed.

```text
Generate → Local checks → AI Critic
                         ↓
                  Needs improvement?
                    ├─ No  → Keep
                    └─ Yes → One revision
```

This gives generated output a second level of review.

---

### Step 6 — Remember recurring problems

`MemoryStore` keeps lightweight finding information between runs.

It does **not** store source code.

For example:

```text
Run 1  → SQL concatenation found
Run 2  → SQL concatenation found again
Run 3  → SQL concatenation found again
                         ↓
                  Recurring finding
```

Recurring findings can receive additional attention in later reviews.

---

### Step 7 — Think across files

When two or more files are reviewed, CodeSentinel runs a separate `ProjectIntegrationAgent`.

This moves the review from:

> **"Is this file okay?"**

to:

> **"Do these files work correctly together?"**

The project agent looks for evidence of:

* API and DTO mismatches
* service/repository contract problems
* duplicated business rules
* inconsistent authentication or authorization
* inconsistent error handling
* suspicious coupling
* configuration/implementation mismatches
* missing integration-boundary tests

For example:

```text
TypeScript Client
       ↓
    Java API
       ↓
  Service Layer
       ↓
 SQL / Database
       ↑
       │
Kubernetes Configuration
```

The individual files may look reasonable on their own while their boundaries still contain a problem.

That is why project-level reasoning is a separate step.

---

## 3. Safe Mode

AI is optional.

If `ANTHROPIC_API_KEY` is not configured:

```text
Source → Deterministic analysis → Findings → Reports
```

No source is sent to the AI service.

This allows CodeSentinel to remain useful in restricted or offline environments.

---

## 4. Secret protection

Before source is included in an AI prompt, `SecretRedactor` looks for credential-like values.

For example:

```text
Before:
apiKey = "real-secret-value"

After:
apiKey = "[REDACTED]"
```

The AI receives the redacted version rather than the original secret-like value.

---

## 5. Failure does not stop the whole review

A failure in one specialist does not discard the complete review.

```text
Security Agent       ✓
Test Agent           ✓
Documentation Agent  ✗
Architecture Agent   ✓
                     ↓
              Partial results kept
```

The failure is reported as a warning and the other available results remain usable.

The same principle applies to the project-level review.

---

## 6. Final quality gate

After the review, `ProjectReportGenerator` brings the evidence together.

The result includes:

* findings
* recurring issues
* specialist results
* project-level risks
* quality score
* key actions

The score is an **indicative triage signal**, not a certification or automatic release decision.

The final decision stays with the developer or reviewer.

---

## 7. The main idea

The complete CodeSentinel approach can be remembered in seven steps:

```text
Evidence
   ↓
Route
   ↓
Specialize
   ↓
Critique
   ↓
Remember
   ↓
Integrate
   ↓
Decide
```

> **CodeSentinel does not replace the developer's judgement. It helps the developer reach that judgement with better evidence, focused analysis and a clearer view of the whole system.**

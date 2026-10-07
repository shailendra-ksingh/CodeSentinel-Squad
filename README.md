# CodeSentinel Squad
## Multi-Agent Code Review for Polyglot Applications

*Evidence-driven review for enterprise application stacks.*

**AI-assisted code review that starts with evidence - not just a prompt.**

CodeSentinel is a multi-agent, polyglot code review assistant for enterprise applications.

It does not simply send source code to an AI model and wait for suggestions.

It first collects deterministic evidence, routes relevant findings to specialist agents, checks generated results, remembers recurring issues, and then looks across files to find risks that only become visible when the pieces are considered together.

> **AI is not the evidence. AI reasons over the evidence.**

That is the central idea behind CodeSentinel.

---

## Why CodeSentinel?

A simple AI code review often looks like:

```text
Code → Prompt → AI → Suggestions
```

CodeSentinel takes a more controlled approach:

```text
Source
   ↓
Language Detection
   ↓
Deterministic Evidence
   ↓
Selective Specialist Agents
   ↓
Critic / Revision
   ↓
Review Memory
   ↓
Cross-File Integration
   ↓
Quality Gate
   ↓
Human Decision
```

The difference is not simply that CodeSentinel uses multiple agents.

The important part is **when and why each part is used**.

### What CodeSentinel brings together

- **Evidence first** - deterministic checks run before AI reasoning.
- **Selective agents** - Security, Test, Documentation and Architecture agents are used when the evidence calls for them.
- **Self-checking AI output** - generated tests go through structural checks and an AI Critic, with a controlled revision loop.
- **Polyglot review** - Java, Python, JavaScript, TypeScript, SQL, YAML and JSON can be reviewed using the same overall approach.
- **Cross-file reasoning** - the Project Integration Agent looks for API, schema, configuration and architectural mismatches across files.
- **Review memory** - recurring findings can be identified across review runs.
- **Safe Mode** - deterministic review continues to work without an AI API key.
- **Secret protection** - credential-like values are redacted before source is sent to AI.
- **Fault isolation** - failure of one agent does not discard the complete review.
- **Human decision** - the quality gate is a review signal, not an autonomous approval.

---

## The problem CodeSentinel is designed to catch

Consider a small order-processing system:

```text
TypeScript Client
       ↓
Java API
       ↓
Python Risk Service
       ↓
SQL Database
       ↓
Kubernetes
```

Each file may look reasonable when reviewed independently.

But imagine the TypeScript client expects:

```text
id
total
customerId
```

while the Java API returns:

```text
orderId
amount
```

The individual files may contain no obvious syntax problem.

The real problem is **between the files**.

CodeSentinel's Project Integration Agent is designed to look for exactly this kind of system-level risk.

The supplied demonstration intentionally contains this mismatch so the evaluator can see the difference between:

> **reviewing files**

and

> **reviewing how the files work together.**

---

## What the evaluator will see

The supplied demonstration can show the complete review path:

1. **Deterministic evidence** is collected.
2. **Relevant specialists** are selected.
3. **Generated tests** can be checked by the Critic.
4. **Recurring findings** can be remembered.
5. **Multiple languages** can be reviewed together.
6. **Cross-file contract problems** can be detected.
7. **Safe Mode** works without an API key.
8. A **quality gate** summarizes the evidence.
9. The final decision remains with the developer or reviewer.

The most important demonstration is the polyglot project review.

It reviews:

```text
Java
TypeScript
Python
SQL
YAML
```

as one connected change set.

---

## Prerequisites

- Java 17+
- Maven 3.8+

Optional for AI mode:

- Anthropic API key
- Outbound HTTPS access to `api.anthropic.com`

---

## Technology

- Java 17
- Maven
- JUnit 5
- JDK `HttpClient`
- Anthropic Claude Messages API
- Configurable Claude model
- No application framework or external runtime dependency

---

## Build

Run:

```text
mvn clean package
```

This runs the automated tests and creates:

```text
target/codesentinel.jar
```

The test suite does not require an API key or network access.

---

## Configuration

| Variable | Required | Purpose | Default |
|---|---|---|---|
| `ANTHROPIC_API_KEY` | AI mode only | Enables AI specialists | Not set = Safe Mode |
| `CODESENTINEL_MODEL` | No | Claude model ID | `claude-sonnet-4-6` |
| `CODESENTINEL_OUTPUT_DIR` | No | Report/output root | `output` |
| `CODESENTINEL_MEMORY_DIR` | No | Review history | `memory` |

Never put credentials in source code, README files, `.env` files committed to the repository, or submission documents.

### Windows PowerShell

```powershell
$env:ANTHROPIC_API_KEY = "<your-api-key>"
```

### macOS / Linux

```text
export ANTHROPIC_API_KEY="<your-api-key>"
```

---

## Required APIs / MCP servers

AI mode uses the Anthropic Messages API:

```text
https://api.anthropic.com/v1/messages
```

No MCP server is required.

---

# Run the review

## Single file

```text
java -jar target/codesentinel.jar tests/samples/SampleVulnerableService.java
```

Without `ANTHROPIC_API_KEY`, CodeSentinel runs in **Safe Mode** using deterministic analysis.

## Multiple files / project review

Pass several files:

```text
java -jar target/codesentinel.jar \
  src/example/OrderService.java \
  src/example/CustomerService.java
```

Or pass a directory:

```text
java -jar target/codesentinel.jar src/
```

Each supported file receives a language-aware deterministic review.

When more than one file is reviewed, CodeSentinel also performs a project-level integration review and creates:

```text
output/project/project-review.md
```

The project report combines:

- file-level finding counts
- recurring issues
- an indicative quality score
- deterministic quality-gate results
- Project Integration Agent findings
- top cross-file actions

The goal is not simply to review more files.

**The goal is to understand the relationships between them.**

---

# Supported languages

CodeSentinel applies the same evidence-first approach across a practical enterprise stack.

| Language / asset | Deterministic checks | AI specialists | Project integration |
|---|---|---|---|
| Java | Security, resources, SQL, errors, null-safety, design, documentation | Security / Test / Docs / Architecture | Yes |
| Python | Secrets, `eval`/`exec`, bare `except`, size/design signals | Security / Architecture | Yes |
| JavaScript | Secrets, `eval`, XSS sinks, design signals | Security / Architecture | Yes |
| TypeScript | JavaScript checks + `any` type-safety signal | Security / Architecture | Yes |
| SQL | Dynamic construction, `SELECT *` | Security / Architecture | Yes |
| YAML | Secrets, privileged containers | Security / Architecture | Yes |
| JSON | Secrets and configuration signals | Security / Architecture | Yes |

The important differentiator is not the number of languages.

The same review methodology is applied across languages, and the Project Integration Agent can then reason about the boundaries between them.

For example:

```text
TypeScript client
       ↓
Java API
       ↓
Python service
       ↓
SQL schema
       ↓
Deployment configuration
```

This allows CodeSentinel to identify problems that may not be visible when each file is reviewed in isolation.

---

# Sample input

Two deliberately flawed Java classes are included under `tests/samples/`:

- `SampleVulnerableService.java` - demonstrates secret handling, SQL concatenation, resource management, broad exception handling, null-safety, transaction and documentation issues.
- `SampleGodClassService.java` - demonstrates excessive responsibilities and a large public surface.

Run them together:

```text
java -jar target/codesentinel.jar \
  tests/samples/SampleVulnerableService.java \
  tests/samples/SampleGodClassService.java
```

## Polyglot sample

A polyglot sample set is available under:

```text
tests/samples/polyglot/
```

It contains a small order-processing flow using:

```text
Java
TypeScript
Python
SQL
Kubernetes YAML
```

The sample intentionally contains one cross-language contract mismatch.

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

The files are individually valid, but the client and API contracts do not match.

This gives the Project Integration Agent a concrete example of a problem that can be missed when files are reviewed separately.

The recommended fix is to align the client and API contract, or introduce an explicit mapping between them.

---

# Sample output

For each reviewed file:

```text
output/<ClassName>/review.md
output/<ClassName>/<ClassName>Test.java
output/<ClassName>/<ClassName>.documented.java
```

For a multi-file review:

```text
output/project/project-review.md
```

A typical console flow is:

```text
[1/5] Running static analysis...
[2/5] Checking previous review history...
[3/5] Coordinator routing work...
[4/5] Review execution...
[5/5] Writing report...
Project report: .../output/project/project-review.md
Reviewed N of N file(s).
```

### Example evaluator report

The repository includes a generated polyglot Safe Mode report:

```text
docs/sample-output/Polyglot-Project-safe-mode.md
```

The example demonstrates:

```text
5 files reviewed
0 critical findings
0 major findings
2 minor findings
96/100 indicative review score
1 cross-file contract mismatch
```

The report identifies the mismatch between the TypeScript and Java response contracts.

---

# Agent squad

| Agent | Purpose | Trigger |
|---|---|---|
| Static Analyzer | Deterministic evidence | Every file |
| Coordinator | Evidence-based routing | Every AI review |
| Security Agent | Security risks and fixes | Security finding |
| Test Generation Agent | JUnit 5 test generation | Testable defect + public method |
| Critic Agent | Structural and semantic test review | Generated tests |
| Documentation Agent | Javadoc and maintainability | Documentation finding |
| Architecture Review Agent | File/module design | Architecture finding |
| **Project Integration Agent** | Cross-file/cross-language contracts, coupling and architecture | 2+ reviewed files |
| Memory Store | Recurrence detection | Every review |
| Quality Gate | Deterministic project score/status | 2+ reviewed files |

---

# How the review works

The complete workflow is:

```text
Source files
     ↓
Language detection
     ↓
Deterministic evidence
     ↓
Coordinator
     ↓
Relevant specialist agents
     ↓
Critic / revision
     ↓
Memory
     ↓
Project integration
     ↓
Quality gate
     ↓
Final report
     ↓
Human decision
```

The design intentionally separates **evidence gathering** from **AI reasoning**.

AI is used where it adds value, while deterministic findings remain visible in the final report.

> **Find the evidence first. Then ask AI to reason about it.**

---

# Three-minute evaluator walkthrough

For the shortest demonstration, use:

[Evaluator Quickstart](docs/quickstart.md)

### Recommended flow

**1. Build**

```text
mvn clean package
```

**2. Run the polyglot sample**

Review the Java, TypeScript, Python, SQL and YAML files together.

**3. Open the file-level report**

Show that deterministic evidence is generated before AI reasoning.

**4. Show selective routing**

Explain that relevant findings determine which specialist agents are used.

**5. Show Critic / revision**

When AI mode is enabled, generated tests are checked before being accepted.

**6. Show memory**

Run the same review again and demonstrate recurring findings.

**7. Open the project report**

```text
output/project/project-review.md
```

**8. Show the cross-file mismatch**

The report identifies:

```text
TypeScript:
id
total
customerId

Java:
orderId
amount
```

**9. Show the quality gate**

The score is an indicative review signal, not an automatic release decision.

### The story to remember

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
Quality Gate
   ↓
Human Decision
```

---

# How to run tests

```text
mvn test
```

Tests require no API key and no network connection. The AI client is replaced with scripted test doubles.

The test suite covers:

- static-analysis rules and false-positive guards
- evidence-based routing
- Safe Mode
- secret redaction
- memory and recurrence
- agent failure isolation
- Critic structural and semantic checks
- generated test revision
- API JSON handling
- multi-file/project integration behaviour
- project quality scoring

The current automated suite contains **61 tests**.

---

# Security and privacy

- No secrets, production credentials, certificates or access tokens are included.
- `ANTHROPIC_API_KEY` is read from the environment and is not logged.
- Credential-like source values are redacted before AI prompts are built.
- Review memory stores findings, not source code.
- Safe Mode keeps source code local.
- AI mode sends source code to Anthropic. Confirm that your organization's data-classification policy permits this.
- Generated tests and documentation are drafts and are not treated as proof of correctness.
- Source code, comments and generated review content are treated as untrusted input by AI review prompts.

---

# Known limitations

CodeSentinel is deliberately practical rather than pretending to be a complete replacement for established engineering tools.

- Static analysis is intentionally lightweight and uses simple rules. It complements tools such as SonarQube, SpotBugs, PMD and IDE inspections; it does not replace them.
- The Project Integration Agent is limited to the files supplied in the review set. It does not fetch missing repository context.
- Very large projects should be reviewed in logical batches because AI prompt size and API cost still matter.
- Generated tests and documentation are not automatically compiled or executed.
- The Critic uses the same configured model family. An independent model would provide stronger adversarial validation.
- Memory is local file-based and is intended as lightweight review history, not enterprise governance storage.
- The quality score is an **indicative triage signal**, not formal compliance or release certification.
- AI-generated recommendations still require developer judgement.

---

# Deployment / CI

For CI:

1. Run `mvn clean package`.
2. Store `ANTHROPIC_API_KEY` as a CI secret if AI review is permitted.
3. Run the JAR against changed files or the relevant module.
4. Publish `output/` as a build artifact.
5. Persist `memory/` between runs if recurring-issue detection is required.

Safe Mode can be used in restricted environments:

```text
java -jar target/codesentinel.jar src/
```

---

# Submission structure

The final submission keeps the required folder structure:

```text
Shailendra_Singh_701096
├── Agent.md
├── README.md
├── pom.xml
├── src/
├── tests/
├── docs/
├── memory/
└── output/
```

`memory/` and `output/` contain only the small set of sample data intended to make the demonstration understandable.

---

# Design documentation

- [Agent](Agent.md) - problem, architecture, business value and security considerations
- [Architecture documentation](docs/architecture.md) - components and runtime design
- [Architecture diagram](docs/architecture.svg) - visual overview
- [Agent workflow](docs/agent-workflow.md) - review workflow
- [Sequence diagrams](docs/sequence-diagrams.md) - key runtime interactions
- [Design document](docs/design-documents.md) - key design decisions
- [Sample review reports](docs/sample-output/SampleVulnerableService-safe-mode.md) - example review results
- [Evaluator Quickstart](docs/quickstart.md) - short demonstration path

---

# Executive Summary

> **CodeSentinel does not just ask AI to review code. It looks for evidence first, sends the right work to specialist agents, checks generated results, remembers recurring findings, and reviews the application as a connected system.**

The design goal is simple:

**make the first code review faster, more structured and easier to act on - without hiding the evidence, uncertainty or need for human judgement.**

---

# Architecture / Flow Diagram

The architecture combines deterministic analysis, specialist agents, memory, critique, project integration and the final quality gate.

![CodeSentinel Architecture](docs/architecture.svg)

For the detailed design, see the [Architecture documentation](docs/architecture.md).

---

## Final thought

A useful code review should answer more than:

> **"What is wrong in this file?"**

It should also ask:

> **"What evidence supports it?"**

> **"Which parts need deeper reasoning?"**

> **"What happens when this file interacts with the rest of the system?"**

That is the problem CodeSentinel is designed to solve.

**Evidence first. Focused reasoning second. Human decision always.**
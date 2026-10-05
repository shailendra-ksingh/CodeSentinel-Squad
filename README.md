# CodeSentinel Squad

**Evidence-driven, Multi-Agent polyglot code review - from file-level defects to cross-language system risk.**

CodeSentinel combines fast deterministic checks with selectively dispatched AI specialists. It can review Java, Python, JavaScript, TypeScript, SQL, YAML and JSON, remember recurring issues, redact secrets before AI calls, and — for multi-file reviews — perform a separate cross-language project-level architecture/integration pass.

## Why this is different

**Evidence → Route → Specialize → Critique → Remember → Integrate → Quality Gate**

- **Evidence first:** deterministic checks run before AI, so agents have concrete signals instead of reviewing blindly.
- **Selective agents:** Security, Test, Documentation and Architecture agents run only when relevant evidence exists.
- **Self-checking output:** generated tests go through local structural checks and an AI Critic, with one revision loop.
- **Polyglot system intelligence:** the **Project Integration Agent** reviews multiple languages together for API/schema/configuration mismatches, coupling, duplicated responsibility and inconsistent cross-cutting concerns.
- **Review memory:** recurring findings are surfaced across runs.
- **Safe Mode:** no API key means no source leaves the machine; deterministic review still works.
- **Fault isolation:** one agent/API failure does not discard the complete review.
- **Secret protection:** credential-like values are redacted before AI prompts are built.
- **Human-in-the-loop:** reports clearly separate evidence, AI advice and the final human quality decision.

## Prerequisites

- Java 17+
- Maven 3.8+
- Optional for AI mode: Anthropic API key and outbound HTTPS access to `api.anthropic.com`

## Technology

- Java 17
- Maven
- JUnit 5
- JDK `HttpClient`
- Anthropic Claude Messages API
- Configurable Claude model
- No runtime framework or external runtime dependency

## Build

```text
mvn clean package
````

The executable JAR is created as:

```text
target/codesentinel.jar
```

`mvn package` also runs the automated tests.

## Configuration

| Variable                  | Required     | Purpose                | Default             |
| ------------------------- | ------------ | ---------------------- | ------------------- |
| `ANTHROPIC_API_KEY`       | AI mode only | Enables AI specialists | not set = Safe Mode |
| `CODESENTINEL_MODEL`      | No           | Claude model ID        | `claude-sonnet-4-6` |
| `CODESENTINEL_OUTPUT_DIR` | No           | Report/output root     | `output`            |
| `CODESENTINEL_MEMORY_DIR` | No           | Review history         | `memory`            |

Never put credentials in source, README, `.env` files committed to the repository, or submission documents.

### Windows PowerShell

```powershell
$env:ANTHROPIC_API_KEY = "<your-api-key>"
```

### macOS / Linux

```text
export ANTHROPIC_API_KEY="<your-api-key>"
```

## Required APIs / MCP servers

AI mode uses the [Anthropic Messages API](https://api.anthropic.com/v1/messages).

No MCP server is required.

## Run — single file

```text
java -jar target/codesentinel.jar tests/samples/SampleVulnerableService.java
```

## Run — multiple files / project review

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

Every supported file receives its language-aware deterministic review. When more than one file is reviewed, CodeSentinel additionally creates:

```text
output/project/project-review.md
```

The project report combines:

* file-by-file finding counts
* recurring issues
* an indicative quality score
* a deterministic quality gate
* Project Integration Agent analysis
* top cross-file actions

This is the key difference between **multi-file execution** and a true **multi-file review**.

## Supported languages

CodeSentinel uses the same review methodology across a practical enterprise stack:

| Language / asset | Deterministic checks                                        | AI specialists                        | Project integration |
| ---------------- | ----------------------------------------------------------- | ------------------------------------- | ------------------- |
| Java             | Security, resources, SQL, errors, null-safety, design, docs | Security / Test / Docs / Architecture | Yes                 |
| Python           | secrets, eval/exec, bare except, size/design signals        | Security / Architecture               | Yes                 |
| JavaScript       | secrets, eval, XSS sinks, design signals                    | Security / Architecture               | Yes                 |
| TypeScript       | JavaScript checks + `any` type-safety signal                | Security / Architecture               | Yes                 |
| SQL              | dynamic construction, `SELECT *`                            | Security / Architecture               | Yes                 |
| YAML             | secrets, privileged containers                              | Security / Architecture               | Yes                 |
| JSON             | secrets/configuration signals                               | Security / Architecture               | Yes                 |

The important differentiator is not the language count. **The same evidence-first workflow is applied across languages, then the Project Integration Agent reasons across the boundaries between them.** For example, a TypeScript client, Java API and SQL schema can be reviewed as one change set.

## Sample input

Two deliberately flawed classes are included under `tests/samples/`:

* `SampleVulnerableService.java` — secret, SQL concatenation, resource leak, broad catch, null-safety, transaction and documentation issues.
* `SampleGodClassService.java` — intentionally excessive responsibilities/public methods.

Run both together to demonstrate the project-level capability:

```text
java -jar target/codesentinel.jar tests/samples/SampleVulnerableService.java tests/samples/SampleGodClassService.java
```

## Sample output

Per file:

```text
output/<ClassName>/review.md
output/<ClassName>/<ClassName>Test.java
output/<ClassName>/<ClassName>.documented.java
```

For a multi-file run:

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

See the [sample review reports](docs/sample-output/) for example reports.

## Agent squad

| Agent                         | Purpose                                                        | Trigger                         |
| ----------------------------- | -------------------------------------------------------------- | ------------------------------- |
| Static Analyzer               | deterministic evidence                                         | every file                      |
| Coordinator                   | evidence-based routing                                         | every AI review                 |
| Security Agent                | security exploitation/risk and fixes                           | Security finding                |
| Test Generation Agent         | JUnit 5 tests                                                  | testable defect + public method |
| Critic Agent                  | structural + semantic test review                              | generated tests                 |
| Documentation Agent           | Javadoc and maintainability                                    | Documentation finding           |
| Architecture Review Agent     | file/module design                                             | Architecture finding            |
| **Project Integration Agent** | cross-file/cross-language contracts, coupling and architecture | 2+ reviewed files               |
| Memory Store                  | recurrence detection                                           | every review                    |
| Quality Gate                  | deterministic project score/status                             | 2+ reviewed files               |

## How to run tests

```text
mvn test
```

Tests require no API key and no network connection. The AI client is replaced with scripted stubs.

The suite covers:

* static-analysis rules and false-positive guards
* evidence-based routing
* Safe Mode
* secret redaction
* memory/recurrence
* agent failure isolation
* Critic structural/semantic loop
* generated test revision
* API JSON handling
* multi-file/project integration behaviour
* project quality scoring

## Final submission checklist

Before uploading to SharePoint:

* Rename the containing folder to `<Firstname>_<Lastname>_<EmpID>` as required by the submission rules.
* Do not add API keys, certificates, production data or other confidential material.
* Keep `src/`, `tests/` and `docs/` in place; the sample `output/` and `memory/` files are included only to make the demo immediately understandable.
* Use `docs/Evaluator-quickstart.md` as the quick three-minute demonstration path for evaluators.

## Security and privacy

* No secrets, production credentials, certificates or access tokens are included.
* `ANTHROPIC_API_KEY` is read from the environment and never logged.
* Credential-like source values are redacted before AI prompts.
* Review memory stores findings only, not source code.
* Safe Mode keeps source local.
* AI mode sends source code to Anthropic; confirm that your organization's data-classification policy permits this.
* Generated tests and documentation are drafts and are not treated as proof of correctness.

## Known limitations

* Static analysis is intentionally lightweight and uses simple rules. It complements SonarQube, SpotBugs, PMD and IDE inspections; it does not replace them.
* The Project Integration Agent is only as good as the files supplied in the review set; it does not fetch missing repository context.
* Very large projects should be reviewed in logical batches; AI prompt size and API cost still matter.
* Generated tests and documentation are not compiled or executed automatically; the strongest automated validation remains deterministic evidence and structural checks.
* The Critic uses the same configured model family; an independent model would provide stronger adversarial validation.
* Memory is local file-based and is intended as lightweight review history, not enterprise governance storage.
* The quality score is an **indicative triage signal**, not a formal compliance or release certification.

## Deployment / CI

For CI:

1. Run `mvn clean package`.
2. Store `ANTHROPIC_API_KEY` as a CI secret if AI review is permitted.
3. Run the JAR against changed files or the relevant module.
4. Publish `output/` as a build artifact.
5. Persist `memory/` between runs if recurring-issue detection is desired.

Safe Mode can be used in restricted environments:

```text
java -jar target/codesentinel.jar src/
```

## Submission structure

```text
Shailendra_Singh_701096
├── Agent.md
├── README.md
├── pom.xml
├── src/
├── tests/
├── docs/
├── memory/          # sample lightweight review history
└── output/          # sample generated reports
```

## Design documentation

* `Agent.md` — problem, innovation, architecture, business value and security considerations
* `docs/architecture.md` — component and sequence design
* `docs/architecture.svg` — visual architecture
* `docs/sample-output/` — example review results
* `docs/Evaluator-quickstart.md` — quick three-minute demonstration for evaluators

## Executive Summary

> **CodeSentinel does not just ask AI to review code — it first looks for evidence, runs the right specialist agents, checks generated results, remembers recurring defects, and then reviews the code as a connected system.**

## Architecture / Flow Diagram

The following diagram shows the complete CodeSentinel workflow, including language detection, evidence collection, specialist agents, memory, critique, project integration and the final quality gate.

![CodeSentinel Architecture](docs/architecture.svg)

For the detailed explanation of the workflow:

* [Architecture documentation](docs/architecture.md)

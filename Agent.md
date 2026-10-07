# CodeSentinel Squad
## **Multi-Agent Code Review for Polyglot Applications**

*Evidence-driven review for enterprise application stacks.*

## Team Details

- **Team Name:** `CodeSentinel Squad`
- **Team Members:** `Shailendra_Singh_701096, Shashank_Shawak_705515`
- **Primary Contact:** `Shailendra Singh`
- **Business / Technical Area:** `Enterprise Java/JEE, Architecture and AI-enabled engineering`

## 1. Problem Statement

Code review across enterprise application stacks can be slow and inconsistent. A reviewer may need to check security, error handling, resource management, testing, documentation and design at the same time. The same issues can also appear repeatedly in later reviews because there is no simple way to remember previous findings.

Using a single general-purpose AI prompt for code review has similar limitations. The review can be broad rather than focused, findings may not always be supported by evidence, and AI processing is used even when there is little to review.

CodeSentinel was designed to provide a more structured first-pass review while keeping the final decision with the developer or reviewer.

## 2. Proposed Agent

CodeSentinel uses:

**Detect -> Analyze -> Route -> Specialize -> Critique -> Remember -> Integrate -> Quality Gate**

For a single file, it performs a focused review using deterministic checks and specialist agents. When multiple files are supplied, it also performs a project-level review to identify issues that may only become visible when the files are considered together.

The workflow is:

1. A deterministic, language-aware analyzer identifies possible issues and provides the initial evidence.
2. A Coordinator uses that evidence to decide which specialist agents should run.
3. Specialist agents focus on specific areas such as Security, Testing, Documentation and Architecture.
4. Generated tests are reviewed by a Critic and can be revised once.
5. Findings are stored so recurring issues can be identified in later reviews.
6. For multi-file reviews, a Project Integration Agent checks relationships between the files.
7. A final quality gate brings the findings together and identifies areas that still require human judgement.

The result is a Markdown report showing the findings, supporting evidence, agents that ran, decisions made by the workflow and items that require human review.

## 3. Key Capabilities

1. **Evidence-driven routing** - specialist agents are selected based on the findings produced by the deterministic analyzer. The report records which agents were run and which were skipped.

2. **Generate-then-critique test creation** - the Test Agent generates JUnit 5 tests when appropriate. Basic structural checks are performed locally and an AI Critic can review the generated tests. If the Critic rejects them, they can be revised once.

3. **Review memory** - previous findings are stored by category and description. Line numbers are not used as the only identifier, so a recurring issue can still be recognised after code movement.

4. **Safe Mode and fault isolation** - the application works without an API key using deterministic analysis and reporting. If an individual agent fails, the failure is recorded and the remaining review can continue.

5. **Secret redaction** - detected credential values are masked before source content is included in an AI request.

## 4. Polyglot Multi-File / Project Review

CodeSentinel supports Java, Python, JavaScript, TypeScript, SQL, YAML and JSON.

When two or more files are reviewed, the existing per-file review pipeline is retained and an additional project-level review is performed.

The Project Integration Agent receives the redacted source from the reviewed files together with the available review results. It looks for issues that are difficult to identify from one file alone, including:

- API, DTO, service and schema contract mismatches
- duplicated business rules
- inconsistent authentication or error handling
- unnecessary coupling between components
- configuration and implementation inconsistencies
- missing tests at application boundaries

A consolidated report is written to:

`output/project/project-review.md`

The project report also includes the quality gate and the main cross-file findings.

## 5. Target Users

- **Developers** - for a structured first review before raising a pull request.
- **Reviewers and technical leads** - for a consistent first pass before detailed human review.
- **Quality and architecture teams** - for repeatable checks and visibility of recurring issues.

## 6. How It Works

```text
Source/config file
        |
        v
Language Detector
        |
        v
Language-aware evidence
        |
        +------> Memory / recurring findings
        |
        v
Coordinator
        |
        +---- Security
        |
        +---- Test -> Critic -> revise
        |
        +---- Documentation
        |
        +---- Architecture
        |
        v
Review Report
        |
        v
Memory

For 2+ files:

Per-file reviews
        |
        v
Project Integration
        |
        v
Cross-file / cross-language findings
        |
        v
Project Quality Gate
````

| Agent               | Runs when                                                                                                                       |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| Security            | A Security finding exists                                                                                                       |
| Test Generation     | A public method exists and a testable finding exists (Security, Null Safety, Error Handling, Resource Leak or JEE Anti-pattern) |
| Documentation       | A Documentation finding exists                                                                                                  |
| Architecture        | The class has more than 6 public methods or more than 120 lines                                                                 |
| Project Integration | 2 or more files are supplied                                                                                                    |

Detailed workflow and diagram:

- [Architecture documentation](docs/architecture.md)
- [Architecture diagram](docs/architecture.svg)

## 7. Technology / Framework

* Java 17
* Maven
* JUnit 5
* JDK `HttpClient`
* Anthropic Claude Messages API
* Configurable Claude model
* Plain-file memory

The application does not depend on an AI framework. The AI service is accessed through the `LlmClient` interface, which also allows the review pipeline to operate in Safe Mode without an API connection.

## 8. Tools / Integrations

* **Anthropic Claude API** - used only in AI mode and requires `ANTHROPIC_API_KEY`.
* **Local file system** - used for source files, review memory and generated reports.
* No database or MCP server is required.

## 9. Expected Business Value

* **Reduced manual effort:** provides a structured first-pass review and can draft tests and documentation for selected findings.
* **More consistent reviews:** the same deterministic checks and specialist routing are applied each time.
* **Better cost control:** AI processing is limited to cases where specialist reasoning is useful; Safe Mode requires no AI service.
* **Recurring issue visibility:** previous findings are retained so repeated problems can be identified in later reviews.
* **Reusable design:** the project demonstrates a relatively small implementation of agent routing, review/critique, memory and project-level analysis.

The impact has not been measured on production projects. The intended benefit is a faster and more consistent first review, while keeping the final decision with a human reviewer.

## 10. Innovation / Differentiator

CodeSentinel combines several parts of the review process instead of relying on one general-purpose AI prompt:

* deterministic analysis is performed before AI review;
* evidence is used to decide which specialist agents should run;
* generated tests can be reviewed and revised by a Critic;
* previous findings can be used to identify recurring issues;
* individual agent failures do not stop the complete review;
* credential values are redacted before AI requests;
* multiple files can be reviewed together rather than only as independent files;
* the project-level pass can identify cross-language and cross-file issues;
* the quality gate separates automated findings from areas requiring human judgement.

The implementation is kept lightweight. The AI service is behind an `LlmClient` interface, allowing the core workflow and automated tests to run without a network connection or API key.

## 11. Architecture / Flow Diagram

The following diagram shows the complete CodeSentinel workflow, including language detection, evidence collection, specialist agents, memory, critique, project integration and the final quality gate.

![CodeSentinel Architecture](docs/architecture.svg)

For the detailed explanation of the workflow:

- [Architecture documentation](docs/architecture.md)

## 12. Effort Spent in Hrs

`Not formally tracked. Development, testing and documentation were completed iteratively as part of the submission.`

## 13. Expected Output

For each reviewed file:

`output/<FileName>/`

For multi-file reviews:

`output/project/project-review.md`

A typical file review contains:

* `review.md` - summary, findings, severity, recurring issues, agent decisions, agent output, Critic feedback and warnings;
* `<ClassName>Test.java` - generated JUnit 5 tests when the Test Agent runs in AI mode;
* `<ClassName>.documented.java` - source with generated Javadoc when the Documentation Agent runs in AI mode.

Example reports are available here:

[Sample review reports](docs/sample-output/SampleVulnerableService-safe-mode.md)

## 14. Data / Security Considerations

* Source code is sent outside the local environment only when AI mode is enabled. Projects should check their data-classification requirements before using AI mode.
* Safe Mode can be used when source code must remain local.
* Detected hard-coded credential values are replaced with `<REDACTED>` before prompts are created.
* Review memory stores finding information such as severity, line, category and description. It does not store source code.
* The Anthropic API key is read from the `ANTHROPIC_API_KEY` environment variable and is not written to reports or logs.
* No real credentials are included in the submission. Sample values are intentionally fake.
* AI-generated tests and documentation are drafts. They are not automatically compiled or executed by CodeSentinel and should be reviewed before being used in a production codebase.

# CodeSentinel Squad - Architecture

## 1. Overview

CodeSentinel Squad is a Java-based multi-agent code review assistant.

The architecture follows a simple flow:

```text
Analyze
   ↓
Route
   ↓
Specialize
   ↓
Critique
   ↓
Report
   ↓
Remember
````

The system first performs deterministic analysis and then uses AI agents only where their input is relevant.

---

## 2. High-Level Architecture

```text
                         +----------------+
                         |   Java Source  |
                         +-------+--------+
                                 |
                                 v
                       +-------------------+
                       |  Static Analyzer  |
                       +---------+---------+
                                 |
                                 v
                       +-------------------+
                       | Finding Evidence  |
                       +---------+---------+
                                 |
                                 v
                       +-------------------+
                       |    Coordinator    |
                       +---------+---------+
                                 |
              +------------------+------------------+
              |                  |                  |
              v                  v                  v
       +-------------+    +-------------+    +-------------+
       |  Security   |    |    Test     |    |    Docs     |
       |    Agent    |    |    Agent    |    |    Agent    |
       +-------------+    +------+------+    +-------------+
                                 |
                                 v
                         +---------------+
                         |    Critic     |
                         +-------+-------+
                                 |
                          +------+------+
                          |             |
                       APPROVE        REVISE
                                        |
                                        v
                                   Test Agent

                                      
              Architecture Agent is also
              selected when architecture
              findings are present.

                                 |
                                 v
                       +-------------------+
                       |  Report Generator |
                       +---------+---------+
                                 |
                    +------------+------------+
                    |                         |
                    v                         v
              Review Report               Memory
```

---

# 3. Main Components

| Component           | Responsibility                                    |
| ------------------- | ------------------------------------------------- |
| `StaticAnalyzer`    | Finds candidate issues using deterministic checks |
| `Finding`           | Represents one review finding                     |
| `CoordinatorAgent`  | Decides which specialist agents should run        |
| `SecurityAgent`     | Reviews security findings                         |
| `TestAgent`         | Generates JUnit 5 tests                           |
| `DocsAgent`         | Handles documentation findings                    |
| `ArchitectureAgent` | Reviews architecture-related findings             |
| `CriticAgent`       | Reviews generated tests                           |
| `ClaudeClient`      | Communicates with Claude when AI mode is enabled  |
| `MemoryStore`       | Stores previous finding information               |
| `ReportGenerator`   | Creates the final review report                   |
| `SquadResult`       | Holds the result of the complete review           |

---

# 4. Review Flow

The complete review has five main stages.

## Stage 1 - Analyze

The `StaticAnalyzer` reads the Java source.

It produces a list of `Finding` objects.

Example:

```text
Security
Resource Leak
Error Handling
Documentation
Architecture
```

The analyzer works without an AI service.

---

## Stage 2 - Route

The `CoordinatorAgent` receives the findings.

Each specialist has a `shouldRun()` method.

The Coordinator uses this to decide whether an agent is relevant.

For example:

```text
Security finding
       ↓
SecurityAgent → RUN

Documentation finding
       ↓
DocsAgent → RUN

No documentation finding
       ↓
DocsAgent → SKIP
```

The Test Agent has an additional condition:

```text
Public method
+
Testable finding
       ↓
TestAgent → RUN
```

This makes the agent selection evidence-driven.

---

# 5. Specialist Agents

## Security Agent

The Security Agent focuses on security-related findings.

It receives the source code and relevant findings and produces a security-focused review.

Typical output includes:

```text
Problem
Risk
Possible scenario
Suggested fix
```

---

## Test Agent

The Test Agent generates JUnit 5 tests.

It is only selected when the source contains a public method and the findings contain an issue that can reasonably be tested.

The generated tests are then passed to the Critic.

---

## Documentation Agent

The Documentation Agent handles documentation findings.

It can generate Javadoc and a short explanation of the class.

It is not run simply because a class contains public methods.

---

## Architecture Agent

The Architecture Agent handles architecture-related findings.

The current analyzer uses simple signals such as:

* Number of public methods
* Source file size

These signals can indicate that a class may have too many responsibilities.

They are intentionally treated as heuristics.

---

# 6. Test and Critic Flow

The Test Agent has a slightly different workflow.

```text
              Test Agent
                   |
                   v
             Generated Test
                   |
                   v
                Critic
                   |
             +-----+-----+
             |           |
          APPROVE       REVISE
                         |
                         v
                    Test Agent
```

The Critic first performs local structural checks.

It checks for:

* Java class declaration
* `@Test`
* Meaningful assertion or `verify()`
* Balanced braces

After that, the Critic performs a semantic review using the configured AI service.

If the result needs improvement, the feedback is sent back to the Test Agent.

The current implementation allows one revision.

---

# 7. Important Boundary

The current Critic does **not** compile the generated test.

Therefore, CodeSentinel does not claim that an approved generated test is guaranteed to compile.

The current flow is:

```text
Generate
   ↓
Structural Check
   ↓
Semantic Review
   ↓
Approve / Revise
```

Automatic compilation can be added later.

---

# 8. Memory

The `MemoryStore` keeps a small local history.

Example:

```text
memory/
    SampleVulnerableService.log
```

The stored information includes:

```text
Timestamp
Severity
Line
Category
Description
```

The source code itself is not stored by the memory component.

---

## Recurring Findings

A finding is considered recurring when the same:

```text
Category + Description
```

appears in both the previous and current review.

The line number is intentionally ignored.

For example:

```text
Previous review:
Line 20 - Security - hardcoded password

Current review:
Line 35 - Security - hardcoded password
```

The issue can still be identified as the same finding signature.

The report describes this as:

> Detected again in the current review.

It does not claim that the system has proven the issue was never fixed.

---

# 9. Safe Mode

If `ANTHROPIC_API_KEY` is not configured, CodeSentinel enters Safe Mode.

```text
Java Source
     |
     v
Static Analyzer
     |
     v
Findings
     |
     v
Memory
     |
     v
Report
```

No AI request is made.

This allows the deterministic part of the review to work without an external AI service.

---

# 10. Failure Handling

A specialist failure is treated as a warning.

```text
Specialist Agent
       |
       X
    Exception
       |
       v
Coordinator
       |
       v
Warning
       |
       v
Other agents continue
```

The warning is stored in `SquadResult` and included in the final report.

This prevents one specialist from unnecessarily stopping the complete review.

---

# 11. Report

`ReportGenerator` creates:

```text
output/
    review.md
    <ClassName>Test.java
    <ClassName>.java
```

Depending on which agents run, not every generated file will necessarily be present.

The review report contains:

* Summary
* Severity counts
* Findings
* Recurring findings
* Dispatched agents
* Skipped agents
* Agent output
* Critic transcript
* Warnings

---

# 12. Safe Separation of Responsibilities

The main classes have simple responsibilities.

```text
StaticAnalyzer
    |
    +--> Finds issues

CoordinatorAgent
    |
    +--> Decides who should work

Specialist Agents
    |
    +--> Perform focused review

CriticAgent
    |
    +--> Reviews generated tests

MemoryStore
    |
    +--> Remembers previous findings

ReportGenerator
    |
    +--> Presents the result
```

This keeps the project easy to understand and test.

---

# 13. Design Principles

### Evidence before AI

Deterministic analysis happens first.

### One responsibility per agent

Each specialist has a clear purpose.

### Selective routing

Agents are only dispatched when relevant evidence exists.

### Review generated work

The Test Agent's output receives a Critic review.

### Remember previous reviews

Recurring finding signatures are surfaced.

### Fail gracefully

One specialist failure does not stop the complete review.

### Keep the system small

The project avoids infrastructure that does not add value to the core idea.

---

# 14. Current Limitations

The current prototype has some known boundaries:

* Analysis is pattern based
* One Java source file is reviewed at a time
* Generated tests are not automatically compiled
* AI mode requires network access
* The Critic uses the configured Claude service
* Architecture checks are heuristic
* The system is not a replacement for full enterprise code-quality tools

These limitations are intentionally documented.

---

# 15. Future Extensions

Possible future improvements include:

```text
AST-based analysis
       ↓
Multi-file project review
       ↓
Generated test compilation
       ↓
Pull-request integration
       ↓
CI/CD integration
       ↓
Persistent review history
```

Another possible improvement would be to use a separate model or service for the Critic so that test generation and test review are more independent.

---

# 16. Core Architecture

The complete design can be summarized as:

```text
          EVIDENCE
             |
             v
          ROUTING
             |
             v
        SPECIALISTS
             |
             v
          CRITIQUE
             |
             v
           REPORT
             |
             v
          MEMORY
```

The central idea is:

> **Use deterministic evidence to decide where AI reasoning is useful, give each agent a clear responsibility, review generated work, and keep a small history of previous findings.**


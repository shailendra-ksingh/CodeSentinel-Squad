# CodeSentinel Squad

## Multi-Agent Code Review Assistant for Java/JEE

### 1. Problem

Code reviews involve more than finding syntax or compilation errors.

A reviewer may need to look at:

- Security
- Error handling
- Resource management
- Unit testing
- Documentation
- Architecture

These areas require different kinds of review.

Another practical problem is that the same issue can appear again in a later review. Without some form of review history, that context is easily lost.

CodeSentinel Squad was designed as a small multi-agent solution for this problem.

---

# 2. Solution

CodeSentinel uses a simple workflow:

```text
Analyze
   ↓
Route
   ↓
Specialize
   ↓
Critique
   ↓
Improve
   ↓
Remember
````

The system first performs deterministic analysis on the Java source.

The resulting findings are then used as evidence by the Coordinator.

The Coordinator decides which specialist agents are relevant.

The specialists perform focused review tasks.

Generated tests receive an additional Critic review.

Finally, the findings are stored locally so that a later review can identify recurring issue signatures.

---

# 3. Architecture

```text
                    Java Source
                         |
                         v
                +------------------+
                | Static Analyzer  |
                +--------+---------+
                         |
                         v
                  Finding Evidence
                         |
                         v
                +------------------+
                |   Coordinator    |
                +--------+---------+
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
      Security         Testing        Docs
        Agent           Agent         Agent
                          |
                          v
                       Critic
                      /      \
                     /        \
                APPROVE       REVISE
                                 |
                                 v
                            Test Agent

                         |
                         v
                    Architecture
                       Agent

                         |
                         v
                  Review Report
                         |
                         v
                      Memory
```

The main design point is that the **Static Analyzer provides evidence before the AI agents are called**.

---

# 4. Static Analyzer

The Static Analyzer is the first stage.

It performs a lightweight, deterministic scan of the Java source.

Examples of checks include:

* Hardcoded credentials
* SQL string concatenation
* Broad exception handling
* Possible resource leaks
* Possible null-safety problems
* Missing documentation
* Public mutable fields
* Possible transaction-related issues
* Large classes with many public methods

The analyzer is intentionally simple and pattern based.

It is not intended to replace tools such as SonarQube, SpotBugs or PMD.

Its main purpose is to provide:

1. A fast first-pass review
2. Evidence for specialist routing
3. A useful offline fallback

---

# 5. Coordinator Agent

The Coordinator is responsible for routing the work.

It does not try to perform every review task itself.

Instead, it checks the findings and asks each specialist whether it should run.

For example:

```text
Security finding
      ↓
Security Agent

Documentation finding
      ↓
Documentation Agent

Architecture finding
      ↓
Architecture Agent

Public method
+
testable finding
      ↓
Test Agent
```

This means the system does not automatically run every agent for every Java file.

The decision is recorded in the review result.

---

# 6. Specialist Agents

Each specialist has a focused responsibility.

## Security Agent

Handles security-related findings.

It can explain:

* What the problem is
* Why it matters
* A possible risk scenario
* A suggested remediation

---

## Test Agent

Generates JUnit 5 tests for findings that can reasonably be tested.

The Test Agent is selectively dispatched.

It requires:

```text
Public method
+
Relevant testable finding
```

The purpose is to avoid generating tests simply because a Java class exists.

---

## Documentation Agent

Handles documentation-related findings.

It can add:

* Javadoc
* A simple explanation of the class

It runs only when the analyzer reports a documentation issue.

---

## Architecture Agent

Handles design-level findings.

For example, the current analyzer can identify a class that has:

* Many public methods
* A large number of lines

These are treated as signals that the class may contain too many responsibilities.

The Architecture Agent can then provide a higher-level review.

---

# 7. Critic Agent

The Critic is an important part of the Test Agent workflow.

Instead of accepting generated tests immediately, CodeSentinel gives them another review step.

The flow is:

```text
Test Agent
    |
    v
Generated Test
    |
    v
Critic
    |
    +---- APPROVED
    |
    +---- REVISE
              |
              v
         Test Agent
```

The Critic performs local checks first.

For example:

* Is there a Java class?
* Is there an `@Test` method?
* Is there a meaningful assertion?
* Are braces balanced?

It then performs a semantic review using the configured AI service.

If the Critic asks for a revision, its feedback is passed back to the Test Agent.

The current prototype allows one revision.

---

# 8. Why the Critic Matters

A generated test can look correct without actually checking anything.

For example:

```java
@Test
void testSomething() {
    service.doSomething();
}
```

This executes code but does not verify the result.

A better test would contain an assertion:

```java
@Test
void testSomething() {
    assertEquals("OK", service.doSomething());
}
```

The Critic therefore gives the generated test another check instead of treating the first generated answer as final.

The current prototype does not compile generated tests automatically.

Therefore, the Critic should be viewed as a **review step**, not as proof that the generated test is production-ready.

---

# 9. Memory

CodeSentinel keeps a small local history under:

```text
memory/
```

The memory contains finding information such as:

```text
timestamp
severity
line
category
description
```

The source code itself is not stored by the memory component.

For recurrence, CodeSentinel compares:

```text
category + description
```

The line number is deliberately ignored.

This is because the same problem can move from one line to another after a code change.

Example:

```text
Review 1

Security
hardcoded password


Review 2

Security
hardcoded password
```

The second review can report that the same issue signature was detected again.

---

# 10. Safe Mode

AI is useful, but the complete application should not become unusable when the AI service is unavailable.

CodeSentinel therefore has a Safe Mode.

If the Anthropic API key is not configured:

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
Report + Memory
```

The deterministic analysis still works.

This also allows the basic project to be demonstrated without an external AI service.

---

# 11. Failure Handling

A specialist failure should not stop the complete review.

For example:

```text
Security Agent
      |
      X
    Failure
      |
      v
Warning recorded
      |
      v
Other specialists continue
      |
      v
Final report
```

The Coordinator records the problem as a warning.

The final report makes the warning visible to the developer.

This is better than silently ignoring the failure or stopping the complete review.

---

# 12. Report

The final report brings the different parts together.

It includes:

* Total findings
* Severity counts
* Recurring findings
* Dispatched agents
* Skipped agents
* Specialist output
* Critic transcript
* Warnings

The idea is to make the review understandable to a developer rather than returning only a raw AI response.

---

# 13. What Is Different About the Approach?

The main idea is not simply "use AI for code review."

The workflow is:

```text
Evidence
   ↓
Decision
   ↓
Specialized Reasoning
   ↓
Critique
   ↓
Revision
   ↓
History
```

There are a few important design choices behind it.

### Evidence before AI

The deterministic analyzer provides the initial evidence.

### Clear agent responsibilities

Each specialist has a defined role.

### Selective routing

Agents run when the evidence makes them relevant.

### Generation followed by review

The Test Agent's output is reviewed by the Critic.

### Review history

Previous findings can be compared with the current review.

### Safe fallback

Static analysis continues to work when AI is unavailable.

### Graceful failures

One specialist failure does not have to stop the complete review.

---

# 14. Why Keep It Small?

The project deliberately avoids adding infrastructure just for the sake of making the architecture look bigger.

The current version does not require:

* Kafka
* Kubernetes
* A separate database
* A web UI
* A large AI framework
* Multiple external services

The focus is on demonstrating the multi-agent workflow clearly.

A smaller system also makes it easier to understand, test and demonstrate.

---

# 15. Example Demo

A simple competition demonstration can follow these steps.

### Step 1 — Review a vulnerable service

```bash
java -jar target/codesentinel.jar tests/SampleVulnerableService.java
```

Show:

```text
Static findings
      ↓
Coordinator
      ↓
Selected agents
```

### Step 2 — Show test generation

Show the Test Agent output.

### Step 3 — Show the Critic

Show:

```text
Test draft
    ↓
Critic
    ↓
APPROVE / REVISE
```

### Step 4 — Show the report

Open:

```text
output/review.md
```

### Step 5 — Run the same review again

Show that recurring findings are identified.

### Step 6 — Review the God Class sample

```bash
java -jar target/codesentinel.jar tests/SampleGodClassService.java
```

Show that different findings can result in different agent routing.

---

# 16. Current Limitations

The current version is a prototype.

Important limitations are intentionally visible:

* Static analysis is pattern based
* One Java file is reviewed at a time
* Generated tests are not automatically compiled
* AI mode requires network access
* The semantic Critic currently uses the configured Claude service
* Architecture findings are heuristic
* The system does not replace a full enterprise code-quality platform

These limitations also define the main areas for future work.

---

# 17. Possible Future Improvements

Future versions could add:

* Automatic compilation of generated tests
* Multi-file project analysis
* Pull-request review
* CI/CD integration
* SonarQube or SpotBugs integration
* Persistent review history
* Independent AI model for Critic review
* More precise AST-based analysis

These are extensions to the current design rather than requirements for the prototype.

---

# 18. One-Line Summary

> **CodeSentinel Squad uses evidence-driven routing, focused specialist agents, test critique, and review memory to make Java code review more structured and repeatable.**

---

# 19. Core Message

The project can be explained simply:

> **Don't ask one AI to do everything. Give each agent a clear job, let another agent review the result, and remember what happened before.**

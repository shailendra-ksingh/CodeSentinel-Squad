# CodeSentinel Squad - Design Notes

This document explains the main design decisions behind CodeSentinel and why the solution is structured this way.

## 1. Start with evidence

CodeSentinel does not send every file directly to the AI.

It first performs local analysis to identify useful evidence such as security issues, code smells, resource problems and other findings.

This gives the AI a better starting point and avoids using AI for checks that can be done deterministically.

**Design idea:**

```text
Source Code
     ↓
Local Evidence
     ↓
Focused AI Review
````

---

## 2. Use the right specialist

Instead of asking one AI agent to do everything, CodeSentinel uses focused agents.

For example:

* Security Agent → security-related findings
* Test Generation Agent → test suggestions
* Documentation Agent → documentation improvements
* Architecture Agent → design and responsibility issues
* Critic Agent → checks generated test recommendations
* Project Integration Agent → checks relationships between files

The Coordinator decides which agents are actually needed.

This keeps the review focused and easier to understand.

---

## 3. Review the system, not only the file

A single file can look correct while still having a problem with another file.

For example:

```text
TypeScript Client
       ↓
    Java API
       ↓
  Service Layer
       ↓
   Database
```

CodeSentinel can review these relationships when multiple files are provided.

This helps identify problems such as:

* API and DTO mismatches
* inconsistent error handling
* duplicated business rules
* configuration mismatches
* missing integration tests

---

## 4. Keep AI output under control

AI-generated output is treated as a recommendation, not as an automatic decision.

For generated tests, CodeSentinel performs local checks and uses the Critic Agent to review the result.

The final quality gate brings the findings together and highlights areas that need attention.

The developer or reviewer remains responsible for the final decision.

---

## 5. Protect sensitive information

Source code may contain passwords, API keys or other credential-like values.

`SecretRedactor` runs before source is included in an AI request.

For example:

```text
Before:
apiKey = "real-secret-value"

After:
apiKey = "[REDACTED]"
```

The intention is simple:

> Keep the useful code context, but do not send the original secret-like value to the AI service.

---

## 6. Remember recurring findings

CodeSentinel keeps lightweight information about previous findings.

It does not need to store the complete source code to identify recurring problems.

For example:

```text
Review 1 → SQL concatenation
Review 2 → SQL concatenation
Review 3 → SQL concatenation
                 ↓
          Recurring finding
```

This gives the review some continuity instead of treating every run as completely independent.

---

## 7. Work even without AI

AI is optional.

Without an API key, CodeSentinel can still perform its local analysis and generate reports.

```text
Source
  ↓
Local Analysis
  ↓
Findings
  ↓
Report
```

When AI is enabled, the review is extended with specialist reasoning and recommendations.

This makes the tool more practical for restricted environments.

---

## 8. Keep failures isolated

One specialist should not prevent the rest of the review from completing.

For example:

```text
Security Agent       ✓
Test Agent           ✓
Architecture Agent   ✓
Documentation Agent  ✗
                       ↓
                 Partial results
```

The failure is reported, while the successful results are still retained.

This is important for a tool intended for real development workflows.

---

## 9. Keep the design easy to extend

The core design separates detection, coordination, specialist analysis and reporting.

This makes it possible to add:

* another programming language
* another specialist agent
* another AI provider
* additional static checks
* new report formats

without changing the complete review flow.

For example:

```text
LlmClient
    ↑
ClaudeClient

LanguageDetector
    ↓
Language-specific Analyzer
```

The interfaces keep the main design independent of one specific implementation.

---

## 10. Final design principle

The main idea behind CodeSentinel is simple:

> **Use deterministic checks to find the evidence, use focused AI agents to understand it, and keep the final decision with the developer.**

The goal is not to replace code review. It is to make the first review faster, more structured and easier to act on.

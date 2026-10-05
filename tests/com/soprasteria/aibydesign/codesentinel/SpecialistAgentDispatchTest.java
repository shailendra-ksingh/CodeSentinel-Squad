package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the routing decisions made by the specialist agents.
 *
 * These tests do not call the live Claude API, so they can run
 * without ANTHROPIC_API_KEY.
 */
class SpecialistAgentDispatchTest {

    @Test
    void securityAgentOnlyRunsWhenSecurityFindingPresent() {

        SecurityAgent agent = new SecurityAgent();

        Finding securityFinding =
                new Finding(
                        Finding.Severity.CRITICAL,
                        1,
                        "Security",
                        "hardcoded secret");

        Finding otherFinding =
                new Finding(
                        Finding.Severity.MINOR,
                        1,
                        "Documentation",
                        "missing javadoc");

        assertTrue(
                agent.shouldRun(
                        "class X {}",
                        List.of(securityFinding)));

        assertFalse(
                agent.shouldRun(
                        "class X {}",
                        List.of(otherFinding)));
    }

    @Test
    void testAgentRunsWhenPublicMethodAndTestableFindingExist() {

        TestAgent agent = new TestAgent();

        Finding securityFinding =
                new Finding(
                        Finding.Severity.CRITICAL,
                        5,
                        "Security",
                        "hardcoded password");

        assertTrue(
                agent.shouldRun(
                        "public void foo() {}",
                        List.of(securityFinding)));
    }

    @Test
    void testAgentDoesNotRunWithoutTestableFinding() {

        TestAgent agent = new TestAgent();

        assertFalse(
                agent.shouldRun(
                        "public void foo() {}",
                        List.of()));
    }

    @Test
    void testAgentDoesNotRunForPrivateMethod() {

        TestAgent agent = new TestAgent();

        Finding securityFinding =
                new Finding(
                        Finding.Severity.CRITICAL,
                        5,
                        "Security",
                        "hardcoded password");

        assertFalse(
                agent.shouldRun(
                        "private void foo() {}",
                        List.of(securityFinding)));
    }

    @Test
    void documentationAgentOnlyRunsForDocumentationFinding() {

        DocsAgent agent = new DocsAgent();

        Finding documentationFinding =
                new Finding(
                        Finding.Severity.MINOR,
                        10,
                        "Documentation",
                        "missing javadoc");

        Finding securityFinding =
                new Finding(
                        Finding.Severity.CRITICAL,
                        5,
                        "Security",
                        "hardcoded password");

        assertTrue(
                agent.shouldRun(
                        "public void foo() {}",
                        List.of(documentationFinding)));

        assertFalse(
                agent.shouldRun(
                        "public void foo() {}",
                        List.of(securityFinding)));

        assertFalse(
                agent.shouldRun(
                        "public void foo() {}",
                        List.of()));
    }

    @Test
    void architectureAgentOnlyRunsWhenArchitectureFindingPresent() {

        ArchitectureAgent agent =
                new ArchitectureAgent();

        Finding architectureFinding =
                new Finding(
                        Finding.Severity.MAJOR,
                        1,
                        "Architecture",
                        "God class");

        Finding otherFinding =
                new Finding(
                        Finding.Severity.MINOR,
                        1,
                        "Documentation",
                        "missing javadoc");

        assertTrue(
                agent.shouldRun(
                        "class X {}",
                        List.of(architectureFinding)));

        assertFalse(
                agent.shouldRun(
                        "class X {}",
                        List.of(otherFinding)));
    }

    @Test
    void criticRejectsEmptyOutput() {

        CriticAgent critic =
                new CriticAgent();

        assertFalse(
                critic.structuralCheck("")
                        .approved);
    }

    @Test
    void criticRejectsOutputWithNoTestMethods() {

        CriticAgent critic =
                new CriticAgent();

        String noTests =
                "class FooTest { void helper() {} }";

        assertFalse(
                critic.structuralCheck(noTests)
                        .approved);
    }

    @Test
    void criticRejectsTestWithoutAssertion() {

        CriticAgent critic =
                new CriticAgent();

        String noAssertion =
                """
                class FooTest {
                    @Test
                    void testSomething() {
                        System.out.println("test");
                    }
                }
                """;

        assertFalse(
                critic.structuralCheck(noAssertion)
                        .approved);
    }

    @Test
    void criticRejectsUnbalancedBraces() {

        CriticAgent critic =
                new CriticAgent();

        String broken =
                "class FooTest { "
                        + "@Test void test() { "
                        + "assertTrue(true); "
                        + "}";

        assertFalse(
                critic.structuralCheck(broken)
                        .approved);
    }

    @Test
    void criticApprovesWellFormedTestClass() {

        CriticAgent critic =
                new CriticAgent();

        String validTest =
                """
                class FooTest {

                    @Test
                    void testSomething() {
                        assertTrue(true);
                    }
                }
                """;

        assertTrue(
                critic.structuralCheck(validTest)
                        .approved);
    }
}
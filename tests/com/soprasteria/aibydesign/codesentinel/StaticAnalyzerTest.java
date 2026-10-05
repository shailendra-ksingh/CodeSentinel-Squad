package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the offline static analysis pass. */
class StaticAnalyzerTest {

    private static boolean has(List<Finding> findings, String category) {
        return findings.stream().anyMatch(f -> f.getCategory().equals(category));
    }

    @Test
    void detectsKnownIssuesInSampleFile() throws Exception {
        String source = Files.readString(Path.of("tests/samples/SampleVulnerableService.java"));
        List<Finding> findings = new StaticAnalyzer().analyze(source);

        assertTrue(has(findings, "Security"), "hardcoded secret / SQL concatenation");
        assertTrue(has(findings, "Error Handling"), "broad catch");
        assertTrue(has(findings, "Resource Leak"), "unclosed FileInputStream");
        assertTrue(has(findings, "Null Safety"), "chained getName().toUpperCase()");
        assertTrue(has(findings, "Documentation"), "missing Javadoc");
        assertTrue(has(findings, "Encapsulation"), "public non-final field with initializer");
        assertTrue(has(findings, "JEE Anti-pattern"), "missing @Transactional");
    }

    @Test
    void cleanCodeProducesNoFindings() {
        String clean = """
                /** A clean class. */
                public class CleanExample {
                    /** Adds two numbers. */
                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """;
        assertTrue(new StaticAnalyzer().analyze(clean).isEmpty());
    }

    @Test
    void longJavadocIsStillRecognised() {
        String code = """
                public class Doc {
                    /**
                     * A deliberately long description that goes well beyond one hundred
                     * and twenty characters so that a naive "look back N characters"
                     * check would miss the opening of the comment entirely.
                     *
                     * @param a first
                     * @return a
                     */
                    @Deprecated
                    public int value(int a) { return a; }
                }
                """;
        assertFalse(has(new StaticAnalyzer().analyze(code), "Documentation"));
    }

    @Test
    void staticPublicMethodWithoutJavadocIsFlagged() {
        String code = "public class S {\n    public static void run() { }\n}\n";
        assertTrue(has(new StaticAnalyzer().analyze(code), "Documentation"));
    }

    @Test
    void commentsAndPlainStringsAreNotFlagged() {
        String code = """
                /** Doc. */
                public class C {
                    // password = "not-real" in a comment
                    /** Doc. */
                    public String note() { return "update the value + 1"; }
                }
                """;
        assertTrue(new StaticAnalyzer().analyze(code).isEmpty());
    }

    @Test
    void tryWithResourcesElsewhereDoesNotHideLeak() {
        String code = """
                /** Doc. */
                public class R {
                    /** Doc. */
                    public void a() throws Exception {
                        try (var in = new FileInputStream("a")) { in.read(); }
                    }
                    /** Doc. */
                    public void b() throws Exception {
                        int x = 1;
                        int y = 2;
                        int z = 3;
                        int w = 4;
                        var in = new FileInputStream("b");
                    }
                }
                """;
        List<Finding> findings = new StaticAnalyzer().analyze(code);
        assertEquals(1, findings.stream().filter(f -> f.getCategory().equals("Resource Leak")).count());
    }

    @Test
    void godClassIsFlaggedAsArchitecture() throws Exception {
        String source = Files.readString(Path.of("tests/samples/SampleGodClassService.java"));
        assertTrue(has(new StaticAnalyzer().analyze(source), "Architecture"));
    }
}

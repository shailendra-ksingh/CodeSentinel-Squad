import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the offline static analysis pass correctly flags the known,
 * deliberately-planted issues in SampleVulnerableService.java.
 */
class StaticAnalyzerTest {

    @Test
    void detectsKnownIssuesInSampleFile() throws Exception {
        String source = Files.readString(Path.of("tests/SampleVulnerableService.java"));
        List<Finding> findings = new StaticAnalyzer().analyze(source);

        assertTrue(findings.stream().anyMatch(f -> f.getCategory().equals("Security")),
                "Expected at least one Security finding (hardcoded password / SQL injection)");

        assertTrue(findings.stream().anyMatch(f -> f.getCategory().equals("Error Handling")),
                "Expected the broad catch(Exception e) to be flagged");

        assertTrue(findings.stream().anyMatch(f -> f.getCategory().equals("Resource Leak")),
                "Expected the unclosed FileInputStream to be flagged");

        assertTrue(findings.stream().anyMatch(f -> f.getCategory().equals("Null Safety")),
                "Expected the chained getName().toUpperCase() call to be flagged");

        assertTrue(findings.stream().anyMatch(f -> f.getCategory().equals("Documentation")),
                "Expected at least one public method with missing Javadoc to be flagged");
    }

    @Test
    void cleanCodeProducesNoFindings() {
        String cleanCode = """
                /**
                 * A trivial, clean class with no issues.
                 */
                public class CleanExample {
                    /**
                     * Adds two numbers.
                     */
                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """;
        List<Finding> findings = new StaticAnalyzer().analyze(cleanCode);
        assertTrue(findings.isEmpty(), "Expected no findings on clean, documented code, but got: " + findings);
    }
}

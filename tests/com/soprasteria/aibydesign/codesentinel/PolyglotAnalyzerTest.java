package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PolyglotAnalyzerTest {
    private final PolyglotAnalyzer analyzer = new PolyglotAnalyzer();

    @Test void pythonSecurityAndExceptionSignals() {
        List<Finding> f = analyzer.analyze("password = 'fake-secret'\neval(user_input)\nexcept:\n", Language.PYTHON);
        assertTrue(f.stream().anyMatch(x -> x.getCategory().equals("Security")));
        assertTrue(f.stream().anyMatch(x -> x.getCategory().equals("Error Handling")));
    }

    @Test void typescriptFindsAnyAndXssSink() {
        List<Finding> f = analyzer.analyze("let value: any; element.innerHTML = value;", Language.TYPESCRIPT);
        assertTrue(f.stream().anyMatch(x -> x.getCategory().equals("Type Safety")));
        assertTrue(f.stream().anyMatch(x -> x.getCategory().equals("Security")));
    }

    @Test void sqlAndYamlSecuritySignals() {
        assertFalse(analyzer.analyze("SELECT * FROM users", Language.SQL).isEmpty());
        assertTrue(analyzer.analyze("privileged: true", Language.YAML).stream()
                .anyMatch(x -> x.getSeverity() == Finding.Severity.CRITICAL));
    }
}

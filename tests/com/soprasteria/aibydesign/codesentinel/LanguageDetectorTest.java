package com.soprasteria.aibydesign.codesentinel;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LanguageDetectorTest {
    @Test void detectsCommonPolyglotStack() {
        assertEquals(Language.JAVA, LanguageDetector.detect(Path.of("OrderService.java")));
        assertEquals(Language.PYTHON, LanguageDetector.detect(Path.of("risk.py")));
        assertEquals(Language.TYPESCRIPT, LanguageDetector.detect(Path.of("OrderApi.tsx")));
        assertEquals(Language.SQL, LanguageDetector.detect(Path.of("schema.sql")));
        assertEquals(Language.YAML, LanguageDetector.detect(Path.of("deployment.yaml")));
        assertEquals(Language.JSON, LanguageDetector.detect(Path.of("package.json")));
    }
}

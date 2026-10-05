package com.soprasteria.aibydesign.codesentinel;

import java.nio.file.Path;
import java.util.Locale;

/** Small deterministic language detector; extension is preferred over content guessing. */
public final class LanguageDetector {
    private LanguageDetector() {}

    public static Language detect(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".java")) return Language.JAVA;
        if (name.endsWith(".py")) return Language.PYTHON;
        if (name.endsWith(".js") || name.endsWith(".jsx")) return Language.JAVASCRIPT;
        if (name.endsWith(".ts") || name.endsWith(".tsx")) return Language.TYPESCRIPT;
        if (name.endsWith(".sql")) return Language.SQL;
        if (name.endsWith(".yml") || name.endsWith(".yaml")) return Language.YAML;
        if (name.endsWith(".json")) return Language.JSON;
        return Language.UNKNOWN;
    }
}

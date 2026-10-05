package com.soprasteria.aibydesign.codesentinel;

/** Supported source/configuration languages. Unknown files are still visible to the project layer. */
public enum Language {
    JAVA("Java"), PYTHON("Python"), JAVASCRIPT("JavaScript"), TYPESCRIPT("TypeScript"),
    SQL("SQL"), YAML("YAML"), JSON("JSON"), UNKNOWN("Unknown");

    private final String displayName;
    Language(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
}

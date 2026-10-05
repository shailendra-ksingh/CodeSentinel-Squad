package com.soprasteria.aibydesign.codesentinel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small helpers for cleaning model output. */
final class TextUtil {

    private static final Pattern FENCE =
            Pattern.compile("(?s)^\\s*```[a-zA-Z]*\\R(.*?)\\R?```\\s*$");

    private TextUtil() { }

    /**
     * Removes a surrounding Markdown code fence if the model added one,
     * even though it was asked not to.
     */
    static String stripCodeFences(String text) {
        if (text == null) {
            return "";
        }
        Matcher m = FENCE.matcher(text);
        return m.matches() ? m.group(1).trim() : text.trim();
    }
}

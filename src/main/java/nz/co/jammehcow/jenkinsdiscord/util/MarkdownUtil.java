package nz.co.jammehcow.jenkinsdiscord.util;

import java.util.Objects;

public final class MarkdownUtil {
    private MarkdownUtil() {
    }

    /**
     * From JDA's MarkdownSanitizer
     * <p>
     * Escapes every single markdown formatting token found in the provided string.
     * <br>Example: {@code escape("**Hello _World_", true)}
     * <p>
     * This code is licensed under the Apache-2.0 license:
     * <pre>
     * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *    https://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     * </pre>
     *
     * @param sequence The string to sanitize
     * @return The string with escaped markdown
     * @throws NullPointerException If provided with a null sequence
     */
    public static String escape(String sequence) {
        Objects.requireNonNull(sequence);
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        boolean newline = true;
        for (int i = 0; i < sequence.length(); i++) {
            char current = sequence.charAt(i);
            if (newline) {
                newline = Character.isWhitespace(current); // might still be a quote if prefixed by whitespace
                if (current == '>') {
                    // Check for quote if line starts with angle bracket
                    if (i + 1 < sequence.length() && Character.isWhitespace(sequence.charAt(i + 1))) {
                        builder.append("\\>"); // simple quote
                    } else if (i + 3 < sequence.length() && sequence.startsWith(">>>", i) &&
                               Character.isWhitespace(sequence.charAt(i + 3))) {
                        builder.append("\\>\\>\\>").append(sequence.charAt(i + 3)); // block quote
                        i += 3; // since we include 3 angle brackets AND whitespace
                    } else {
                        builder.append(current); // just a normal angle bracket
                    }
                    continue;
                }
            }

            if (escaped) {
                builder.append(current);
                escaped = false;
                continue;
            }
            // Handle average case
            switch (current) {
                case '*': // simple markdown escapes for single characters
                case '_':
                case '`':
                    builder.append('\\').append(current);
                    break;
                case '|': // cases that require at least 2 characters in sequence
                case '~':
                    if (i + 1 < sequence.length() && sequence.charAt(i + 1) == current) {
                        builder.append('\\').append(current)
                               .append('\\').append(current);
                        i++;
                    } else
                        builder.append(current);
                    break;
                case '\\': // escape character
                    builder.append(current);
                    escaped = true;
                    break;
                case '\n': // linefeed is a special case for quotes
                    builder.append(current);
                    newline = true;
                    break;
                default:
                    builder.append(current);
            }
        }
        return builder.toString();
    }

    public static String formatMarkdownUrl(String text, String url) {
        return String.format("[%s](%s)", text, url.replaceAll("\\)", "\\\\\\)"));
    }
}

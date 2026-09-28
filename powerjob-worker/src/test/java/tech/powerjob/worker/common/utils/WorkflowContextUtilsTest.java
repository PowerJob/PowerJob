package tech.powerjob.worker.common.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowContextUtilsTest {

    @Test
    void absentContextDoesNotExceedTheLimit() {
        assertFalse(WorkflowContextUtils.isExceededLengthLimit(null, 8192));
        assertFalse(WorkflowContextUtils.isExceededLengthLimit(null, 0));
    }

    @Test
    void emptyContextStillCountsItsJsonBraces() {
        assertFalse(WorkflowContextUtils.isExceededLengthLimit(Collections.emptyMap(), 2));
        assertTrue(WorkflowContextUtils.isExceededLengthLimit(Collections.emptyMap(), 1));
    }

    @Test
    void acceptsContextAtTheJsonLengthLimit() {
        // {"key":"value"} is 15 characters, including JSON syntax.
        assertFalse(WorkflowContextUtils.isExceededLengthLimit(Collections.singletonMap("key", "value"), 15));
    }

    @Test
    void rejectsContextOneCharacterOverTheLimit() {
        assertTrue(WorkflowContextUtils.isExceededLengthLimit(Collections.singletonMap("key", "value"), 14));
    }

    @Test
    void checksBothSidesOfTheDefaultLimit() {
        char[] characters = new char[8184];
        Arrays.fill(characters, 'x');
        String value = new String(characters);
        // The JSON object adds eight characters to the value: {"k":"..."}.
        assertFalse(WorkflowContextUtils.isExceededLengthLimit(Collections.singletonMap("k", value.substring(1)), 8192));
        assertFalse(WorkflowContextUtils.isExceededLengthLimit(Collections.singletonMap("k", value), 8192));
        assertTrue(WorkflowContextUtils.isExceededLengthLimit(Collections.singletonMap("k", value + "x"), 8192));
    }

    @Test
    void countsUnicodeAsJavaStringCharacters() {
        // {"状态":"好😀"} has 12 UTF-16 code units, not 12 UTF-8 bytes.
        Map<String, String> context = Collections.singletonMap("状态", "好😀");
        assertFalse(WorkflowContextUtils.isExceededLengthLimit(context, 12));
        assertTrue(WorkflowContextUtils.isExceededLengthLimit(context, 11));
    }

    @Test
    void countsEscapedJsonCharacters() {
        Map<String, String> context = Collections.singletonMap("k", "\"\\\n");
        assertFalse(WorkflowContextUtils.isExceededLengthLimit(context, 14));
        assertTrue(WorkflowContextUtils.isExceededLengthLimit(context, 13));
    }

    @Test
    void rejectsContextThatCannotBeSerialized() {
        Map<String, String> context = new HashMap<>();
        context.put(null, "value");
        assertTrue(WorkflowContextUtils.isExceededLengthLimit(context, 8192));
    }
}

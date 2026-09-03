package nz.co.jammehcow.jenkinsdiscord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicTest {

    @Test
    void webhookClassDoesntThrow() {
        assertDoesNotThrow(() -> {
            DiscordWebhook wh = new DiscordWebhook("http://exampl.e");
            wh.setContent("content");
            wh.setDescription("desc");
            wh.setStatus(DiscordWebhook.StatusColor.GREEN);
            wh.send();
        });
    }

    @Test
    void pipelineDoesntThrow() {
        assertDoesNotThrow(() -> {
            DiscordPipelineStep step = new DiscordPipelineStep("http://exampl.e");
            step.setTitle("Test title");
            DiscordPipelineStep.DiscordPipelineStepExecution execution =
                    new DiscordPipelineStep.DiscordPipelineStepExecution();
            execution.step = step;
            execution.listener = () -> System.out;
            execution.run();
        });
    }

    @Test
    void parseColorHexWithHash() {
        assertEquals(0x19A959, DiscordWebhook.parseColor("#19A959"));
    }

    @Test
    void parseColorHexWithoutHash() {
        assertEquals(0x19A959, DiscordWebhook.parseColor("19A959"));
    }

    @Test
    void parseColorDecimal() {
        // 1681177 == 0x19A719 (GREEN default)
        assertEquals(1681177, DiscordWebhook.parseColor("1681177"));
    }

    @Test
    void parseColorHexDecimalEquivalent() {
        assertEquals(DiscordWebhook.parseColor("1681177"), DiscordWebhook.parseColor("#19A719"));
    }

    @Test
    void parseColorInvalidThrows() {
        NumberFormatException ex = assertThrows(NumberFormatException.class,
                () -> DiscordWebhook.parseColor("not-a-color"));
        // Verify message mentions the original input
        assertTrue(ex.getMessage().contains("not-a-color"));
    }

    @Test
    void parseColorSixDigitNumericWithoutHashInterpretedAsHex() {
        assertEquals(0x123456, DiscordWebhook.parseColor("123456"));
    }

    @Test
    void parseColorOutOfRangeThrows() {
        assertThrows(NumberFormatException.class, () -> DiscordWebhook.parseColor("16777216"));
    }

    @Test
    void parseColorInvalidWithHashThrows() {
        assertThrows(NumberFormatException.class, () -> DiscordWebhook.parseColor("#ZZZZZZ"));
    }

    @Test
    void setStatusByColorDoesntThrow() {
        assertDoesNotThrow(() -> {
            DiscordWebhook wh = new DiscordWebhook("http://exampl.e");
            wh.setStatusByColor(0xFF0000);
        });
    }

    @Test
    void statusColorGetCodeGreen() {
        assertEquals(1681177L, DiscordWebhook.StatusColor.GREEN.getCode());
    }

    @Test
    void statusColorGetCodeYellow() {
        assertEquals(16776970L, DiscordWebhook.StatusColor.YELLOW.getCode());
    }

    @Test
    void statusColorGetCodeRed() {
        assertEquals(11278871L, DiscordWebhook.StatusColor.RED.getCode());
    }

    @Test
    void statusColorGetCodeGrey() {
        assertEquals(13487565L, DiscordWebhook.StatusColor.GREY.getCode());
    }

    @Test
    void pipelineWithCustomColorsDoesntThrow() {
        assertDoesNotThrow(() -> {
            DiscordPipelineStep step = new DiscordPipelineStep("http://exampl.e");
            step.setTitle("Test title");
            step.setSuccessColor("#00FF00");
            step.setUnstableColor("#FFFF00");
            step.setFailureColor("#FF0000");
            step.setAbortedColor("#AAAAAA");
            DiscordPipelineStep.DiscordPipelineStepExecution execution =
                    new DiscordPipelineStep.DiscordPipelineStepExecution();
            execution.step = step;
            execution.listener = () -> System.out;
            execution.run();
        });
    }

    @Test
    void pipelineCustomColorGettersRoundTrip() {
        DiscordPipelineStep step = new DiscordPipelineStep("http://exampl.e");
        step.setSuccessColor("#00FF00");
        step.setUnstableColor("FFFF00");
        step.setFailureColor("16711680");
        step.setAbortedColor("#AAAAAA");
        assertEquals("#00FF00", step.getSuccessColor());
        assertEquals("FFFF00", step.getUnstableColor());
        assertEquals("16711680", step.getFailureColor());
        assertEquals("#AAAAAA", step.getAbortedColor());
    }
}

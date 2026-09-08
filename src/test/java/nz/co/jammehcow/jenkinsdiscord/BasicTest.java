package nz.co.jammehcow.jenkinsdiscord;

import nz.co.jammehcow.jenkinsdiscord.exception.WebhookException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicTest {

    @Test
    void webhookThrowsOnUnreachableHost() {
        DiscordWebhook wh = new DiscordWebhook("http://127.0.0.1:1");
        wh.setContent("content");
        wh.setDescription("desc");
        wh.setStatus(DiscordWebhook.StatusColor.GREEN);
        WebhookException e = assertThrows(WebhookException.class, wh::send);
        assertNotNull(e.getCause(), "network failure should preserve its cause");
    }

    @Test
    void pipelineDoesntThrow() {
        assertDoesNotThrow(() -> {
            DiscordPipelineStep step = new DiscordPipelineStep("http://127.0.0.1:1");
            step.setTitle("Test title");
            DiscordPipelineStep.DiscordPipelineStepExecution execution =
                    new DiscordPipelineStep.DiscordPipelineStepExecution();
            execution.step = step;
            execution.listener = () -> System.out;
            execution.run();
        });
    }
}

package nz.co.jammehcow.jenkinsdiscord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
}

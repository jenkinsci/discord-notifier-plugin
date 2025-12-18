package nz.co.jammehcow.jenkinsdiscord;

import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class BasicTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configRoundTrip() {
        try {
            DiscordPipelineStep step = new DiscordPipelineStep("http://exampl.e");
            step.setTitle("Test title");

            DiscordPipelineStep roundtrippedStep = new StepConfigTester(this.j).configRoundTrip(step);
            this.j.assertEqualDataBoundBeans(step, roundtrippedStep);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

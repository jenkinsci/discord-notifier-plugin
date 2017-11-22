package nz.co.jammehcow.jenkinsdiscord;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import nz.co.jammehcow.jenkinsdiscord.exception.WebhookException;
import nz.co.jammehcow.jenkinsdiscord.util.EmbedDescription;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class DiscordPipelineStep extends AbstractStepImpl {
    private final String webhookURL;

    private String title;
    private boolean titleWasSet;
    private String link;
    private boolean linkWasSet;
    private String description;
    private boolean descriptionWasSet;
    private String footer;
    private boolean footerWasSet;
    private boolean successful;
    private boolean successfulWasSet;

    @DataBoundConstructor
    public DiscordPipelineStep(String webhookURL) {
        this.webhookURL = webhookURL;
    }

    public String getWebhookURL() {
        return webhookURL;
    }

    public String getTitle() {
        return title;
    }

    @DataBoundSetter
    public void setTitle(String title) {
        this.title = title;
        titleWasSet = true;
    }

    public String getLink() {
        return link;
    }

    @DataBoundSetter
    public void setLink(String link) {
        this.link = link;
        linkWasSet = true;
    }

    public String getDescription() {
        return description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
        descriptionWasSet = true;
    }

    public String getFooter() {
        return footer;
    }

    @DataBoundSetter
    public void setFooter(String footer) {
        this.footer = footer;
        footerWasSet = true;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @DataBoundSetter
    public void setSuccessful(boolean successful) {
        this.successful = successful;
        successfulWasSet = true;
    }

    public static class DiscordPipelineStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        @Inject
        transient DiscordPipelineStep step;
        
        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Run build;

        @Override
        protected Void run() throws Exception {
            listener.getLogger().println("Sending notification to Discord.");
            
            JenkinsLocationConfiguration globalConfig = new JenkinsLocationConfiguration();

            DiscordWebhook wh = new DiscordWebhook(step.getWebhookURL());
            
            if (step.titleWasSet) {
                wh.setTitle(step.getTitle());
            } else {
                wh.setTitle(build.getParent().getDisplayName() + " #" + build.getId());
            }

            if (step.linkWasSet) {
                wh.setURL(step.getLink());
            } else if (StringUtils.isNotEmpty(globalConfig.getUrl())) {
                wh.setURL(globalConfig.getUrl() + build.getUrl());
            } else {
                listener.getLogger().println("Your Jenkins URL is not set (or is set to localhost)! Disabling linking.");
            }
            
            String resultStr;
            if (step.successfulWasSet) {
                resultStr = step.isSuccessful() ? "success" : "failure";
                wh.setStatus(step.isSuccessful());
            } else {
                Result result = build.getResult(); // Always returns null. Why?
                resultStr = result != null ? result.toString().toLowerCase() : "null";
                wh.setStatus(result != null && result.isBetterOrEqualTo(Result.SUCCESS));
            }
            
            if (step.descriptionWasSet) {
                wh.setDescription(step.getDescription());
            } else {
                wh.setDescription(new EmbedDescription(build, globalConfig,
                    "**Status:** " + resultStr, false).toString());
            }
            
            if (step.footerWasSet) {
                wh.setFooter(step.getFooter());
            } else {
                wh.setFooter("Jenkins ver. " + Jenkins.getVersion() + ", "
                    + WebhookPublisher.NAME + " ver. " + WebhookPublisher.VERSION);
            }
            
            try { wh.send(); }
            catch (WebhookException e) { e.printStackTrace(); }

            return null;
        }
        
        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() { super(DiscordPipelineStepExecution.class); }

        @Override
        public String getFunctionName() {
            return "discordSend";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Send an embed message to Webhook URL";
        }
    }
}

package nz.co.jammehcow.jenkinsdiscord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.JenkinsLocationConfiguration;
import nz.co.jammehcow.jenkinsdiscord.util.EmbedUtil;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;


public class DiscordPipelineStep extends AbstractStepImpl {
    private final String webhookURL;

    private String title = null;
    private String link = null;
    private String description = null;
    private String footer = null;
    private String image = null;
    private String thumbnail = null;
    private String result = null;
    private String notes = null;
    private String customAvatarUrl = null;
    private String customUsername = null;
    private String customFile = null;
    private DynamicFieldContainer dynamicFieldContainer = null;
    private boolean successful = false;
    private boolean unstable = false;
    private boolean enableArtifactsList = false;
    private boolean prettyArtifactsList = false;
    private boolean showChangeset = false;
    private String scmWebUrl = null;

    @DataBoundConstructor
    public DiscordPipelineStep(String webhookURL) {
        this.webhookURL = webhookURL;
    }

    public String getWebhookURL() {
        return this.webhookURL;
    }

    public String getTitle() {
        return this.title;
    }

    @DataBoundSetter
    public void setTitle(String title) {
        this.title = StringUtils.stripToNull(title);
    }

    public String getLink() {
        return this.link;
    }

    @DataBoundSetter
    public void setLink(String link) {
        this.link = StringUtils.stripToNull(link);
    }

    public String getDescription() {
        return this.description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = StringUtils.stripToNull(description);
    }

    public String getFooter() {
        return this.footer;
    }

    @DataBoundSetter
    public void setFooter(String footer) {
        this.footer = StringUtils.stripToNull(footer);
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    @DataBoundSetter
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public boolean isUnstable() {
        return this.unstable;
    }

    @DataBoundSetter
    public void setUnstable(boolean unstable) {
        this.unstable = unstable;
    }

    public String getImage() {
        return this.image;
    }

    @DataBoundSetter
    public void setImage(String url) {
        this.image = StringUtils.stripToNull(url);
    }

    public String getThumbnail() {
        return this.thumbnail;
    }

    @DataBoundSetter
    public void setThumbnail(String url) {
        this.thumbnail = StringUtils.stripToNull(url);
    }

    public String getResult() {
        return this.result;
    }

    @DataBoundSetter
    public void setResult(String result) {
        this.result = StringUtils.stripToNull(result);
    }

    public String getNotes() {
        return this.notes;
    }

    @DataBoundSetter
    public void setNotes(String notes) {
        this.notes = StringUtils.stripToNull(notes);
    }

    public String getCustomAvatarUrl() {
        return this.customAvatarUrl;
    }

    @DataBoundSetter
    public void setCustomAvatarUrl(String customAvatarUrl) {
        this.customAvatarUrl = StringUtils.stripToNull(customAvatarUrl);
    }

    public String getCustomUsername() {
        return this.customUsername;
    }

    @DataBoundSetter
    public void setCustomUsername(String customUsername) {
        this.customUsername = StringUtils.stripToNull(customUsername);
    }

    public String getCustomFile() {
        return this.customFile;
    }

    @DataBoundSetter
    public void setCustomFile(String customFile) {
        this.customFile = StringUtils.stripToNull(customFile);
    }

    public boolean getEnableArtifactsList() {
        return this.enableArtifactsList;
    }

    @DataBoundSetter
    public void setEnableArtifactsList(boolean enable) {
        this.enableArtifactsList = enable;
    }

    public boolean getPrettyArtifactsList() {
        return this.prettyArtifactsList;
    }

    @DataBoundSetter
    public void setPrettyArtifactsList(boolean enable) {
        this.prettyArtifactsList = enable;
    }

    public boolean getShowChangeset() {
        return this.showChangeset;
    }

    @DataBoundSetter
    public void setShowChangeset(boolean show) {
        this.showChangeset = show;
    }

    public String getScmWebUrl() {
        return this.scmWebUrl;
    }

    @DataBoundSetter
    public void setScmWebUrl(String url) {
        this.scmWebUrl = StringUtils.stripToNull(url);
    }

    public String getDynamicFieldContainer() {
        if (this.dynamicFieldContainer == null) {
            return "";
        }
        return this.dynamicFieldContainer.toString();
    }

    @DataBoundSetter
    public void setDynamicFieldContainer(String fieldsString) {
        this.dynamicFieldContainer = DynamicFieldContainer.of(fieldsString);
    }

    public DynamicFieldContainer getActualDynamicFieldContainer() {
        return this.dynamicFieldContainer;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return super.start(context);
    }

    public static class DiscordPipelineStepExecution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private final transient DiscordPipelineStep step;

        protected DiscordPipelineStepExecution(DiscordPipelineStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            Run build = getContext().get(Run.class);

            listener.getLogger().println("Sending notification to Discord.");

            WebhookMessage message = EmbedUtil.createEmbed(
                    build,
                    JenkinsLocationConfiguration.get(),
                    this.step,
                    getContext(),
                    listener
            );

            try (WebhookClient client = WebhookClient.withUrl(this.step.getWebhookURL())) {
                listener.getLogger().println("Sending notification to Discord.");
                client.send(message).get();
            } catch (Exception e) {
                e.printStackTrace(listener.getLogger());
            }

            return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(DiscordPipelineStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "discordSend";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Send an embed message to Webhook URL";
        }
    }
}

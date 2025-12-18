package nz.co.jammehcow.jenkinsdiscord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Plugin;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import nz.co.jammehcow.jenkinsdiscord.util.EmbedUtil;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.regex.Matcher;

/**
 * Author: jammehcow.
 * Date: 22/04/17.
 */

@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Requires triage")
public class WebhookPublisher extends Notifier {
    private static final String NAME = "Discord Notifier";
    private static final String SHORT_NAME = "discord-notifier";
    private final String webhookURL;
    private final String branchName;
    private final String statusTitle;
    private final String thumbnailURL;
    private final String notes;
    private final String customAvatarUrl;
    private final String customUsername;
    private final boolean sendOnStateChange;
    private final boolean sendOnlyFailed;
    private final boolean enableArtifactList;
    private final boolean enableFooterInfo;
    private final boolean showChangeset;
    private final boolean sendLogFile;
    private final boolean sendStartNotification;
    private final String scmWebUrl;
    private DynamicFieldContainer dynamicFieldContainer;
    private boolean enableUrlLinking;

    @DataBoundConstructor
    public WebhookPublisher(
            String webhookURL,
            String thumbnailURL,
            boolean sendOnStateChange,
            String statusTitle,
            String notes,
            String branchName,
            String customAvatarUrl,
            String customUsername,
            boolean sendOnStateFailed,
            boolean sendOnlyFailed,
            boolean enableUrlLinking,
            boolean enableArtifactList,
            boolean enableFooterInfo,
            boolean showChangeset,
            boolean sendLogFile,
            boolean sendStartNotification,
            String scmWebUrl
    ) {
        this.webhookURL = webhookURL;
        this.thumbnailURL = thumbnailURL;
        this.sendOnStateChange = sendOnStateChange;
        this.sendOnlyFailed = sendOnlyFailed;
        this.enableUrlLinking = enableUrlLinking;
        this.enableArtifactList = enableArtifactList;
        this.enableFooterInfo = enableFooterInfo;
        this.showChangeset = showChangeset;
        this.branchName = branchName;
        this.statusTitle = statusTitle;
        this.notes = notes;
        this.customAvatarUrl = customAvatarUrl;
        this.customUsername = customUsername;
        this.sendLogFile = sendLogFile;
        this.sendStartNotification = sendStartNotification;
        this.scmWebUrl = scmWebUrl;
    }

    private static String getMarkdownHyperlink(String content, String url) {
        url = url.replaceAll("\\)", "\\\\\\)");
        return "[" + content + "](" + url + ")";
    }

    public String getWebhookURL() {
        return this.webhookURL;
    }

    public String getBranchName() {
        return this.branchName;
    }

    public String getStatusTitle() {
        return this.statusTitle;
    }

    public String getCustomAvatarUrl() {
        return this.customAvatarUrl;
    }

    public String getCustomUsername() {
        return this.customUsername;
    }

    public String getNotes() {
        return this.notes;
    }

    public String getThumbnailURL() {
        return this.thumbnailURL;
    }

    public boolean isSendOnStateChange() {
        return this.sendOnStateChange;
    }

    public boolean isSendOnlyFailed() {
        return this.sendOnlyFailed;
    }

    public boolean isEnableUrlLinking() {
        return this.enableUrlLinking;
    }

    public boolean isEnableArtifactList() {
        return this.enableArtifactList;
    }

    public boolean isEnableFooterInfo() {
        return this.enableFooterInfo;
    }

    public boolean isShowChangeset() {
        return this.showChangeset;
    }

    public boolean isSendLogFile() {
        return this.sendLogFile;
    }

    public boolean isSendStartNotification() {
        return this.sendStartNotification;
    }

    public String getScmWebUrl() {
        return this.scmWebUrl;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
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

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        listener.getLogger().println(this.sendStartNotification);
        if (!this.sendStartNotification)
            return true;

        final EnvVars env;
        try {
            env = build.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.getLogger());
            return false;
        }

        String title;
        if (this.statusTitle != null && !this.statusTitle.isEmpty())
            title = String.format("Build started: %s", env.expand(this.statusTitle));
        else
            title = String.format("Build started: %s #%s", build.getProject().getDisplayName(), build.getId());

        String branch = null;
        if (this.branchName != null && !this.branchName.isEmpty())
            branch = env.expand(this.branchName);

        String webhookNotes = null;
        if (this.notes != null && !this.notes.isEmpty())
            webhookNotes = env.expand(this.notes);

        WebhookMessage message = EmbedUtil.createEmbed(
                build,
                JenkinsLocationConfiguration.get(),
                listener,
                title,
                null,
                null,
                this.enableFooterInfo
                        ? String.format("Jenkins v%s, %s v%s", build.getHudsonVersion(), getDescriptor().getDisplayName(), getDescriptor().getPluginVersion())
                        : null,
                null,
                this.thumbnailURL.isEmpty() ? null : this.thumbnailURL,
                StatusColor.GREEN,
                false,
                null,
                this.enableUrlLinking,
                branch,
                webhookNotes,
                StringUtils.stripToNull(this.customAvatarUrl),
                StringUtils.stripToNull(this.customUsername),
                null,
                null,
                this.dynamicFieldContainer,
                false,
                false,
                null
        );

        try (WebhookClient client = WebhookClient.withUrl(this.webhookURL)) {
            listener.getLogger().println("Sending notification to Discord.");
            client.send(message).get();
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
        }

        return true;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        final EnvVars env = build.getEnvironment(listener);
        // The global configuration, used to fetch the instance url
        JenkinsLocationConfiguration globalConfig = JenkinsLocationConfiguration.get();
        if (build.getResult() == null) {
            listener.getLogger().println("[Discord Notifier] build.getResult() is null!");
            return true;
        }

        if (this.webhookURL.isEmpty()) {
            // Stop the plugin from continuing when the webhook URL isn't set. Shouldn't happen due to form validation
            listener.getLogger().println("The Discord webhook is not set!");
            return true;
        }

        if (this.enableUrlLinking && (globalConfig.getUrl() == null || globalConfig.getUrl().isEmpty())) {
            // Disable linking when the instance URL isn't set
            listener.getLogger().println("Your Jenkins URL is not set (or is set to localhost)! Disabling linking.");
            this.enableUrlLinking = false;
        }

        if (this.sendOnStateChange && build.getPreviousBuild() != null && build.getResult().equals(build.getPreviousBuild().getResult())) {
            // Stops the webhook payload being created if the status is the same as the previous
            return true;
        }

        if (this.sendOnlyFailed && !build.getResult().equals(Result.FAILURE)) {
            return true;
        }

        Result buildresult = build.getResult();
        StatusColor statusColor = StatusColor.GREEN;
        if (!buildresult.isCompleteBuild())
            return true;
        if (buildresult.isBetterOrEqualTo(Result.SUCCESS))
            statusColor = StatusColor.GREEN;
        if (buildresult.isWorseThan(Result.SUCCESS))
            statusColor = StatusColor.YELLOW;
        if (buildresult.isWorseThan(Result.UNSTABLE))
            statusColor = StatusColor.RED;

        String title;
        if (this.statusTitle != null && !this.statusTitle.isEmpty())
            title = env.expand(this.statusTitle);
        else
            title = String.format("%s #%s", build.getProject().getDisplayName(), build.getId());

        MatrixConfiguration matrixConfiguration = null;
        if (build.getProject() instanceof MatrixConfiguration) {
            title = String.format("%s #%s", build.getProject().getParent().getDisplayName(), build.getId());
            matrixConfiguration = (MatrixConfiguration) build.getProject();
        }

        String branch = null;
        if (this.branchName != null && !this.branchName.isEmpty())
            branch = env.expand(this.branchName);

        String webhookNotes = null;
        if (this.notes != null && !this.notes.isEmpty())
            webhookNotes = env.expand(this.notes);

        WebhookMessage message = EmbedUtil.createEmbed(
                build,
                globalConfig,
                listener,
                title,
                this.enableUrlLinking ? globalConfig.getUrl() + build.getUrl() : null,
                null,
                this.enableFooterInfo
                        ? String.format("Jenkins v%s, %s v%s", build.getHudsonVersion(), getDescriptor().getDisplayName(), getDescriptor().getPluginVersion())
                        : null,
                null,
                this.thumbnailURL.isEmpty() ? null : this.thumbnailURL,
                statusColor,
                true,
                matrixConfiguration,
                this.enableUrlLinking,
                branch,
                webhookNotes,
                StringUtils.stripToNull(this.customAvatarUrl),
                StringUtils.stripToNull(this.customUsername),
                this.sendLogFile ? String.format("build-%d.log", build.getNumber()) : null,
                this.sendLogFile ? build.getLogInputStream() : null,
                this.dynamicFieldContainer,
                this.enableArtifactList,
                this.showChangeset,
                StringUtils.stripToNull(this.scmWebUrl)
        );

        try (WebhookClient client = WebhookClient.withUrl(this.webhookURL)) {
            listener.getLogger().println("Sending notification to Discord.");
            client.send(message).get();
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
        }

        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private final Plugin plugin = Jenkins.get().getPlugin(SHORT_NAME);

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public FormValidation doCheckWebhookURL(@QueryParameter String value) {
            Matcher matcher = WebhookClientBuilder.WEBHOOK_PATTERN.matcher(value);
            if (!matcher.matches())
                return FormValidation.error("Please enter a valid Discord webhook URL.");
            return FormValidation.ok();
        }

        @NonNull
        public String getDisplayName() {
            if (this.plugin == null) {
                return NAME;
            } else {
                return this.plugin.getWrapper().getDisplayName();
            }
        }

        public String getPluginVersion() {
            if (this.plugin == null) {
                return "";
            } else {
                return this.plugin.getWrapper().getVersion();
            }
        }
    }
}

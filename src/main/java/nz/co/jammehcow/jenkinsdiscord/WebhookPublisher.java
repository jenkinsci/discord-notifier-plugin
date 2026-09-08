package nz.co.jammehcow.jenkinsdiscord;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Plugin;
import hudson.PluginWrapper;
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
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import nz.co.jammehcow.jenkinsdiscord.exception.WebhookException;
import nz.co.jammehcow.jenkinsdiscord.util.EmbedDescription;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Author: jammehcow.
 * Date: 22/04/17.
 */

@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Requires triage")
public class WebhookPublisher extends Notifier {
    private final String webhookURL;
    private final String branchName;
    private final String statusTitle;
    private final String thumbnailURL;
    private final String notes;
    private final String customAvatarUrl;
    private final String customUsername;
    private DynamicFieldContainer dynamicFieldContainer;
    private final boolean sendOnStateChange;
    private final boolean sendOnlyFailed;
    private boolean enableUrlLinking;
    private final boolean enableArtifactList;
    private final boolean enableFooterInfo;
    private boolean showChangeset;
    private boolean sendLogFile;
    private boolean sendStartNotification;
    private static final String NAME = "Discord Notifier";
    private static final String SHORT_NAME = "discord-notifier";
    private final String scmWebUrl;
    private String successColor;
    private String unstableColor;
    private String failureColor;

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

    @DataBoundSetter
    public void setDynamicFieldContainer(String fieldsString) {
      this.dynamicFieldContainer = DynamicFieldContainer.of(fieldsString);
    }

    public String getDynamicFieldContainer() {
        if(dynamicFieldContainer == null){
            return "";
        }
        return dynamicFieldContainer.toString();
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

    public String getSuccessColor() {
        return this.successColor;
    }

    @DataBoundSetter
    public void setSuccessColor(String successColor) {
        this.successColor = successColor;
    }

    public String getUnstableColor() {
        return this.unstableColor;
    }

    @DataBoundSetter
    public void setUnstableColor(String unstableColor) {
        this.unstableColor = unstableColor;
    }

    public String getFailureColor() {
        return this.failureColor;
    }

    @DataBoundSetter
    public void setFailureColor(String failureColor) {
        this.failureColor = failureColor;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        final EnvVars env;
        listener.getLogger().println(sendStartNotification);
        if (sendStartNotification) {
            try {
                env = build.getEnvironment(listener);
                DiscordWebhook wh = new DiscordWebhook(env.expand(this.webhookURL));
                AbstractProject project = build.getProject();
                String description;
                JenkinsLocationConfiguration globalConfig = JenkinsLocationConfiguration.get();
                wh.setStatus(DiscordWebhook.StatusColor.GREEN);
                if (this.statusTitle != null && !this.statusTitle.isEmpty()) {
                    wh.setTitle("Build started: " + env.expand(this.statusTitle));
                } else {
                    wh.setTitle("Build started: " + project.getDisplayName() + " #" + build.getId());
                }
                String branchNameString = "";
                if (branchName != null && !branchName.isEmpty()) {
                    branchNameString = "**Branch:** " + env.expand(branchName) + "\n";
                }
                if (this.enableUrlLinking) {
                    String url = globalConfig.getUrl() + build.getUrl();
                    description = branchNameString
                            + "**Build:** "
                            + getMarkdownHyperlink(build.getId(), url);
                    wh.setURL(url);
                } else {
                    description = branchNameString
                            + "**Build:** "
                            + build.getId();
                }
                wh.setDescription(new EmbedDescription(build, globalConfig, description, false, false, null).toString());

                addDynamicFieldsToWebhook(dynamicFieldContainer, wh, env);

                // Send the webhook
                wh.send();
            } catch (WebhookException | InterruptedException | IOException e1) {
                e1.printStackTrace(listener.getLogger());
            }
        }
        return true;
    }

    //TODO clean this function
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        final EnvVars env = build.getEnvironment(listener);
        // The global configuration, used to fetch the instance url
        JenkinsLocationConfiguration globalConfig = JenkinsLocationConfiguration.get();
        if (build.getResult() == null) {
            listener.getLogger().println("[Discord Notifier] build.getResult() is null!");
            return true;
        }

        // Create a new webhook payload
        DiscordWebhook wh = new DiscordWebhook(env.expand(this.webhookURL));

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

        if (this.sendOnStateChange) {
            if (build.getPreviousBuild() != null && build.getResult().equals(build.getPreviousBuild().getResult())) {
                // Stops the webhook payload being created if the status is the same as the previous
                return true;
            }
        }

        if (this.sendOnlyFailed) {
            if (!build.getResult().equals(Result.FAILURE)) {
                return true;
            }
        }

        if (this.sendLogFile) {
            wh.setFile(build.getLogInputStream(), "build" + build.getNumber() + ".log");
        }

        Result buildresult = build.getResult();
        if (!buildresult.isCompleteBuild()) return true;

        AbstractProject project = build.getProject();
        StringBuilder combinationString = new StringBuilder();
        if (this.statusTitle != null && !this.statusTitle.isEmpty()) {
            wh.setTitle(env.expand(this.statusTitle));
        } else {
            wh.setTitle(project.getDisplayName() + " #" + build.getId());
        }

        //Check if MatrixConfiguration
        if (project instanceof MatrixConfiguration) {
            wh.setTitle(project.getParent().getDisplayName() + " #" + build.getId());
            combinationString.append("**Configuration matrix:**\n");
            for (Map.Entry e : ((MatrixConfiguration) project).getCombination().entrySet())
                combinationString.append(" - ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }

        String branchNameString = "";
        if (branchName != null && !branchName.isEmpty()) {
            branchNameString = "**Branch:** " + env.expand(branchName) + "\n";
        }

        String descriptionPrefix;
        // Adds links to the description and title if enableUrlLinking is enabled
        if (this.enableUrlLinking) {
            String url = globalConfig.getUrl() + build.getUrl();
            descriptionPrefix = branchNameString
                    + "**Build:** "
                    + getMarkdownHyperlink(build.getId(), url)
                    + "\n**Status:** "
                    + getMarkdownHyperlink(build.getResult().toString().toLowerCase(Locale.ENGLISH), url) + "\n";
            wh.setURL(url);
        } else {
            descriptionPrefix = branchNameString
                    + "**Build:** "
                    + build.getId()
                    + "\n**Status:** "
                    + build.getResult().toString().toLowerCase(Locale.ENGLISH) + "\n";
        }
        descriptionPrefix += combinationString;

        if (notes != null && !notes.isEmpty()) {
            wh.setContent(env.expand(notes));
        }

        if (customAvatarUrl != null && !customAvatarUrl.isEmpty()) {
            wh.setCustomAvatarUrl(customAvatarUrl);
        }

        if (customUsername != null && !customUsername.isEmpty()) {
            wh.setCustomUsername(customUsername);
        }

        wh.setThumbnail(thumbnailURL);
        wh.setDescription(
                new EmbedDescription(build, globalConfig, descriptionPrefix, this.enableArtifactList, this.showChangeset, this.scmWebUrl)
                        .toString()
        );

        addDynamicFieldsToWebhook(dynamicFieldContainer, wh, env);
        wh.setStatusByColor(resolveColor(listener, buildresult, successColor, unstableColor, failureColor));

        if (this.enableFooterInfo)
            wh.setFooter("Jenkins v" + build.getHudsonVersion() + ", " + getDescriptor().getDisplayName() + " v" + getDescriptor().getPluginVersion());

        try {
            listener.getLogger().println("Sending notification to Discord.");
            wh.send();
        } catch (WebhookException e) {
            e.printStackTrace(listener.getLogger());
        }

        return true;
    }

    /**
     * Add all key value field pairs to the webhook
    */
    private void addDynamicFieldsToWebhook(DynamicFieldContainer dynamicFieldContainer, DiscordWebhook wh, EnvVars env){
        // Early exit if we don't have any dynamicFieldContainer set
        if(dynamicFieldContainer == null){
            return;
        }
        // Go through all fields and add them to the webhook
        dynamicFieldContainer.getFields().forEach(pair -> wh.addField(pair.getKey() + ":", env.expand(pair.getValue())));
    }

    /**
     * Resolves the integer color code to use for the embed based on the build result.
     * Uses the provided custom color string when non-empty and parseable; falls back to the default.
     *
     * @param buildResult   the build result used to select the appropriate color bucket
     * @param successColor  custom hex/decimal color string for successful builds, or null/empty for default
     * @param unstableColor custom hex/decimal color string for unstable builds, or null/empty for default
     * @param failureColor  custom hex/decimal color string for failed builds, or null/empty for default
     * @return the resolved integer color code
     */
    private static int resolveColor(
            BuildListener listener,
            Result buildResult,
            String successColor,
            String unstableColor,
            String failureColor
    ) {
        String custom;
        String customFieldName;
        DiscordWebhook.StatusColor defaultColor;
        if (buildResult.isBetterOrEqualTo(Result.SUCCESS)) {
            custom = successColor;
            customFieldName = "successColor";
            defaultColor = DiscordWebhook.StatusColor.GREEN;
        } else if (buildResult.isWorseThan(Result.UNSTABLE)) {
            custom = failureColor;
            customFieldName = "failureColor";
            defaultColor = DiscordWebhook.StatusColor.RED;
        } else {
            custom = unstableColor;
            customFieldName = "unstableColor";
            defaultColor = DiscordWebhook.StatusColor.YELLOW;
        }
        if (custom != null && !custom.isEmpty()) {
            try {
                return DiscordWebhook.parseColor(custom);
            } catch (NumberFormatException e) {
                listener.getLogger().println(
                        "[Discord Notifier] Invalid " + customFieldName + " value '" + custom
                                + "'. Using default color."
                );
            }
        }
        return (int) defaultColor.getCode();
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
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        final Plugin p = Jenkins.get().getPlugin(SHORT_NAME);

        public FormValidation doCheckWebhookURL(@QueryParameter String value) {
            if (!value.matches("https://(canary\\.|ptb\\.|)discord(app)*\\.com/api/webhooks/\\d{18,19}/(\\w|-|_)*(/?)"))
                return FormValidation.error("Please enter a valid Discord webhook URL.");
            return FormValidation.ok();
        }

        @NonNull
        public String getDisplayName() {
            if (p == null) {
                return NAME;
            } else {
                return p.getWrapper().getDisplayName();
            }
        }

        public String getPluginVersion() {
            if (p == null) {
                return "";
            } else {
                return p.getWrapper().getVersion();
            }
        }
    }

    private static String getMarkdownHyperlink(String content, String url) {
        url = url.replaceAll("\\)", "\\\\\\)");
        return "[" + content + "](" + url + ")";
    }
}

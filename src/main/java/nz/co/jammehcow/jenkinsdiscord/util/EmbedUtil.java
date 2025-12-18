package nz.co.jammehcow.jenkinsdiscord.util;

import club.minnced.discord.webhook.send.WebhookEmbed.EmbedAuthor;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedField;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedFooter;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedTitle;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import hudson.FilePath;
import hudson.matrix.MatrixConfiguration;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import jenkins.model.JenkinsLocationConfiguration;
import jenkins.scm.RunWithSCM;
import nz.co.jammehcow.jenkinsdiscord.DiscordPipelineStep;
import nz.co.jammehcow.jenkinsdiscord.DynamicFieldContainer;
import nz.co.jammehcow.jenkinsdiscord.StatusColor;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

public class EmbedUtil {
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_EMBED_DESCRIPTION_LENGTH = 4096;
    private static final int MAX_EMBED_TITLE_LENGTH = 256;
    private static final int MAX_EMBED_FIELD_LENGTH = 1024;
    private static final int MAX_EMBED_FOOTER_LENGTH = 2048;

    private EmbedUtil() {
    }

    public static WebhookMessage createEmbed(
            Run build,
            JenkinsLocationConfiguration globalConfig,
            DiscordPipelineStep step,
            StepContext context,
            TaskListener listener
    ) throws IOException, InterruptedException {
        StatusColor statusColor = StatusColor.YELLOW;
        if (step.getResult() == null) {
            if (step.isSuccessful())
                statusColor = StatusColor.GREEN;
            if (step.isSuccessful() && step.isUnstable())
                statusColor = StatusColor.YELLOW;
            if (!step.isSuccessful() && !step.isUnstable())
                statusColor = StatusColor.RED;
        } else {
            Result result;
            if (step.getResult() != null)
                result = Result.fromString(step.getResult());
            else
                result = build.getResult();

            if (result == null)
                throw new IllegalStateException("[Discord Notifier] build.getResult() is null!");

            if (result.equals(Result.SUCCESS)) {
                statusColor = StatusColor.GREEN;
            } else if (result.equals(Result.UNSTABLE)) {
                statusColor = StatusColor.YELLOW;
            } else if (result.equals(Result.FAILURE)) {
                statusColor = StatusColor.RED;
            } else if (result.equals(Result.ABORTED) || result.equals(Result.NOT_BUILT)) {
                statusColor = StatusColor.GREY;
            }
        }
        return EmbedUtil.createEmbed(build,
                globalConfig,
                listener,
                step.getTitle(),
                step.getLink(),
                step.getDescription(),
                step.getFooter(),
                step.getImage(),
                step.getThumbnail(),
                statusColor,
                true,
                null,
                true,
                null,
                step.getNotes(),
                step.getCustomAvatarUrl(),
                step.getCustomUsername(),
                step.getCustomFile(),
                EmbedUtil.getFileInputStream(context, step.getCustomFile()),
                step.getActualDynamicFieldContainer(),
                step.getEnableArtifactsList(),
                step.getShowChangeset(),
                step.getScmWebUrl()
        );
    }

    public static WebhookMessage createEmbed(
            Run<?, ?> build,
            JenkinsLocationConfiguration globalConfig,
            TaskListener listener,
            String title,
            String link,
            String description,
            String footer,
            String image,
            String thumbnail,
            StatusColor statusColor,
            boolean showStatus,
            MatrixConfiguration matrixConfiguration,
            boolean urlLinking,
            String branch,
            String notes,
            String customAvatarUrl,
            String customUsername,
            String customFile,
            InputStream customFileInputStream,
            DynamicFieldContainer dynamicFieldContainer,
            boolean enableArtifactsList,
            boolean showChangeset,
            String scmWebUrlTemplate
    ) {
        String artifactsURL = globalConfig.getUrl() + build.getUrl() + "artifact/";

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder();
        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();

        messageBuilder.setContent(EmbedUtil.truncateToLimit(listener, "content", notes, EmbedUtil.MAX_CONTENT_LENGTH));

        embedBuilder.setTitle(new EmbedTitle(EmbedUtil.truncateToLimit(listener, "title", title, EmbedUtil.MAX_EMBED_TITLE_LENGTH), link));

        embedBuilder.setDescription(EmbedUtil.truncateToLimit(listener, "description", StringUtils.stripToNull(description), EmbedUtil.MAX_EMBED_DESCRIPTION_LENGTH));

        if (branch != null) {
            embedBuilder.addField(new EmbedField(true, "Branch", branch));
        }

        String buildResult = Objects.requireNonNull(build.getResult()).toString().toLowerCase(Locale.ENGLISH);
        if (urlLinking) {
            embedBuilder.addField(new EmbedField(true, "Build", MarkdownUtil.formatMarkdownUrl(build.getId(), link)));
            if (showStatus)
                embedBuilder.addField(new EmbedField(true, "Status", MarkdownUtil.formatMarkdownUrl(buildResult, link)));
        } else {
            embedBuilder.addField(new EmbedField(true, "Build", build.getId()));
            if (showStatus)
                embedBuilder.addField(new EmbedField(true, "Status", buildResult));
        }

        if (matrixConfiguration != null) {
            StringBuilder descriptionBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : matrixConfiguration.getCombination().entrySet())
                descriptionBuilder.append(String.format("- %s: %s", entry.getKey(), entry.getValue()))
                                  .append('\n');

            String matrixDescription = EmbedUtil.truncateToLimit(
                    listener,
                    "matrix configuration",
                    descriptionBuilder.toString().strip(),
                    EmbedUtil.MAX_EMBED_FIELD_LENGTH
            );

            embedBuilder.addField(new EmbedField(false, "Configuration matrix", matrixDescription));
        }

        LinkedList<String> changesList = new LinkedList<>();
        if (showChangeset) {
            changesList = new LinkedList<>(EmbedUtil.buildChangesetList((RunWithSCM) build, scmWebUrlTemplate));
        }

        LinkedList<String> artifactsList = new LinkedList<>();
        if (enableArtifactsList) {
            artifactsList = new LinkedList<>(EmbedUtil.buildArtifactList(build, artifactsURL));
        }


        if (!changesList.isEmpty()) {
            // shorten to at most 1024 characters (max field size)
            while (EmbedUtil.getLengthForList(changesList) > EmbedUtil.MAX_EMBED_FIELD_LENGTH) {
                changesList.removeLast();
            }

            StringBuilder changesDescription = new StringBuilder();
            for (String changeEntry : changesList)
                changesDescription.append(changeEntry)
                                  .append('\n');

            embedBuilder.addField(new EmbedField(false, "Changes", changesDescription.toString().strip()));
        } else if (showChangeset) {
            embedBuilder.addField(new EmbedField(false, "Changes", "*No changes.*"));
        }

        if (!artifactsList.isEmpty()) {
            StringBuilder artifactsDescription = new StringBuilder();
            for (String artifact : artifactsList)
                artifactsDescription.append(artifact)
                                    .append('\n');

            if (artifactsDescription.length() > EmbedUtil.MAX_EMBED_FIELD_LENGTH)
                embedBuilder.addField(new EmbedField(false, "Artifacts", artifactsURL));
            else
                embedBuilder.addField(new EmbedField(false, "Artifacts", artifactsDescription.toString().strip()));
        } else if (enableArtifactsList) {
            embedBuilder.addField(new EmbedField(false, "Artifacts", "*No artifacts saved.*"));
        }

        embedBuilder.setColor(statusColor.getCode());
        embedBuilder.setImageUrl(image);
        embedBuilder.setThumbnailUrl(thumbnail);

        if (footer != null) {
            EmbedFooter embedFooter = new EmbedFooter(
                    EmbedUtil.truncateToLimit(listener, "footer", footer, EmbedUtil.MAX_EMBED_FOOTER_LENGTH),
                    null
            );

            embedBuilder.setFooter(embedFooter);
        }

        String username = EmbedUtil.withFallback(customUsername, "Jenkins");
        String avatar = EmbedUtil.withFallback(customAvatarUrl, "https://get.jenkins.io/art/jenkins-logo/1024x1024/headshot.png");
        embedBuilder.setAuthor(new EmbedAuthor(username, avatar, null));

        if (dynamicFieldContainer != null) {
            dynamicFieldContainer.getFields().forEach(pair -> {
                String fieldTitle = EmbedUtil.truncateToLimit(listener, pair.getKey(), pair.getKey(), EmbedUtil.MAX_EMBED_TITLE_LENGTH);
                String fieldDescription = EmbedUtil.truncateToLimit(listener, pair.getKey(), pair.getValue(), EmbedUtil.MAX_EMBED_TITLE_LENGTH);
                embedBuilder.addField(new EmbedField(false, fieldTitle, fieldDescription));
            });
        }

        if (customFile != null && customFileInputStream != null)
            messageBuilder.addFile(customFile, customFileInputStream);

        messageBuilder.addEmbeds(embedBuilder.build());

        return messageBuilder.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<String> buildArtifactList(Run build, String artifactsURL) {

        List<Run.Artifact> artifacts = build.getArtifacts();
        if (artifacts.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> artifactList = new ArrayList<>();

        for (Run.Artifact artifact : artifacts)
            artifactList.add(String.format("- [%s](%s)", artifact.getFileName(), artifactsURL + artifact.getHref()));

        return artifactList;
    }

    @SuppressWarnings({"unchecked", "rawtypes", "SuspiciousArrayCast"})
    public static List<String> buildChangesetList(RunWithSCM build, String scmWebUrl) {
        List<ChangeLogSet.Entry> changes = new ArrayList<>();

        for (ChangeLogSet<?> changelogset : (List<ChangeLogSet<?>>) build.getChangeSets())
            changes.addAll(Arrays.asList((ChangeLogSet.Entry[]) changelogset.getItems()));

        if (changes.isEmpty()) {
            return new ArrayList<>();
        }

        boolean withLinks;
        try {
            String dummy = String.format(scmWebUrl, "");
            withLinks = true;
        } catch (Exception ex) { // null or illegal format specification
            withLinks = false;
        }

        List<String> changesetList = new ArrayList<>();

        for (ChangeLogSet.Entry entry : changes) {
            String commitID = entry.getCommitId();
            String commitDisplayStr;
            if (commitID == null)
                commitDisplayStr = "null";
            else
                commitDisplayStr = commitID.substring(0, Math.min(commitID.length(), 6));

            String msg = MarkdownUtil.escape(entry.getMsg().strip().split("\n")[0]);

            String commitAuthor = entry.getAuthor().getFullName();

            if (withLinks) {
                changesetList.add(String.format("- [`%s`](%s) *%s - %s*", commitDisplayStr, String.format(scmWebUrl, commitID), msg, commitAuthor));
            } else {
                changesetList.add(String.format("- `%s` *%s - %s*", commitDisplayStr, msg, commitAuthor));
            }
        }

        return changesetList;
    }

    private static <T> T withFallback(T value, T fallback) {
        if (value == null)
            return fallback;
        return value;
    }

    private static int getLengthForList(List<String> list) {
        return list.stream().mapToInt(String::length).reduce(0, Integer::sum) + list.size();
    }

    private static String truncateToLimit(TaskListener listener, String fieldName, String value, int limit) {
        if (value == null)
            return null;

        if (value.length() > limit) {
            listener.getLogger().printf("Warning: '%s' field has more than %d characters (%d). It will be truncated.%n",
                    fieldName,
                    limit,
                    value.length());
            return value.substring(0, limit);
        }

        return value;
    }

    private static InputStream getFileInputStream(StepContext context, String file) throws IOException, InterruptedException {
        FilePath ws = context.get(FilePath.class);
        if (ws == null)
            return null;

        FilePath fp = ws.child(file);
        if (fp.exists()) {
            try {
                return fp.read();
            } catch (InvalidPathException var3) {
                throw new IOException(var3);
            }
        } else {
            String message = "No such file: " + file;
            return new ByteArrayInputStream(message.getBytes(Charset.defaultCharset()));
        }
    }
}

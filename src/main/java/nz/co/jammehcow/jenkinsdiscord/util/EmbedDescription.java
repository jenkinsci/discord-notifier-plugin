package nz.co.jammehcow.jenkinsdiscord.util;

import jenkins.scm.RunWithSCM;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import java.util.ArrayList;
import java.util.Arrays;
import jenkins.model.JenkinsLocationConfiguration;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * @author jammehcow
 */

public class EmbedDescription {
    private static final int maxEmbedStringLength = 2048; // The maximum length of an embed description.

    private LinkedList<String> changesList = new LinkedList<>();
    private LinkedList<String> artifactsList = new LinkedList<>();
    private String prefix;
    private String finalDescription;

    public EmbedDescription(
            Run build,
            JenkinsLocationConfiguration globalConfig,
            String prefix,
            boolean enableArtifactsList,
            boolean showChangeset
    ) {
        String artifactsURL = globalConfig.getUrl() + build.getUrl() + "artifact/";
        this.prefix = prefix.trim();

        if (showChangeset) {
            ArrayList<Object> changes = new ArrayList<>();
            List<ChangeLogSet> changeSets = ((RunWithSCM)build).getChangeSets();
            for (ChangeLogSet i : changeSets)
                changes.addAll(Arrays.asList(i.getItems()));
            if (changes.isEmpty()) {
                this.changesList.add("\n*No changes.*\n");
            } else {
                this.changesList.add("\n**Changes:**\n");
                for (Object o : changes) {
                    ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
                    String commitID;
                    if (entry.getCommitId() == null) commitID = "null";
                    else if (entry.getCommitId().length() < 6) commitID = entry.getCommitId();
                    else commitID = entry.getCommitId().substring(0, 6);

                    String msg = entry.getMsg().trim();
                    int nl = msg.indexOf("\n");
                    if (nl >= 0)
                        msg = msg.substring(0, nl).trim();

                    this.changesList.add(String.format("   - ``%s`` *%s - %s*%n",
                            commitID, EscapeMarkdown(msg), entry.getAuthor().getFullName()));
                }
            }
        }

        if (enableArtifactsList) {
            this.artifactsList.add("\n**Artifacts:**\n");
            //noinspection unchecked
            List<Run.Artifact> artifacts = build.getArtifacts();
            if (artifacts.size() == 0) {
                this.artifactsList.add("\n*No artifacts saved.*");
            } else {
                for (Run.Artifact artifact : artifacts) {
                    this.artifactsList.add(" - " + artifactsURL + artifact.getHref() + "\n");
                }
            }
        }

        while (this.getCurrentDescription().length() > maxEmbedStringLength) {
            if (this.changesList.size() > 5) {
                // Dwindle the changes list down to 5 changes.
                while (this.changesList.size() != 5) this.changesList.removeLast();
            } else if (this.artifactsList.size() > 1) {
                this.artifactsList.clear();
                this.artifactsList.add(artifactsURL);
            } else {
                // Worst case scenario: truncate the description.
                this.finalDescription = this.getCurrentDescription().substring(0, maxEmbedStringLength - 1);
                return;
            }
        }

        this.finalDescription = this.getCurrentDescription();
    }

    private String getCurrentDescription() {
        StringBuilder description = new StringBuilder();
        if (StringUtils.isNotEmpty(this.prefix))
            description.append(this.prefix);

        // Collate the changes and artifacts into the description.
        for (String changeEntry : this.changesList) description.append(changeEntry);
        for (String artifact : this.artifactsList) description.append(artifact);

        return description.toString().trim();
    }

    @Override
    public String toString() {
        return this.finalDescription;
    }

    // https://support.discord.com/hc/en-us/articles/210298617
    private static String EscapeMarkdown(String text) {
        return text.replace("\\", "\\\\").replace("*", "\\*").replace("_", "\\_").replace("~", "\\~")
            .replace("`", "\\`");
    }
}

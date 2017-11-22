package nz.co.jammehcow.jenkinsdiscord.util;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * @author jammehcow
 */

public class EmbedDescription {
    private static final int maxEmbedStringLength = 2048; // The maximum length of an embed description.

    private LinkedList<String> changesList = new LinkedList<>();
    private LinkedList<String> artifactsList = new LinkedList<>();
    private String prefix;
    private String finalDescription;

    public EmbedDescription(Run build, JenkinsLocationConfiguration globalConfig, String prefix, boolean enableArtifactsList) {
        String artifactsURL = globalConfig.getUrl() + build.getUrl() + "artifact/";
        this.prefix = prefix;
        this.changesList.add("\n**Changes:**\n");
        if (enableArtifactsList) this.artifactsList.add("\n**Artifacts:**\n");
        
        ArrayList<Object> changes = new ArrayList<>();
        if (build instanceof AbstractBuild) {
            AbstractBuild abstractBuild = (AbstractBuild)build;
            changes.addAll(Arrays.asList(abstractBuild.getChangeSet().getItems()));
        } else if (build instanceof WorkflowRun) {
            WorkflowRun workflowRun = (WorkflowRun)build;
            for (ChangeLogSet i : workflowRun.getChangeSets())
                changes.addAll(Arrays.asList(i.getItems()));
        } else {
            throw new IllegalArgumentException(
                "build is neither an AbstractBuild nor a WorkflowRun");
        }
        
        if (changes.isEmpty()) {
            this.changesList.add("*No changes.*");
        } else {
            for (Object o : changes) {
                ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
                String commitID = (entry.getParent().getKind().equalsIgnoreCase("svn")) ? entry.getCommitId() : entry.getCommitId().substring(0, 6);

                String msg = entry.getMsg().trim();
                int nl = msg.indexOf("\n");
                if (nl >= 0) {
                    msg = msg.substring(0, nl);
                }

                this.changesList.add("   - ``" + commitID + "`` *" + msg
                    + " - " + entry.getAuthor().getFullName() + "*\n");
            }
        }

        if (enableArtifactsList) {
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
        description.append(this.prefix);

        // Collate the changes and artifacts into the description.
        for (String changeEntry : this.changesList) description.append(changeEntry);
        for (String artifact : this.artifactsList) description.append(artifact);

        return description.toString();
    }

    @Override
    public String toString() {
        return this.finalDescription;
    }
}

package org.powerimo.jenkins.pman;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public abstract class BasePmanStep extends Step implements Serializable {
    private static final long serialVersionUID = -8622973595226055889L;

    @DataBoundSetter
    @Getter
    @Setter
    private boolean failOnError = true;

    @DataBoundSetter
    @Getter
    @Setter
    private boolean dryRun = false;

    @DataBoundSetter
    @Getter
    @Setter
    private String apiKey;

    @DataBoundSetter
    @Getter
    @Setter
    private String shelfId;

    @DataBoundSetter
    @Getter
    @Setter
    private String valueName;

    protected String getLogPrefix() {
        return this.getClass().getSimpleName();
    }

    protected UUID getAccountId() {
        if (apiKey == null)
            throw new IllegalArgumentException("apiKey argument is empty");
        if (!apiKey.contains(":"))
            throw new IllegalArgumentException("apiKey argument must be <UUID:accountId>:<String:Secret>");
        String a = apiKey.substring(0, apiKey.indexOf(":"));
        try {
            return UUID.fromString(a);
        } catch (Exception ex) {
            throw new IllegalArgumentException("apiKey argument doesn't contains a valid account ID (UUID)" );
        }
    }

    protected UUID getShelfId() {
        try {
            return UUID.fromString(shelfId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("shelfId argument is not a valid UUID");
        }
    }
}

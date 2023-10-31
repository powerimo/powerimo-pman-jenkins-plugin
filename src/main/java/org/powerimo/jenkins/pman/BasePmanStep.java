package org.powerimo.jenkins.pman;

import lombok.*;
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

    protected String getAccountIdStringFromApiKey() {
        return getAccountPartFromApiKey(apiKey);
    }

    protected UUID getShelfId() {
        try {
            return UUID.fromString(shelfId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("shelfId argument is not a valid UUID: '" + shelfId + "'");
        }
    }

    protected String getShelfIdString() {
        return shelfId;
    }

    public static String getAccountPartFromApiKey(String s) {
        if (null == s || !s.contains(":")) {
            return null;
        }
        return s.substring(0, s.indexOf(":"));
    }
}

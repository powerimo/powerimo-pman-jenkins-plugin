package org.powerimo.jenkins.pman;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BasePmanStep extends Step implements Serializable {
    private static final long serialVersionUID = -8622973595226055889L;

    private String name;

    @DataBoundSetter
    private Map remote;

    @DataBoundSetter
    private boolean failOnError = true;

    @DataBoundSetter
    private boolean dryRun = false;

    protected String getLogPrefix() {
        return this.getClass().getSimpleName();
    }
}

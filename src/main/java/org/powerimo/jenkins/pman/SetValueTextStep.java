package org.powerimo.jenkins.pman;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.powerimo.pman.dto.ShelfValue;

import java.io.IOException;
import java.util.Set;

@Slf4j
public class SetValueTextStep extends BasePmanStep {
    private static final long serialVersionUID = 4900672908494689058L;

    @Getter
    private final String valueName;

    @DataBoundSetter
    @Getter
    @Setter
    private String value;

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet
                    .of(Launcher.class, FilePath.class, Run.class, TaskListener.class, EnvVars.class);
        }

        @Override
        public String getFunctionName() {
            return "pmanSetValueText";
        }
    }

    @DataBoundConstructor
    public SetValueTextStep(String valueName) {
        this.valueName = valueName;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SetValueStepExecutor(this, context);
    }


    public static class SetValueStepExecutor extends BasePmanExecutor {
        private static final long serialVersionUID = -4597125370466116668L;

        protected SetValueStepExecutor(BasePmanStep step, @Nonnull StepContext context) throws InterruptedException, IOException {
            super(step, context);
        }

        private SetValueTextStep getMyStep() {
            if (this.step instanceof SetValueTextStep) {
                return (SetValueTextStep) step;
            }
            throw new RuntimeException("The step class is not applicable");
        }

        @Override
        protected Object run() throws IOException, InterruptedException {
            if (getValueName() == null || getValueName().isEmpty()) {
                throw new IllegalArgumentException("valueName argument must be not null");
            }

            log.info("Value is prepared for send to PMan. accountId={}; shelfId={}, valueName={}, value={}",
                    getEffectiveAccountId(),
                    getEffectiveShelfId(),
                    getValueName(),
                    getMyStep().getValue());

            ShelfValue added;
            if (step.isDryRun()) {
                listener.getLogger().println("Because dryRun=true the real value won't be set");
                added = ShelfValue.builder()
                        .name(getValueName())
                        .description("dry run")
                        .tags("dry-run")
                        .value(getMyStep().getValue())
                        .build();
            } else {
                added = pmanHttpClient.addValue(getEffectiveAccountId(), getEffectiveShelfId(), getValueName(), getMyStep().getValue());
            }

            listener.getLogger().println("Value created or updated: " + added);
            return added.getValue();
        }
    }

}

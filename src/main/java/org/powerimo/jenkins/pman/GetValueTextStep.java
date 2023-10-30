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
import lombok.extern.slf4j.Slf4j;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.powerimo.pman.dto.ShelfValue;

import java.io.IOException;
import java.util.Set;

@Slf4j
public class GetValueTextStep extends BasePmanStep {
    private static final long serialVersionUID = 5188962788139782713L;

    @Getter
    private final String valueName;

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet
                    .of(Launcher.class, FilePath.class, Run.class, TaskListener.class, EnvVars.class);
        }

        @Override
        public String getFunctionName() {
            return "pmanGetValueText";
        }
    }

    @DataBoundConstructor
    public GetValueTextStep(String apiKey, String shelfId, String valueName) {
        this.valueName = valueName;
        setApiKey(apiKey);
        setShelfId(shelfId);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GetValueStepExecutor(this, context);
    }


    public static class GetValueStepExecutor extends BasePmanExecutor {
        private static final long serialVersionUID = -8912678341956289050L;

        protected GetValueStepExecutor(BasePmanStep step, @Nonnull StepContext context) throws InterruptedException, IOException {
            super(step, context);
        }

        private GetValueTextStep getMyStep() {
            if (this.step instanceof GetValueTextStep) {
                return (GetValueTextStep) step;
            }
            throw new RuntimeException("The step class is not applicable");
        }

        @Override
        protected Object run() {
            var vName = getMyStep().getValueName();

            if (vName == null || vName.isEmpty()) {
                throw new IllegalArgumentException("valueName argument must be not null");
            }

            log.info("getting value: {}", vName);

            ShelfValue shelfValue;
            if (step.isDryRun()) {
                shelfValue = ShelfValue.builder()
                        .name(vName)
                        .value("DRY RUN SAMPLE VALUE")
                        .tags("dry-run")
                        .description("DRY RUN SAMPLE VALUE")
                        .build();
            } else {
                try {
                    shelfValue = pmanHttpClient.getValue(
                            getEffectiveAccountId(),
                            getEffectiveShelfId(),
                            vName
                    );
                    log.info("shelfValue: {}", shelfValue);
                } catch (Exception ex) {
                    return ex.getMessage();
                }
            }
            listener.getLogger().println("Got PMan value: " + shelfValue.getValue());

            return shelfValue.getValue();
        }
    }

}

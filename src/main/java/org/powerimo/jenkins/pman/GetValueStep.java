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
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.powerimo.pman.dto.ShelfValue;

import java.io.IOException;
import java.util.Set;

@Getter
public class GetValueStep extends BasePmanStep {
    private static final long serialVersionUID = -6875024203992113582L;

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
            return "pmanGetValue";
        }
    }

    @DataBoundConstructor
    public GetValueStep(String apiKey, String shelfId, String valueName) {
        this.valueName = valueName;
        setApiKey(apiKey);
        setShelfId(shelfId);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GetValueStepExecutor(this, context);
    }

    public static class GetValueStepExecutor extends BasePmanExecutor {
        private static final long serialVersionUID = 895465461950089330L;

        protected GetValueStepExecutor(BasePmanStep step, @Nonnull StepContext context) throws InterruptedException, IOException {
            super(step, context);
        }

        private GetValueStep getValueStep() {
            if (step instanceof GetValueStep) {
                return (GetValueStep) step;
            }
            throw new RuntimeException("The step class is not applicable");
        }

        @Override
        protected Object run() throws IOException, InterruptedException {
            var accountId = getEffectiveAccountId();
            var shelfId = getEffectiveShelfId();
            var vName = getValueStep().getValueName();

            if (vName == null || vName.isEmpty()) {
                throw new IllegalArgumentException("valueName argument must be not null");
            }

            listener.getLogger().println("Getting value for accountId="
                    + accountId.toString()
                    + "; shelfId=" + shelfId
                    + "; valueName=" + vName
                    + "; url=" + getConfig().getUrl()
            );

            ShelfValue shelfValue;
            if (step.isDryRun()) {
                shelfValue = ShelfValue.builder()
                        .name(vName)
                        .value("DRY RUN SAMPLE VALUE")
                        .tags("dry-run")
                        .description("DRY RUN SAMPLE VALUE")
                        .build();
            } else {
                shelfValue = pmanHttpClient.getValue(
                        accountId,
                        shelfId,
                        vName
                );
            }
            listener.getLogger().println("Got value: " + shelfValue);
            listener.getLogger().println("Result: " + shelfValue.getValue());

            return shelfValue;
        }
    }

}

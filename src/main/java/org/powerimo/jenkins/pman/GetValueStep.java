package org.powerimo.jenkins.pman;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jakarta.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Set;

public class GetValueStep extends BasePmanStep {
    private static final long serialVersionUID = -6875024203992113582L;

    private final String name;

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
    public GetValueStep(String name) {
        this.name = name;
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

        @Override
        protected Object run() {
            getListener().getLogger().println(getLogPrefix() + " PLUGIN HAS BEEN EXECUTED");
            String result = getLogPrefix() + " success:" + (getStep().getName() == null ? "NULL VALUE" : getStep().getName());
            getListener().getLogger().println(result);
            return result;
        }
    }

    public static class Execution extends StepExecution {
        private final transient TaskListener listener;
        private final transient Launcher launcher;
        private final GetValueStep step;
        private Throwable stopCause;

        protected Execution(GetValueStep step, @Nonnull StepContext context)
                throws IOException, InterruptedException {
            super(context);
            listener = context.get(TaskListener.class);
            launcher = context.get(Launcher.class);
            this.step = step;
        }

        @Override
        public boolean start() {
            try {
                listener.getLogger().println("PLUGIN HAS BEEN EXECUTED");
                String result = "success:" + step.name;
                getContext().onSuccess(result);
            } catch (Throwable ex) {
                if (stopCause == null) {
                    getContext().onFailure(ex);
                } else {
                    stopCause.addSuppressed(ex);
                }
            }
            return false;
        }
    }

}

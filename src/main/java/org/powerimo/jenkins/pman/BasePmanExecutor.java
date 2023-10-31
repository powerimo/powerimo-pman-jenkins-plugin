package org.powerimo.jenkins.pman;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.ClassLoaderSanityThreadFactory;
import hudson.util.DaemonThreadFactory;
import hudson.util.NamingThreadFactory;
import io.jenkins.cli.shaded.org.slf4j.MDC;
import jakarta.annotation.Nonnull;
import jenkins.model.Jenkins;
import lombok.extern.slf4j.Slf4j;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.powerimo.http.okhttp.BaseOkHttpApiClientLocalConfig;
import org.powerimo.http.okhttp.DefaultPayloadConverter;
import org.powerimo.pman.client.PmanHttpClient;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class BasePmanExecutor<T> extends StepExecution {
    private static ExecutorService executorService;
    private transient volatile Future<?> task;
    private transient String threadName;
    private transient Throwable stopCause;
    protected transient final TaskListener listener;
    protected transient final Launcher launcher;
    protected transient final BasePmanStep step;
    protected transient final PmanHttpClient pmanHttpClient;
    protected transient final BaseOkHttpApiClientLocalConfig config;

    protected BasePmanExecutor(BasePmanStep step, @Nonnull StepContext context) throws InterruptedException, IOException {
        super(context);
        listener = context.get(TaskListener.class);
        launcher = context.get(Launcher.class);
        this.step = step;

        config = BaseOkHttpApiClientLocalConfig.builder()
                .url("https://app.powerimo.cloud/pman")
                .apiKey(getEffectiveApiKey())
                .build();
        pmanHttpClient = new PmanHttpClient();
        pmanHttpClient.setConfig(config);
        pmanHttpClient.setPayloadConverter(new DefaultPayloadConverter());
    }

    static synchronized ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool(
                    new NamingThreadFactory(
                            new ClassLoaderSanityThreadFactory(new DaemonThreadFactory()),
                            "org.powerimo.jenkins.pman.BasePmanExecutor"
                    )
            );
        }
        return executorService;
    }

    protected abstract T run() throws Exception;

    @Override
    public boolean start() throws Exception {
        var auth = Jenkins.getAuthentication2();
        AtomicBoolean result = new AtomicBoolean(false);
        task = getExecutorService().submit(() -> {
            threadName = Thread.currentThread().getName();
            try {
                MDC.put("execution.id", UUID.randomUUID().toString());
                T ret;
                try (ACLContext acl = ACL.as2(auth)) {
                    ret = run();
                }
                getContext().onSuccess(ret);
                result.set(true);
            } catch (Throwable ex) {
                if (stopCause == null) {
                    getContext().onFailure(ex);
                } else {
                    stopCause.addSuppressed(ex);
                }
            } finally {
                MDC.clear();
            }
        });
        return false;
    }

    @Override
    public void stop(Throwable cause) throws Exception {
        if (task != null) {
            stopCause = cause;
            task.cancel(true);
        }
        super.stop(cause);
    }

    @Override
    public void onResume() {
        listener.getLogger().println("");
        getContext().onFailure(
                new Exception("Resume after a restart not supported for non-blocking synchronous steps"));
    }

    @Override
    public String getStatus() {
        if (threadName != null) {
            return "running in thread: " + threadName;
        } else {
            return "not yet scheduled";
        }
    }

    protected BaseOkHttpApiClientLocalConfig getConfig() {
        return config;
    }

    public UUID getShelfId() {
        return step.getShelfId();
    }

    public String getValueName() {
        return step.getValueName();
    }

    protected String getEffectiveApiKey() throws IOException, InterruptedException {
        if (step.getApiKey() != null)
            return step.getApiKey();
        EnvVars envVars = getContext().get(EnvVars.class);
        return envVars.get(PluginConst.ENV_VAR_PMAN_API_KEY);
    }

    protected UUID getEffectiveShelfId() throws IOException, InterruptedException {
        if (step.getShelfIdString() != null && !step.getShelfIdString().isEmpty()) {
            log.debug("shelfId string ({}) will be converted to UUID", step.getShelfIdString());
            return getShelfId();
        }
        log.trace("shelfId value for the step cannot be used: {}", step.getShelfIdString());
        EnvVars envVars = getContext().get(EnvVars.class);
        assert envVars != null;
        String s = envVars.get(PluginConst.ENV_VAR_PMAN_SHELF_ID);
        var data = s != null ? UUID.fromString(s) : null;
        log.info("extracted effective ShelfID: {}", data);
        return data;
    }

    protected UUID getEffectiveAccountId() throws IOException, InterruptedException {
        // get value from arguments
        var s = step.getAccountIdStringFromApiKey();
        if (s != null) {
            try {
                return UUID.fromString(s);
            } catch (Exception ex) {
                throw new IllegalArgumentException("apiKey argument doesn't contains a valid account ID (UUID)" );
            }
        }

        // get value from EnvVars
        EnvVars envVars = getContext().get(EnvVars.class);
        assert envVars != null;

        s = envVars.get(PluginConst.ENV_VAR_PMAN_API_KEY);
        if (s == null) {
            throw new PmanJenkinsException("No effective API Key found (parameter apiKey or environment variable " + PluginConst.ENV_VAR_PMAN_API_KEY);
        }

        try {
            var accountPart = BasePmanStep.getAccountPartFromApiKey(s);
            var accountId = UUID.fromString(accountPart);
            log.info("extracted accountId: {}", accountId);
            return accountId;
        } catch (Exception ex) {
            throw new IllegalArgumentException(PluginConst.ENV_VAR_PMAN_API_KEY + " environment variable doesn't contains a valid account ID (UUID)" );
        }
    }
}

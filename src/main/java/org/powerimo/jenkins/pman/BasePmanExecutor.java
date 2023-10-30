package org.powerimo.jenkins.pman;

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
import lombok.Getter;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.powerimo.http.okhttp.BaseOkHttpApiClientLocalConfig;
import org.powerimo.http.okhttp.BaseOkHttpClientConfig;
import org.powerimo.pman.client.PmanHttpClient;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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
                .build();
        pmanHttpClient = new PmanHttpClient();
        pmanHttpClient.setConfig(config);
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

    protected String getLogPrefix() {
        return step != null ? step.getLogPrefix() : "[StepExecutor]";
    }

    protected void checkApiKey() {
        step.getAccountId();
    }

    protected BaseOkHttpApiClientLocalConfig getConfig() {
        return config;
    }
}

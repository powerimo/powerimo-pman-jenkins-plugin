package org.powerimo.jenkins.pman;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.powerimo.common.utils.Utils;

public class SetValueTextStepTest extends BaseTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void build_success() throws Exception {
        // Создание и настройка Pipeline
        WorkflowJob job = jenkinsRule.createProject(WorkflowJob.class, "test-step-execution");

        String script = Utils.readTextResource("SetValue.groovy");
        job.setDefinition(new CpsFlowDefinition(
                script,
                true));

        // Запуск и ожидание завершения Pipeline
        WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));

        // Проверка лога на наличие ожидаемого результата
        jenkinsRule.assertLogContains("Finished: SUCCESS", run);
    }
}
package org.powerimo.jenkins.pman;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.jvnet.hudson.test.JenkinsRule;
import org.powerimo.pman.dto.ShelfValue;

import java.util.UUID;

public class GetValueStepTest extends BaseTest {
    GetValueStep.GetValueStepExecutor stepExecution;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();


    @Test
    public void testBuild() throws Exception {
        // Создание и настройка Pipeline
        WorkflowJob job = jenkinsRule.createProject(WorkflowJob.class, "test-step-execution");

        String script = "node {\n" +
                "  def result = pmanGetValue(dryRun: true, valueName: 'test.value', apiKey: \"" + UUID.randomUUID() + ":secretsecret\")\n" + // Использование вашего шага
                "  echo \"Result is ${result}\"\n" + // Проверяем значение
                "}\n";
        job.setDefinition(new CpsFlowDefinition(
                script,
                true));

        // Запуск и ожидание завершения Pipeline
        WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));

        // Проверка лога на наличие ожидаемого результата
        jenkinsRule.assertLogContains("Finished: SUCCESS", run);
    }

    @Test
    public void GetValue_noParams() throws Exception {
        final String apiKey = UUID.randomUUID() + ":mySecret";
        final GetValueStep step = new GetValueStep(apiKey, UUID.randomUUID().toString(), "sampleValue");
        step.setDryRun(true);
        stepExecution = new GetValueStep.GetValueStepExecutor(step, contextMock);

        // Execute and assert Test.
        var result = stepExecution.run();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(ShelfValue.class, result.getClass());
    }

}

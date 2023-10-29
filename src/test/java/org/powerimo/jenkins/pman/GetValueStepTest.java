package org.powerimo.jenkins.pman;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;

import java.io.IOException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class GetValueStepTest extends BaseTest {
    final String path = "test.sh";
    final String filterBy = "name";
    final String filterRegex = null;

    @Mock
    FilePath filePathMock;

    GetValueStep.GetValueStepExecutor stepExecution;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    final String name = "Bobby";

    //@Test
    public void testBuild() throws Exception {
        // Создание и настройка Pipeline
        WorkflowJob job = jenkinsRule.createProject(WorkflowJob.class, "test-step-execution");
        job.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def result = pmanGetValue('someName')\n" + // Использование вашего шага
                        "  echo \"Result is ${result}\"\n" + // Проверяем значение
                        "}\n",
                true));

        // Запуск и ожидание завершения Pipeline
        WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));

        // Проверка лога на наличие ожидаемого результата
        jenkinsRule.assertLogContains("Result is success", run);
    }

    @Test
    public void GetValue_noParams() throws Exception {
        final GetValueStep step = new GetValueStep("aaa");
        step.setDryRun(true);
        stepExecution = new GetValueStep.GetValueStepExecutor(step, contextMock);


        // Execute and assert Test.
        var result = stepExecution.run();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(String.class, result.getClass());
        // Assertions.assertThrows(IllegalArgumentException.class, () -> stepExecution.run());
    }

    @Before
    public void setup() throws IOException, InterruptedException {
        when(filePathMock.child(any())).thenReturn(filePathMock);
        when(filePathMock.exists()).thenReturn(true);
        when(filePathMock.isDirectory()).thenReturn(false);
        when(filePathMock.getRemote()).thenReturn(path);

        when(contextMock.get(FilePath.class)).thenReturn(filePathMock);

    }
}

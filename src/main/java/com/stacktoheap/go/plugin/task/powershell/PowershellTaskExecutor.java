package com.stacktoheap.go.plugin.task.powershell;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.*;

import java.util.ArrayList;
import java.util.List;

public class PowershellTaskExecutor implements TaskExecutor {

    @Override
    public ExecutionResult execute(TaskConfig taskConfig, TaskExecutionContext taskExecutionContext) {
        ProcessBuilder powershell = createPowershellCommand(taskExecutionContext, taskConfig);

        Console console = taskExecutionContext.console();
        console.printLine("Launching command: " + powershell.command());

        try {
            Process process = powershell.start();
            console.readErrorOf(process.getErrorStream());
            console.readOutputOf(process.getInputStream());

            int exitCode = process.waitFor();
            process.destroy();

            if (exitCode != 0) {
                return ExecutionResult.failure("Build Failure");
            }
        }
        catch(Exception e) {
            return ExecutionResult.failure("Failed while running Powershell task ", e);
        }

        return ExecutionResult.success("Build Success");
    }

    ProcessBuilder createPowershellCommand(TaskExecutionContext taskContext, TaskConfig taskConfig) {
        String scriptFile = taskConfig.getValue(PowershellTask.FILE);
        String noProfile = taskConfig.getValue(PowershellTask.NOPROFILE);

        List<String> command = new ArrayList<String>();
        command.add("powershell");
        command.add("-File");
        command.add(scriptFile);
        if (noProfile.equals("true")) {
            command.add("-NoProfile");
        }

        return new ProcessBuilder(command);
    }
}
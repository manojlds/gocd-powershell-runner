package com.stacktoheap.go.plugin.task.powershell;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.*;

import java.io.File;
import java.util.*;

public class PowershellTaskExecutor implements TaskExecutor {

    private static final Map<String, String> PowershellPath = new HashMap<String, String>();

    static {
        PowershellPath.put("x86", "C:\\Windows\\SysWOW64\\WindowsPowerShell\\v1.0\\powershell.exe");
        PowershellPath.put("x64", "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe");
    }

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
        String bitness = taskConfig.getValue(PowershellTask.BITNESS);
        String scriptFile = taskConfig.getValue(PowershellTask.FILE);
        String noProfile = taskConfig.getValue(PowershellTask.NOPROFILE);
        String bypassExecutionPolicy = taskConfig.getValue(PowershellTask.BYPASS);

        List<String> command = new ArrayList<String>();
        command.add(PowershellPath.get(bitness));
        command.add("-NonInteractive");
        command.add("-File");
        command.add(scriptFile);
        if (noProfile != null && noProfile.equals("true")) {
            command.add("-NoProfile");
        }
        if (bypassExecutionPolicy != null && bypassExecutionPolicy.equals("true")) {
            command.add("-ExecutionPolicy ByPass");
            command.add("ByPass");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(taskContext.workingDir()));
        return processBuilder;
    }
}
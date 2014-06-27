package com.stacktoheap.go.plugin.task.powershell;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.*;
import com.thoughtworks.go.plugin.api.task.Console;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
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

            PutScriptIntoPowershellStdin(taskExecutionContext, taskConfig, process);

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

    private void PutScriptIntoPowershellStdin(TaskExecutionContext taskExecutionContext, TaskConfig taskConfig, Process process) throws IOException {
        String executionMode = taskConfig.getValue(PowershellTask.MODE);
        if(executionMode.equals("Command")) {
            OutputStream outputStream = process.getOutputStream();
            String file = taskConfig.getValue(PowershellTask.FILE);
            File workingDir = new File(taskExecutionContext.workingDir());
            File script = new File(workingDir, file);
            InputStream fis = new FileInputStream(script);
            byte[] buffer = new byte[1024];
            int read;
            while((read = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            fis.close();
        }
    }

    ProcessBuilder createPowershellCommand(TaskExecutionContext taskContext, TaskConfig taskConfig) {
        String bitness = taskConfig.getValue(PowershellTask.BITNESS);

        List<String> command = new ArrayList<String>();
        command.add(PowershellPath.get(bitness));
        AddPowershellArguments(taskConfig, command);
        AddScript(taskConfig, command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.directory(new File(taskContext.workingDir()));
        return processBuilder;
    }

    private void AddScript(TaskConfig taskConfig, List<String> command) {
        String scriptFile = taskConfig.getValue(PowershellTask.FILE);
        String executionMode = taskConfig.getValue(PowershellTask.MODE);

        if(executionMode.equals("File")) {
            command.add("-File");
            command.add(scriptFile);

            String scriptParameters = taskConfig.getValue(PowershellTask.SCRIPTPARAMETERS);
            ConvertToParameterList(command, scriptParameters);

        } else {
            command.add("-Command");
            command.add("-");
        }
    }

    private void AddPowershellArguments(TaskConfig taskConfig, List<String> command) {
        String noProfile = taskConfig.getValue(PowershellTask.NOPROFILE);
        String bypassExecutionPolicy = taskConfig.getValue(PowershellTask.BYPASS);
        String noLogo = taskConfig.getValue(PowershellTask.NOLOGO);
        String parameters = taskConfig.getValue(PowershellTask.PARAMETERS);

        command.add("-NonInteractive");
        if (noProfile != null && noProfile.equals("true")) {
            command.add("-NoProfile");
        }
        if (bypassExecutionPolicy != null && bypassExecutionPolicy.equals("true")) {
            command.add("-ExecutionPolicy ByPass");
            command.add("ByPass");
        }
        if (noLogo != null && noLogo.equals("true")) {
            command.add("-NoLogo");
        }

        ConvertToParameterList(command, parameters);
    }

    private void ConvertToParameterList(List<String> command, String parameters) {
        if (!StringUtils.isBlank(parameters)) {
            for (String parameter : parameters.split("\\s+")) {
                command.add(parameter);
            }
        }
    }
}
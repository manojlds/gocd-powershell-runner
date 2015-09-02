package com.stacktoheap.go.plugin.task.powershell;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.*;
import com.thoughtworks.go.plugin.api.task.Console;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.*;

public class PowershellTaskExecutor implements TaskExecutor {

    private static final HashMap<String, String> PowershellPath = new HashMap<String, String>();
    private static final String BasePowershellPath = "C:\\Windows\\%s\\WindowsPowerShell\\v1.0\\powershell.exe";

    static {
        String arch = System.getProperty("os.arch");
        Boolean is64bit = arch.contains("64");

        if(is64bit) {
            PowershellPath.put("x86", String.format(BasePowershellPath, "SysWOW64"));
            PowershellPath.put("x64", String.format(BasePowershellPath, "system32"));
        } else {
            PowershellPath.put("x86", String.format(BasePowershellPath, "system32"));
            PowershellPath.put("x64", String.format(BasePowershellPath, "sysnative"));
        }
    }

    @Override
    public ExecutionResult execute(TaskConfig taskConfig, TaskExecutionContext taskExecutionContext) {
        ProcessBuilder powershell = createPowershellCommand(taskExecutionContext, taskConfig);

        Console console = taskExecutionContext.console();
        console.printLine("Launching command: " + StringUtils.join(powershell.command(), " "));

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
            console.printLine(e.getMessage());
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
            InputStream fis = new FileInputStream(script.getAbsolutePath());
            byte[] buffer = new byte[1024];
            int read;
            while((read = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            fis.close();
            outputStream.close();
        }
    }

    ProcessBuilder createPowershellCommand(TaskExecutionContext taskContext, TaskConfig taskConfig) {
        String bitness = taskConfig.getValue(PowershellTask.BITNESS);

        List<String> command = new ArrayList<String>();
        command.add(PowershellPath.get(bitness));
        AddPowershellArguments(taskConfig, command);
        AddScript(taskConfig, command);

		Map<String, String> environmentVariables = taskContext.environment().asMap();
		
        ProcessBuilder processBuilder = new ProcessBuilder(command);
		if (environmentVariables != null && !environmentVariables.isEmpty()) {
            processBuilder.environment().putAll(environmentVariables);
        }
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
        if (noLogo != null && noLogo.equals("true")) {
            command.add("-NoLogo");
        }
        if (bypassExecutionPolicy != null && bypassExecutionPolicy.equals("true")) {
            command.add("-ExecutionPolicy");
            command.add("ByPass");
        }

        ConvertToParameterList(command, parameters);
    }

    private void ConvertToParameterList(List<String> command, String parameters) {
        if (!StringUtils.isBlank(parameters)) {
            Collections.addAll(command, parameters.split("[\r\n]+"));
        }
    }
}
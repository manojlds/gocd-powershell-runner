package com.stacktoheap.go.plugin.task.powershell;

import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@Extension
public class PowershellTask implements Task {
    public static final String BITNESS = "Bitness";
    public static final String FILE = "File";
    public static final String MODE = "Mode";
    public static final String NOPROFILE = "NoProfile";
    public static final String BYPASS = "Bypass";
    public static final String NOLOGO = "NoLogo";
    public static final String PARAMETERS = "Parameters";
    public static final String SCRIPTPARAMETERS = "ScriptParameters";

    @Override
    public TaskConfig config() {
        TaskConfig config = new TaskConfig();
        config.addProperty(BITNESS);
        config.addProperty(FILE);
        config.addProperty(MODE);
        config.addProperty(NOPROFILE);
        config.addProperty(BYPASS);
        config.addProperty(NOLOGO);
        config.addProperty(PARAMETERS);
        config.addProperty(SCRIPTPARAMETERS);
        return config;
    }

    @Override
    public TaskExecutor executor() {
        return new PowershellTaskExecutor();
    }

    @Override
    public TaskView view() {
        TaskView taskView = new TaskView() {
            @Override
            public String displayValue() {
                return "Powershell";
            }

            @Override
            public String template() {
                try {
                    return IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8");
                } catch (Exception e) {
                    return "Failed to find template: " + e.getMessage();
                }
            }
        };
        return taskView;
    }

    @Override
    public ValidationResult validate(TaskConfig configuration) {
        ValidationResult validationResult = new ValidationResult();
        String bitness = configuration.getValue(BITNESS);
        if (StringUtils.isBlank(bitness)) {
            validationResult.addError(new ValidationError(BITNESS, "Bitness must be specified"));
        }

        String scriptFile = configuration.getValue(FILE);
        if (StringUtils.isBlank(scriptFile)) {
            validationResult.addError(new ValidationError(FILE, "Script to be run cannot be empty"));
        }

        String executionMode = configuration.getValue(MODE);
        if (StringUtils.isBlank(executionMode)) {
            validationResult.addError(new ValidationError(MODE, "Execution mode must be specified"));
        }

        return validationResult;
    }
}

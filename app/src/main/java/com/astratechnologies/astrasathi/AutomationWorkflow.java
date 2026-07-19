package com.astratechnologies.astrasathi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AutomationWorkflow {
    public final String name;
    public final String source;
    public final List<WorkflowStep> steps;
    public final String error;

    public AutomationWorkflow(String name, String source, List<WorkflowStep> steps, String error) {
        this.name = name == null ? "ভয়েস workflow" : name;
        this.source = source == null ? "" : source;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.error = error == null ? "" : error;
    }

    public boolean isValid() { return error.isEmpty() && !steps.isEmpty(); }

    public boolean requiresConfirmation() {
        for (WorkflowStep step : steps) {
            if (step.command != null && (step.command.needsConfirmation()
                    || step.command.isProtectedUiAction()
                    || step.command.type == Command.Type.UNINSTALL_APP
                    || step.command.type == Command.Type.SECURITY_SETTINGS)) return true;
        }
        return false;
    }

    public String summary() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) out.append(" → ");
            out.append(steps.get(i).label);
        }
        return out.toString();
    }
}

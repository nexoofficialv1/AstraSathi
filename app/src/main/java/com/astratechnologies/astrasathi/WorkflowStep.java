package com.astratechnologies.astrasathi;

public final class WorkflowStep {
    public enum Kind { COMMAND, DELAY, WAIT_FOR_TEXT }

    public final Kind kind;
    public final Command command;
    public final long durationMs;
    public final String expectedText;
    public final String label;

    private WorkflowStep(Kind kind, Command command, long durationMs, String expectedText, String label) {
        this.kind = kind; this.command = command; this.durationMs = durationMs;
        this.expectedText = expectedText == null ? "" : expectedText;
        this.label = label == null ? "" : label;
    }

    public static WorkflowStep command(Command command, String label) {
        return new WorkflowStep(Kind.COMMAND, command, 0, "", label);
    }

    public static WorkflowStep delay(long durationMs, String label) {
        return new WorkflowStep(Kind.DELAY, null, durationMs, "", label);
    }

    public static WorkflowStep waitForText(String text, long timeoutMs) {
        return new WorkflowStep(Kind.WAIT_FOR_TEXT, null, timeoutMs, text,
                "‘" + text + "’ লেখা আসা পর্যন্ত অপেক্ষা");
    }
}

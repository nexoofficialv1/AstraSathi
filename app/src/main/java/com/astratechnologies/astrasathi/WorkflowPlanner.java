package com.astratechnologies.astrasathi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WorkflowPlanner {
    private final CommandRouter router = new CommandRouter();

    public AutomationWorkflow plan(String raw) {
        String normalized = BengaliText.normalize(raw)
                .replaceFirst("^(একটা|একটি)?\\s*(কাজ|কমান্ড)\\s*(করো|হলো|হচ্ছে)?\\s*", "");
        String[] segments = normalized.split("\\s+(?:তারপর|এরপর|তার\\s+পরে|এর\\s+পরে)\\s+");
        List<WorkflowStep> steps = new ArrayList<>();
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.isEmpty()) continue;
            Matcher delay = Pattern.compile("(\\d+)\\s*(সেকেন্ড|মিনিট)\\s+অপেক্ষা করো").matcher(segment);
            if (delay.matches()) {
                long value = Long.parseLong(delay.group(1));
                long millis = "মিনিট".equals(delay.group(2)) ? value * 60_000 : value * 1000;
                steps.add(WorkflowStep.delay(Math.min(millis, 10 * 60_000), segment));
                continue;
            }
            Matcher waitText = Pattern.compile("(.+?)\\s+লেখা\\s+(?:আসা|দেখা)\\s+পর্যন্ত অপেক্ষা করো").matcher(segment);
            if (waitText.matches()) {
                steps.add(WorkflowStep.waitForText(waitText.group(1).trim(), 15_000));
                continue;
            }
            Command command = router.parse(segment);
            if (command.type == Command.Type.UNKNOWN || command.type == Command.Type.WORKFLOW
                    || command.type == Command.Type.ROUTINE_SAVE || command.type == Command.Type.ROUTINE_RUN
                    || command.type == Command.Type.ROUTINE_LIST || command.type == Command.Type.MACRO_START
                    || command.type == Command.Type.MACRO_STOP || command.type == Command.Type.MACRO_CANCEL) {
                return new AutomationWorkflow("ভয়েস workflow", raw, steps,
                        (i + 1) + " নম্বর ধাপটি বুঝতে পারিনি: “" + segment + "”");
            }
            if (command.type == Command.Type.TRADE_ORDER)
                return new AutomationWorkflow("ভয়েস workflow", raw, steps,
                        "Financial order সাধারণ screen workflow-এর মধ্যে চালানো যাবে না।");
            steps.add(WorkflowStep.command(command, segment));
        }
        if (steps.size() < 2)
            return new AutomationWorkflow("ভয়েস workflow", raw, steps, "Workflow-এ অন্তত দুইটি ধাপ দরকার।");
        return new AutomationWorkflow("ভয়েস workflow", raw, steps, "");
    }
}

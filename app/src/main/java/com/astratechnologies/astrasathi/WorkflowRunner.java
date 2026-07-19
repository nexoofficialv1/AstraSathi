package com.astratechnologies.astrasathi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public final class WorkflowRunner {
    public interface Callback {
        void onProgress(int current, int total, String message);
        void onComplete(boolean success, String message);
    }

    private static final WorkflowRunner INSTANCE = new WorkflowRunner();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AutomationWorkflow workflow;
    private Callback callback;
    private Context context;
    private int index;
    private long waitStarted;
    private static volatile String lastSuccessfulSource = "";

    public static WorkflowRunner get() { return INSTANCE; }
    public static String getLastSuccessfulSource() { return lastSuccessfulSource; }
    public boolean isRunning() { return workflow != null; }

    public synchronized void run(Context context, AutomationWorkflow workflow, Callback callback) {
        cancel(false);
        this.context = context.getApplicationContext();
        this.workflow = workflow;
        this.callback = callback;
        this.index = 0;
        next();
    }

    public synchronized void cancel(boolean notify) {
        handler.removeCallbacksAndMessages(null);
        if (notify && callback != null) callback.onComplete(false, "Workflow বাতিল করা হয়েছে।");
        workflow = null; callback = null; context = null; index = 0;
    }

    private void next() {
        if (workflow == null) return;
        if (index >= workflow.steps.size()) {
            lastSuccessfulSource = workflow.source;
            finish(true, "সব " + workflow.steps.size() + "টি ধাপ সফল হয়েছে।");
            return;
        }
        WorkflowStep step = workflow.steps.get(index);
        if (callback != null) callback.onProgress(index + 1, workflow.steps.size(), step.label);
        switch (step.kind) {
            case DELAY -> handler.postDelayed(() -> { index++; next(); }, step.durationMs);
            case WAIT_FOR_TEXT -> { waitStarted = System.currentTimeMillis(); pollForText(step); }
            case COMMAND -> executeCommand(step.command);
        }
    }

    private void executeCommand(Command command) {
        DeviceController.Result result = VoiceActionExecutor.execute(context, command);
        if (!result.success) { finish(false, (index + 1) + " নম্বর ধাপে থেমেছি: " + result.message); return; }
        index++;
        handler.postDelayed(this::next, delayAfter(command.type));
    }

    private void pollForText(WorkflowStep step) {
        SathiAccessibilityService service = SathiAccessibilityService.get();
        if (service != null && service.hasVisibleText(step.expectedText)) {
            index++; next(); return;
        }
        if (System.currentTimeMillis() - waitStarted >= step.durationMs) {
            finish(false, "অপেক্ষার সময় শেষ: ‘" + step.expectedText + "’ লেখা আসেনি।"); return;
        }
        handler.postDelayed(() -> pollForText(step), 300);
    }

    private long delayAfter(Command.Type type) {
        return switch (type) {
            case OPEN_APP, WIFI_CONTROL, BLUETOOTH_CONTROL, LOCATION_SETTINGS,
                    SECURITY_SETTINGS, PRIVACY_SETTINGS, DISPLAY_SETTINGS, SOUND_SETTINGS,
                    APP_INFO, UNINSTALL_APP -> 1500;
            case CLICK_TEXT, CLICK_NUMBER -> 750;
            case TYPE_TEXT -> 500;
            default -> 350;
        };
    }

    private void finish(boolean success, String message) {
        Callback done = callback;
        workflow = null; callback = null; context = null; index = 0;
        if (done != null) done.onComplete(success, message);
    }
}

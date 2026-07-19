import com.astratechnologies.astrasathi.AutomationWorkflow;
import com.astratechnologies.astrasathi.Command;
import com.astratechnologies.astrasathi.WorkflowPlanner;

public class WorkflowPlannerTest {
    public static void main(String[] args) {
        WorkflowPlanner planner = new WorkflowPlanner();
        AutomationWorkflow flow = planner.plan("হোয়াটসঅ্যাপ খোলো তারপর সার্চে চাপ দাও তারপর লিখো রাহুল");
        check(flow.isValid(), flow.error);
        check(flow.steps.size() == 3, "Expected 3 steps");
        check(flow.steps.get(0).command.type == Command.Type.OPEN_APP, "Step 1 should open app");
        check(flow.steps.get(1).command.type == Command.Type.CLICK_TEXT, "Step 2 should click");
        check(flow.steps.get(2).command.type == Command.Type.TYPE_TEXT, "Step 3 should type");

        AutomationWorkflow wait = planner.plan("ইউটিউব খোলো তারপর 2 সেকেন্ড অপেক্ষা করো তারপর নিচে স্ক্রল করো");
        check(wait.isValid() && wait.steps.size() == 3, "Delay workflow invalid");
        check(wait.steps.get(1).durationMs == 2000, "Delay should be 2 seconds");
        System.out.println("WorkflowPlanner-এর সব পরীক্ষা সফল হয়েছে।");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}

package com.astratechnologies.astrasathi;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;

public class SathiAccessibilityService extends AccessibilityService {
    private static volatile SathiAccessibilityService instance;
    private volatile String currentPackage = "";
    private final List<ActionableElement> lastActionableSnapshot = new ArrayList<>();
    private String snapshotPackage = "";
    private int snapshotWindowId = -1;

    public static boolean isConnected() { return instance != null; }
    public static SathiAccessibilityService get() { return instance; }
    public String getCurrentPackageName() { return currentPackage; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getPackageName() != null
                && event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            currentPackage = event.getPackageName().toString();
        MacroRecorder.get().onAccessibilityEvent(event);
    }

    @Override public void onInterrupt() { }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        if (MacroRecorder.get().isRecording()) MacroRecorder.get().cancel();
        super.onDestroy();
    }

    public boolean global(int action) { return performGlobalAction(action); }

    public boolean clickByText(String wanted) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || wanted == null || wanted.trim().isEmpty()) return false;
        AccessibilityNodeInfo selected = findBestLabelMatch(root, wanted);
        AccessibilityNodeInfo clickable = clickableAncestor(selected);
        return clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    public boolean clickByExactText(String... labels) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || labels == null) return false;
        Set<String> wanted = new LinkedHashSet<>();
        for (String label : labels) {
            String normalized = BengaliText.normalize(label);
            if (!normalized.isEmpty()) wanted.add(normalized);
        }
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (wanted.contains(BengaliText.normalize(nodeLabel(node)))) {
                AccessibilityNodeInfo clickable = clickableAncestor(node);
                if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return false;
    }

    public synchronized List<ActionableElement> listActionableElements(int max) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return Collections.emptyList();
        int limit = Math.max(1, Math.min(30, max));
        List<ActionableElement> found = collectActionableElements(root, limit);
        lastActionableSnapshot.clear();
        lastActionableSnapshot.addAll(found);
        snapshotPackage = currentPackage;
        snapshotWindowId = root.getWindowId();
        return new ArrayList<>(found);
    }

    public String describeActionableElements() {
        List<ActionableElement> elements = listActionableElements(15);
        if (elements.isEmpty()) return "এই screen-এ চাপার মতো কোনো control পাইনি।";
        StringBuilder out = new StringBuilder("এই screen-এ ");
        for (ActionableElement element : elements) {
            if (out.length() > 20) out.append("। ");
            out.append(element.index).append(" নম্বর ").append(element.spokenLabel());
        }
        out.append("। বলতে পারেন—৩ নম্বরে চাপ দাও।");
        return out.toString();
    }

    public synchronized boolean clickActionNumber(int number) {
        if (number < 1) return false;
        if (lastActionableSnapshot.isEmpty()) listActionableElements(15);
        if (number > lastActionableSnapshot.size() || !snapshotPackage.equals(currentPackage)) return false;
        ActionableElement wanted = lastActionableSnapshot.get(number - 1);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getWindowId() != snapshotWindowId) return false;
        AccessibilityNodeInfo selected = findSnapshotMatch(root, wanted);
        AccessibilityNodeInfo clickable = clickableAncestor(selected);
        return clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    public boolean typeIntoFocusedField(String value) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (field == null || !field.isEditable()) field = findEditable(root);
        if (field == null || field.isPassword()) return false;
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    public String readVisibleText() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "স্ক্রিনের লেখা পড়া যাচ্ছে না।";
        Set<String> lines = new LinkedHashSet<>();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty() && lines.size() < 60) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isPassword()) continue;
            addText(lines, node.getText());
            addText(lines, node.getContentDescription());
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        if (lines.isEmpty()) return "স্ক্রিনে পড়ার মতো লেখা পাইনি।";
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (out.length() + line.length() > 1800) break;
            if (out.length() > 0) out.append("। ");
            out.append(line);
        }
        return out.toString();
    }

    public boolean hasVisibleText(String wanted) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        return root != null && findContains(root, wanted) != null;
    }

    public boolean scroll(boolean forward) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo scrollable = findScrollable(root);
        if (scrollable != null) return scrollable.performAction(forward
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        return swipe(forward ? 0 : 1);
    }

    public boolean swipe(int direction) {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        float startX = width * 0.5f, startY = height * 0.72f, endX = startX, endY = height * 0.28f;
        if (direction == 1) { startY = height * 0.28f; endY = height * 0.72f; }
        if (direction == 2) { startX = width * 0.78f; endX = width * 0.22f; startY = endY = height * 0.5f; }
        if (direction == 3) { startX = width * 0.22f; endX = width * 0.78f; startY = endY = height * 0.5f; }
        Path path = new Path();
        path.moveTo(startX, startY); path.lineTo(endX, endY);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 350)).build();
        return dispatchGesture(gesture, null, null);
    }

    private AccessibilityNodeInfo findContains(AccessibilityNodeInfo root, String wanted) {
        String needle = BengaliText.normalize(wanted);
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>(); queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            String text = node.getText() == null ? "" : BengaliText.normalize(node.getText().toString());
            String desc = node.getContentDescription() == null ? "" : BengaliText.normalize(node.getContentDescription().toString());
            if (text.contains(needle) || desc.contains(needle)) return node;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i); if (child != null) queue.add(child);
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findBestLabelMatch(AccessibilityNodeInfo root, String wanted) {
        String query = BengaliText.normalize(wanted);
        AccessibilityNodeInfo best = null;
        double bestScore = 0;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            String label = nodeLabel(node);
            String idTail = resourceTail(node.getViewIdResourceName());
            double score = Math.max(TextSimilarity.score(query, label), TextSimilarity.score(query, idTail));
            if (score > bestScore && clickableAncestor(node) != null) {
                best = node; bestScore = score;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return bestScore >= 0.68 ? best : null;
    }

    private List<ActionableElement> collectActionableElements(AccessibilityNodeInfo root, int limit) {
        List<ActionableElement> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty() && result.size() < limit) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isEnabled() && !node.isPassword() && (node.isClickable() || node.isEditable())) {
                String label = nodeLabel(node);
                String viewId = value(node.getViewIdResourceName());
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                String key = viewId + "|" + label + "|" + bounds.flattenToString();
                if (seen.add(key)) result.add(new ActionableElement(result.size() + 1, label, viewId,
                        value(node.getClassName()), node.isEditable(), bounds.left, bounds.top, bounds.right, bounds.bottom));
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return result;
    }

    private AccessibilityNodeInfo findSnapshotMatch(AccessibilityNodeInfo root, ActionableElement wanted) {
        AccessibilityNodeInfo best = null;
        double bestScore = 0;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isEnabled() && !node.isPassword() && (node.isClickable() || node.isEditable())) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                String viewId = value(node.getViewIdResourceName());
                double score = TextSimilarity.score(wanted.label, nodeLabel(node));
                if (!wanted.viewId.isEmpty() && wanted.viewId.equals(viewId)) score += 0.6;
                if (wanted.left == bounds.left && wanted.top == bounds.top
                        && wanted.right == bounds.right && wanted.bottom == bounds.bottom) score += 0.25;
                if (score > bestScore) { best = node; bestScore = score; }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return bestScore >= 0.68 ? best : null;
    }

    private AccessibilityNodeInfo clickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null && !current.isClickable()) current = current.getParent();
        return current;
    }

    private String nodeLabel(AccessibilityNodeInfo node) {
        if (node == null) return "";
        String text = value(node.getText());
        if (!text.isEmpty()) return text;
        String description = value(node.getContentDescription());
        if (!description.isEmpty()) return description;
        String hint = value(node.getHintText());
        if (!hint.isEmpty()) return hint;
        String descendant = firstDescendantLabel(node);
        return descendant.isEmpty() ? resourceTail(node.getViewIdResourceName()) : descendant;
    }

    private String firstDescendantLabel(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) queue.add(child);
        }
        int visited = 0;
        while (!queue.isEmpty() && visited++ < 20) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (!node.isPassword()) {
                String text = value(node.getText());
                if (!text.isEmpty()) return text;
                String description = value(node.getContentDescription());
                if (!description.isEmpty()) return description;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return "";
    }

    private String resourceTail(String viewId) {
        if (viewId == null || viewId.isEmpty()) return "";
        int slash = viewId.lastIndexOf('/');
        return (slash >= 0 ? viewId.substring(slash + 1) : viewId).replace('_', ' ').trim();
    }

    private String value(Object value) { return value == null ? "" : value.toString().trim(); }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>(); queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isEditable()) return node;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i); if (child != null) queue.add(child);
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo root) {
        if (root == null) return null;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>(); queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isScrollable()) return node;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i); if (child != null) queue.add(child);
            }
        }
        return null;
    }

    private void addText(Set<String> lines, CharSequence value) {
        if (value == null) return;
        String clean = value.toString().replaceAll("\\s+", " ").trim();
        if (!clean.isEmpty() && clean.length() < 300) lines.add(clean);
    }
}

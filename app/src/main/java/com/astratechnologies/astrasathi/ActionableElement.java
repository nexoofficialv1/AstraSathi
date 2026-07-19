package com.astratechnologies.astrasathi;

public final class ActionableElement {
    public final int index;
    public final String label;
    public final String viewId;
    public final String className;
    public final boolean editable;
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    public ActionableElement(int index, String label, String viewId, String className,
                             boolean editable, int left, int top, int right, int bottom) {
        this.index = index;
        this.label = label == null ? "" : label;
        this.viewId = viewId == null ? "" : viewId;
        this.className = className == null ? "" : className;
        this.editable = editable;
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
    }

    public String spokenLabel() {
        return label.isEmpty() ? (editable ? "লেখার ঘর" : "নামহীন বোতাম") : label;
    }
}

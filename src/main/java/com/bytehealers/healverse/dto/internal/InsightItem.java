package com.bytehealers.healverse.dto.internal;

public class InsightItem {
    private String content;
    private String type; // BETTER, SUGGESTION, WARNING, DANGER

    public InsightItem() {}

    public InsightItem(String content, String type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
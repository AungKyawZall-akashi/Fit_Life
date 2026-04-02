package com.example.fitlife.models;

import java.util.List;

public class PlanItem {
    private String documentId;
    private String userId;
    private String type;
    private String key;
    private String title;
    private String notes;
    private boolean addedToWeeklyPlan;
    private long createdAt;
    private boolean systemDefault;
    private List<PlanEntry> entries;
    private boolean purchased;
    private long expiresAt;

    public PlanItem() {}

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isAddedToWeeklyPlan() { return addedToWeeklyPlan; }
    public void setAddedToWeeklyPlan(boolean addedToWeeklyPlan) { this.addedToWeeklyPlan = addedToWeeklyPlan; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isSystemDefault() { return systemDefault; }
    public void setSystemDefault(boolean systemDefault) { this.systemDefault = systemDefault; }

    public List<PlanEntry> getEntries() { return entries; }
    public void setEntries(List<PlanEntry> entries) { this.entries = entries; }

    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}

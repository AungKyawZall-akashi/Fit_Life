package com.example.fitlife.models;

public class PlanEntry {
    private String name;
    private String instructions;
    private String meta;
    private String imageKey;

    public PlanEntry() {}

    public PlanEntry(String name, String instructions) {
        this.name = name;
        this.instructions = instructions;
    }

    public PlanEntry(String name, String instructions, String meta, String imageKey) {
        this.name = name;
        this.instructions = instructions;
        this.meta = meta;
        this.imageKey = imageKey;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }

    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }
}

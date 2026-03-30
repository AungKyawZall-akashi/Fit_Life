package com.example.fitlife.models;

public class Workout {
    private String documentId;
    private String userId; // Dedicated field for user isolation
    private int id;
    private String title;
    private String description;
    private int duration;
    private boolean isCompleted;
    private String exercises;
    private String equipment;
    private String instructions;

    public Workout() {}

    public Workout(int id, String title, String description, int duration, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.isCompleted = isCompleted;
    }

    public Workout(int id, String title, String description, int duration, boolean isCompleted, String exercises, String equipment, String instructions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.isCompleted = isCompleted;
        this.exercises = exercises;
        this.equipment = equipment;
        this.instructions = instructions;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getExercises() { return exercises; }
    public void setExercises(String exercises) { this.exercises = exercises; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}

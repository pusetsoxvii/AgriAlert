package com.example.agrialert.model;

public class Alert {
    private int id;
    private String title;
    private String message;
    private String dateCreated;

    public Alert(int id, String title, String message, String dateCreated) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.dateCreated = dateCreated;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getDateCreated() { return dateCreated; }
}
package com.example.agrialert.model;

public class Report {
    private int id;
    private int userId;
    private String animalType;
    private String symptoms;
    private int numberAffected;
    private String dateObserved;
    private String status;
    private double latitude;
    private double longitude;
    private String imagePath;

    public Report(int id, int userId, String animalType, String symptoms, int numberAffected, 
                  String dateObserved, String status, double latitude, double longitude, String imagePath) {
        this.id = id;
        this.userId = userId;
        this.animalType = animalType;
        this.symptoms = symptoms;
        this.numberAffected = numberAffected;
        this.dateObserved = dateObserved;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getAnimalType() { return animalType; }
    public String getSymptoms() { return symptoms; }
    public int getNumberAffected() { return numberAffected; }
    public String getDateObserved() { return dateObserved; }
    public String getStatus() { return status; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getImagePath() { return imagePath; }
}

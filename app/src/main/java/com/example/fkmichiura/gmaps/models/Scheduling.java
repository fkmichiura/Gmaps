package com.example.fkmichiura.gmaps.models;

public class Scheduling {

    private String id;
    private String email;
    private String date;
    private String time;

    public Scheduling(String id, String email, String date, String time) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.email = email;
    }

    public Scheduling() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}

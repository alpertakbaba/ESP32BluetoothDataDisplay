package com.example.esp32bluetoothdatadisplay;

public class Lap {
    private int id;
    private int lapNumber;
    private String lapTime;
    private String date;
    private boolean isFastest;
    private String datetime;

    public Lap(int id, int lapNumber, String lapTime, String date, boolean isFastest,String datetime) {
        this.id = id;
        this.lapNumber = lapNumber;
        this.lapTime = lapTime;
        this.date = date;
        this.isFastest = isFastest;
        this.datetime = datetime;
    }

    // Getter ve Setter metodları
    public int getId() { return id; }
    public int getLapNumber() { return lapNumber; }
    public String getLapTime() { return lapTime; }
    public String getDate() { return date; }
    public boolean isFastest() { return isFastest; }
    public String getTime() {return datetime; }

    public void setId(int id) { this.id = id; }
    public void setLapNumber(int lapNumber) { this.lapNumber = lapNumber; }
    public void setLapTime(String lapTime) { this.lapTime = lapTime; }
    public void setDate(String date) { this.date = date; }
    public void setFastest(boolean fastest) { isFastest = fastest; }
    public void setTime(String datetime) { this.datetime = datetime; }
}

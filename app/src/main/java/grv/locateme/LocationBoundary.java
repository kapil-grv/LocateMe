package grv.locateme;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class LocationBoundary implements Serializable {
    private String name;
    private String phoneNumber;
    private String message;
    private double latitude;
    private double longitude;
    private int boundaryRadius;
    private int frequency;

    public LocationBoundary(String name, String phoneNumber, String message, double latitude, double longitude, int boundaryRadius, int frequency) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.latitude = latitude;
        this.longitude = longitude;
        this.boundaryRadius = boundaryRadius;
        this.frequency = frequency;
    }

    public String getName() { return name;}

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getBoundaryRadius() {
        return boundaryRadius;
    }

    // Setter methods

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setBoundaryRadius(int boundaryRadius) {
        this.boundaryRadius = boundaryRadius;
    }
    public void setFrequency(int frequency) { this.frequency = frequency; }

    public int getFrequency() {
        return frequency;
    }

    @NonNull
    @Override
    public String toString() {
        return "LocationBoundary{" +
                "name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", message='" + message + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", boundaryRadius=" + boundaryRadius +
                ", frequency=" + frequency +
                '}';
    }
}
package grv.locateme;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class LocationBoundary implements Serializable {
    String name;
    String phoneNumber;
    String message;
    double latitude;
    double longitude;
    int boundaryRadius;
    int frequency;
    String email;

    public LocationBoundary(String name, String phoneNumber, String message, double latitude, double longitude, int boundaryRadius, int frequency, String email) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.latitude = latitude;
        this.longitude = longitude;
        this.boundaryRadius = boundaryRadius;
        this.frequency = frequency;
        this.email = email;
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
    public void setBoundaryRadius(int boundaryRadius) {
        this.boundaryRadius = boundaryRadius;
    }
    public void setFrequency(int frequency) { this.frequency = frequency; }
    public int getFrequency() {
        return frequency;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
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
                ", email='" + email + '\'' +
                '}';
    }
}
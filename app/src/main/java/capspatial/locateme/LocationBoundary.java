package capspatial.locateme;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class LocationBoundary implements Serializable {
    private String name;
    private String phoneNumber;
    private String message;
    private double latitude;
    private double longitude;
    private int boundaryRadius;

    public LocationBoundary(String name, String phoneNumber, String message, double latitude, double longitude, int boundaryRadius) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.latitude = latitude;
        this.longitude = longitude;
        this.boundaryRadius = boundaryRadius;
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
                '}';
    }
}
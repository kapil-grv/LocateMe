package com.example.locateme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LocationUtils {

    private static final List<LocationBoundary> locationBoundaries = new ArrayList<>();
    private static final String phoneNumber = "+919607359454";
    private static final int defaultBoundaryRadiusInMeters = 100;

    static {
        // Initialize the list of location boundaries
        locationBoundaries.add(new LocationBoundary("reached office", phoneNumber, "Reached office baby !", 12.966, 77.610, defaultBoundaryRadiusInMeters));
        locationBoundaries.add(new LocationBoundary("reached home", phoneNumber, "Reached home baby doo !", 12.94487, 77.67797, defaultBoundaryRadiusInMeters));
        locationBoundaries.add(new LocationBoundary("reached home 2", phoneNumber, "Open the door cute ass !", 12.93532, 77.61969, defaultBoundaryRadiusInMeters));
        // Add more boundaries as needed
    }

    public static void addLocationBoundary(LocationBoundary locationBoundary) {
        locationBoundaries.add(locationBoundary);
    }

    public static List<LocationBoundary> getAllLocationBoundaries() {
        return locationBoundaries;
    }

    public static void requestLocation(Context context, FusedLocationProviderClient fusedLocationClient) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Create a location request
            LocationRequest locationRequest = new LocationRequest.Builder(0).setMinUpdateDistanceMeters(5).build();

            LocationCallback locationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult.getLocations().size() > 0) {
                        Location location = locationResult.getLocations().get(0);

                        // Check each boundary and send SMS if within the boundary
                        for (LocationBoundary boundary : locationBoundaries) {
                            sendSmsIfWithinBoundaryOncePerDay(context, boundary, location);
                        }

                        // Update the location using a callback or another appropriate method
                        // (Since you don't have access to the UI elements here, you might want to use a callback)
                        // Log.i("LocationUpdateService", "Updating coordinates to " + location.getLatitude() +"|"+ location.getLongitude());
                    }
                }
            };

            // Request location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        }
    }

    private static void sendSms(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();

        // Send the SMS
        Log.i("Message sent to ", phoneNumber);
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private static boolean isWithinBoundary(Location currentLocation, double targetLatitude, double targetLongitude, float boundaryRadius) {
        float[] distance = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                targetLatitude, targetLongitude,
                distance);

        return distance[0] <= boundaryRadius;
    }

    private static void sendSmsIfWithinBoundaryOncePerDay(Context context, LocationBoundary boundary, Location currentLocation) {
        if (isWithinBoundary(currentLocation, boundary.getLatitude(), boundary.getLongitude(), boundary.getBoundaryRadius())) {
            // Check if the message has been sent today for this boundary
            if (!hasMessageBeenSentToday(context, boundary)) {
                // Send the SMS
                sendSms(boundary.getPhoneNumber(), boundary.getMessage());

                // Update the last sent date for this boundary
                updateLastMessageSentDate(context, boundary);
            }
        }
    }

    private static boolean hasMessageBeenSentToday(Context context, LocationBoundary boundary) {
        SharedPreferences preferences = context.getSharedPreferences(
                "sms_preferences_" + boundary.getPhoneNumber(),
                Context.MODE_PRIVATE
        );
        long lastSentDateMillis = preferences.getLong("last_message_sent_date", 0);

        // Get the current date
        Calendar currentDate = Calendar.getInstance();

        // Get the last sent date
        Calendar lastSentDate = Calendar.getInstance();
        lastSentDate.setTimeInMillis(lastSentDateMillis);

        // Check if the message has been sent today for this boundary
        return currentDate.get(Calendar.YEAR) == lastSentDate.get(Calendar.YEAR) &&
                currentDate.get(Calendar.DAY_OF_YEAR) == lastSentDate.get(Calendar.DAY_OF_YEAR);
    }

    private static void updateLastMessageSentDate(Context context, LocationBoundary boundary) {
        SharedPreferences preferences = context.getSharedPreferences(
                "sms_preferences_" + boundary.getPhoneNumber(),
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = preferences.edit();

        // Save the current date as the last sent date for this boundary
        Calendar currentDate = Calendar.getInstance();
        editor.putLong("last_message_sent_date", currentDate.getTimeInMillis());

        editor.apply();
    }

    // Method to get a location boundary by its name
    public static LocationBoundary getLocationBoundaryByName(String name) {
        for (LocationBoundary boundary : locationBoundaries) {
            if (boundary.getName().equals(name)) {
                return boundary;
            }
        }
        return null; // Return null if no matching entry is found
    }

    public static void deleteLocationBoundaryByName(String name) {
        // Find the index of the location boundary with the specified name
        int indexToRemove = -1;
        for (int i = 0; i < locationBoundaries.size(); i++) {
            LocationBoundary boundary = locationBoundaries.get(i);
            if (boundary.getName().equals(name)) {
                indexToRemove = i;
                break;
            }
        }

        // Remove the location boundary if found
        if (indexToRemove != -1) {
            locationBoundaries.remove(indexToRemove);
        }
    }
}


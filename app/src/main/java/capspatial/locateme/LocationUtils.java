package capspatial.locateme;

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
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocationUtils {

    private static final String SMS_PREFERENCES_PREFIX = "sms_preferences_";
    private static final String LOCATION_PREFERENCES_PREFIX = "location_preferences_";
    private static final String LOCATION_PREFERENCES_KEY = "location_boundaries";

    public static void addLocationBoundary(Context context, LocationBoundary locationBoundary) {
        List<LocationBoundary> locationBoundaries = getAllLocationBoundaries(context);
        Log.i("Current Entries", locationBoundaries.toString());
        locationBoundaries.add(locationBoundary);
        saveLocationBoundaries(context, locationBoundaries);
    }

    public static void updateLocationBoundary(Context context, LocationBoundary updatedEntry) {

        // Update the entry in SharedPreferences
        updateInSharedPreferences(context, updatedEntry);
    }

    private static void updateInSharedPreferences(Context context, LocationBoundary updatedEntry) {
        SharedPreferences preferences = context.getSharedPreferences(
                LOCATION_PREFERENCES_KEY,
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = preferences.edit();

        // Get the existing entries
        String json = preferences.getString("location_boundaries", "[]");
        Type type = new TypeToken<List<LocationBoundary>>() {}.getType();
        List<LocationBoundary> locationBoundaries = new Gson().fromJson(json, type);

        // Find and update the entry
        for (int i = 0; i < locationBoundaries.size(); i++) {
            LocationBoundary entry = locationBoundaries.get(i);
            if (entry.getName().equals(updatedEntry.getName())) {
                // Update the entry
                locationBoundaries.set(i, updatedEntry);
                break; // Exit the loop after updating
            }
        }

        // Save the updated list to SharedPreferences
        json = new Gson().toJson(locationBoundaries);
        editor.putString("location_boundaries", json);

        editor.apply();
    }


    public static List<LocationBoundary> getAllLocationBoundaries(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                LOCATION_PREFERENCES_KEY,
                Context.MODE_PRIVATE
        );

        String json = preferences.getString("location_boundaries", "[]");

        Type type = new TypeToken<List<LocationBoundary>>() {}.getType();
        return new Gson().fromJson(json, type);
    }

    private static void saveLocationBoundaries(Context context, List<LocationBoundary> locationBoundaries) {
        SharedPreferences preferences = context.getSharedPreferences(
                LOCATION_PREFERENCES_KEY,
                Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = preferences.edit();

        String json = new Gson().toJson(locationBoundaries);
        editor.putString("location_boundaries", json);

        editor.apply();
    }

    public static void requestLocation(Context context, FusedLocationProviderClient fusedLocationClient) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Create a location request
            LocationRequest locationRequest = new LocationRequest.Builder(0).build();

            LocationCallback locationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult.getLocations().size() > 0) {
                        Location location = locationResult.getLocations().get(0);

                        List<LocationBoundary> locationBoundaries = getAllLocationBoundaries(context);
                        Log.i("Location request", locationBoundaries.toString());

                        // Check each boundary and send SMS if within the boundary
                        for (LocationBoundary boundary : locationBoundaries) {

                            // Check if the message has been sent today for this boundary
                            if (!hasMessageBeenSentToday(context, boundary)) {
                                sendSmsIfWithinBoundaryOncePerDay(context, boundary, location);
                            }  // else -> Log.i("Message already sent", boundary.getName());

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
            // Check if the person has moved more than a kilometer in the last hour
            if (hasMovedMoreThanKilometerInLastHour(context, boundary, currentLocation)) {
                // Send the SMS
                sendSms(boundary.getPhoneNumber(), boundary.getMessage());
                updateLastLocationUpdateDate(context, boundary);

                // Update the last sent date for this boundary
                updateLastMessageSentDate(context, boundary);
            } else {
                Log.i("User Movement", "Not moved more than 1KM in the last hour");
            }
        }
    }

    private static boolean hasMessageBeenSentToday(Context context, LocationBoundary boundary) {
        SharedPreferences preferences = context.getSharedPreferences(
                LOCATION_PREFERENCES_PREFIX + boundary.getName(),
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
                LOCATION_PREFERENCES_PREFIX + boundary.getName(),
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = preferences.edit();

        // Save the current date as the last sent date for this boundary
        Calendar currentDate = Calendar.getInstance();
        editor.putLong("last_message_sent_date", currentDate.getTimeInMillis());

        editor.apply();
    }

    // Method to get a location boundary by its name
    public static LocationBoundary getLocationBoundaryByName(String name, List<LocationBoundary> locationBoundaries) {
        for (LocationBoundary boundary : locationBoundaries) {
            if (boundary.getName().equals(name)) {
                return boundary;
            }
        }
        return null; // Return null if no matching entry is found
    }

    public static void deleteLocationBoundaryByName(Context context, String name) {
        List<LocationBoundary> allEntries = getAllLocationBoundaries(context);

        // Remove the entry from SharedPreferences
        removeFromSharedPreferences(context, Objects.requireNonNull(getLocationBoundaryByName(name, allEntries)).getName());
    }

    private static void removeFromSharedPreferences(Context context, String nameToRemove) {
        SharedPreferences preferences = context.getSharedPreferences(
                LOCATION_PREFERENCES_KEY,
                Context.MODE_PRIVATE
        );

        Map<String, ?> allEntries = preferences.getAll();

        // Print all entries before removal
        Log.i("Before entry removal", allEntries.toString());

        SharedPreferences.Editor editor = preferences.edit();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue() instanceof String) {
                // Try to parse the JSON string
                String jsonString = (String) entry.getValue();
                List<LocationBoundary> boundaries = new Gson().fromJson(jsonString, new TypeToken<List<LocationBoundary>>() {}.getType());

                // Check if the parsed entry has the specified name
                if (boundaries != null && !boundaries.isEmpty() && boundaries.get(0).getName().equals(nameToRemove)) {
                    // Remove the entire entry with the specified name
                    editor.remove(entry.getKey());
                    break; // Exit the loop after removal
                }
            }
        }

        // Apply the changes
        editor.apply();

        // Print all entries after removal
        Log.i("After removal", preferences.getAll().toString());
    }

    private static boolean hasMovedMoreThanKilometerInLastHour(Context context, LocationBoundary boundary, Location currentLocation) {
        SharedPreferences preferences = context.getSharedPreferences(
                LOCATION_PREFERENCES_PREFIX + boundary.getName(),
                Context.MODE_PRIVATE
        );
        long lastLocationUpdateMillis = preferences.getLong("last_location_update_date", 0);

        // Calculate the time elapsed since the last location update
        long currentTimeMillis = System.currentTimeMillis();
        long timeElapsed = currentTimeMillis - lastLocationUpdateMillis;

        // Check if the time elapsed is greater than one hour
        if (lastLocationUpdateMillis > 0) {
            if (timeElapsed > (60 * 60 * 1000)) {
                // Calculate the distance traveled
                float[] distance = new float[1];
                Location.distanceBetween(
                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                        boundary.getLatitude(), boundary.getLongitude(),
                        distance);

                // Check if the distance traveled is more than one kilometer
                return distance[0] > 1000;
            }
        } else {
            return true;
        }

        // Return false if the time elapsed is greater than or equal to one hour
        return false;
    }

    private static void updateLastLocationUpdateDate(Context context, LocationBoundary boundary) {
        SharedPreferences preferences = context.getSharedPreferences(
                LOCATION_PREFERENCES_PREFIX + boundary.getName(),
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = preferences.edit();

        // Save the current date as the last location update date for this boundary
        long currentTimeMillis = System.currentTimeMillis();
        editor.putLong("last_location_update_date", currentTimeMillis);

        editor.apply();
    }
}


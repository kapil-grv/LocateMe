package grv.locateme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationUtils {
    private static final String LOCATION_PREFERENCES_PREFIX = "location_preferences_";
    private static final String LOCATION_PREFERENCES_KEY = "location_boundaries";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    @SuppressLint("StaticFieldLeak")
    private static MainActivity mainActivity;

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
        Type type = new TypeToken<List<LocationBoundary>>() {
        }.getType();
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

        try {
            // Gson parsing
            Type type = new TypeToken<List<LocationBoundary>>() {
            }.getType();
            return new Gson().fromJson(json, type);
        } catch (JsonSyntaxException e) {
            // Handle the exception or log an error message
            e.printStackTrace();
            return new ArrayList<>();
        }
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
            LocationRequest locationRequest = new LocationRequest.Builder(0).setMinUpdateDistanceMeters(5).build();

            LocationCallback locationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult.getLocations().size() > 0) {
                        Location location = locationResult.getLocations().get(0);

                        List<LocationBoundary> locationBoundaries = getAllLocationBoundaries(context);
                        Log.i("Location request", locationBoundaries.toString());

                        // Check each boundary and send SMS if within the boundary
                        for (LocationBoundary boundary : locationBoundaries) {

                            // Check if the message has been sent for the given frequency
                            sendSmsIfWithinBoundaryWithFrequency(context, boundary, location);
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
                List<LocationBoundary> boundaries = new Gson().fromJson(jsonString, new TypeToken<List<LocationBoundary>>() {
                }.getType());

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

    private static void sendSmsIfWithinBoundaryWithFrequency(Context context, LocationBoundary boundary, Location currentLocation) {
        if (isWithinBoundary(currentLocation, boundary.getLatitude(), boundary.getLongitude(), boundary.getBoundaryRadius())) {
            if (shouldSendMessageBasedOnFrequency(context, boundary)) {
                // Send the SMS
                // sendSms(boundary.getPhoneNumber(), boundary.getMessage());

                // Send Email
                // Create an instance of the ApiService
                ApiService apiService = ApiClient.getClient().create(ApiService.class);

                // Create an instance of EmailRequest with your data
                EmailRequest emailRequest = new EmailRequest(boundary.getEmail(), boundary.getName(), boundary.getMessage());

                // Make the API call
                Call<String> call = apiService.sendEmail(emailRequest);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                            String responseBody = response.body();
                            // Handle the successful response
                        } else {
                            // Handle non-successful response
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        // Handle the failure
                    }
                });

                updateLastLocationUpdateDate(context, boundary);
                updateLastMessageSentDate(context, boundary);

                // Send notification when the message is sent successfully
                sendNotification(context, "Message Sent", "Message sent successfully for " + boundary.getName());
            }
        }
    }

    private static boolean shouldSendMessageBasedOnFrequency(Context context, LocationBoundary boundary) {
        // Check if the last message sent date is more than the specified frequency interval
        long lastSentDateMillis = getLastMessageSentDate(context, boundary);
        long currentTimeMillis = System.currentTimeMillis();
        long timeElapsed = currentTimeMillis - lastSentDateMillis;

        return timeElapsed >= ((long) boundary.getFrequency() * 60 * 1000);
    }

    private static long getLastMessageSentDate(Context context, LocationBoundary boundary) {
        SharedPreferences preferences = context.getSharedPreferences(
                LOCATION_PREFERENCES_PREFIX + boundary.getName(),
                Context.MODE_PRIVATE
        );
        return preferences.getLong("last_message_sent_date", 0);
    }

    public static double parseAndValidateDouble(String value, String errorMessage, Context context) {
        if (value.isEmpty()) {
            showToast(context, errorMessage);
            return 0.0;  // Set a default value or handle accordingly
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            showToast(context, errorMessage);
            e.printStackTrace();  // Log the error or handle accordingly
            return 0.0;  // Set a default value or handle accordingly
        }
    }

    public static int parseAndValidateInt(String value, String errorMessage, Context context) {
        if (value.isEmpty()) {
            showToast(context, errorMessage);
            return 0;  // Set a default value or handle accordingly
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            showToast(context, errorMessage);
            e.printStackTrace();  // Log the error or handle accordingly
            return 0;  // Set a default value or handle accordingly
        }
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void sendNotification(Context context, String title, String message) {
        // Create an intent to launch when the notification is clicked
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channel_id")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(context);
            }
        }
        notificationManager.notify(1, builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private static void requestPermissions(Context context) {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.VIBRATE, // Replace POST_NOTIFICATIONS with VIBRATE
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.POST_NOTIFICATIONS
        };

        // Check if any of the permissions are not granted, then request them
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.i("Requesting Permission", String.valueOf(ActivityCompat.checkSelfPermission(context, permission)));
                ActivityCompat.requestPermissions(mainActivity, permissions, LOCATION_PERMISSION_REQUEST_CODE);
                break;  // Request only once if any permission is not granted
            }
        }
    }

}


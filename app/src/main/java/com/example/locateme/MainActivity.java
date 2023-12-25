package com.example.locateme;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements Serializable {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<Intent> notificationSettingsLauncher;
    private TextView locationTextView;
    private LocationBoundary selectedEntry;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    // TODO: Consider calling
    //    * Pages:
    //      - Home
    //         - Recent pings (Name, Status, Time)
    //      - Tasks
    //          - New task
    //              - Recipient phone number with international code (+919008314988)
    //              - Notification message (Text to be shared with recipient)
    //              - Notification interval in hours (24 hours by default)
    //              - Notification frequency
    //                  - Repeat count (number of times)
    //                  - Repeat duration (Repeat check after n minutes)
    //          - Current tasks (List of current tasks, with on click edit button. Edit button will be similar to new task page, but will refilled values based on the task selected and option to modify and re-save that task)
    //      - Message delivery notification
    //      - Analytics (need login)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request location and SMS permissions
        requestPermissions();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationTextView = findViewById(R.id.locationTextView);

        Button getLocationButton = findViewById(R.id.getLocationButton);
        getLocationButton.setOnClickListener(view -> Executors.newSingleThreadExecutor().execute(new LocationTask()));

    }

    // Method to set the selected entry
    private void setSelectedEntry(LocationBoundary entry) {
        selectedEntry = entry;
    }

    // Method to get the selected entry
    public LocationBoundary getSelectedEntry() {
        return selectedEntry;
    }

    // Method to handle item selection, call this when an entry is selected
    private void onItemSelected(LocationBoundary entry) {
        Log.d("MainActivity", "Item selected: " + entry.getName());
        setSelectedEntry(entry);

        // Call editEntry directly when an entry is clicked
        editEntry(selectedEntry);
    }


    private void enableEditButton() {
        Button editButton = findViewById(R.id.buttonEditEntry);
        editButton.setEnabled(true);
    }

    private void listAllEntries() {
        // Retrieve all entries (modify this based on your data structure)
        List<LocationBoundary> allEntries = LocationUtils.getAllLocationBoundaries();

        // Use a dialog or start a new activity to display the list
        // For simplicity, let's use a dialog in this example
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an Entry");

        // Convert entry names to an array (modify this based on your data structure)
        String[] entryNames = new String[allEntries.size()];
        for (int i = 0; i < allEntries.size(); i++) {
            // Assuming your LocationBoundary class has a method like getName()
            entryNames[i] = allEntries.get(i).getName();
        }

        builder.setItems(entryNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle item click
                LocationBoundary selectedEntry = allEntries.get(which);
                Log.i("Editing entry", selectedEntry.toString());
                onItemSelected(selectedEntry);
            }
        });

        builder.show();
    }

    public void showEntries(View view) {
        // Show a list of all entries
        listAllEntries();
    }

    public void createEntry(View view) {
        switchToLocationEntryActivity();
    }

    private void switchToLocationEntryActivity() {
        Intent intent = new Intent(this, LocationEntryActivity.class);
        intent.putExtra("LATITUDE", currentLatitude);
        intent.putExtra("LONGITUDE", currentLongitude);
        startActivity(intent);
    }

    public void editEntry(LocationBoundary entry) {
        // Assuming you have a selectedEntry variable representing the entry to edit
        if (entry != null) {
            switchToLocationEntryActivity(true, entry);
        }
    }

    private void switchToLocationEntryActivity(boolean editMode, LocationBoundary existingEntry) {
        Intent intent = new Intent(this, LocationEntryActivity.class);
        intent.putExtra("EDIT_MODE", editMode);

        if (editMode) {
            // Use consistent keys when putting extras
            intent.putExtra("NAME", existingEntry.getName());
            intent.putExtra("PHONE_NUMBER", existingEntry.getPhoneNumber());
            intent.putExtra("MESSAGE", existingEntry.getMessage());
            intent.putExtra("LATITUDE", existingEntry.getLatitude());
            intent.putExtra("LONGITUDE", existingEntry.getLongitude());
            intent.putExtra("BOUNDARY_RADIUS", existingEntry.getBoundaryRadius());
        }

        startActivity(intent);
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE
        };

        // Check if any of the permissions are not granted, then request them
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.i("Requesting Permission", String.valueOf(ActivityCompat.checkSelfPermission(this, permission)));
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
                break;  // Request only once if any permission is not granted
            }
        }

        // Start the location update service with the MainActivity instance
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
    }

    public void requestLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Create a location request
            LocationRequest locationRequest = new LocationRequest.Builder(0).setMinUpdateDistanceMeters(5).build();

            LocationCallback locationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult.getLocations().size() > 0) {
                        Location location = locationResult.getLocations().get(0);

                        updateLocationTextView(location.getLatitude(), location.getLongitude());
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        Log.i("LocationUpdateService", "Updating coordinates to " + location.getLatitude() +"|"+ location.getLongitude());
                    }
                }
            };

            // Request location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateLocationTextView(double latitude, double longitude) {
        locationTextView.setText(String.format("Latitude: %s\nLongitude: %s", latitude, longitude));
    }

    private class LocationTask implements Runnable {
        @Override
        public void run() {
            Executors.newSingleThreadExecutor().execute(MainActivity.this::requestLocation);
        }
    }

    // Send a notification
    private void sendNotification(String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "test", NotificationManager.IMPORTANCE_MIN);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create an intent for the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1")
                .setSmallIcon(R.drawable.ic_banner_foreground)
                .setContentTitle("Boundary Notification")
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private boolean areNotificationsEnabled() {
        // Check if notifications are enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0 (Oreo) and above
            return NotificationManagerCompat.from(this).areNotificationsEnabled();
        } else {
            // For versions below Android 8.0, check if the notification channel is blocked
            return areNotificationsEnabledBeforeO();
        }
    }

    private boolean areNotificationsEnabledBeforeO() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo appInfo = getApplicationInfo();
        String pkg = getApplicationContext().getPackageName();
        int uid = appInfo.uid;

        // 11 is the code for OP_POST_NOTIFICATION
        int appOpsMode = appOps.checkOpNoThrow(String.valueOf(11), uid, pkg);
        return appOpsMode == AppOpsManager.MODE_ALLOWED || appOpsMode == AppOpsManager.MODE_DEFAULT;
    }

    private void showNotificationEnableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enable Notifications");
        builder.setMessage("To receive important updates, enable notifications for this app.");
        builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openNotificationSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle cancel action if needed
            }
        });
        builder.show();
    }

    private void openNotificationSettings() {
        // Open the app's notification settings using ActivityResultLauncher
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
        notificationSettingsLauncher.launch(intent);
    }
}

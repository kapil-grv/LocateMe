package capspatial.locateme;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationUpdateService extends Service {

    private Handler handler;
    private MainActivity mainActivity;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Log.i("Location updater", "GPS update Initiated");

        // Initialize and start the handler for periodic updates
        handler = new Handler();
        handler.postDelayed(locationUpdateRunnable, 5000); // Update every 5 seconds (adjust as needed)
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the handler when the service is destroyed
        handler.removeCallbacks(locationUpdateRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class LocalBinder extends Binder {
        LocationUpdateService getService() {
            return LocationUpdateService.this;
        }
    }

    // Periodic task for location updates
    private final Runnable locationUpdateRunnable = new Runnable() {
        @Override
        public void run() {

            LocationUtils.requestLocation(LocationUpdateService.this, fusedLocationClient);
            Log.i("LocationUpdateService", "Initiating location updates");

            // Continue running the task periodically
            handler.postDelayed(this, 5000); // Update every 5 seconds (adjust as needed)

            // Start the service in the foreground
            startForeground(1, createMinimalNotification()); // You can customize the notification

        }
    };

    private Notification createMinimalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "minimal_channel_id",
                    "Minimal Channel",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        return new NotificationCompat.Builder(this, "minimal_channel_id")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Location Update Service")
                .setContentText("Running in the background")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}

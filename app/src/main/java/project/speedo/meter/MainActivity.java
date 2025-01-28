package project.speedo.meter;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {


    private TextView tvSpeed, tvUnit, tvAccuracy, tvHeading, tvMaxSpeed,txtdistance;
    private static final String[] unit = {"km/h", "mph", "meter/sec", "knots"};
    private int unitType;
    private NotificationCompat.Builder mbuilder;
    private NotificationManager mnotice;
    private double maxSpeed = -100.0;
     double dlat,dlon,dlat2,dlon2;
    private MainActivity activity;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSpeed = (TextView) findViewById(R.id.tvSpeed);
        tvMaxSpeed = (TextView) findViewById(R.id.tvMaxSpeed);
        txtdistance = (TextView) findViewById(R.id.txtdistance);
        tvUnit = (TextView) findViewById(R.id.tvUnitc);

        tvAccuracy = (TextView) findViewById(R.id.tvAccuracy);
        tvHeading = (TextView) findViewById(R.id.tvHeading);
        Typeface font = Typeface.createFromAsset(getBaseContext().getAssets(), "font/lcdn.ttf");
        tvSpeed.setTypeface(font);
        txtdistance.setTypeface(font);
        dlat =33.633000;
        dlon =72.969477;
        tvHeading.setTypeface(font);
        tvAccuracy.setTypeface(font);
        tvMaxSpeed.setTypeface(font);

        activity = this;
        //for handling notification
        mbuilder = new NotificationCompat.Builder(this);
        mnotice = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        unitType = Integer.parseInt(prefs.getString("unit", "1"));
        tvUnit.setText(unit[unitType - 1]);

        if (savedInstanceState != null) {
            maxSpeed = savedInstanceState.getDouble("maxspeed", -100.0);

        }

        if (!this.isLocationEnabled(this)) {


            //show dialog if Location Services is not enabled


            android.app.AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.gps_not_found_title);  // GPS not found
            builder.setMessage(R.string.gps_not_found_message); // Want to enable?
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {

                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    activity.startActivity(intent);
                }
            });

            //if no - bring user to selecting Static Location Activity
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(activity, "Please enable Location-based service / GPS", Toast.LENGTH_LONG).show();


                }


            });
            builder.create().show();


        }


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK, "My wakelook");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        new SpeedTask(this).execute("string");


        tvSpeed.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);

                return false;
            }


        });


    }

    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

        bundle.putDouble("maxspeed", maxSpeed);


    }

    protected void onRestoreInstanceState(Bundle bundle) {

        super.onRestoreInstanceState(bundle);

        maxSpeed = bundle.getDouble("maxspeed", -100.0);

    }


    protected void onResume() {
        super.onResume();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        unitType = Integer.parseInt(prefs.getString("unit", "1"));
        maxSpeed = prefs.getFloat("maxspeed", -100.0f);


        tvUnit.setText(unit[unitType - 1]);

        if (maxSpeed > 0) {

            float multiplier = 3.6f;

            switch (unitType) {
                case 1:
                    multiplier = 3.6f;
                    break;
                case 2:
                    multiplier = 2.25f;
                    break;
                case 3:
                    multiplier = 1.0f;
                    break;

                case 4:
                    multiplier = 1.943856f;
                    break;

            }
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            numberFormat.setMaximumFractionDigits(0);

            tvMaxSpeed.setText(numberFormat.format(maxSpeed * multiplier));

        }

        removeNotification();


    }

    protected void onStop() {
        super.onStop();

        displayNotification();


    }

    protected void onPause() {
        super.onPause();

        float tempMaxpeed = 0.0f;
        try {

            tempMaxpeed = Float.parseFloat(tvMaxSpeed.getText().toString());


        } catch (java.lang.NumberFormatException nfe) {

            tempMaxpeed = 0.0f;

        }

        prefs.edit().putFloat("maxSpeed", tempMaxpeed);


    }

    private void displayNotification() {

        mbuilder.setSmallIcon(R.drawable.logo);
        mbuilder.setContentTitle("Car tracking is running...");
        mbuilder.setContentText("Click to view");

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        TaskStackBuilder stackBuilder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            stackBuilder = TaskStackBuilder.create(this);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            stackBuilder.addParentStack(MainActivity.class);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            stackBuilder.addNextIntent(resultIntent);
        }

        PendingIntent resultPendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        mbuilder.setContentIntent(resultPendingIntent);


        mnotice.notify(1337, mbuilder.build());


    }

    private void removeNotification() {
        mnotice.cancel(1337);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent();
                intent.setClass(this, SettingsActivity.class);
                startActivity(intent);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private class SpeedTask extends AsyncTask<String, Void, String> {
        final MainActivity activity;
        float speed = 0.0f;
        double lat;
        double lon;
        LocationManager locationManager;

        public SpeedTask(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        protected String doInBackground(String... params) {
            locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);


            return null;

        }

        protected void onPostExecute(String result) {
            tvUnit.setText(unit[unitType - 1]);
            LocationListener listener = new LocationListener() {
                float filtSpeed;
                float localspeed;

                @Override
                public void onLocationChanged(Location location) {
                    speed = location.getSpeed();
                    float multiplier = 3.6f;

                    switch (unitType) {
                        case 1:
                            multiplier = 3.6f;
                            break;
                        case 2:
                            multiplier = 2.25f;
                            break;
                        case 3:
                            multiplier = 1.0f;
                            break;

                        case 4:
                            multiplier = 1.943856f;
                            break;

                    }

                    if (maxSpeed < speed) {
                        maxSpeed = speed;
                    }


                    localspeed = speed * multiplier;

                    filtSpeed = filter(filtSpeed, localspeed, 2);


                    NumberFormat numberFormat = NumberFormat.getNumberInstance();
                    numberFormat.setMaximumFractionDigits(0);


                    lat = location.getLatitude();
                    lon = location.getLongitude();
                    //speed=(float) location.getLatitude();
                    Log.d("net.mypapit.speedview", "Speed " + localspeed + "latitude: " + lat + " longitude: " + location.getLongitude());


                    //measureDistance(dlat,location.getLatitude(),dlon,location.getLongitude());


                    tvSpeed.setText(numberFormat.format(filtSpeed));

                    tvMaxSpeed.setText(numberFormat.format(maxSpeed * multiplier));

                    if (location.hasAltitude()) {
                        tvAccuracy.setText(numberFormat.format(location.getAccuracy()) + " m");
                    } else {
                        tvAccuracy.setText("NIL");
                    }

                    numberFormat.setMaximumFractionDigits(0);


                    if (location.hasBearing()) {

                        double bearing = location.getBearing();
                        String strBearing = "NIL";
                        if (bearing < 20.0) {
                            strBearing = "North";
                        } else if (bearing < 65.0) {
                            strBearing = "North-East";
                        } else if (bearing < 110.0) {
                            strBearing = "East";
                        } else if (bearing < 155.0) {
                            strBearing = "South-East";
                        } else if (bearing < 200.0) {
                            strBearing = "South";
                        } else if (bearing < 250.0) {
                            strBearing = "South-West";
                        } else if (bearing < 290.0) {
                            strBearing = "West";
                        } else if (bearing < 345.0) {
                            strBearing = "North-West";
                        } else if (bearing < 361.0) {
                            strBearing = "North";
                        }

                        tvHeading.setText(strBearing);
                    } else {
                        tvHeading.setText("NIL");
                    }

                    NumberFormat nf = NumberFormat.getInstance();

                    nf.setMaximumFractionDigits(4);


                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onProviderEnabled(String provider) {
                    tvSpeed.setText("STDBY");
                    tvMaxSpeed.setText("NIL");
                    tvHeading.setText("HEADING");
                    tvAccuracy.setText("ACCURACY");

                }

                @Override
                public void onProviderDisabled(String provider) {
                    tvSpeed.setText("NOFIX");
                    tvMaxSpeed.setText("NOGPS");
                    tvHeading.setText("HEADING");
                    tvAccuracy.setText("ACCURACY");


                }

            };


            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);


        }

        /**
         * Simple recursive filter
         *
         * @param prev Previous value of filter
         * @param curr New input value into filter
         * @return New filtered value
         */
        private float filter(final float prev, final float curr, final int ratio) {
            // If first time through, initialise digital filter with current values
            if (Float.isNaN(prev))
                return curr;
            // If current value is invalid, return previous filtered value
            if (Float.isNaN(curr))
                return prev;
            // Calculate new filtered value
            return (float) (curr / ratio + prev * (1.0 - 1.0 / ratio));
        }


    }

    private boolean isLocationEnabled(Context mContext) {


        LocationManager locationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public double measureDistance(Double lat1,Double lat2,Double lon1,Double lon2){//custom method that calculates distance between two points
        //formula found on the web
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        double dis = Math.round(distance/1000.0);
        String dist = String.valueOf(dis);
        txtdistance.setText(dist+" KM");
        return distance;
    }


}
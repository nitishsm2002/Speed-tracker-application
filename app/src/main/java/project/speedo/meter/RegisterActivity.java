package project.speedo.meter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import project.util.HttpsTrustManager;
import project.util.SharedHelper;
import project.util.URLHelper;
import project.util.VolleySingleton;

public class RegisterActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{
    Button navLogin;
    EditText nameEdText,emailEdText,passwordEdText,phoneEdText;
    Button btn_Signup;
    private static GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        btn_Signup = findViewById(R.id.btnsignup);
        nameEdText = findViewById(R.id.nameEdText);
        emailEdText = findViewById(R.id.emailEdText);
        passwordEdText = findViewById(R.id.passwordEdText);
        phoneEdText = findViewById(R.id.phoneEdText);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        navLogin = findViewById(R.id.navLogin);
        navLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                Intent i = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(i);
            }
        });

        btn_Signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(nameEdText.getText().toString().equalsIgnoreCase("")){
                    nameEdText.setError("Enter your Name");
                }else if(phoneEdText.getText().toString().equalsIgnoreCase("")){
                    phoneEdText.setError("Enter your Phone");
                }else if(emailEdText.getText().toString().equalsIgnoreCase("")){
                    emailEdText.setError("Enter your Email");
                }else if(passwordEdText.getText().toString().equalsIgnoreCase("")){
                    passwordEdText.setError("Enter your Password");
                }else {
                    signup();
                }
            }
        });
    }

    public void signup(){
        HttpsTrustManager.allowAllSSL();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URLHelper.signUp,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(@Nullable String response) {
                        try {
                            if (response != null) {
                                JSONObject obj = new JSONObject(response);
                                String status = obj.getString("status");
                                String data = obj.getString("message");
                                if (status.equals("success")) {
                                    Toast.makeText(getApplicationContext(),data,Toast.LENGTH_LONG).show();
                                    Intent i = new Intent(getApplicationContext(),LoginActivity.class);
                                    startActivity(i);
                                } else {
                                    displayMessage(data);
                                    finish();
                                }
                            } else {
                                displayMessage(getString(R.string.error_msg));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull VolleyError error) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_msg), Toast.LENGTH_SHORT).show();
                    }
                })
        {
            @NonNull
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("name",nameEdText.getText().toString());
                params.put("email", emailEdText.getText().toString());
                params.put("password", passwordEdText.getText().toString());
                params.put("mobile", phoneEdText.getText().toString());

                return params;
            }
        };
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);

    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLocation == null){
            startLocationUpdates();
        }
        if (mLocation != null) {
            double latitude = mLocation.getLatitude();
            double longitude = mLocation.getLongitude();
            StringBuilder result = new StringBuilder();
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    result.append(address.getSubLocality());
                    result.append(address.getLocality());
                    result.append(address.getCountryName());

                }
                SharedHelper.putKey(getApplicationContext(),"address",String.valueOf(result));
                SharedHelper.putKey(getApplicationContext(),"latitude",String.valueOf(latitude));
                SharedHelper.putKey(getApplicationContext(),"longitude",String.valueOf(longitude));

            } catch (IOException e) {
                Log.e("tag", e.getMessage());
            }
        } else {
             Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    public void displayMessage(@NonNull String toastString) {
        try {
            Snackbar.make(getCurrentFocus(), toastString, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show();
        }
    }

    protected void startLocationUpdates() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(0)
                .setFastestInterval(0);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    public void onLocationChanged(Location location) {

    }
}

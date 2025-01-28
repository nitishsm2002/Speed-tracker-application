package project.speedo.meter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import project.util.HttpsTrustManager;
import project.util.SharedHelper;
import project.util.URLHelper;
import project.util.VolleySingleton;

public class LoginActivity extends AppCompatActivity {
    Button navRegister;
    EditText txtLoginEmail,txtLoginPassword;
    Button customer_login;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        navRegister = findViewById(R.id.navRegister);
        txtLoginEmail = findViewById(R.id.txtLoginEmail);
        txtLoginPassword = findViewById(R.id.txtLoginPassword);
        customer_login = findViewById(R.id.customer_login);

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Please wait");
        navRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                Intent i = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(i);
            }
        });

        customer_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txtLoginEmail.getText().toString().equalsIgnoreCase("")) {
                    txtLoginEmail.setError("Enter your Email address");
                } else if(txtLoginPassword.getText().toString().equalsIgnoreCase("")) {
                    txtLoginPassword.setError("Enter your password");
                }else {
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    login();

                }
            }
        });
    }

    public void login(){
        HttpsTrustManager.allowAllSSL();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URLHelper.signIn,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(@Nullable String response) {

                        try {

                            if(response!=null){
                                JSONObject obj = new JSONObject(response);
                                String status= obj.getString("status");
                                if(status.equals("success")){
                                    JSONObject jsonObject =new JSONObject(response);
                                    JSONObject obj2 = (JSONObject)jsonObject.get("data");
                                    String token = obj2.getString("loginToken");
                                    String userId = obj2.getString("id");
                                    String userName = obj2.getString("name");
                                    String email    = obj2.getString("email");
                                    String phone    = obj2.getString("mobile");
                                   // displayMessage(message);
                                    SharedHelper.putKey(getApplicationContext(),"token",token);
                                    SharedHelper.putKey(getApplicationContext(),"userId",userId);
                                    SharedHelper.putKey(getApplicationContext(),"userName",userName);
                                    SharedHelper.putKey(getApplicationContext(),"email",email);
                                    SharedHelper.putKey(getApplicationContext(),"phone",phone);

                                    progressDialog.dismiss();
                                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(i);
                                }
                                else {
                                    progressDialog.dismiss();
                                    //displayMessage(message);

                                }
                            }else {
                                progressDialog.dismiss();
                                displayMessage(getString(R.string.SomethingWrong));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            progressDialog.dismiss();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull VolleyError error) {
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        displayMessage(error.getMessage());
                    }
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("email", txtLoginEmail.getText().toString());
                params.put("password", txtLoginPassword.getText().toString());

                return params;
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
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
}

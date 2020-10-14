package com.example.shoppingcartapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "demo";

    TextInputLayout email_TIL,password_TIL;
    TextInputEditText email_TIET,password_TIET;

    Gson gson =  new Gson();
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Login");


        email_TIET = findViewById(R.id.email_TIET);
        password_TIET = findViewById(R.id.password_TIET);
        email_TIL = findViewById(R.id.email_TIL);
        password_TIL = findViewById(R.id.password_TIL);


        //checking if users has already loged in
//        checkIfUserIsLogedIN();

        findViewById(R.id.signup_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: signup button");
                Intent i =  new Intent(MainActivity.this,SignUp.class);
                startActivityForResult(i,100);
            }
        });

        findViewById(R.id.sigin_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CheckIfEmailAndPasswordAreEmpty()){
                    String loginText = email_TIET.getText().toString().trim();
                    String passwordText = password_TIET.getText().toString().trim();
                    Log.d("demo",loginText+" "+passwordText);
                    Log.d(TAG, "onClick: calling async");
                    new getTokeyAsync(loginText, passwordText).execute();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == 200){
            finish();
        }
    }


    //    private void checkIfUserIsLogedIN() {
//        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
//        String pastTokenKey = preferences.getString("TOKEN_KEY", null);
//        if(pastTokenKey!=null && !pastTokenKey.equals("")){
//            new getUser().execute();
//        }
//    }


    private boolean CheckIfEmailAndPasswordAreEmpty() {
        if(email_TIET.getText().toString().equals("")){
            email_TIL.setError("Cannot be empty");
            return false;
        }else{
            email_TIL.setError("");
        }
        if(password_TIET.getText().toString().equals("")){
            password_TIL.setError("Cannot be empty");
            return false;
        }else{
            password_TIL.setError("");
        }
        return true;
    }

    public class getTokeyAsync extends AsyncTask<String, Void, String> {

        String username, password;
        boolean isStatus =true;

        public getTokeyAsync(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected String doInBackground(String... strings) {
            final OkHttpClient client = new OkHttpClient();
            String decodedValue = username+":"+password;

            Log.d(TAG, "doInBackground: async called for login");

            byte[] encodedValue = new byte[0];
            try {
                encodedValue = decodedValue.getBytes("UTF-8");
                String encodedString = Base64.encodeToString(encodedValue, Base64.NO_WRAP);

                Request request = new Request.Builder()
                        .url(getResources().getString(R.string.endPointUrl)+"api/v1/users/login")
                        .header("Authorization", "Basic " + encodedString)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String result = response.body().string();
                    Log.d(TAG, "doInBackground: login response=>"+result);
                    if (response.isSuccessful()){
                        isStatus = true;
                    }else{
                        isStatus = false;
                    }
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (UnsupportedEncodingException e) {
                Toast.makeText(MainActivity.this, "Some problem occured with the password", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            return "";
        }

        @Override
        protected void onPostExecute(String result1) {
            super.onPostExecute(result1);
            JSONObject root = null;
            Log.d("demo",result1);
            try {
                root = new JSONObject(result1);
                if(isStatus){
                    Log.d("demo",root.toString());
                    User user = new User();
                    user.id = root.getString("_id");
                    user.fname = root.getString("firstName");
                    user.lname = root.getString("lastName");
                    user.gender = root.getString("gender");
                    user.email = root.getString("email");
                    user.customerId = root.getString("customerId");
                    preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
                    editor = preferences.edit();
                    editor.putString("TOKEN_KEY",root.getString("token"));
                    editor.putString("ID",user.id);
                    editor.putString("USER",gson.toJson(user));
                    editor.commit();
                    Toast.makeText(MainActivity.this, "Logged in Successfully", Toast.LENGTH_SHORT).show();
//                    Intent intent = new Intent(MainActivity.this, ShoppingActivity.class);
//                    intent.putExtra("UserObject", user);
//                    startActivity(intent);
                    finish();
                }else{
                    //It means that they are some error while signing up.
                    Toast.makeText(MainActivity.this, root.getString("error"), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

//    public class getUser extends AsyncTask<String, Void, String> {
//        boolean isStatus = true;
//        @Override
//        protected String doInBackground(String... strings) {
//            final OkHttpClient client = new OkHttpClient();
//            SharedPreferences preferences = getApplicationContext().getSharedPreferences("TokeyKey", 0);
//
//            try {
//                Request request = new Request.Builder()
//                        .url("http://167.99.228.2:3000/api/v1/users/"+preferences.getString("ID", null))
//                        .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
//                        .build();
//                try (Response response = client.newCall(request).execute()) {
//                    if (response.isSuccessful()){
//                        isStatus = true;
//                    }else{
//                        isStatus = false;
//                    }
//                    return response.body().string();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }catch (Exception e){
//
//            }
//            return "";
//        }
//
//        @Override
//        protected void onPostExecute(String result1) {
//            super.onPostExecute(result);
//            JSONObject root = null;
//            Log.d(TAG,result);
//            try {
//                root = new JSONObject(result1);
//                if(isStatus){
//                    Log.d("demo",root.toString());
//                    User user = new User();
//                    user.id = root.getString("_id");
//                    user.fname = root.getString("firstName");
//                    user.lname = root.getString("lastName");
//                    user.gender = root.getString("gender");
//                    user.email = root.getString("email");
//                    user.age = root.getString("age");
////                    Intent intent = new Intent(MainActivity.this, UserListActivity.class);
////                    intent.putExtra("UserObject", user);
////                    startActivity(intent);
//                    // to send user to next activity is this is successful
//                    Log.d(TAG, "onPostExecute: user token exists");
//                    result = "okay";
//                }else{
//                    //It means that they are some error while signing up.
//                    Toast.makeText(MainActivity.this, "Session has expired. Please login again!", Toast.LENGTH_SHORT).show();
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            toStopAnimationOFIcon = true;
//        }
//    }

}
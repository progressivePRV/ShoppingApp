package com.example.shoppingcartapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignUp extends AppCompatActivity {

    private static final String TAG = "demo";
    TextInputLayout firstName_TIL,lastName_TIL,age_TIL,email_TIL,password_TIL;
    TextInputEditText firstName_TIET,lastName_TIET,age_TIET,email_TIET,password_TIET;

    RadioGroup genderRadioGroup;
    Gson gson =  new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firstName_TIET = findViewById(R.id.firstName_TIET);
        lastName_TIET = findViewById(R.id.lastName_TIET);
        age_TIET = findViewById(R.id.age_TIET);
        email_TIET = findViewById(R.id.email_TIET);
        password_TIET = findViewById(R.id.password_TIET);
        firstName_TIL = findViewById(R.id.firstName_TIL);
        lastName_TIL = findViewById(R.id.lastName_TIL);
        age_TIL = findViewById(R.id.age_TIL);
        email_TIL = findViewById(R.id.email_TIL);
        password_TIL = findViewById(R.id.password_TIL);
        genderRadioGroup = findViewById(R.id.radio_group_male_female);

        findViewById(R.id.sigin_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CheckIfFieldAreEmpty()){
                    //call async task
                    String fnameValue = firstName_TIET.getText().toString().trim();
                    String lnameValue = lastName_TIET.getText().toString().trim();
                    String passwordValue = password_TIET.getText().toString().trim();
                    //String repeatPasswordValue = repeatPassword.getText().toString().trim();
                    String emailValue = email_TIET.getText().toString().trim();
                    String ageValue = age_TIET.getText().toString().trim();
                    String genderValue;
                    if(R.id.female == genderRadioGroup.getCheckedRadioButtonId()){
                        genderValue = "Female";
                    }else{
                        genderValue = "Male";
                    }

                    new createNewUser(emailValue,passwordValue,fnameValue,lnameValue,ageValue,genderValue).execute("");
                }
            }
        });
    }

    public boolean CheckIfFieldAreEmpty(){
        if(firstName_TIET.getText().toString().equals("")){
            firstName_TIL.setError("Cannot be empty");
            return false;
        }else{
            firstName_TIL.setError("");
        }
        if(lastName_TIET.getText().toString().equals("")) {
            lastName_TIL.setError("Cannot be empty");
            return false;
        }else{
            lastName_TIL.setError("");
        }
        if(genderRadioGroup.getCheckedRadioButtonId() == -1){
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        if(age_TIET.getText().toString().equals("")){
            age_TIL.setError("Cannot be empty");
            return false;
        }else{
            age_TIL.setError("");
        }
        if(email_TIET.getText().toString().equals("")){
            email_TIL.setError("Cannot be empty");
            return false;
        }else{
            email_TIL.setError("");
        }
        String x = password_TIET.getText().toString();
        if(x.equals("")){
            password_TIL.setError("Cannot be empty");
            return false;
        }
        else{
            password_TIL.setError("");
        }
        return true;
    }

    public class createNewUser extends AsyncTask<String, Void, String> {
        boolean isStatus = true;

        String emailValue, passwordValue, fnameValue, lnameValue, gender;
        String age;
        public createNewUser(String emailValue, String passwordValue, String fnameValue, String lnameValue, String age, String gender) {
            this.emailValue = emailValue;
            this.passwordValue = passwordValue;
            this.fnameValue = fnameValue;
            this.lnameValue = lnameValue;
            this.age = age;
            this.gender = gender;
        }

        @Override
        protected String doInBackground(String... strings) {

            Log.d(TAG, getResources().getString(R.string.endPointUrl)+"api/v1/signup");
            final OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("firstName",fnameValue)
                    .add("lastName",lnameValue)
                    .add("age",age)
                    .add("gender",gender)
                    .add("email", emailValue)
                    .add("password",passwordValue)
                    .build();
            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/users/signup")
                    .post(formBody)
                    .build();
            String responseValue = null;
            try (Response response = client.newCall(request).execute()) {
                if(response.isSuccessful()){
                    isStatus = true;
                }else{
                    isStatus = false;
                }
                Log.d("demo"," "+response.isSuccessful());
                responseValue = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return responseValue;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Log.d("demo",s);
            if(s!=null){
                JSONObject root = null;
                try {
                    root = new JSONObject(s);
                    if(isStatus){
                        User user = new User();
                        user.id = root.getString("_id");
                        user.fname = root.getString("firstName").trim();
                        user.lname = root.getString("lastName").trim();
                        user.gender = root.getString("gender");
                        user.customerId = root.getString("customerId");
                        user.email = root.getString("email").trim();
                        SharedPreferences preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("TOKEN_KEY",root.getString("token"));
                        editor.putString("ID",user.id);
                        editor.putString("USER",gson.toJson(user));
                        editor.commit();
                        Toast.makeText(SignUp.this, "User Successfully created", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "onPostExecute: User Successfully created");
                        Intent intent = new Intent(SignUp.this, ShoppingActivity.class);
                        startActivityForResult(intent, 100);
                        finish();
                    }else{
                        //Handling the error scenario here
                        JSONObject error = root.getJSONObject("error");
                        if(error.length() > 1){
                            //It means duplicate email issue
                            JSONObject keyValue = error.getJSONObject("keyValue");
                            if(keyValue.getString("email") != null){
                                Toast.makeText(SignUp.this, "Email already exist. Please use another email!", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(SignUp.this, "Some error occured. Please try again!", Toast.LENGTH_SHORT).show();
                            }
                        }else if(error.length() == 1){
                            JSONArray message = error.getJSONArray("errors");
                            JSONObject arrayObject = message.getJSONObject(0);
                            Toast.makeText(SignUp.this, arrayObject.getString("msg"), Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(SignUp.this, "Some error occured. Please try again!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
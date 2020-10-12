package com.example.shoppingcartapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "okay";
    private float totalPrice = 10;

    SharedPreferences preferences;
    TextInputLayout address_TIL, city_TIL, state_TIL, pinCode_TIL,phone_TIL;
    TextInputEditText address_TIET, city_TIET, state_TIET, pinCode_TIET,phone_TIET;
    String global_clientToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);

        address_TIL = findViewById(R.id.address_TIL);
        city_TIL = findViewById(R.id.city_TIL);
        state_TIL= findViewById(R.id.state_TIL);
        pinCode_TIL = findViewById(R.id.pinCode_TIL);
        phone_TIL = findViewById(R.id.phone_TIL);
        address_TIET = findViewById(R.id.address_TIET);
        city_TIET = findViewById(R.id.city_TIET);
        state_TIET = findViewById(R.id.state_TIET);
        pinCode_TIET = findViewById(R.id.pinCode_TIET);
        phone_TIET = findViewById(R.id.phone_TIET);

        findViewById(R.id.confirm_To_Pay_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate the fields
                if(checkValidation() && !global_clientToken.isEmpty()){
                    onBraintreeSubmit(global_clientToken);
                }
            }
        });

        new LoadClientTokenAsync().execute();

    }

    private boolean checkValidation() {
        if (address_TIET.getText().toString().isEmpty()){
            address_TIL.setError("can't be empty");
            return false;
        }else{
            address_TIL.setError("");
        }
        if (city_TIET.getText().toString().isEmpty()){
            city_TIL.setError("can't be empty");
            return false;
        }else {
            city_TIL.setError("");
        }
        if (state_TIET.getText().toString().isEmpty()){
            state_TIL.setError("can't be empty");
            return false;
        }else {
            state_TIL.setError("");
        }
        if (pinCode_TIET.getText().toString().isEmpty()){
            pinCode_TIL.setError("can't be empty");
            return false;
        }else {
            pinCode_TIL.setError("");
        }
        if (phone_TIET.getText().toString().isEmpty()){
            phone_TIL.setError("can't be empty");
            return false;
        }else if (phone_TIET.getText().toString().length() < 10){
            phone_TIL.setError("minimum 10 digits and maximum 11 digits");
            return false;
        }else{
            phone_TIL.setError("");
        }
        return true;
    }

    public class LoadClientTokenAsync extends AsyncTask<String, Void, String> {
        boolean isStatus = true;

        @Override
        protected String doInBackground(String... strings) {
            final OkHttpClient client = new OkHttpClient();
            String brainTreeToken = null;

            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/customerToken")
                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()){
                    isStatus = true;
                }else{
                    isStatus = false;
                }
                brainTreeToken = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return brainTreeToken;
        }

        @Override
        protected void onPostExecute(String brainTreeToken) {
            super.onPostExecute(brainTreeToken);

            if (brainTreeToken != null) {
                try {
                    JSONObject root = new JSONObject(brainTreeToken);
                    if (isStatus) {
                        String clientToken = root.getString("clientToken");
                        global_clientToken = clientToken;
                        //it is now called in onclick listner
//                        onBraintreeSubmit(clientToken);
                        Log.d("demo", clientToken.toString());
                    }else{
                        //some error has occurred.. Have to handle it.
                        Toast.makeText(PaymentActivity.this, "Some error occured!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onBraintreeSubmit(String clientToken) {
        DropInRequest dropInRequest = new DropInRequest()
                .clientToken(clientToken);
        dropInRequest.collectDeviceData(true);
        startActivityForResult(dropInRequest.getIntent(this), 150);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 150) {
            if (resultCode == RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                // use the result to update your UI and send the payment method nonce to your server
                Log.d(TAG, "It is coming inside the result OK.. Yay!!!");
                Log.d(TAG, result.getDeviceData());
//                Log.d("describeCOntents", String.valueOf(result.describeContents()));

                Log.d(TAG, "Nounce"+result.getPaymentMethodNonce().getNonce());
                /////////sending data to server
                String nonce = result.getPaymentMethodNonce().getNonce();
                String deviceData = result.getDeviceData();
                String amount = getIntent().getStringExtra("total");
                new sendNounceToSever(deviceData,nonce,amount).execute();
                /////////

            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "It is coming inside the cancel");
                // the user canceled
            } else {
                // handle errors here, an exception may be available in
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.d("demo", "It is coming inside the error");
            }
        }
    }


    class sendNounceToSever extends AsyncTask<String ,Void,String>{

        String result = "";
        String deviceData = "";
        String nonce = "";
        String amount = "";
        String error = "";

        public sendNounceToSever( String deviceData, String nonce, String amount) {
            this.deviceData = deviceData;
            this.nonce = nonce;
            this.amount = amount;
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: sending nonce to server");
            final OkHttpClient client = new OkHttpClient();

            //////getting values
            String address = address_TIET.getText().toString().trim();
            String city = city_TIET.getText().toString().trim();
            String state = state_TIET.getText().toString().trim();
            String pinCode = pinCode_TIET.getText().toString().trim();
            String phone = phone_TIET.getText().toString().trim();
            Date d  =  new Date();

            RequestBody formBody = new FormBody.Builder()
                    .add("deviceData", deviceData)
                    .add("date",d.toString())
                    .add("nonce", nonce)
                    .add("amount", amount)
                    .add("address",address)
                    .add("city",city)
                    .add("state",state)
                    .add("zipCode",pinCode)
                    .add("phoneNumber",phone)
                    .build();

            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/checkout")
                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .post(formBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                result = response.body().string();
                Log.d(TAG, "doInBackground: after sending nounce response is=>"+result);
                Log.d(TAG, "doInBackground: code"+response.code()+" "+response.isSuccessful());
                if (!response.isSuccessful()){
                    Log.d(TAG, "doInBackground: error should occure after this");
                    throw new IOException("Unexpected code " + response);
                }

            }catch (Exception e){
                error = e.getMessage();
                Log.d(TAG, "onResponse: error occured while sending the nounce=>"+e.getMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (error.isEmpty()){
                Log.d(TAG, "onPostExecute: after sending nonce result=>"+result);
                Toast.makeText(PaymentActivity.this, "Payment was succefull", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }else{
                Log.d(TAG, "onPostExecute:after sending nonce error=>"+error);
                Toast.makeText(PaymentActivity.this, "Payment failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
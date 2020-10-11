package com.example.shoppingcartapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PaymentActivity extends AppCompatActivity {
    private float totalPrice = 10;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);

        new LoadClientTokenAsync().execute();

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
                        onBraintreeSubmit(clientToken);
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
        startActivityForResult(dropInRequest.getIntent(this), 150);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 150) {
            if (resultCode == RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                // use the result to update your UI and send the payment method nonce to your server
                Log.d("demo", "It is coming inside the result OK.. Yay!!!");
//                Log.d("deviceData", result.getDeviceData());
//                Log.d("describeCOntents", String.valueOf(result.describeContents()));
                Log.d("Nounce", result.getPaymentMethodNonce().getNonce());
            } else if (resultCode == RESULT_CANCELED) {
                Log.d("demo", "It is coming inside the cancel");
                // the user canceled
            } else {
                // handle errors here, an exception may be available in
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.d("demo", "It is coming inside the error");
            }
        }
    }
}
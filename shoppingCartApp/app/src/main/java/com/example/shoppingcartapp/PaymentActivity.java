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
                String nounce = result.getPaymentMethodNonce().getNonce();
                String deviceData = result.getDeviceData();
                sendNounceToSever(deviceData,nounce,"10");
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

    void sendNounceToSever(String deviceData,String nounce,String amount){
        final OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("deviceData", deviceData)
                .add("nounce", nounce)
                .add("amount", amount)
                .build();

        Request request = new Request.Builder()
                .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/checkout")
                .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }
                    System.out.println(responseBody.string());
                    Toast.makeText(PaymentActivity.this, "sent the nounce data sucessful", Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    Log.d(TAG, "onResponse: error occured while sending the nounce=>"+e.getMessage());
                }
            }
        });
    }
}
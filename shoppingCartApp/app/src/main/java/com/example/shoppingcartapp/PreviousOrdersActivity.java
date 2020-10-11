package com.example.shoppingcartapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PreviousOrdersActivity extends AppCompatActivity {

    private static final String TAG = "okay";
    SharedPreferences preferences;
    Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_orders);

        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);

        // just for testing
        new GetPreviousOrders().execute();
    }

    class GetPreviousOrders extends AsyncTask<String, Void, String> {
        boolean isStatus = true;

        @Override
        protected String doInBackground(String... strings) {
            final OkHttpClient client = new OkHttpClient();
            String jsonString = null;

            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/orders")
                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()){
                    isStatus = true;
                }else{
                    isStatus = false;
                }
                jsonString = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return jsonString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG, "onPostExecute: Previous order activity s=>"+s);
            JsonParser parser = new JsonParser();
            JsonElement tradeElement = parser.parse(s);
            JsonArray jsonArray = tradeElement.getAsJsonArray();
            for(int i=0;i<jsonArray.size();i++ ){
                JsonObject jsonObject = (JsonObject) jsonArray.get(i);
                PreviousOrderClass poc = gson.fromJson(jsonObject,PreviousOrderClass.class);
                Log.d(TAG, "onPostExecute: converting to json is successful");
                Log.d(TAG, "onPostExecute: poc dat="+poc.date);
                Log.d(TAG, "onPostExecute: poc first item name =>"+poc.items.get(0).name);
            }


        }
    }
}
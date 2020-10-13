package com.example.shoppingcartapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PreviousOrdersActivity extends AppCompatActivity {

    private static final String TAG = "okay";
    SharedPreferences preferences;
    Gson gson = new Gson();
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    ArrayList<PreviousOrderClass> orders =  new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_orders);

        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);

        recyclerView = findViewById(R.id.rv_container_in_previousOrders);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new PreviousOrderAdapter(orders, this);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // just for testing
        new GetPreviousOrders().execute();
    }

    class GetPreviousOrders extends AsyncTask<String, Void, String> {

        String error ="";

        @Override
        protected String doInBackground(String... strings) {
            final OkHttpClient client = new OkHttpClient();
            String jsonString = "";

            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/orders")
                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                jsonString = response.body().string();
                if (!response.isSuccessful()){
                    error = jsonString;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return jsonString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG, "onPostExecute: Previous order activity s=>"+s);
            if (error.isEmpty()){
                JsonParser parser = new JsonParser();
                JsonElement tradeElement = parser.parse(s);
                JsonArray jsonArray = tradeElement.getAsJsonArray();
                for(int i=0;i<jsonArray.size();i++ ){
                    JsonObject jsonObject = (JsonObject) jsonArray.get(i);
                    PreviousOrderClass poc = gson.fromJson(jsonObject,PreviousOrderClass.class);
                    orders.add(poc);
                    Log.d(TAG, "onPostExecute: converting to json is successful");
                    Log.d(TAG, "onPostExecute: poc dat="+poc.date);
                    Log.d(TAG, "onPostExecute: poc first item name =>"+poc.items.get(0).name);
                }
                SortAndNotifyDataSetChange();
                mAdapter.notifyDataSetChanged();
            }else{
                JSONObject root = null;
                try {
                    root = new JSONObject(s);

                    JSONObject error = root.getJSONObject("error");
                    //If it comes here it means that the jwt has been expired
                    if(error.getString("message").equals("jwt expired")){
                        Toast.makeText(PreviousOrdersActivity.this, "Session Expired. Please login to see the previous orders", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(PreviousOrdersActivity.this, MainActivity.class);
                        startActivity(intent);
                    }else if (error.getString("message").equals("jwt malformed")) {
                        //again the user has to go to login page
                        Toast.makeText(PreviousOrdersActivity.this, " Please login to see the previous orders", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(PreviousOrdersActivity.this, MainActivity.class);
                        startActivity(intent);
                    }else{
                        Toast.makeText(PreviousOrdersActivity.this, error.getString("message"), Toast.LENGTH_SHORT).show();
                        JSONArray message = error.getJSONArray("errors");
                        JSONObject arrayObject = message.getJSONObject(0);
                        Toast.makeText(PreviousOrdersActivity.this, arrayObject.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    try {
                        root = new JSONObject(s);
                        if(root.getString("error").equals("no previous orders found")){
                            Toast.makeText(PreviousOrdersActivity.this, "No Previous Orders found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
//                    e.printStackTrace();
                }

//             Toast.makeText(PreviousOrdersActivity.this, "Cart is empty", Toast.LENGTH_SHORT).show();
//                finish();
            }

        }
    }

    private void SortAndNotifyDataSetChange() {
        Collections.sort(orders, new Comparator<PreviousOrderClass>() {
            @Override
            public int compare(PreviousOrderClass o1, PreviousOrderClass o2) {
                SimpleDateFormat formater =  new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
                Date d1 = null;
                Date d2 = null;
                try {
                    d1 = formater.parse(o1.date);
                    d2 = formater.parse(o2.date);
                } catch (ParseException e) {
                    Log.d(TAG, "compare: error while sorting the previous orders=>"+e.getMessage());
                    e.printStackTrace();
                }
                return d2.compareTo(d1);
            }
        });
        mAdapter.notifyDataSetChanged();
    }
}
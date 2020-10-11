package com.example.shoppingcartapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CartCheckoutActivity extends AppCompatActivity {

    SharedPreferences preferences;
    ArrayList<Products> cartArrayList = new ArrayList<>();
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private TextView cartTotalPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_checkout);
        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
        cartTotalPrice = findViewById(R.id.cartTotalPrice);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerViewProductCart);

        layoutManager = new LinearLayoutManager(CartCheckoutActivity.this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new CartListAdapter(cartArrayList, CartCheckoutActivity.this);
        recyclerView.setAdapter(mAdapter);

        //checkout page where the user can delete all the items in the cart, or update a single item quantity or delete the item.

        findViewById(R.id.buttonProceedToPay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CartCheckoutActivity.this, PaymentActivity.class);
                startActivity(intent);
            }
        });


        new getCartAsync().execute();

    }

    public class getCartAsync extends AsyncTask<String, Void, String> {
        boolean isStatus = true;

        @Override
        protected String doInBackground(String... strings) {
            cartArrayList.clear();

            final OkHttpClient client = new OkHttpClient();
            String listofProducts = null;

            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/cart")
                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()){
                    isStatus = true;
                }else{
                    isStatus = false;
                }
                listofProducts = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return listofProducts;
        }

        @Override
        protected void onPostExecute(String listofProducts) {
            super.onPostExecute(listofProducts);

            if (listofProducts != null) {
                try {
                    JSONObject root = new JSONObject(listofProducts);
                    if (isStatus) {
                        String totalPrice = root.getString("total");
                        cartTotalPrice.setText("$"+totalPrice);
                        JSONArray rootArray = root.getJSONArray("cart");
                        for (int i = 0; i < rootArray.length(); i++) {
                            JSONObject arrayObject = rootArray.getJSONObject(i);
                            Products products = new Products();
                            products.id = arrayObject.getInt("id");
                            products.discount = arrayObject.getInt("discount");
                            products.name = arrayObject.getString("name");
                            products.photo = arrayObject.getString("photo");
                            products.price = arrayObject.getLong("price");
                            products.quantity = arrayObject.getInt("quantity");
                            cartArrayList.add(products);
                        }
                    }else{
                        //some error has occurred.. Have to handle it.
                        Toast.makeText(CartCheckoutActivity.this, "Some error occured!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("demo", cartArrayList.toString());

                if (cartArrayList.size() > 0) {
                    mAdapter.notifyDataSetChanged();
//                    getProductItemImage();
                }else{
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(CartCheckoutActivity.this, "Sorry no products available in the cart!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
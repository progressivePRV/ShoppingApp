package com.example.shoppingcartapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CartCheckoutActivity extends AppCompatActivity implements CartListAdapter.InteractWithRecyclerView{

    private static final String TAG = "okay";
    SharedPreferences preferences;
    ArrayList<Products> cartArrayList = new ArrayList<>();
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private TextView cartTotalPrice;
    private ProgressDialog progressDialog;

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

        findViewById(R.id.buttonDeleteCart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressBarDialog();
                new deleteCart().execute();
            }
        });

        showProgressBarDialog();
        new getCartAsync().execute();

    }

    @Override
    public void selectedItem(int id, String operation) {
        if(operation.equals("add")){
            //adding a quantity and then have to call the total
            showProgressBarDialog();
            Log.d("demo","Add operation started");
            new UpdateCartInCart(String.valueOf(id), String.valueOf(1), operation).execute();

        }else if(operation.equals("remove")){
            //removing a quantity and then have to call the total again
            showProgressBarDialog();
            Log.d("demo","Remove operation started");
            new UpdateCartInCart(String.valueOf(id), String.valueOf(1), operation).execute();

        }
    }

    @Override
    public void deleteItem(Products products) {
        new UpdateCartInCart(String.valueOf(products.id), String.valueOf(products.quantity), "remove").execute();
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
            Log.d(TAG, "onPostExecute: got cart string in cartCheckoutActivity=>"+listofProducts);
            if (listofProducts != null) {
                try {
                    JSONObject root = new JSONObject(listofProducts);
                    if (isStatus) {
                        String totalPrice = root.getString("total");
                        cartTotalPrice.setText("$"+totalPrice);
                        if(!totalPrice.equals("0")){
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
                        }

                    }else{
                        //some error has occurred.. Have to handle it.
                        hideProgressBarDialog();
                        Toast.makeText(CartCheckoutActivity.this, "Some error occured!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    hideProgressBarDialog();
                    e.printStackTrace();
                }
                Log.d("demo", cartArrayList.toString());

                if (cartArrayList.size() > 0) {
                    //For Sorting
                    Collections.sort(cartArrayList, new Comparator<Products>() {
                        @Override
                        public int compare(Products o1, Products o2) {
                            return o1.id - o2.id;
                        }
                    });
                    mAdapter.notifyDataSetChanged();
                    getProductItemImage();
                }else{
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(CartCheckoutActivity.this, "Sorry no products available in the cart!", Toast.LENGTH_SHORT).show();
                    hideProgressBarDialog();
                    finish();
                }
            }
        }
    }

    public void getProductItemImage(){
        for(int i=0; i<cartArrayList.size(); i++) {
            new LoadProductImageAsync().execute(cartArrayList.get(i).photo,String.valueOf(i));
        }
        hideProgressBarDialog();
    }


    //This is for loading the images Async
    public class LoadProductImageAsync extends AsyncTask<String, Void, Bitmap> {
        boolean isStatus = true;
        int value;

        @Override
        protected Bitmap doInBackground(String... strings) {

            final OkHttpClient client = new OkHttpClient();
            String listImage = null;
            Bitmap bitmap = null;
            value = Integer.valueOf(strings[1]);

            Log.d("demo",strings[0]);
            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/images/"+strings[0])
                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()){
                    InputStream inputStream = response.body().byteStream();
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    isStatus = true;
                }else{
                    isStatus = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (bitmap != null) {
                if (isStatus) {
                    Products pro = cartArrayList.get(value);
                    pro.productImage = bitmap;
                    cartArrayList.set(value, pro);
//                    Log.d("Demo", cartArrayList.get(value).toString());
//                    Toast.makeText(CartCheckoutActivity.this, "Hi it is coming to image", Toast.LENGTH_SHORT).show();
                    mAdapter.notifyDataSetChanged();
                }
                else{
                    //It means that there are no images present in the server. Leave it as it is
                    hideProgressBarDialog();
                }
            }

        }
    }

    public class UpdateCartInCart extends AsyncTask<String, Void, String> {
        boolean isStatus = true;

        String id, quantity;
        String operation;

        public UpdateCartInCart(String id, String quantity, String operation) {
            this.id = id;
            this.quantity = quantity;
            this.operation = operation;
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d("demo","it is coming inside do inbackground");
            final OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("id",id)
                    .add("quantity",quantity)
                    .add("operation",operation)
                    .build();
            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/items")
                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .put(formBody)
                    .build();
            String responseValue = null;
            try (Response response = client.newCall(request).execute()) {
                if(response.isSuccessful()){
                    isStatus = true;
                    Log.d("demo",operation +" is successful");
                }else{
                    isStatus = false;
                }
                responseValue = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return responseValue;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("demo",s);
            if(s!=null){
                JSONObject root = null;
                try {
                    root = new JSONObject(s);
                    if(isStatus){
                        Toast.makeText(CartCheckoutActivity.this, "Cart updated", Toast.LENGTH_SHORT).show();
                        new getCartAsync().execute();
                    }else{
                        //Handling the error scenario here
                        JSONObject error = root.getJSONObject("error");
                        JSONArray message = error.getJSONArray("errors");
                        JSONObject arrayObject = message.getJSONObject(0);
                        Toast.makeText(CartCheckoutActivity.this, arrayObject.getString("msg"), Toast.LENGTH_SHORT).show();
                        hideProgressBarDialog();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    hideProgressBarDialog();
                }
            }
        }
    }

    public class deleteCart extends AsyncTask<String, Void, String> {
        boolean isStatus = true;

        @Override
        protected String doInBackground(String... strings) {
            Log.d("demo","it is coming inside do inbackground");
            final OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/shop/cart")
                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .delete()
                    .build();
            String responseValue = null;
            try (Response response = client.newCall(request).execute()) {
                if(response.isSuccessful()){
                    isStatus = true;
                }else{
                    isStatus = false;
                }
                responseValue = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return responseValue;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("demo",s);
            if(s!=null){
                JSONObject root = null;
                try {
                    root = new JSONObject(s);
                    if(isStatus){
                        Toast.makeText(CartCheckoutActivity.this, "All Cart items Deleted. Going back to shopping page!", Toast.LENGTH_SHORT).show();
                        finish();
                    }else{
                        //Handling the error scenario here
                        JSONObject error = root.getJSONObject("error");
                        JSONArray message = error.getJSONArray("errors");
                        JSONObject arrayObject = message.getJSONObject(0);
                        Toast.makeText(CartCheckoutActivity.this, arrayObject.getString("msg"), Toast.LENGTH_SHORT).show();
                        hideProgressBarDialog();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    hideProgressBarDialog();
                }
            }
        }
    }

    //for showing the progress dialog
    public void showProgressBarDialog()
    {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    //for hiding the progress dialog
    public void hideProgressBarDialog()
    {
        progressDialog.dismiss();
    }
}
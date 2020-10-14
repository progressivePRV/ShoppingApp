package com.example.shoppingcartapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShoppingActivity extends AppCompatActivity implements ShoppingProductListAdapter.InteractWithRecyclerView{

    ArrayList<Products> productsArrayList = new ArrayList<>();
    private SharedPreferences preferences;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    SharedPreferences.Editor editor;
    Gson gson =  new Gson();


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_item_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String token_key = preferences.getString("TOKEN_KEY", null);
        if(token_key==null || token_key.isEmpty()){
            menu.getItem(0).setEnabled(true);
            menu.getItem(1).setEnabled(false);
            menu.getItem(2).setEnabled(false);
            menu.getItem(3).setEnabled(false);
        }else{
            menu.getItem(0).setEnabled(false);
            menu.getItem(1).setEnabled(true);
            menu.getItem(2).setEnabled(true);
            menu.getItem(3).setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.login_menu:
                Intent intent = new Intent(ShoppingActivity.this, MainActivity.class);
                startActivity(intent);
                return true;

            case R.id.showCart:
                //Still needs to be done, as Aditi is working on it
                Intent data = new Intent(ShoppingActivity.this, CartCheckoutActivity.class);
                startActivity(data);
                return true;

            case R.id.logout:
                // log out needs to be handled
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.commit();
                Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.previousOrders:
                // got to previous order intent
                Intent i = new Intent(this,PreviousOrdersActivity.class);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);

        setTitle("Products");

        preferences = getApplicationContext().getSharedPreferences("TokeyKey", 0);

        recyclerView = (RecyclerView) findViewById(R.id.productsRecyclerView);

        layoutManager = new LinearLayoutManager(ShoppingActivity.this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new ShoppingProductListAdapter(productsArrayList, ShoppingActivity.this);
        recyclerView.setAdapter(mAdapter);

        Toast.makeText(this, "Fetching all the prodcuts", Toast.LENGTH_SHORT).show();

        new LoadProductsAsync().execute("");
    }

    @Override
    public void getDetails(Products products) {
        Intent intent = new Intent(ShoppingActivity.this, ProductDetailActivity.class);
        preferences = getApplicationContext().getSharedPreferences("TokeyKey",0);
        editor = preferences.edit();
        editor.putString("PRODUCTS",gson.toJson(products));
        editor.commit();
        startActivity(intent);
    }

    public class LoadProductsAsync extends AsyncTask<String, Void, String> {
        boolean isStatus = true;

        @Override
        protected String doInBackground(String... strings) {
            productsArrayList.clear();

            final OkHttpClient client = new OkHttpClient();
            String listofProducts = null;

            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.endPointUrl)+"api/v1/items")
//                    .header("Authorization", "Bearer "+ preferences.getString("TOKEN_KEY", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()){
                    isStatus = true;
                }else{
                    isStatus = false;
                }
                listofProducts = response.body().string();
            } catch (IOException e) {
                Log.d("demo","Shopping activity exception");
                e.printStackTrace();
            }

            return listofProducts;
        }

        @Override
        protected void onPostExecute(String users) {
            super.onPostExecute(users);

            if (users != null) {
                try {
                    JSONObject root = new JSONObject(users);
                    if (isStatus) {
                        JSONArray rootArray = root.getJSONArray("results");
                        for (int i = 0; i < rootArray.length(); i++) {
                            JSONObject arrayObject = rootArray.getJSONObject(i);
                            Products products = new Products();
                            products.id = arrayObject.getInt("id");
                            products.discount = arrayObject.getInt("discount");
                            products.name = arrayObject.getString("name");
                            products.photo = arrayObject.getString("photo");
                            products.price = arrayObject.getDouble("price");
                            productsArrayList.add(products);
                        }
                    }else{
                        //some error has occurred.. Have to handle it.
                        Toast.makeText(ShoppingActivity.this, "Some error occured!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("demo", productsArrayList.toString());

                if (productsArrayList.size() > 0) {
                    mAdapter.notifyDataSetChanged();
//                    getProductItemImage();
                }else{
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(ShoppingActivity.this, "Sorry no products available for sale now!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void getProductItemImage(){
        for(int i=0; i<productsArrayList.size(); i++) {
            new LoadProductImageAsync().execute(productsArrayList.get(i).photo,String.valueOf(i));
        }
    }

    public class LoadProductImageAsync extends AsyncTask<String, Void, Bitmap> {
        boolean isStatus = true;
        int value;

        @Override
        protected Bitmap doInBackground(String... strings) {

            final OkHttpClient client = new OkHttpClient();
            String listImage = null;
            Bitmap bitmap = null;
            value = Integer.valueOf(strings[1]);

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
                    Products pro = productsArrayList.get(value);
                    pro.productImage = bitmap;
                    productsArrayList.set(value, pro);
                    mAdapter.notifyDataSetChanged();
                }
                else{
                    //It means that there are no images present in the server. Leave it as it is
                }
            }

        }
    }
}
package com.example.shoppingcartapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imageProductDetail;
    TextView textProductDetailName, textProductDetailDiscount, textProductDetailActualPrice, textActualDetailDiscountPrice;
    private SharedPreferences preferences;
    Gson gson = new Gson();
    private Button buttonProductDetailAdd;
    private ImageButton imageButtonAddQuantity, imageButtonRemoveQuantity;
    EditText textProductDetailQuantity;
    Products products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        preferences = getSharedPreferences("TokeyKey", 0);

        String token_key = preferences.getString("TOKEN_KEY", null);

        String pro =  preferences.getString("PRODUCTS",null);
        products = gson.fromJson(pro, Products.class);

        setTitle(products.name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageProductDetail = findViewById(R.id.imageProductDetail);
        textProductDetailName = findViewById(R.id.textProductDetailName);
        textProductDetailDiscount = findViewById(R.id.textProductDetailDiscount);
        textProductDetailActualPrice = findViewById(R.id.textProductDetailActualPrice);
        textActualDetailDiscountPrice = findViewById(R.id.textActualDetailDiscountPrice);
        imageProductDetail = findViewById(R.id.imageProductDetail);
        textProductDetailQuantity = findViewById(R.id.textProductDetailQuantity);

        buttonProductDetailAdd = findViewById(R.id.buttonProductDetailAdd);
        buttonProductDetailAdd.setEnabled(false);

        imageButtonAddQuantity = findViewById(R.id.imageButtonAddQuantity);
        imageButtonRemoveQuantity = findViewById(R.id.imageButtonRemoveQuantity);

        imageButtonRemoveQuantity.setEnabled(false);

        if(products.productImage != null){
            imageProductDetail.setImageBitmap(products.productImage);
        }else{
            imageProductDetail.setImageResource(R.drawable.ic_baseline_shopping_basket_24);
        }

        textProductDetailName.setText("Name: "+products.name);
        textProductDetailDiscount.setText("Discount: "+products.discount+"%");
        textProductDetailActualPrice.setText("Actual Price: $"+products.price);

        double discountPrice = (double) products.price * ((double)products.discount/100);
        double price = products.price - discountPrice;

        textActualDetailDiscountPrice.setText("Discount Price: $"+String.format("%.2f", price));

        imageButtonAddQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.valueOf(textProductDetailQuantity.getText().toString());
                quantity = quantity + 1;
                textProductDetailQuantity.setText(String.valueOf(quantity));
                if(quantity > 0){
                    buttonProductDetailAdd.setEnabled(true);
                    imageButtonRemoveQuantity.setEnabled(true);
                }else{
                    buttonProductDetailAdd.setEnabled(false);
                    imageButtonRemoveQuantity.setEnabled(false);
                }
            }
        });

        imageButtonRemoveQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.valueOf(textProductDetailQuantity.getText().toString().trim());
                quantity = quantity - 1;
                textProductDetailQuantity.setText(String.valueOf(quantity));
                if(quantity == 0){
                    buttonProductDetailAdd.setEnabled(false);
                    imageButtonRemoveQuantity.setEnabled(false);
                }else {
                    buttonProductDetailAdd.setEnabled(true);
                    imageButtonRemoveQuantity.setEnabled(true);
                }
            }
        });

        buttonProductDetailAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UpdateCart(String.valueOf(products.id), textProductDetailQuantity.getText().toString(), "add").execute("");
            }
        });
    }

    public class UpdateCart extends AsyncTask<String, Void, String> {
        boolean isStatus = true;

        String id, quantity;
        String operation;

        public UpdateCart(String id, String quantity, String operation) {
            this.id = id;
            this.quantity = quantity;
            this.operation = operation;
        }

        @Override
        protected String doInBackground(String... strings) {

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
                }else{
                    isStatus = false;
                }
                responseValue = response.body().string();
            } catch (IOException e) {
                Log.d("demo","Update cart exception");
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
                        Toast.makeText(ProductDetailActivity.this, "Cart updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }else{
                        Log.d("demo",root.length()+"");
                        //Handling the error scenario here
                        JSONObject error = root.getJSONObject("error");
                        //If it comes here it means that the jwt has been expired
                        if(error.getString("message").equals("jwt expired")){
                            Toast.makeText(ProductDetailActivity.this, "Session Expired. Please login before you add the items in the cart", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ProductDetailActivity.this, MainActivity.class);
                            startActivity(intent);
                        } else if (error.getString("message").equals("jwt malformed")) {
                            //again the user has to go to login page
                            Toast.makeText(ProductDetailActivity.this, "Please login before you add the items in the cart", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ProductDetailActivity.this, MainActivity.class);
                            startActivity(intent);
                        }else{
                            Toast.makeText(ProductDetailActivity.this, error.getString("message"), Toast.LENGTH_SHORT).show();
                            JSONArray message = error.getJSONArray("errors");
                            JSONObject arrayObject = message.getJSONObject(0);
                            Toast.makeText(ProductDetailActivity.this, arrayObject.getString("msg"), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
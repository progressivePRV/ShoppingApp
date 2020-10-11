package com.example.shoppingcartapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PreviousOrderProductAdapter extends RecyclerView.Adapter<PreviousOrderProductAdapter.ViewHolder> {

    private static final String TAG = "okay";
    ArrayList<Products> products =  new ArrayList<>();
    private SharedPreferences preferences;
    Context ctx;

    public PreviousOrderProductAdapter(ArrayList<Products> products, Context ctx) {
        this.products = products;
        preferences = ctx.getSharedPreferences("TokeyKey", 0);
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_product_layout,parent,false);
        PreviousOrderProductAdapter.ViewHolder viewHolder =  new PreviousOrderProductAdapter.ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: called in previous order product adapter");
        Products p = products.get(position);
        Log.d(TAG, "onBindViewHolder: product is=>"+p);
        holder.productQuantity.setText(""+p.quantity);
        holder.productPrice.setText(""+p.price);
        holder.productName.setText(p.name);
        GetImageAndSetIt(holder.productImage,p.photo);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName,productPrice,productQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image_inorderProduct);
            productName = itemView.findViewById(R.id.product_name_inorderProduct);
            productPrice = itemView.findViewById(R.id.product_price_inorderProduct);
            productQuantity = itemView.findViewById(R.id.product_quantity_inOrderProduct);
        }
    }

    void GetImageAndSetIt(final ImageView iv, String photo){
        String token = preferences.getString("TOKEN_KEY", null);

        final OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(ctx.getResources().getString(R.string.endPointUrl)+"api/v1/shop/images/"+photo)
                    .header("Authorization", "Bearer "+ token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        Log.d(TAG, "onResponse: in getting image in previous order=>"+response.body());
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                        //getting bitmat and showing in the image view
                        InputStream inputStream = response.body().byteStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        iv.setImageBitmap(bitmap);
                    }catch (Exception e){
                        Log.d(TAG, "onResponse: error in getting image in previous order");
                    }
                }
            });


    }
}

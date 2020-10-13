package com.example.shoppingcartapp;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ShoppingProductListAdapter extends RecyclerView.Adapter<ShoppingProductListAdapter.MyViewHolder> {
    private ArrayList<Products> mDataset;
    public static InteractWithRecyclerView interact;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ShoppingProductListAdapter(ArrayList<Products> myDataset, Context ctx) {
        mDataset = myDataset;
        interact = (InteractWithRecyclerView) ctx;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ShoppingProductListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_list, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        Products products = mDataset.get(position);

        holder.textItemName.setText(products.name);
        holder.textActualPrice.setText("$"+products.price);

        double discountPrice = (double) products.price * ((double)products.discount/100);
        Log.d("demo",discountPrice+"");
        double price = products.price - discountPrice;

//        Log.d("demo"," "+price);
        holder.textAfterDiscount.setText("$"+String.format("%.2f", price));
        holder.textDiscount.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        holder.textDiscount.setText("Discount : "+products.discount+"%");
        holder.textActualPrice.setPaintFlags(holder.textActualPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);


        Log.d("demo",products.photo);
        if(!products.photo.equals("")){
            Picasso.get()
                    .load("http://64.227.27.167:3000/api/v1/images/"+products.photo)
                    .into(holder.imageItem, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            holder.imageItem.setImageResource(R.drawable.ic_baseline_shopping_basket_24);
                        }
                    });
        }else{
            holder.imageItem.setImageResource(R.drawable.ic_baseline_shopping_basket_24);
        }


        holder.productItemConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Demo","Selected Position is :" + mDataset.get(position));
                interact.getDetails(mDataset.get(position));
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder{
        // each data item is just a string in this case
        ImageView imageItem;
        TextView textItemName,textDiscount, textActualPrice, textAfterDiscount;
        ConstraintLayout productItemConstraintLayout;
        public MyViewHolder(View view) {
            super(view);
            imageItem = view.findViewById(R.id.imageItem);
            textItemName = view.findViewById(R.id.textItemName);
            textDiscount = view.findViewById(R.id.textDiscount);
            textActualPrice = view.findViewById(R.id.textActualPrice);
            textAfterDiscount = view.findViewById(R.id.textAfterDiscount);
            productItemConstraintLayout = view.findViewById(R.id.productItemConstraintLayout);
        }
    }

    public interface InteractWithRecyclerView{
        public void getDetails(Products products);
    }
}

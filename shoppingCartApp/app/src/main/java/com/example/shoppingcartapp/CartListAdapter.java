package com.example.shoppingcartapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CartListAdapter  extends RecyclerView.Adapter<CartListAdapter.MyViewHolder> {
    private ArrayList<Products> mDataset;
    public static InteractWithRecyclerView interact;

    // Provide a suitable constructor (depends on the kind of dataset)
    public CartListAdapter(ArrayList<Products> myDataset, Context ctx) {
        mDataset = myDataset;
        interact = (InteractWithRecyclerView) ctx;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public CartListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cart_list, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        Products products = mDataset.get(position);

        holder.cartProductName.setText(products.name);
        holder.cartEditTextQuantity.setText(String.valueOf(products.quantity));

        double discountPrice = (double) products.price * ((double)products.discount/100);
        double price = products.price - discountPrice;

        holder.cartProductPrice.setText("$"+String.format("%.2f", price));
//        if(products.productImage != null){
//            holder.cartProductImage.setImageBitmap(products.productImage);
//        }else{
//            holder.cartProductImage.setImageResource(R.drawable.ic_baseline_shopping_basket_24);
//        }

        if(!products.photo.equals("")){
            Picasso.get()
                    .load("http://64.227.27.167:3000/api/v1/images/"+products.photo)
                    .into(holder.cartProductImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            holder.cartProductImage.setImageResource(R.drawable.ic_baseline_shopping_basket_24);
                        }
                    });
        }else{
            holder.cartProductImage.setImageResource(R.drawable.ic_baseline_shopping_basket_24);
        }

        if(products.quantity > 1){
            holder.cartButtonQuantityRemove.setEnabled(true);
        }else{
            holder.cartButtonQuantityRemove.setEnabled(false);
        }

        holder.cartButtonQuantityAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interact.selectedItem(mDataset.get(position).id,"add");
            }
        });

        holder.cartButtonQuantityRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interact.selectedItem(mDataset.get(position).id,"remove");
            }
        });
//
        holder.cartDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interact.deleteItem(mDataset.get(position));
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

        ImageView cartProductImage;
        TextView cartProductName, cartProductPrice;
        ImageButton cartButtonQuantityRemove, cartButtonQuantityAdd, cartDeleteButton;
        EditText cartEditTextQuantity;


        public MyViewHolder(View view) {
            super(view);
            cartProductImage = view.findViewById(R.id.cartProductImage);
            cartProductName = view.findViewById(R.id.cartProductName);
            cartProductPrice = view.findViewById(R.id.cartProductPrice);
            cartButtonQuantityRemove = view.findViewById(R.id.cartButtonQuantityRemove);
            cartButtonQuantityAdd = view.findViewById(R.id.cartButtonQuantityAdd);
            cartDeleteButton = view.findViewById(R.id.cartDeleteButton);
            cartEditTextQuantity = view.findViewById(R.id.cartEditTextQuantity);
        }

    }
//
    public interface InteractWithRecyclerView{
        public void selectedItem(int position, String operation);
        public void deleteItem(Products products);
    }

}

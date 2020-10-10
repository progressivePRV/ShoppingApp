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

import java.util.ArrayList;

public class CartListAdapter  extends RecyclerView.Adapter<CartListAdapter.MyViewHolder> {
    private ArrayList<Products> mDataset;
//    public static InteractWithRecyclerView interact;

    // Provide a suitable constructor (depends on the kind of dataset)
    public CartListAdapter(ArrayList<Products> myDataset, Context ctx) {
        mDataset = myDataset;
//        interact = (InteractWithRecyclerView) ctx;
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
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        Products products = mDataset.get(position);

        holder.cartProductName.setText(products.name);
        holder.cartEditTextQuantity.setText(String.valueOf(products.quantity));
        holder.cartProductPrice.setText(String.valueOf(products.price));

//
//        holder.cartDeleteButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                interact.selectedItem(position);
//            }
//        });

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
//    public interface InteractWithRecyclerView{
//        public void selectedItem(int position);
//    }



}

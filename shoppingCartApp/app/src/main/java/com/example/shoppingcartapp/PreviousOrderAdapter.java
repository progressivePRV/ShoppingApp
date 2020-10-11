package com.example.shoppingcartapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PreviousOrderAdapter extends RecyclerView.Adapter<PreviousOrderAdapter.ViewHolder> {

    private static final String TAG = "okay";
    ArrayList<PreviousOrderClass> orders = new ArrayList<>();
//    PreviousOrderAdapterINteractWithActivity interact;
    Context ctx;

    public PreviousOrderAdapter(ArrayList<PreviousOrderClass> orders, Context ctx) {
        this.orders = orders;
//        this.interact = (PreviousOrderAdapterINteractWithActivity) ctx;
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.previous_order_layout,parent,false);
        ViewHolder viewHolder =  new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final boolean[] isOpen = {false};
        final PreviousOrderClass poc = orders.get(position);
        Log.d(TAG, "onBindViewHolder: called in  PreviousOrderAdapter poc is=>"+poc);
        holder.amount_tv.setText(poc.amount);
        SimpleDateFormat formater =  new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
        SimpleDateFormat format =  new SimpleDateFormat("dd MMM yyyy hh:mm:ss");
        try {
            Date date = formater.parse(poc.date);
            Log.d(TAG, "onBindViewHolder: new date=>"+format.format(date));
        } catch (ParseException e) {
            Log.d(TAG, "onBindViewHolder: was not able to parse the date");
            e.printStackTrace();
        }
        holder.date_tv.setText(poc.date);
        RecyclerView recyclerView;
        RecyclerView.Adapter mAdapter;
        RecyclerView.LayoutManager layoutManager;
        recyclerView = (RecyclerView) holder.rv_container;
        layoutManager = new LinearLayoutManager(ctx);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new PreviousOrderProductAdapter(poc.items,ctx);
        recyclerView.setAdapter(mAdapter);
        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.d(TAG, "onClick: called for previous order at postion="+position);
                if (isOpen[0]){
                    holder.rv_container.setVisibility(View.GONE);
//                    holder.rv_container.animate()
//                            .translationY(0)
//                            .alpha(0.0f);
                    holder.quantity_tv.setVisibility(View.GONE);
                    holder.price_tv.setVisibility(View.GONE);
                    isOpen[0] = false;
                }else {
                    holder.rv_container.setVisibility(View.VISIBLE);
//                    holder.rv_container.animate()
//                            .translationY(holder.rv_container.getHeight())
//                            .alpha(0.0f);
                    holder.quantity_tv.setVisibility(View.VISIBLE);
                    holder.price_tv.setVisibility(View.VISIBLE);
                    isOpen[0] = true;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView date_tv,amount_tv,price_tv,quantity_tv;
        ConstraintLayout container;
        RecyclerView rv_container;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            amount_tv = itemView.findViewById(R.id.amount_tv_inPreviousOrderLayout);
            date_tv = itemView.findViewById(R.id.date_tv_inPreviousOrderLayout);
            container = itemView.findViewById(R.id.container_inPreviousOrderLayout);
            price_tv = itemView.findViewById(R.id.price_tv_inPreviousOrderLayout);
            quantity_tv = itemView.findViewById(R.id.quantity_tv_inPreviousOrderLayout);
            rv_container = itemView.findViewById(R.id.rv_container);
        }
    }

//    interface PreviousOrderAdapterINteractWithActivity{
//        void ShowOrder(int x);
//    }
}

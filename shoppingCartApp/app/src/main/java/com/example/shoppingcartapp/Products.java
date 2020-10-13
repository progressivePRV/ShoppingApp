package com.example.shoppingcartapp;

import android.graphics.Bitmap;

import java.io.Serializable;

public class Products implements Serializable {
    int id, discount;
    String name, photo;
    double price;
    Bitmap productImage;
    int quantity;

    @Override
    public String toString() {
        return "Products{" +
                "id=" + id +
                ", quanity="+ quantity+
                ", discount=" + discount +
                ", name='" + name + '\'' +
                ", photo='" + photo + '\'' +
                ", price=" + price +
                ", productImage=" + productImage +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

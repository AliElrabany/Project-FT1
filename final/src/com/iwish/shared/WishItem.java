package com.iwish.shared;

import java.io.Serializable;

public class WishItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int ownerId;
    private String name;
    private String description;
    private double price;
    private double amountContributed;
    private boolean fulfilled;

    public WishItem(int id, int ownerId, String name, String description,
                     double price, double amountContributed, boolean fulfilled) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.amountContributed = amountContributed;
        this.fulfilled = fulfilled;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public double getAmountContributed() {
        return amountContributed;
    }

    public double getAmountRemaining() {
        double remaining = price - amountContributed;
        if (remaining < 0) {
            remaining = 0;
        }
        return remaining;
    }

    public boolean isFulfilled() {
        return fulfilled;
    }

    public int getProgressPercent() {
        if (price <= 0) {
            return 0;
        }
        int percent = (int) ((amountContributed / price) * 100);
        if (percent > 100) {
            percent = 100;
        }
        return percent;
    }
}

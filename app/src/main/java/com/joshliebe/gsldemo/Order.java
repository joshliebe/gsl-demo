package com.joshliebe.gsldemo;

import com.braintreepayments.api.dropin.DropInResult;

public class Order {

    DropInResult result;
    Double latitude;
    Double longitude;

    public Order(DropInResult result, Double latitude, Double longitude) {
        this.result = result;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

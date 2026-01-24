package com.restaurant.qrorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RestaurantQROrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantQROrderApplication.class, args);
    }
}

package com.restaurant.qrorder.util;

import java.util.ArrayList;
import java.util.List;

public class VectorUtils {

    public static List<Double> parse(String vector){

        vector = vector.replace("[","")
                .replace("]","");

        String[] parts = vector.split(",");

        List<Double> result = new ArrayList<>();

        for(String p : parts){

            result.add(Double.parseDouble(p));

        }

        return result;
    }
}
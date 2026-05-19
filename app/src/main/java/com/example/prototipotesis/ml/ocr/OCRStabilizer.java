package com.example.prototipotesis.ml.ocr;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class OCRStabilizer {
    public static String mostFrecuentText(List<String> record){ // record = historial
        if(record == null || record.isEmpty()){
            return null;
        }

        Map<String, Integer> counter = new HashMap<>();

        for(String text : record){
            int frecuency = counter.containsKey(text) ? counter.get(text) : 0;
            counter.put(text, frecuency + 1);
        }
        String bestText = null;
        int bestFrecuency = 0;

        for(Map.Entry<String, Integer> input : counter.entrySet()){
            if(input.getValue() > bestFrecuency){
                bestFrecuency = input.getValue();
                bestText = input.getKey();

            }
        }
        return bestText;
    }
}

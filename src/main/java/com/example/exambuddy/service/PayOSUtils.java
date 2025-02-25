//package com.example.exambuddy.service;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.util.Base64;
//import java.util.*;
//
//import org.apache.commons.codec.digest.HmacUtils;
//import org.json.JSONObject;
//
//import org.apache.commons.codec.digest.HmacUtils;
//import org.json.JSONObject;
//
//import java.util.*;
//
//public class PayOSUtils {
//
//    private static final String SECRET_KEY = "780d39bae9a6bb0a1937fcd4d3e26e6eb8b4e3e504cf942df450daeaba763576"; // Thay bằng Secret Key thực tế
//
//    public static Boolean isValidData(String transaction, String transactionSignature) {
//        try {
//            JSONObject jsonObject = new JSONObject(transaction);
//            Iterator<String> sortedIt = sortedIterator(jsonObject.keys(), (a, b) -> a.compareTo(b));
//
//            StringBuilder transactionStr = new StringBuilder();
//            while (sortedIt.hasNext()) {
//                String key = sortedIt.next();
//                String value = jsonObject.get(key).toString();
//                transactionStr.append(key);
//                transactionStr.append('=');
//                transactionStr.append(value);
//                if (sortedIt.hasNext()) {
//                    transactionStr.append('&');
//                }
//            }
//
//            String signature = new HmacUtils("HmacSHA256", SECRET_KEY).hmacHex(transactionStr.toString());
//            return signature.equals(transactionSignature);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//    private static Iterator<String> sortedIterator(Iterator<?> it, Comparator<String> comparator) {
//        List<String> list = new ArrayList<>();
//        while (it.hasNext()) {
//            list.add((String) it.next());
//        }
//        list.sort(comparator);
//        return list.iterator();
//    }
//}
//
//

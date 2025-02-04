package com.example.exambuddy.service;

import com.example.exambuddy.model.User;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

public class UserService {
    private static final String COLLECTION_NAME = "users";
    public static User getUserData(String username){
        Firestore firestore = FirestoreClient.getFirestore();
        User user = new User();
        try{
            DocumentSnapshot documentSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();
            if (!documentSnapshot.exists()) return user;
            user.setEmail(documentSnapshot.getString("email"));
            user.setPhone(documentSnapshot.getString("phone"));
            user.setUsername(documentSnapshot.getString("username"));
            System.out.println("email: " + user.getEmail());
        }
        catch (Exception e){

        }

        return user;
    }



}

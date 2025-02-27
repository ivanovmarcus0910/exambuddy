package com.example.exambuddy.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

public class CloudinaryConfig {

    public Cloudinary getCloudinary() {
        Cloudinary cloudinary = null;
        Map config = new HashMap();
        config.put("cloud_name", "dsuav027e");
        config.put("api_key", "197582935118964");
        config.put("api_secret", "xiN2BKGHCHmPdFTYt71ueYKR_To");
        cloudinary = new Cloudinary(config);
        return cloudinary;
    }


}

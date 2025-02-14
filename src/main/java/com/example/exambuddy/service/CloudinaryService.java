package com.example.exambuddy.service;

import com.example.exambuddy.config.CloudinaryConfig;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.*;

@Service
public class CloudinaryService {
    private CloudinaryConfig x;
    private Cloudinary cloudinary;
    public CloudinaryService() {
         x = new CloudinaryConfig();
        cloudinary=x.getCloudinary();
    }


    public String upLoadFile(MultipartFile file) {
        try{
            Map data = this.cloudinary.uploader().upload(file.getBytes(), Map.of());
            String url = (String) data.get("url");
            return url;
        }catch (Exception e){
            System.out.println("Image upload fail");
            return null;
        }
    }
}

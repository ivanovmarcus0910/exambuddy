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
    private final CloudinaryConfig x;
    private final Cloudinary cloudinary;

    public CloudinaryService() {
        x = new CloudinaryConfig();
        cloudinary = x.getCloudinary();
    }


    public String upLoadImg(MultipartFile file, String foldername) {
        try {
            Map data = this.cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", foldername));
            String url = (String) data.get("url");
            return url;
        } catch (Exception e) {
            System.out.println("Image upload fail");
            return null;
        }
    }
    public String upLoadImgAvt(MultipartFile file, String folder, String username){
        try{
        String publicId = folder + "/" + username;  // Tạo public_id cố định cho user
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,  // Giữ nguyên ID để ghi đè ảnh cũ
                "overwrite", true,      // Bật ghi đè
                "invalidate", true,     // Xóa cache ảnh cũ
                "use_filename", false,  // Không tự động tạo tên mới
                "unique_filename", false // Không tạo file ngẫu nhiên
        ));
            return (String) uploadResult.get("secure_url");

        }
        catch (Exception e){
            System.out.println("Image upload fail");
            return null;
            }
    }
    public boolean deleteImageByUrl(String imageUrl) {
        String publicId = extractPublicIdFromUrl(imageUrl);
        if (publicId == null) return false;

        try {
            Map result = this.cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            System.out.println("Image delete failed: " + e.getMessage());
            return false;
        }
    }


    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            // Lấy phần sau `/upload/`
            String path = imageUrl.split("/upload/")[1];

            // Loại bỏ timestamp nếu có (bắt đầu bằng "v" + số)
            String[] parts = path.split("/");
            if (parts[0].matches("v\\d+")) {
                path = path.substring(parts[0].length() + 1);
            }

            // Loại bỏ phần mở rộng (.jpg, .png, ...)
            return path.substring(0, path.lastIndexOf("."));
        } catch (Exception e) {
            System.out.println("Failed to extract public ID: " + e.getMessage());
            return null;
        }
    }




    // Phương thức để upload từ byte[]
    public String uploadImgFromBytes(byte[] imageData, String folderName) {
        try {
            Map uploadParams = ObjectUtils.asMap("folder", folderName);
            Map data = this.cloudinary.uploader().upload(imageData, uploadParams);
            String url = (String) data.get("url");
            return url;
        } catch (Exception e) {
            System.out.println("Image upload fail: " + e.getMessage());
            return null;
        }
    }

}

package com.example.exambuddy.controller;

import com.example.exambuddy.model.Post;
import com.example.exambuddy.service.CloudinaryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller

public class ProfileController {
    @Autowired
    private CloudinaryService cloudinaryService;

    @RequestMapping("/profile")
    public String profilePage(HttpSession session, Model model ) {
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "login";
        }
        User user = UserService.getUserData(username);
        model.addAttribute("user", user);

        // Kiểm tra xem người dùng có phải là giáo viên chờ xét duyệt (Pending Teacher) hay không
        if ("PENDING_TEACHER".equalsIgnoreCase(user.getRole().toString())) {
            model.addAttribute("pendingTeacherMessage", "Bạn đang chờ xét duyệt làm giảng viên. Vui lòng cập nhật thông tin đầy đủ để admin duyệt.");
        }
        if ("TEACHER".equalsIgnoreCase(user.getRole().toString()) || "PENDING_TEACHER".equalsIgnoreCase(user.getRole().toString())) {
                return "profileTeacher"; // file profileTeacher.html
            } else {
                return "profileStudent"; // file profileStudent.html
            }
        }

    /*
    Cập nhật hồ sơ học sinh
     */
    @PostMapping("/profile/student/update")
    public String updateProfileStudent(HttpSession session,
                                       @RequestParam String firstName,
                                       @RequestParam String lastName,
                                       @RequestParam String phone,
                                       @RequestParam(required = false) String birthDate,
                                       @RequestParam(required = false) String grade,
                                       @RequestParam(required = false) String studentId,
                                       @RequestParam(required = false) String address,
                                       @RequestParam(required = false) String description,
                                       RedirectAttributes redirectAttributes,
                                       Model model ){
        String username = (String) session.getAttribute("loggedInUser");
        if(username == null){
            return "login";
        }

        // Cập nhật thông tin cho học sinh
        UserService.updateUserField(username, "firstName", firstName);
        UserService.updateUserField(username, "lastName", lastName);
        UserService.updateUserField(username, "phone", phone);
        if (birthDate != null && !birthDate.isEmpty()) {
            UserService.updateUserField(username, "birthDate", birthDate);
        }
        if (address != null && !address.isEmpty()) {
            UserService.updateUserField(username, "address", address);
        }
        if (grade != null && !grade.isEmpty()) {
            UserService.updateUserField(username, "grade", grade);
        }
        if (studentId != null && !studentId.isEmpty()) {
            UserService.updateUserField(username, "studentId", studentId);
        }
        if (description != null && !description.isEmpty()) {
            UserService.updateUserField(username, "description", description);
        }

        //Cập nhật lại thông tin
        User updateUser = UserService.getUserData(username);
        session.setAttribute("updateUser", updateUser);
        model.addAttribute("user", updateUser);
        // Thêm flash attribute để thông báo thành công
        redirectAttributes.addFlashAttribute("success", "Cập nhật thành công!");
        return "redirect:/profile";
    }

    /*
    Cập nhật hồ sơ giáo viên
     */
    @PostMapping("/profile/teacher/update")
    public String updateProfileTeacher(HttpSession session,
                                       @RequestParam String firstName,
                                       @RequestParam String lastName,
                                       @RequestParam String phone,
                                       @RequestParam(required = false) String birthDate,
                                       @RequestParam(required = false) String address,
                                       @RequestParam(required = false) String teacherCode,
                                       @RequestParam(required = false) String school,
                                       @RequestParam(required = false) String speciality,
                                       @RequestParam(required = false) Integer experience,
                                       @RequestParam(required = false) MultipartFile degreeFile,
                                       @RequestParam(required = false) String description,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {

        String username = (String) session.getAttribute("loggedInUser");
        if(username == null){
            return "login";
        }
        // Lấy thông tin người dùng hiện tại
        User user = UserService.getUserData(username);
        if (user == null) {
            // Nếu không tìm thấy người dùng, redirect về trang đăng nhập
            return "redirect:/login";
        }

        UserService.updateUserField(username, "firstName", firstName);
        UserService.updateUserField(username, "lastName", lastName);
        UserService.updateUserField(username, "phone", phone);
        if (birthDate != null && !birthDate.isEmpty()) {
            UserService.updateUserField(username, "birthDate", birthDate);
        }
        if (address != null && !address.isEmpty()) {
            UserService.updateUserField(username, "address", address);
        }
        if (description != null && !description.isEmpty()) {
            UserService.updateUserField(username, "description", description);
        }
        // Cập nhật các trường riêng của giáo viên
        if (teacherCode != null && !teacherCode.isEmpty()) {
            UserService.updateUserField(username, "teacherCode", teacherCode);
        }
        if (school != null && !school.isEmpty()) {
            UserService.updateUserField(username, "school", school);
        }
        if (speciality != null && !speciality.isEmpty()) {
            UserService.updateUserField(username, "speciality", speciality);
        }
        if (experience != null) {
            UserService.updateUserField(username, "experience", experience);
        }
        if (degreeFile != null && !degreeFile.isEmpty()) {
            // Upload file bằng cấp/chứng chỉ và lấy URL
            String degreeUrl = cloudinaryService.upLoadImg(degreeFile, "degreeFiles");
            UserService.updateUserField(username, "degreeFile", degreeUrl);
        }
//        // Lấy lại thông tin người dùng đã cập nhật
//        User updatedUser = UserService.getUserData(username);
//        session.setAttribute("user", updatedUser);
//        model.addAttribute("user", updatedUser);
//        // Thêm flash attribute để thông báo thành công
//        redirectAttributes.addFlashAttribute("success", "Cập nhật thành công!");
        // Kiểm tra vai trò hiện tại của người dùng
        if ("TEACHER".equalsIgnoreCase(user.getRole().toString())) {
            // Nếu người dùng đã là giáo viên (TEACHER), chỉ thông báo cập nhật thành công
            redirectAttributes.addFlashAttribute("success", "Cập nhật thành công.");
        } else if ("PENDING_TEACHER".equalsIgnoreCase(user.getRole().toString())) {
            // Nếu người dùng là PENDING_TEACHER, thay đổi trạng thái và gửi yêu cầu cho admin xét duyệt
            UserService.updateUserField(username, "teacherStatus", "WAITING_FOR_APPROVAL");
            redirectAttributes.addFlashAttribute("success", "Thông tin của bạn đã được gửi đến admin để xét duyệt.");
        }
        return "redirect:/profile";
    }




}

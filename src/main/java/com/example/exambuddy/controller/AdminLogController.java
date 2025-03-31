package com.example.exambuddy.controller;

import com.example.exambuddy.model.AdminLog;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.FirebaseAuthService;
import com.example.exambuddy.service.UserService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/admin/logs")
public class AdminLogController {

    @Autowired
    private FirebaseAuthService authService;

    @Autowired
    private UserService userService;

    private List<AdminLog> getAdminLogs() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<AdminLog> logs = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = db.collection("adminLogs").orderBy("timestamp").get();
        for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
            AdminLog log = doc.toObject(AdminLog.class);
            log.setId(doc.getId());
            logs.add(log);
        }
        return logs;
    }

    @GetMapping("")
    public String adminLogs(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }
        List<AdminLog> logs = getAdminLogs();
        model.addAttribute("logs", logs);
        User adminUser = userService.getUserByUsername(loggedInUser);
        model.addAttribute("adminUser", adminUser);
        return "adminLogs"; // File Thymeleaf hiển thị log của admin
    }
}

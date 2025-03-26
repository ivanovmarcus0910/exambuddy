package com.example.exambuddy.service;

import com.example.exambuddy.model.Payment;
import com.example.exambuddy.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    private static final String COLLECTION_NAME = "users";
    @Autowired
    private PaginationService<User> paginationService;
    private static final String EXAM_COLLECTION_NAME = "exams";
    public static User getUserData(String username) {
        Firestore firestore = FirestoreClient.getFirestore();

        try {
            DocumentSnapshot documentSnapshot = firestore.collection(COLLECTION_NAME)
                    .document(username)
                    .get()
                    .get();

            if (!documentSnapshot.exists()) {
                System.out.println("User not found in Firestore: " + username);
                return null;
            }

            User user = documentSnapshot.toObject(User.class);
            if (user == null) {
                System.out.println("Failed to map Firestore document to User object.");
                return null;
            }

            user.setUsername(username); // Đảm bảo username không bị null
            return user;

        } catch (Exception e) {
            System.out.println("Error fetching user data for: " + username);
            e.printStackTrace();
            return null;
        }
    }

    public static Boolean changeAvatar(String username, String url) {

        Firestore firestore = FirestoreClient.getFirestore();
        try {
            // 🔍 Tìm người dùng theo username trong Firestore
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();
            if (!userSnapshot.exists()) {
                System.out.println("❌ Không tìm thấy tài khoản với username: " + username);
                return false;
            }
            userSnapshot.getReference().update("avatarUrl", url);
        } catch (Exception e) {
            System.out.println("Lỗi khi đổi avatar");
            return false;
        }
        return true;
    }

    // ✅ Thêm mới hàm lưu tài khoản OAuth2 vào Firestore
        public static void saveOAuth2User(String email, String name, String avatarUrl) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference users = firestore.collection(COLLECTION_NAME);

        try {
            // Kiểm tra xem email này đã có trong DB chưa
            User existingUser = getUserData(email);
            if (existingUser != null && existingUser.getEmail() != null) {
                System.out.println("Tài khoản đã tồn tại: " + email);
                return; // Nếu đã tồn tại thì bỏ qua
            }


            User user = new User();
            user.setEmail(email);
            user.setUsername(name);
            user.setAvatarUrl(avatarUrl);
            user.setPhone(""); // Để trống nếu không có sđt
            user.setPassword(""); // OAuth2 không có password
            user.setVerified(true); // Đăng nhập Google thì coi như đã xác thực luôn

            users.document(email).set(user);
            System.out.println("✅ Đã lưu tài khoản Google vào Firestore: " + email);

        } catch (Exception e) {
            System.out.println("❌ Lỗi khi lưu tài khoản Google vào Firestore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getAvatarUrlByUsername(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            // Lấy dữ liệu của user từ Firestore
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();


            if (userSnapshot.exists()) {
                return userSnapshot.getString("avatarUrl"); // Lấy avatarUrl
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi lấy avatar của: " + username);
        }
        return "https://res.cloudinary.com/dsuav027e/image/upload/v1740403342/imgAvatar/sp9pms05th5guermrcve.png"; // Trả về avatar mặc định nếu không tìm thấy
    }

    public static void updateUserField(String username, String field, Object value) {
        Firestore firestore = FirestoreClient.getFirestore();
        DocumentReference userRef = firestore.collection(COLLECTION_NAME).document(username);

        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);

        try {
            userRef.update(updates).get();  // Cập nhật giá trị vào Firestore
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public List<User> getAllUsers() throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        List<User> userList = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();

        for (DocumentSnapshot document : future.get().getDocuments()) {
            User user = document.toObject(User.class);
            if (user != null) {
                user.setUsername(document.getId()); // Lấy ID từ Firestore
                userList.add(user);
            }
        }
        return userList;
    }

    public void updateUserRole(String username, User.Role role) {
        Firestore firestore = FirestoreClient.getFirestore();
        firestore.collection(COLLECTION_NAME).document(username).update("role", role.name());
    }

    public void deleteUser(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        firestore.collection(COLLECTION_NAME).document(username).delete();
    }

    public User getUserByUsername(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot userSnapshot = firestore.collection("users").

                    document(username).get().get();
            return userSnapshot.exists() ? userSnapshot.toObject(User.class) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addPaymentTransaction(long paymentCode, long amount, String link, String status, String username, String note) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            CollectionReference transaction = firestore.collection("Transactions");
            Long time = System.currentTimeMillis();
            Payment payment = new Payment(paymentCode, amount, link, status, username, time, note);
            transaction.document().set(payment);
            return true;
        }
        catch (Exception e) {
            return false;

        }
    }

    public static List<Payment> getPaymentsByPage(String username, int pageSize, Long lastTimestamp) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference transaction = firestore.collection("Transactions");

        Query query = transaction.whereEqualTo("username", username)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(pageSize);

        // Nếu không phải trang đầu tiên, dùng startAfter() để lấy tiếp
        if (lastTimestamp != null) {
            query = query.startAfter(lastTimestamp);
        }

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<Payment> payments = new ArrayList<>();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            payments.add(document.toObject(Payment.class));
        }
        return payments;
    }
    public static List<Payment> getPayments(String username) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference transaction = firestore.collection("Transactions");

        Query query = transaction.whereEqualTo("username", username);


        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<Payment> payments = new ArrayList<>();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            payments.add(document.toObject(Payment.class));
        }
        return payments;
    }

    public boolean changeCoinBalance(long change, String username){
        Firestore firestore = FirestoreClient.getFirestore();
        DocumentReference userRef = firestore.collection("users").document(username);
    try {
        ApiFuture<Boolean> transactionFuture = firestore.runTransaction(transaction -> {
            // Lấy document của user
            DocumentSnapshot snapshot = transaction.get(userRef).get();
            Long coinLong = snapshot.getLong("coin");
            long currentCoin = coinLong != null ? coinLong.longValue() : 0;
            long newCoin = currentCoin + change;
            if (newCoin < 0) {
                return false;
            }
            // Cập nhật lại giá trị coin mới vào document
            transaction.update(userRef, "coin", newCoin);
            return true;
            // Trả về giá trị coin mới
        });
        return transactionFuture.get();
    }
    catch (Exception e) {
        return false;
    }
    }

    public String updatePaymentStatus(long paymentCode, long amount, String status) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference transactions = firestore.collection("Transactions");
        Query query = transactions
                .whereEqualTo("paymentCode", paymentCode)
                .whereEqualTo("amount", amount);

        ApiFuture<QuerySnapshot> future = query.get();
        try {
            QuerySnapshot snapshot = future.get();
            if (snapshot.isEmpty()) {
                return null;
            } else {
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    document.getReference().update("status", status);
                    return document.getString("username");
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updatePaymentStatusFail(long paymentCode, String status) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference transactions = firestore.collection("Transactions");
        Query query = transactions
                .whereEqualTo("paymentCode", paymentCode);

        ApiFuture<QuerySnapshot> future = query.get();
        try {
            QuerySnapshot snapshot = future.get();
            if (snapshot.isEmpty()) {
                return false;
            } else {
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    document.getReference().update("status", status);
                    return true;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- Phương thức mới: cập nhật trạng thái active cho User ---
    public void updateUserStatus(String username, boolean newStatus) {
        Firestore firestore = FirestoreClient.getFirestore();
        DocumentReference userRef = firestore.collection(COLLECTION_NAME).document(username);
        try {
            userRef.update("active", newStatus).get();
            System.out.println("Cập nhật trạng thái active của user " + username + " thành " + newStatus);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Lỗi khi cập nhật trạng thái cho user " + username);
            e.printStackTrace();
        }
    }

    public boolean updateUserPremium(String username, long time){
        Firestore firestore = FirestoreClient.getFirestore();
        User user = getUserByUsername(username);
        long timeExpire = user.getTimeExpriredPremium();
        long newTime = Math.max(System.currentTimeMillis(), timeExpire) + time;
        DocumentReference userRef = firestore.collection(COLLECTION_NAME).document(username);
        try{
            userRef.update("timeExpriredPremium", newTime);
            return true;
        } catch (Exception e){
            return false;
        }
    }
    public boolean isExamCreator(String examId, String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot examSnapshot = firestore.collection(EXAM_COLLECTION_NAME)
                    .document(examId)
                    .get()
                    .get();
            if (!examSnapshot.exists()) {
                System.out.println("Exam not found: " + examId);
                return false;
            }
            String creator = examSnapshot.getString("username");
            return username != null && username.equals(creator);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error checking exam creator for examId: " + examId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy danh sách người dùng cho một trang với 10 phần tử mỗi trang, sắp xếp theo "joinTime".
     *
     * @param page Số trang (bắt đầu từ 0)
     * @param pageSize Số phần tử mỗi trang (ví dụ 10)
     * @return Danh sách người dùng của trang hiện tại.
     * @throws Exception Nếu có lỗi trong quá trình truy vấn.
     */
    public List<User> getUserPage(int page, int pageSize) throws Exception {
        return paginationService.getPage("users", "joinTime", page, pageSize, User.class);
    }



}

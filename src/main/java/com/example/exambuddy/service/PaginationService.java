package com.example.exambuddy.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class PaginationService<T> {

    /**
     * Lấy 1 trang dữ liệu từ Firestore theo thứ tự sắp xếp dựa trên field.
     *
     * @param collectionName Tên collection trong Firestore.
     * @param orderByField   Trường để sắp xếp (ví dụ "joinTime").
     * @param page           Số trang (bắt đầu từ 0).
     * @param pageSize       Số phần tử mỗi trang (ví dụ: 10).
     * @param clazz          Lớp kiểu đối tượng chuyển đổi.
     * @return Danh sách các đối tượng thuộc trang được truy vấn.
     * @throws Exception Nếu có lỗi xảy ra trong quá trình truy vấn.
     */
    public List<T> getPage(String collectionName, String orderByField, int page, int pageSize, Class<T> clazz) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionRef = db.collection(collectionName);

        // Sắp xếp theo trường được chỉ định và giới hạn số kết quả của trang hiện tại
        Query query = collectionRef.orderBy(orderByField).limit(pageSize);

        // Nếu không phải trang đầu tiên, lấy document cuối của trang trước đó để làm cursor
        if (page > 0) {
            Query previousQuery = collectionRef.orderBy(orderByField).limit(page * pageSize);
            ApiFuture<QuerySnapshot> previousFuture = previousQuery.get();
            List<? extends DocumentSnapshot> previousDocs;
            try {
                previousDocs = previousFuture.get().getDocuments();
            } catch (InterruptedException | ExecutionException e) {
                throw new Exception("Lỗi khi lấy dữ liệu của trang trước", e);
            }
            if (!previousDocs.isEmpty()) {
                DocumentSnapshot lastDoc = previousDocs.get(previousDocs.size() - 1);
                query = query.startAfter(lastDoc);
            }
        }

        ApiFuture<QuerySnapshot> future = query.get();
        List<? extends DocumentSnapshot> documents;
        try {
            documents = future.get().getDocuments();
        } catch (InterruptedException | ExecutionException e) {
            throw new Exception("Lỗi khi lấy dữ liệu trang hiện tại", e);
        }

        // Trả về danh sách đối tượng
        return documents.stream()
                .map(doc -> doc.toObject(clazz))
                .collect(Collectors.toList());
    }

}

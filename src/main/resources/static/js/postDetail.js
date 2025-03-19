function submitComment(event) {
    event.preventDefault(); // Ngăn chặn load lại trang

    const commentForm = document.getElementById("comment-form");
    const commentContent = document.getElementById("commentContent");
    const fileInput = document.querySelector("input[name='commentImages']");
    const parentCommentIdInput = document.getElementById("parentCommentId");

    const content = commentContent.value.trim();
    if (!content) {
        alert("Vui lòng nhập nội dung bình luận!");
        return;
    }

    const formData = new FormData();
    formData.append("postId", commentForm.querySelector("input[name='postId']").value);
    formData.append("content", content);
    formData.append("parentCommentId", parentCommentIdInput.value || "");

    const files = fileInput.files;
    if (files.length > 0) {
        for (let file of files) {
            formData.append("commentImages", file);
        }
    }

    // Tạo phản hồi tạm thời
    const tempComment = {
        commentId: "temp_" + Date.now(),
        postId: commentForm.querySelector("input[name='postId']").value,
        parentCommentId: parentCommentIdInput.value || "",
        username: document.querySelector("input[name='username']").value || "Ẩn danh",
        avatarUrl: document.querySelector("input[name='avatarUrl']")?.value || "default-avatar.jpg",
        content: content,
        timeAgo: "Vừa xong",
        likeCount: 0,
        liked: false,
        imageUrls: []
    };

    if (files.length > 0) {
        for (let file of files) {
            const imageUrl = URL.createObjectURL(file);
            tempComment.imageUrls.push(imageUrl);
        }
    }

    // Nếu là phản hồi, chèn vào phần replies
    if (parentCommentIdInput.value) {
        const parentCommentEl = document.getElementById("comment-" + parentCommentIdInput.value);
        const replyContainer = parentCommentEl.querySelector(".replies");

        if (replyContainer) {
            replyContainer.insertAdjacentHTML("beforeend", createCommentElement(tempComment));
        }
    } else {
        // Nếu là bình luận mới, thêm vào danh sách bình luận chính
        const commentList = document.querySelector(".comment-list");
        commentList.insertAdjacentHTML("beforeend", createCommentElement(tempComment));
    }

    // Gửi request lên server
    fetch("/forum/comment", {
        method: "POST",
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.commentId) {
                let tempCommentEl = document.getElementById("comment-" + tempComment.commentId);
                if (tempCommentEl) {
                    tempCommentEl.setAttribute("data-post-id", data.postId);
                    tempCommentEl.id = "comment-" + data.commentId;
                    tempCommentEl.querySelector(".heart-btn").setAttribute("data-comment-id", data.commentId);

                    let replyButtons = tempCommentEl.querySelectorAll(".reply-btn");
                    replyButtons.forEach(btn => {
                        btn.setAttribute("onclick", `setReplyForm('${data.commentId}', '${tempComment.username}', '${tempComment.parentCommentId || ""}')`);
                    });
                }
            } else {
                alert("Lỗi: Không thể đăng bình luận.");
            }
        })
        .catch(error => {
            console.error("Lỗi khi gửi bình luận:", error);
            alert("Không thể gửi bình luận. Hãy thử lại!");
        });

    // Reset form sau khi gửi
    commentContent.value = "";
    fileInput.value = "";
    parentCommentIdInput.value = "";
}

function createCommentElement(comment) {
    let imagesHtml = "";
    if (comment.imageUrls.length > 0) {
        imagesHtml = comment.imageUrls.map(url =>
            `<img src="${url}" class="comment-image" onclick="openImageModal(this)">`
        ).join("");
    }

    return `
        <div id="comment-${comment.commentId}" class="comment-item border-bottom py-2">
            <div class="d-flex">
                <img src="${comment.avatarUrl}" class="rounded-circle me-3 mt-1" width="35" height="35" alt="User">
                <div class="flex-grow-1">
                    <div class="card bg-light border-0 p-2">
                        <div class="d-flex align-items-center justify-content-between">
                            <h6 class="fw-bold mb-0">${comment.username}</h6>
                        </div>
                        <div class="dropdown">
                            <button class="btn btn-sm" type="button" data-bs-toggle="dropdown">
                                <i class="bi bi-three-dots-vertical"></i>
                            </button>
                            <ul class="dropdown-menu">
                                <li>
                                    <a class="dropdown-item text-primary" href="#"
                                        onclick="event.preventDefault(); editComment(this)"
                                        data-comment-id="${comment.commentId}"
                                        data-content="${comment.content}"
                                        data-images='${JSON.stringify(comment.imageUrls)}'
                                        data-username="${comment.username}">
                                       <i class="bi bi-pencil"></i> Chỉnh sửa
                                    </a>
                                </li>
                                <li><a class="dropdown-item text-danger" href="#" onclick="deleteComment('${comment.commentId}')"><i class="bi bi-trash"></i> Xoá</a></li>
                                <li><a class="dropdown-item text-secondary" href="#" onclick="reportComment('${comment.commentId}')"><i class="bi bi-flag"></i> Báo cáo</a></li>
                            </ul>
                        </div>
                        <span class="comment-content">${comment.content}</span>
                    </div>
                    <div class="comment-images mt-2">${imagesHtml}</div>
                    <div class="d-flex align-items-center mt-2">
                        <button class="btn btn-outline-secondary heart-btn" data-post-id="${comment.postId}"
                                data-comment-id="${comment.commentId}" onclick="toggleLikeComment(this)">
                            <i class="bi bi-heart"></i> <span class="heart-count">0</span>
                        </button>
                        <button class="btn btn-sm btn-link reply-btn text-dark text-decoration-none ms-2" onclick="setReplyForm('${comment.commentId}', '${comment.username}', ${comment.parentCommentId})">
                            Phản hồi
                        </button>
                        <span style="font-size: 11px; color: gray">Vừa xong</span>
                    </div>
                </div>
            </div>
            <div class="replies ms-4 mt-2"></div>
        </div>`;
}
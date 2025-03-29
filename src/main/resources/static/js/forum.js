document.addEventListener("DOMContentLoaded", function () {
    const links = document.querySelectorAll('.list-group-item');
    const urlParams = new URLSearchParams(window.location.search);
    const subject = urlParams.get("subject"); // Lấy giá trị subject từ URL

    let activeSet = false;

    links.forEach(link => {
        if (subject && link.href.includes("subject=" + subject)) {
            link.classList.add("active");
            activeSet = true;
        } else {
            link.classList.remove("active");
        }
    });

    // Nếu không có subject nào trong URL, mặc định chọn "Tất cả"
    if (!activeSet) {
        document.querySelector('.list-group-item[href="/forum"]').classList.add("active");
    }
});

// document.getElementById('commentImageInput').addEventListener('change', function(event) {
//     let fileList = event.target.files;
//     let output = document.getElementById('commentFiles');
//     output.innerHTML = ''; // Xóa danh sách cũ
//
//     if (fileList.length > 0) {
//         output.innerHTML = "<strong>Đã chọn:</strong> " +
//             Array.from(fileList).map(file => file.name).join(', ');
//     }
// });

function showAllImages(element) {
    const hiddenImages = element.nextElementSibling;
    const collapseButton = hiddenImages.nextElementSibling;

    if (hiddenImages) {
        hiddenImages.style.display = "flex";
    }
    if (collapseButton) {
        collapseButton.style.display = "block";
    }

    element.style.display = "none";
}

function collapseImages(element) {
    const hiddenImages = element.previousElementSibling;
    const moreImagesButton = hiddenImages.previousElementSibling;

    if (hiddenImages) {
        hiddenImages.style.display = "none";
    }
    if (element) {
        element.style.display = "none";
    }
    if (moreImagesButton) {
        moreImagesButton.style.display = "inline";
    }
}

function toggleLike(btn) {
    const usernameInput = document.querySelector('input[name="username"]');
    const username = usernameInput ? usernameInput.value : null;

    if (!username) {
        alert("Bạn cần đăng nhập để thích bài viết!");
        return;
    }

    const heartIcon = btn.querySelector('.bi');
    const heartCount = btn.querySelector('.heart-count');
    const postId = btn.getAttribute("data-post-id");
    let isLiked = heartIcon.classList.contains('bi-heart-fill');

    // Cập nhật giao diện trước khi gửi request
    if (isLiked) {
        heartIcon.classList.remove('bi-heart-fill');
        heartIcon.classList.add('bi-heart');
        heartCount.textContent = parseInt(heartCount.textContent) - 1;
        btn.classList.remove('text-danger');
    } else {
        heartIcon.classList.remove('bi-heart');
        heartIcon.classList.add('bi-heart-fill');
        heartCount.textContent = parseInt(heartCount.textContent) + 1;
        btn.classList.add('text-danger');
    }

    // Gửi request cập nhật trạng thái like
    fetch("/forum/like", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: `postId=${postId}&username=${username}&liked=${!isLiked}`
    })
        .then(response => response.json())
        .then(data => {
            if (data.likeCount !== undefined) {
                heartCount.textContent = data.likeCount;
            }
        })
        .catch(error => {
            console.error('❌ Lỗi khi cập nhật số lượt thích:', error);
        });
}

// Kiểm tra đăng nhập trước khi bình luận
document.querySelectorAll('#comment-form').forEach(form => {
    form.addEventListener('submit', function (event) {
        const username = document.querySelector('input[name="username"]').value;
        if (!username) {
            alert("Bạn cần đăng nhập để bình luận!");
            event.preventDefault(); // Ngăn form gửi đi
        }
    });
});


function showMoreComments(button) {
    const commentsSection = button.parentElement;
    const hiddenComments = commentsSection.querySelectorAll('.hidden-comment');
    let commentsToShow = 2;

    for (let i = 0; i < commentsToShow && i < hiddenComments.length; i++) {
        hiddenComments[i].classList.remove('hidden-comment');
    }

    // Ẩn nút "Hiển thị thêm bình luận" nếu không còn bình luận nào để hiển thị
    if (commentsSection.querySelectorAll('.hidden-comment').length === 0) {
        button.style.display = 'none';
    }
}

document.addEventListener('click', function (event) {
    const shareButton = event.target.closest('.share-button');
    if (!shareButton) return;

    // Lấy `postId` nếu có (chỉ có trên forum)
    const postId = shareButton.getAttribute('data-post-id');

    // Nếu có `postId`, tạo link `/postDetail/{postId}`
    let postUrl = window.location.href;
    if (postId) {
        postUrl = `${window.location.origin}/postDetail/${postId}`;
    }
    // ✅ Lưu link vào clipboard trước khi mở Share API
    navigator.clipboard.writeText(postUrl).then(() => {
        console.log('🔗 Link đã sao chép vào clipboard:', postUrl);
    }).catch(error => console.error('Lỗi khi sao chép link:', error));

    if (navigator.share) {
        navigator.share({
            title: document.title,
            url: postUrl
        }).then(() => {
            alert('🔗 Link bài viết đã được sao chép!');
        }).catch(error => console.error('Lỗi khi chia sẻ:', error));
    } else {
        alert('🔗 Link bài viết đã được sao chép!');
    }
});


function openImageModal(imgElement) {
    let modalImage = document.getElementById('modalImage');
    modalImage.src = imgElement.src;

    let modal = new bootstrap.Modal(document.getElementById('imageModal'));
    modal.show();
}

function openPostModal(event, postId) {
    event.preventDefault();
    let modal = new bootstrap.Modal(document.getElementById('postModal'));
    modal.show();

    let postContent = document.getElementById('postContent');
    postContent.innerHTML = "<p>Đang tải bài viết...</p>";

    // Cập nhật link cho nút phóng to
    document.getElementById('viewFullPost').href = `/postDetail/${encodeURIComponent(postId)}`;

    fetch(`/postDetail/${encodeURIComponent(postId)}?modal=true`)
        .then(response => response.text())
        .then(data => {
            postContent.innerHTML = data;
        })
        .catch(error => {
            console.error("Lỗi tải bài viết:", error);
            postContent.innerHTML = "<p class='text-danger'>Không thể tải bài viết.</p>";
        });
}

function openEditPostModal(button) {
    const postUsername = button.getAttribute('data-post-username');
    const currentUsername = document.querySelector('input[name="username"]').value;// Lấy tên người dùng hiện tại

    if (postUsername !== currentUsername) {
        alert("Bạn chỉ có thể chỉnh sửa bài viết của mình");
        return;
    }

    const postId = button.getAttribute('data-post-id');
    const content = button.getAttribute('data-post-content');
    const imagesAttr = button.getAttribute('data-post-images');
    const images = imagesAttr ? imagesAttr.split(',') : [];

    // Đặt giá trị vào modal
    document.getElementById('editPostId').value = postId;
    document.getElementById('editContent').value = content;

    // Xóa ảnh cũ và hiển thị ảnh mới (nếu có)
    const previewContainer = document.getElementById('editPostImagePreview');
    previewContainer.innerHTML = ""; // Xóa ảnh cũ
    images.forEach(imgUrl => {
        if (imgUrl.trim() !== "") {
            const imgElement = document.createElement('img');
            imgElement.src = imgUrl;
            imgElement.classList.add('img-thumbnail', 'm-1');
            imgElement.style.width = "100px";
            previewContainer.appendChild(imgElement);
        }
    });
    // Hiển thị modal
    var editModal = new bootstrap.Modal(document.getElementById('editPostModal'));
    editModal.show();
}

document.getElementById('edit-post-form').addEventListener('submit', function(event) {
    event.preventDefault();

    const formData = new FormData(this);

    fetch('/forum/edit', {
        method: 'POST',
        body: formData
    })
        .then(response => response.text())
        .then(data => {
            if (data === "success") {
                alert("Bài viết đã được cập nhật thành công!");
                var editModal = bootstrap.Modal.getInstance(document.getElementById('editPostModal'));
                editModal.hide(); // Ẩn modal sau khi cập nhật
                location.reload(); // Reload lại trang
            } else {
                alert("Lỗi: " + data);
            }
        })
        .catch(error => console.error('Lỗi:', error));
});

document.addEventListener("DOMContentLoaded", function () {
    const hiddenPosts = JSON.parse(localStorage.getItem("hiddenPosts")) || [];

    // Ẩn các bài viết đã bị ẩn trước đó
    hiddenPosts.forEach(postId => {
        const postElement = document.querySelector(`[data-post-id="${postId}"]`)?.closest(".post");
        if (postElement) postElement.style.display = "none";
    });

    // Hàm ẩn bài viết
    window.hidePost = function (event, postId) {
        event.preventDefault();
        const confirmHide = confirm("Bạn có chắc chắn muốn ẩn bài viết này không?");
        if (!confirmHide) return;

        // Lưu vào LocalStorage
        let hiddenPosts = JSON.parse(localStorage.getItem("hiddenPosts")) || [];
        if (!hiddenPosts.includes(postId)) {
            hiddenPosts.push(postId);
            localStorage.setItem("hiddenPosts", JSON.stringify(hiddenPosts));
        }

        // Ẩn bài viết trên giao diện
        const postElement = document.querySelector(`[data-post-id="${postId}"]`)?.closest(".post");
        if (postElement) postElement.style.display = "none";
    };
});

function confirmDeletePost(button) {
    const postId = button.getAttribute('data-post-id');
    const postUsername = button.getAttribute('data-post-username');
    const currentUsername = document.querySelector('input[name="username"]').value;// Lấy tên người dùng hiện tại

    // Kiểm tra quyền xóa
    if (postUsername !== currentUsername) {
        alert("Bạn chỉ có thể xóa bài viết của mình");
        return;
    }

    // Hiển thị hộp thoại xác nhận
    if (confirm("Bạn có chắc muốn xóa bài viết này không?")) {
        deletePost(postId);
    }
}

function deletePost(postId) {
    fetch(`/forum/delete/${postId}`, {
        method: 'DELETE'
    })
        .then(response => response.text())
        .then(data => {
            alert("Bài viết đã được xóa thành công!");
            window.location.href = "/forum"; // Reload trang sau khi xóa
        })
        .catch(error => console.error('Lỗi:', error));
}

function toggleLikeComment(btn) {
    const usernameInput = document.querySelector('input[name="username"]');
    const username = usernameInput ? usernameInput.value : null;

    if (!username) {
        alert("Bạn cần đăng nhập để thích bình luận!");
        return;
    }

    const heartIcon = btn.querySelector('.bi');
    const heartCount = btn.querySelector('.heart-count');
    const postId = btn.getAttribute("data-post-id");
    const commentId = btn.getAttribute("data-comment-id");
    let isLiked = heartIcon.classList.contains('bi-heart-fill');

    // Cập nhật giao diện trước khi gửi request
    if (isLiked) {
        heartIcon.classList.remove('bi-heart-fill');
        heartIcon.classList.add('bi-heart');
        heartCount.textContent = parseInt(heartCount.textContent) - 1;
        btn.classList.remove('text-danger');
    } else {
        heartIcon.classList.remove('bi-heart');
        heartIcon.classList.add('bi-heart-fill');
        heartCount.textContent = parseInt(heartCount.textContent) + 1;
        btn.classList.add('text-danger');
    }

    // Gửi request cập nhật trạng thái like
    fetch("/forum/likeComment", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: `postId=${postId}&commentId=${commentId}&username=${username}&liked=${!isLiked}`
    })
        .then(response => response.json())
        .then(data => {
            if (data.likeCount !== undefined) {
                heartCount.textContent = data.likeCount;
            }
        })
        .catch(error => {
            console.error('❌ Lỗi khi cập nhật số lượt thích bình luận:', error);
        });
}

function toggleReplyForm(button, commentId) {
    let form = document.getElementById("reply-form-" + commentId);
    form.style.display = form.style.display === "none" ? "block" : "none";
}

function setReplyForm(commentId, username, parentCommentId) {
    let form = document.getElementById("comment-form");
    let parentInput = document.getElementById("parentCommentId");
    let contentInput = document.getElementById("commentContent");
    let replyInfo = document.getElementById("reply-info");
    let replyUsername = document.getElementById("reply-username");

    // Nếu commentId là temp_xxx, kiểm tra xem có ID thật chưa
    let actualCommentEl = document.getElementById("comment-" + commentId);
    if (actualCommentEl && actualCommentEl.id.startsWith("comment-temp_")) {
        commentId = actualCommentEl.getAttribute("data-comment-id") || commentId;
    }

    // Nếu có parentCommentId (tức là đang phản hồi vào reply), thì lấy comment gốc làm parent
    if (parentCommentId) {
        parentInput.value = parentCommentId;
    } else {
        parentInput.value = commentId;
    }

    // Cập nhật tên người được phản hồi
    replyUsername.innerText = username;
    replyInfo.classList.remove("d-none"); // Hiển thị thông tin phản hồi

    // Hiển thị form ngay dưới comment chính
    let commentElement = document.getElementById("comment-" + (parentCommentId ? parentCommentId : commentId));
    if (commentElement) {
        commentElement.appendChild(form);
    } else {
        console.warn("⚠ Không tìm thấy comment để gán form reply:", commentId);
    }

    // Focus vào ô nhập nội dung
    contentInput.focus();
}

function cancelReply() {
    let parentInput = document.getElementById("parentCommentId");
    let replyInfo = document.getElementById("reply-info");
    let form = document.getElementById("comment-form");
    let contentInput = document.getElementById("commentContent");

    // Xóa ID của comment cha (trở lại trạng thái bình luận mới)
    parentInput.value = "";

    // Ẩn phần thông tin phản hồi
    replyInfo.classList.add("d-none");

    // Đưa form về vị trí mặc định (bình luận cho bài viết)
    document.querySelector(".add-comment").appendChild(form);

    // Reset nội dung nhập vào
    contentInput.value = "";
}

document.addEventListener("DOMContentLoaded", function () {
    const reportModal = new bootstrap.Modal(document.getElementById("reportModal"));
    let selectedPostId = "";

    // Hàm mở modal báo cáo
    window.openReportModal = function (event, postId) {
        event.preventDefault(); // Ngăn chặn load lại trang
        selectedPostId = postId;
        document.getElementById("reportPostId").value = selectedPostId;
        reportModal.show();
    };

    // Xử lý submit báo cáo
    document.getElementById("submitReportBtn").addEventListener("click", function (event) {
        event.preventDefault(); // Ngăn chặn hành vi mặc định của button submit

        const reasons = Array.from(document.querySelectorAll('input[name="reportReason"]:checked'))
            .map(checkbox => checkbox.value); // Lấy danh sách lý do đã chọn
        const description = document.getElementById("reportDescription").value;

        if (!selectedPostId) {
            alert("Lỗi: Không tìm thấy bài viết để báo cáo.");
            return;
        }

        if (reasons.length === 0) {
            alert("Vui lòng chọn ít nhất một lý do báo cáo.");
            return;
        }

        // Gửi request đến backend
        fetch("/reportPost", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                postId: selectedPostId,
                reasons: reasons, // Gửi danh sách lý do thay vì một chuỗi đơn lẻ
                description: description
            })
        })
            .then(response => response.text()) // Đổi sang text vì ResponseEntity trả về String
            .then(data => {
                alert(data); // Hiển thị thông báo từ server
                reportModal.hide();
                document.getElementById("reportForm").reset(); // Xóa dữ liệu sau khi gửi
            })
            .catch(error => {
                console.error("Lỗi khi gửi báo cáo:", error);
                alert("Có lỗi xảy ra, vui lòng thử lại!");
            });
    });
});

function editComment(element) {
    event.preventDefault();

    // Lấy dữ liệu từ `data-*`
    const commentId = element.getAttribute("data-comment-id");
    const content = element.getAttribute("data-content");
    const username = element.getAttribute("data-username");
    const images = element.getAttribute("data-images") ? element.getAttribute("data-images").split(",") : [];

    // Kiểm tra quyền chỉnh sửa
    const currentUsername = document.querySelector("input[name='username']").value;
    if (currentUsername !== username) {
        alert("Bạn chỉ có thể chỉnh sửa bình luận của mình!");
        return;
    }

    // Gán dữ liệu vào modal
    document.getElementById("editCommentId").value = commentId;
    document.getElementById("editCommentContent").value = content;

    // Hiển thị ảnh hiện tại
    const previewContainer = document.getElementById("editCommentImagePreview");
    previewContainer.innerHTML = ""; // Xóa ảnh cũ trước khi thêm mới

    if (images.length > 0 && images[0] !== "") {
        images.forEach(url => {
            const img = document.createElement("img");
            img.src = url;
            img.classList.add("comment-image", "me-2");
            img.style.width = "80px";
            img.style.height = "80px";
            previewContainer.appendChild(img);
        });
    }

    // Khi chọn ảnh mới, xóa ảnh cũ khỏi preview
    document.getElementById("editCommentImage").addEventListener("change", function () {
        if (this.files.length > 0) {
            previewContainer.innerHTML = ""; // Xóa ảnh cũ
        }
    });

    // Hiển thị modal chỉnh sửa bình luận
    new bootstrap.Modal(document.getElementById("editCommentModal")).show();
}

function submitEditComment() {
    let commentId = document.getElementById("editCommentId").value;
    let postId = document.getElementById("comment-form").querySelector("input[name='postId']").value;
    let content = document.getElementById("editCommentContent").value;
    let keepOldImages = document.getElementById("editCommentImage").files.length === 0;
    let formData = new FormData();

    formData.append("commentId", commentId);
    formData.append("postId", postId);
    formData.append("content", content);
    formData.append("keepOldImages", keepOldImages);

    if (!keepOldImages) {
        let fileInput = document.getElementById("editCommentImage");
        for (let file of fileInput.files) {
            formData.append("newImages", file);
        }
    }
    console.log(" commentId: ", commentId);
    console.log("🔍 postId:", postId);

    fetch("/forum/comment/edit", {
        method: "POST",
        body: formData
    }).then(response => response.json()).then(data => {
        if (data.success) {
            alert("Cập nhật bình luận thành công!");
            window.location.href = "/postDetail/" + postId;
        } else {
            alert("Lỗi: " + data.message);
        }
    }).catch(error => console.error("❌ Lỗi:", error));
}

function confirmDeleteComment(element) {
    event.preventDefault();
    const commentId = element.getAttribute("data-comment-id");
    const username = element.getAttribute("data-username");

    // Kiểm tra quyền chỉnh sửa
    const currentUsername = document.querySelector("input[name='username']").value;
    if (currentUsername !== username) {
        alert("Bạn chỉ có thể chỉnh sửa bình luận của mình!");
        return;
    }

    if (confirm("Bạn có chắc chắn muốn xoá bình luận này không?")) {
        deleteComment(commentId);
    }
}

function deleteComment(commentId) {
    const postId = document.querySelector("input[name='postId']").value;

    fetch("/forum/comment/delete", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ postId: postId, commentId: commentId })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert("Xóa bình luận thành công!");
                document.getElementById(`comment-${commentId}`).remove(); // Xóa UI
            } else {
                alert("Lỗi: " + data.message);
            }
        })
        .catch(error => console.error("❌ Lỗi:", error));
}










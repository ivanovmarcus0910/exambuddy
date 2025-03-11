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

    // Ẩn bài viết đã lưu trong LocalStorage
    hiddenPosts.forEach(postId => {
        const postElement = document.querySelector(`[data-post-id="${postId}"]`).closest(".post");
        if (postElement) postElement.style.display = "none";
    });

    // Xử lý sự kiện khi nhấn "Ẩn bài viết"
    document.querySelectorAll(".hide-post-btn").forEach(button => {
        button.addEventListener("click", function (event) {
            event.preventDefault();
            const postId = this.getAttribute("data-post-id");

            const confirmHide = confirm("Bạn có chắc chắn muốn ẩn bài viết này không?");
            if (!confirmHide) return;

            // Lưu postId vào danh sách ẩn
            let hiddenPosts = JSON.parse(localStorage.getItem("hiddenPosts")) || [];
            if (!hiddenPosts.includes(postId)) {
                hiddenPosts.push(postId);
                localStorage.setItem("hiddenPosts", JSON.stringify(hiddenPosts));
            }
            // Ẩn bài viết trên giao diện
            const postElement = this.closest(".post");
            if (postElement) postElement.style.display = "none";
        });
    });
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

function setReplyForm(commentId, username) {
    let form = document.getElementById("comment-form");
    let parentInput = document.getElementById("parentCommentId");
    let contentInput = document.getElementById("commentContent");
    let replyInfo = document.getElementById("reply-info");
    let replyUsername = document.getElementById("reply-username");

    // Cập nhật giá trị ID của comment cha
    parentInput.value = commentId;

    // Cập nhật tên người được phản hồi
    replyUsername.innerText = username;
    replyInfo.classList.remove("d-none"); // Hiển thị thông tin phản hồi

    // Hiển thị form ngay dưới comment được phản hồi
    let commentElement = document.getElementById("comment-" + commentId);
    commentElement.appendChild(form);

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







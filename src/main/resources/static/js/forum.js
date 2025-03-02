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

document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll('.toggle-comments').forEach(button => {
        button.addEventListener('click', function () {
            // Tìm phần tử cha lớn nhất chứa cả post-actions và comments-section
            const postContainer = this.closest('.post');

            // Tìm comments-section bên trong post đó
            const commentsSection = postContainer.querySelector('.comments-section');

            if (commentsSection) {
                commentsSection.style.display = (commentsSection.style.display === 'none' || commentsSection.style.display === '')
                    ? 'block'
                    : 'none';
            }
        });
    });

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

function togglePostSettingsMenu(element) {
    const menu = element.nextElementSibling;
    menu.classList.toggle('hidden');
}

function editPost(button) {
    const postUsername = button.getAttribute('data-post-username');
    const currentUsername = '[[@{${username}}]]'; // Lấy tên người dùng hiện tại

    if (postUsername !== currentUsername) {
        alert("Bạn chỉ có thể chỉnh sửa bài viết của mình");
        return;
    }

    const postId = button.getAttribute('data-post-id');
    window.location.href = `/forum/edit/${postId}`;
}

// Xoá bài viết
function deletePost(button) {
    const postUsername = button.getAttribute('data-post-username');
    const currentUsername = '[[@{${username}}]]';

    if (postUsername !== currentUsername) {
        alert("Bạn chỉ có thể xoá bài viết của mình");
        return;
    }

    const postId = button.getAttribute('data-post-id');
    if (confirm("Bạn có chắc muốn xoá bài viết này không?")) {
        window.location.href = `/forum/delete/${postId}`;
    }
}

// Báo cáo bài viết
function reportPost(button) {
    const postId = button.getAttribute('data-post-id');
    alert(`Đã báo cáo bài viết với ID: ${postId}`);
}

document.querySelectorAll('.share-button').forEach(button => {
    button.addEventListener('click', function () {
        const postUrl = window.location.href; // Lấy URL hiện tại
        if (navigator.share) {
            // Nếu trình duyệt hỗ trợ Web Share API
            navigator.share({
                title: document.title,
                url: postUrl
            }).catch(error => console.error('Lỗi khi chia sẻ:', error));
        } else {
            // Nếu không hỗ trợ Web Share API, hiển thị hộp thoại copy link
            navigator.clipboard.writeText(postUrl).then(() => {
                alert('🔗 Link bài viết đã được sao chép!');
            }).catch(error => console.error('Lỗi khi sao chép link:', error));
        }
    });
});

function openImageModal(imgElement) {
    let modalImage = document.getElementById('modalImage');
    modalImage.src = imgElement.src;

    let modal = new bootstrap.Modal(document.getElementById('imageModal'));
    modal.show();
}

function openPostModal(event, postId) {
    event.preventDefault(); // Ngăn chặn điều hướng trang
    console.log("Đã nhấn vào bài viết có ID:", postId); // Kiểm tra log trong Console

    let modal = new bootstrap.Modal(document.getElementById('postModal'));
    modal.show();

    let postContent = document.getElementById('postContent');
    postContent.innerHTML = "<p>Đang tải bài viết...</p>";

    // Kiểm tra đường dẫn có đúng không
    let url = `/postDetail/${encodeURIComponent(postId)}`;
    console.log("Đang tải bài viết từ:", url);

    fetch(url)
        .then(response => response.text())
        .then(html => {
            postContent.innerHTML = html;
        })
        .catch(error => {
            console.error("Lỗi tải bài viết:", error);
            postContent.innerHTML = "<p class='text-danger'>Không thể tải bài viết.</p>";
        });
}



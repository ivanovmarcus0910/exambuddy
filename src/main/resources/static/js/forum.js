function showAllImages(element) {
    const hiddenImages = element.nextElementSibling;
    const collapseButton = hiddenImages.nextElementSibling;

    hiddenImages.style.display = "flex";
    collapseButton.style.display = "block";
    element.style.display = "none";
}

function collapseImages(element) {
    // Tìm phần tử chứa các ảnh ẩn
    const hiddenImages = element.previousElementSibling;
    // Tìm phần "+x ảnh"
    const moreImagesButton = hiddenImages.previousElementSibling;

    // Ẩn các ảnh thừa
    hiddenImages.style.display = "none";
    // Ẩn nút "Thu gọn"
    element.style.display = "none";

    // Hiển thị lại phần "+x ảnh"
    moreImagesButton.style.display = "flex";
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
            const commentsSection = this.closest('.post-actions').nextElementSibling;

            if (commentsSection.style.display === 'none' || commentsSection.style.display === '') {
                commentsSection.style.display = 'block';
            } else {
                commentsSection.style.display = 'none';
            }
        });
    });

    // Hiển thị menu hành động khi hover vào bình luận
    document.querySelectorAll('.comment-item').forEach(item => {
        item.addEventListener('mouseenter', function () {
            this.querySelector('.comment-actions').style.display = 'block';
        });
        item.addEventListener('mouseleave', function () {
            this.querySelector('.comment-actions').style.display = 'none';
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

function openImage(img) {
    // Lấy thẻ lightbox và ảnh lớn
    const lightbox = document.getElementById('lightbox');
    const lightboxImage = document.getElementById('lightbox-image');
    const lightboxClose = document.getElementById('lightbox-close');

    // Gán ảnh nguồn vào lightbox
    lightboxImage.src = img.src;

    // Hiển thị lightbox
    lightbox.classList.add('show');

    // Đóng lightbox khi nhấn nút "X"
    lightboxClose.onclick = function () {
        lightbox.classList.remove('show');
    };

    // Đóng lightbox khi nhấn ra ngoài ảnh
    lightbox.onclick = function (e) {
        if (e.target === lightbox) {
            lightbox.classList.remove('show');
        }
    };
}
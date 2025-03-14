document.addEventListener("DOMContentLoaded", function () {
    // Hàm cập nhật hiển thị sao
    function updateStars(rate, targetStars) {
        targetStars.forEach(star => {
            const starRate = parseInt(star.getAttribute('data-rate'));
            if (starRate <= rate) {
                star.className = 'fa-solid fa-star checked';
            } else {
                star.className = 'fa-regular fa-star';
            }
        });
    }

    // Hàm hiển thị modal thông báo
    function showModal(message, isSuccess = true) {
        const modalBody = document.getElementById('feedbackModalBody');
        const modalTitle = document.getElementById('feedbackModalLabel');
        modalBody.textContent = message;
        modalTitle.textContent = isSuccess ? 'Thành công' : 'Lỗi';
        modalBody.className = 'modal-body ' + (isSuccess ? 'text-success' : 'text-danger');
        const modal = new bootstrap.Modal(document.getElementById('feedbackModal'));
        modal.show();
    }


    // Xử lý đánh giá sao (form gửi mới)
    const stars = document.querySelectorAll('#feedback-form .star-rating .fa-star');
    const rateInput = document.getElementById('rate');
    let selectedRate = parseInt(rateInput?.value) || 0;

    if (!rateInput) {
        console.error("Không tìm thấy phần tử #rate!");
    } else {
        stars.forEach(star => {
            star.addEventListener('click', function () {
                selectedRate = parseInt(this.getAttribute('data-rate'));
                rateInput.value = selectedRate;
                updateStars(selectedRate, stars);
            });

            star.addEventListener('mouseover', function () {
                updateStars(parseInt(this.getAttribute('data-rate')), stars);
            });

            star.addEventListener('mouseout', function () {
                updateStars(selectedRate, stars);
            });
        });

        document.getElementById('feedback-form').addEventListener('submit', function (event) {
            if (!rateInput.value || parseInt(rateInput.value) === 0) {
                showModal("Vui lòng chọn số sao đánh giá!", false);
                event.preventDefault();
            }
        });

        updateStars(selectedRate, stars);
    }

    // Xử lý chỉnh sửa và xóa feedback/reply
    function attachEditEvents() {
        document.querySelectorAll('.edit-feedback').forEach(button => {
            button.removeEventListener('click', handleEditClick);
            button.addEventListener('click', handleEditClick);
        });
    }

    function handleEditClick(e) {
        e.stopPropagation();
        const feedbackId = this.getAttribute('data-id');
        const item = document.getElementById(`feedback-${feedbackId}`);
        const contentDisplay = item.querySelector('.feedback-content') || item.querySelector('p');
        const editForm = item.querySelector('.edit-feedback-inline');
        const editStars = editForm.querySelectorAll('.star-rating .fa-star');
        const editRateInput = editForm.querySelector('.edit-rate-inline');
        let editRate = editRateInput ? parseInt(editRateInput.value) || 0 : 0;

        if (!contentDisplay || !editForm) {
            console.error(`Không tìm thấy contentDisplay hoặc editForm cho feedback-${feedbackId}`);
            return;
        }

        contentDisplay.style.display = 'none';
        editForm.style.display = 'block';
        if (editStars.length > 0) updateStars(editRate, editStars);

        editStars.forEach(star => {
            star.removeEventListener('click', handleEditStarClick);
            star.removeEventListener('mouseover', handleEditStarMouseover);
            star.removeEventListener('mouseout', handleEditStarMouseout);

            star.addEventListener('click', handleEditStarClick);
            star.addEventListener('mouseover', handleEditStarMouseover);
            star.addEventListener('mouseout', handleEditStarMouseout);
        });

        function handleEditStarClick() {
            editRate = parseInt(this.getAttribute('data-rate'));
            editRateInput.value = editRate;
            updateStars(editRate, editStars);
        }

        function handleEditStarMouseover() {
            updateStars(parseInt(this.getAttribute('data-rate')), editStars);
        }

        function handleEditStarMouseout() {
            updateStars(editRate, editStars);
        }
    }

    // Xử lý nút Cancel
    document.querySelectorAll('.cancel-edit-inline').forEach(button => {
        button.addEventListener('click', function () {
            const item = this.closest('.feedback-item') || this.closest('.reply-item');
            const contentDisplay = item.querySelector('.feedback-content') || item.querySelector('p');
            item.querySelector('.edit-feedback-inline').style.display = 'none';
            contentDisplay.style.display = 'block';
        });
    });

    // Xử lý form chỉnh sửa inline
    document.querySelectorAll('.edit-feedback-inline').forEach(form => {
        form.addEventListener('submit', function (event) {
            const rateInput = this.querySelector('.edit-rate-inline');
            if (rateInput && (!rateInput.value || parseInt(rateInput.value) === 0)) {
                showModal("Vui lòng chọn số sao đánh giá!", false);
                event.preventDefault();
            }
        });
    });

    // Xử lý nút Delete với modal xác nhận
    document.querySelectorAll('.delete-feedback').forEach(button => {
        button.addEventListener('click', function (e) {
            e.stopPropagation();
            const feedbackId = this.getAttribute('data-id');
            const item = this.closest('.feedback-item') || this.closest('.reply-item');
            const examId = item.getAttribute('data-exam-id');

            const confirmModal = new bootstrap.Modal(document.getElementById('confirmDeleteModal'));
            confirmModal.show();

            // Xóa sự kiện cũ trên nút "Xác nhận" để tránh trùng lặp
            const confirmBtn = document.getElementById('confirmDeleteBtn');
            confirmBtn.onclick = null; // Xóa sự kiện trước đó

            // Gắn sự kiện cho nút "Xác nhận"
            confirmBtn.onclick = function () {
                confirmModal.hide();

                const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

                fetch(`/exams/${examId}/feedback/delete/${feedbackId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrfHeader]: csrfToken
                    },
                })
                    .then(response => {
                        if (response.ok) {
                            document.getElementById(`feedback-${feedbackId}`).remove();
                            showModal('Xóa phản hồi thành công!');
                        } else {
                            return response.text().then(text => {
                                throw new Error(`Xóa thất bại: ${text}`);
                            });
                        }
                    })
                    .catch(error => {
                        console.error('Lỗi khi xóa:', error);
                        showModal(`Xóa phản hồi thất bại: ${error.message}`, false);
                    });
            };
        });
    });

    // Xử lý nhấp vào dấu 3 chấm
    function attachMoreOptionsEvents() {
        document.querySelectorAll('.more-options').forEach(option => {
            option.removeEventListener('click', toggleMenu);
            option.addEventListener('click', toggleMenu);
        });
    }

    function toggleMenu(event) {
        event.stopPropagation();
        const menu = this.nextElementSibling;
        if (menu) {
            document.querySelectorAll('.action-menu').forEach(m => m.style.display = 'none');
            menu.style.display = menu.style.display === 'none' || !menu.style.display ? 'block' : 'none';
            attachEditEvents();
        }
    }

    attachMoreOptionsEvents();
    attachEditEvents();

    // Ẩn menu khi nhấp ra ngoài
    document.addEventListener('click', function (e) {
        document.querySelectorAll('.action-menu').forEach(menu => {
            if (!menu.contains(e.target) && !menu.previousElementSibling.contains(e.target)) {
                menu.style.display = 'none';
            }
        });
    });
});
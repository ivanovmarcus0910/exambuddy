document.addEventListener("DOMContentLoaded", function () {
    var userModal = document.getElementById('userModal');

    userModal.addEventListener('show.bs.modal', function (event) {
        var button = event.relatedTarget; // Nút "Xem chi tiết" được click

        // Lấy các data-* từ nút
        var role = button.getAttribute('data-role');           // STUDENT, TEACHER, ADMIN, UPGRADED_USER, PENDING_TEACHER
        var avatarUrl = button.getAttribute('data-avatar');      // URL avatar
        var username = button.getAttribute('data-username');
        var email = button.getAttribute('data-email');
        var phone = button.getAttribute('data-phone');
        var active = button.getAttribute('data-status');         // 'true' hoặc 'false'
        var coin = button.getAttribute('data-coin');
        var joinDate = button.getAttribute('data-join-date');
        var verified = button.getAttribute('data-verified');     // 'true' hoặc 'false'

        var firstname = button.getAttribute('data-firstname');
        var lastname = button.getAttribute('data-lastname');
        var birth = button.getAttribute('data-birth');
        var address = button.getAttribute('data-address');
        var description = button.getAttribute('data-description');
        var school = button.getAttribute('data-school');

        // Dành cho STUDENT & UPGRADED_USER
        var grade = button.getAttribute('data-grade');
        var studentId = button.getAttribute('data-student-id');

        // Dành cho TEACHER & PENDING_TEACHER
        var teacherCode = button.getAttribute('data-teacher-code');
        var speciality = button.getAttribute('data-speciality');
        var experience = button.getAttribute('data-experience');
        var degreeUrl = button.getAttribute('data-degree-url');

        // 1. Đổi tiêu đề modal theo role
        var modalTitle = document.getElementById('userModalTitle');
        if (role === 'Học sinh' || role === 'Tài khoản nâng cấp') {
            modalTitle.textContent = 'Thông tin Học sinh';
        } else if (role === 'Giáo viên' || role === 'Xét duyệt giáo viên') {
            modalTitle.textContent = 'Thông tin Giáo viên';
        } else if (role === 'Quản trị viên') {
            modalTitle.textContent = 'Thông tin Quản trị viên';
        } else {
            modalTitle.textContent = 'Thông tin Người dùng';
        }

        // 2. Gán dữ liệu vào phần chung
        document.getElementById('modal-avatar').src = avatarUrl || '/img/default-avatar.png';
        document.getElementById('modal-username').textContent = username || '';
        document.getElementById('modal-email').textContent = email || '';
        document.getElementById('modal-phone').textContent = phone || '';

        // Sử dụng badge cho trạng thái
        var activeEl = document.getElementById('modal-active');
        if (active === 'true') {
            activeEl.textContent = 'Active';
            activeEl.className = 'badge badge-active'; // CSS: .badge-active { background-color: #28a745; }
        } else {
            activeEl.textContent = 'Inactive';
            activeEl.className = 'badge badge-inactive'; // CSS: .badge-inactive { background-color: #dc3545; }
        }

        document.getElementById('modal-role').textContent = role || '';
        document.getElementById('modal-coin').textContent = coin || '0';
        document.getElementById('modal-join-date').textContent = joinDate || '';

        // Badge cho xác thực
        var verifiedEl = document.getElementById('modal-verified');
        if (verified === 'true') {
            verifiedEl.textContent = 'Đã xác thực';
            verifiedEl.className = 'badge badge-verified'; // CSS: .badge-verified { background-color: #17a2b8; }
        } else {
            verifiedEl.textContent = 'Chưa xác thực';
            verifiedEl.className = 'badge badge-unverified'; // CSS: .badge-unverified { background-color: #ffc107; color: #333; }
        }

        document.getElementById('modal-firstname').textContent = firstname || '';
        document.getElementById('modal-lastname').textContent = lastname || '';
        document.getElementById('modal-birth').textContent = birth || '';
        document.getElementById('modal-address').textContent = address || '';
        document.getElementById('modal-school').textContent = school || '';
        document.getElementById('modal-description').textContent = description || '';

        // 3. Ẩn tất cả các nhóm thông tin riêng theo role
        document.getElementById('group-student').style.display = 'none';
        document.getElementById('group-teacher').style.display = 'none';
        document.getElementById('group-upgraded').style.display = 'none';
        document.getElementById('group-pending').style.display = 'none';
        document.getElementById('group-admin').style.display = 'none';

        // 4. Hiển thị nhóm thông tin riêng theo role và gán dữ liệu
        if (role === 'Học sinh' || role === 'Tài khoản nâng cấp') {
            document.getElementById('group-student').style.display = 'block';
            document.getElementById('modal-grade').textContent = grade || '';
            document.getElementById('modal-student-id').textContent = studentId || '';
        } else if (role === 'Giáo viên' || role === 'Xét duyệt giáo viên') {
            document.getElementById('group-teacher').style.display = 'block';
            document.getElementById('modal-teacher-code').textContent = teacherCode || '';
            document.getElementById('modal-speciality').textContent = speciality || '';
            document.getElementById('modal-experience').textContent = experience || '';
            document.getElementById('modal-degree-url').textContent = degreeUrl || '';
        }
        // Bạn có thể bổ sung xử lý cho group-upgraded, group-pending, group-admin nếu cần.
    });
});

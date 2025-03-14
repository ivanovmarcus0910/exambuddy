let questions = []; // Mảng lưu câu hỏi tạm thời
let editingIndex = -1;
document.addEventListener("DOMContentLoaded", function () {
    loadExamData(examData); // Gọi hàm khi trang được load
});

function loadExamData(exam) {
    if (!exam || !exam.questions) {
        console.error("Dữ liệu đề thi không hợp lệ:", exam);
        return;
    }

    // Gán dữ liệu vào các trường input
    document.getElementById("examName").value = exam.examName || "";
    document.getElementById("grade").value = exam.grade || "";
    document.getElementById("subject").value = exam.subject || "";
    document.getElementById("tags").value = exam.tags ? exam.tags.join(", ") : "";
    document.getElementById("examType").value = exam.examType || "";
    document.getElementById("city").value = exam.city || "";

    // Gán lại danh sách câu hỏi
    questions = exam.questions.map(q => ({
        questionText: q.questionText,
        options: q.options,
        correctAnswers: q.correctAnswers
    }));
    // Hiển thị danh sách câu hỏi lên giao diện
    displayQuestions();
}



function addOptionField() {
    let optionsContainer = document.getElementById("optionsContainer");
    let optionIndex = optionsContainer.children.length;
    let newOptionDiv = document.createElement("div");
    newOptionDiv.classList.add("input-group", "mb-2");

    newOptionDiv.innerHTML = `
        <input type="text" class="form-control" name="option" placeholder="Đáp án ${optionIndex + 1}">
        <div class="input-group-append">
            <div class="input-group-text">
                <input type="checkbox" name="correctOption" value="${optionIndex}" class="form-check-input mt-0">
            </div>
            <button type="button" class="btn btn-danger" onclick="removeOption(this)">🗑️</button>
        </div>
    `;


    optionsContainer.appendChild(newOptionDiv);
}

function removeOption(button) {
    button.parentNode.remove();
}

function addQuestion() {
    let questionText = document.getElementById("questionText").value;
    let options = [];
    let correctAnswers = [];

    document.querySelectorAll("#optionsContainer > .input-group").forEach((div, index) => {
        let optionText = div.querySelector("input[name='option']").value;
        let isChecked = div.querySelector("input[name='correctOption']").checked;

        if (optionText.trim() !== "") {
            options.push(optionText);
            if (isChecked) {
                correctAnswers.push(index);
            }
        }
    });

    if (questionText.trim() === "" || options.length < 2 || correctAnswers.length === 0) {
        alert("Vui lòng nhập câu hỏi, ít nhất 2 đáp án và chọn đáp án đúng!");
        return;
    }

    let question = {questionText, options, correctAnswers};
    questions.push(question);
    displayQuestions();
    resetForm();
}

function resetForm() {
    document.getElementById("questionText").value = "";
    document.getElementById("optionsContainer").innerHTML = "";
    document.getElementById("addBtn").style.display = "inline";
    document.getElementById("saveEditBtn").style.display = "none";
    editingIndex = -1;
}

function displayQuestions() {
    let questionList = document.getElementById("questionList");
    questionList.innerHTML = "";

    questions.forEach((question, index) => {
        let correctAnswers = question.correctAnswers.map(i => question.options[i]).join(", ");
        let li = document.createElement("li");
        li.className = "list-group-item"; // Sử dụng Bootstrap class

        li.innerHTML = `
        <div class="p-2 border rounded shadow-sm">
            <b class="text-primary">${index + 1}. ${question.questionText}</b>
            <br>
            <span class="text-secondary">(${question.options.join(", ")})</span>
            <br>
            <span class="text-success font-weight-bold">✅ Đáp án đúng: ${correctAnswers}</span>
            <div class="mt-2">
                <button class="btn btn-warning btn-sm" onclick="editQuestion(${index})">✏️ Sửa</button>
                <button class="btn btn-danger btn-sm" onclick="deleteQuestion(${index})">❌ Xóa</button>
            </div>
        </div>
    `;

        questionList.appendChild(li);
    });
}

function editQuestion(index) {
    let question = questions[index];
    editingIndex = index;

    document.getElementById("questionText").value = question.questionText;
    let optionsContainer = document.getElementById("optionsContainer");
    optionsContainer.innerHTML = "";
    question.options.forEach((option, i) => {
        let newOptionDiv = document.createElement("div");
        newOptionDiv.innerHTML = `
          <input type="text" name="option" value="${option}">
          <input type="checkbox" name="correctOption" value="${i}" ${question.correctAnswers.includes(i) ? "checked" : ""}>
          <button type="button" onclick="removeOption(this)">🗑️</button>
        `;
        optionsContainer.appendChild(newOptionDiv);
    });

    document.getElementById("addBtn").style.display = "none";
    document.getElementById("saveEditBtn").style.display = "inline";
}

function saveEdit() {
    if (editingIndex === -1) return;

    let correctAnswers = [];
    let options = [];

    document.querySelectorAll("#optionsContainer div").forEach((div, i) => {
        let optionText = div.querySelector("input[name='option']").value;
        let isChecked = div.querySelector("input[name='correctOption']").checked;

        if (optionText.trim() !== "") {
            options.push(optionText);
            if (isChecked) {
                correctAnswers.push(i);
            }
        }
    });

    questions[editingIndex] = {
        questionText: document.getElementById("questionText").value,
        options,
        correctAnswers
    };
    document.getElementById("saveEditBtn").style.display = "none";

    displayQuestions();
    resetForm();
}

function deleteQuestion(index) {
    questions.splice(index, 1);
    console.log(questions);
    displayQuestions();
}

function getCookie(name) {
    let cookies = document.cookie.split(';');
    for (let i = 0; i < cookies.length; i++) {
        let cookie = cookies[i].trim();
        if (cookie.startsWith(name + '=')) {
            return cookie.substring(name.length + 1);
        }
    }
    return null;
}

function submitQuestions() {

    if (!name) {
        alert("Bạn chưa đăng nhập!");
        return;
    }

    if (questions.length === 0) {
        alert("Chưa có câu hỏi nào để gửi!");
        return;
    }

    let examData = {
        examName: document.getElementById("examName").value,
        grade: document.getElementById("grade").value, // Đúng id "grade"
        subject: document.getElementById("subject").value,
        tags: document.getElementById("tags").value.split(","),
        examType: document.getElementById("examType").value, // Đúng id "examType"
        city: document.getElementById("city").value, // Đúng id "city"
        username: name,
        date: new Date().toISOString(),
        questions: questions
    };


    fetch(`/exams/edit/${examId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(examData),
        credentials: 'include' // Để gửi session cookie (quan trọng nếu dùng HttpSession)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(err => { throw new Error(err); });
            }
            return response.text();
        })
        .then(message => {
            alert(message); // Hiển thị thông báo từ server
            questions = [];
            displayQuestions();
        })
        .catch(error => {
            console.error("Lỗi:", error);
            alert(error.message);
        });
}


document.getElementById("grade").addEventListener("change", function () {
    let grade = this.value;
    let examTypeSelect = document.getElementById("examType");


    // Xóa tất cả các tùy chọn hiện có
    examTypeSelect.innerHTML = '<option value="">Chọn dạng đề</option>';


    if (grade === "10" || grade === "11") {
        // Nếu chọn lớp 10 hoặc 11, chỉ hiển thị 2 tùy chọn
        examTypeSelect.innerHTML +=
            '<option value="Đề kiểm tra">Đề kiểm tra</option>' +
            '<option value="Đề luyện tập">Đề luyện tập</option>';
    } else if (grade === "12") {
        // Nếu chọn lớp 12, hiển thị cả 3 tùy chọn
        examTypeSelect.innerHTML +=
            '<option value="Đề kiểm tra">Đề kiểm tra</option>' +
            '<option value="Đề luyện tập">Đề luyện tập</option>' +
            '<option value="Đề THPT">Đề THPT QUỐC GIA</option>';
    }
});

const cities = ["Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ", "An Giang", "Bà Rịa - Vũng Tàu",
    "Bắc Giang", "Bắc Kạn", "Bạc Liêu", "Bắc Ninh", "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước", "Bình Thuận", "Cà Mau",
    "Cao Bằng", "Đắk Lắk", "Đắk Nông", "Điện Biên", "Đồng Nai", "Đồng Tháp", "Gia Lai", "Hà Giang", "Hà Nam", "Hà Tĩnh", "Hải Dương",
    "Hậu Giang", "Hòa Bình", "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum", "Lai Châu", "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An",
    "Nam Định", "Nghệ An", "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên", "Quảng Bình", "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị",
    "Sóc Trăng", "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên", "Thanh Hóa", "Thừa Thiên Huế", "Tiền Giang", "Trà Vinh", "Tuyên Quang", "Vĩnh Long",
    "Vĩnh Phúc", "Yên Bái"];
const citySelect = document.getElementById('city');
cities.forEach(city => {
    const option = document.createElement('option');
    option.value = city; // Giữ nguyên giá trị gốc
    option.textContent = city;
    citySelect.appendChild(option);
});




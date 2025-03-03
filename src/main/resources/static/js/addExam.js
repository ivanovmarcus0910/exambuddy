let questions = []; // Mảng lưu câu hỏi tạm thời
let editingIndex = -1;

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

    displayQuestions();
    resetForm();
}

function deleteQuestion(index) {
    questions.splice(index, 1);
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
    let username = getCookie("noname"); // Lấy username từ cookie
    if (!username) {
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
        username: username,
        date: new Date().toISOString(),
        questions: questions
    };


    fetch("/exams/addExam", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(examData)
    })
        .then(response => response.text())
        .then(data => {
            alert("Đã lưu đề thi thành công!");
            questions = [];
            displayQuestions();
        })
        .catch(error => console.error("Lỗi:", error));
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

function importFromExcel() {
    const fileInput = document.getElementById('excelFile');
    const file = fileInput.files[0];
    if (!file) {
        alert('Vui lòng chọn file Excel!');
        return;
    }

    const reader = new FileReader();
    reader.onload = function(e) {
        const data = new Uint8Array(e.target.result);
        const workbook = XLSX.read(data, { type: 'array' });
        const sheet = workbook.Sheets[workbook.SheetNames[0]];
        const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 });

        console.log('Rows from Excel:', rows); // Debug log

        rows.forEach(row => {
            if (row.length >= 6) {
                const questionText = row[0];
                const rawOptions = [row[1], row[2], row[3], row[4]]; // Giá trị thô từ Excel
                const correctAnswerStr = row[5].toString().toUpperCase();
                const correctAnswers = [];

                // Chuyển đổi các option thành chuỗi
                const options = rawOptions.map(option => String(option));

                correctAnswerStr.split(',').forEach(answer => {
                    switch (answer.trim()) {
                        case 'A': correctAnswers.push(0); break;
                        case 'B': correctAnswers.push(1); break;
                        case 'C': correctAnswers.push(2); break;
                        case 'D': correctAnswers.push(3); break;
                        default: console.warn(`Đáp án không hợp lệ: ${answer}`);
                    }
                });

                if (questionText && options.every(opt => opt) && correctAnswers.length > 0) {
                    questions.push({ questionText, options, correctAnswers });
                    console.log('Added question:', { questionText, options, correctAnswers });
                } else {
                    console.warn('Skipped row due to invalid data:', { questionText, options, correctAnswers });
                }
            } else {
                console.warn('Row skipped, not enough columns:', row);
            }
        });

        displayQuestions();
        fileInput.value = ''; // Reset input
        alert(`Đã import ${questions.length} câu hỏi từ file Excel.`);
    };
    reader.readAsArrayBuffer(file);
}

function importFromDocx() {
    const fileInput = document.getElementById('docxFile');
    const file = fileInput.files[0];
    if (!file) {
        alert('Vui lòng chọn file DOCX!');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('examName', document.getElementById('examName').value);
    formData.append('grade', document.getElementById('grade').value);
    formData.append('subject', document.getElementById('subject').value);
    formData.append('examType', document.getElementById('examType').value);
    formData.append('city', document.getElementById('city').value);
    formData.append('tags', document.getElementById('tags').value);

    fetch('/exams/importDocx', {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            alert(data.message);
            if (data.message.includes('thành công')) {
                // Reset danh sách câu hỏi tạm thời nếu cần
                questions = [];
                displayQuestions();
                fileInput.value = ''; // Reset input file
            }
        })
        .catch(error => {
            console.error('Lỗi:', error);
            alert('Lỗi khi import file DOCX!');
        });
}
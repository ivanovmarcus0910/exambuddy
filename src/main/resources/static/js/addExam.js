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
    const inputGroup = button.closest(".input-group");
    if (inputGroup) {
        inputGroup.remove();
    }
}


function addQuestion() {
    let questionText = document.getElementById("questionText").value;
    let options = [];
    let correctAnswers = [];

    const optionDivs = document.querySelectorAll("#optionsContainer .input-group");

    optionDivs.forEach((div) => {
        const optionInput = div.querySelector("input[name='option']");
        const text = optionInput.value.trim();
        if (text !== "") {
            options.push(text);
        }
    });

    // Duyệt lại để lấy correctAnswers theo thứ tự thực tế
    let visibleIndex = 0;
    optionDivs.forEach((div) => {
        const optionInput = div.querySelector("input[name='option']");
        const checkbox = div.querySelector("input[name='correctOption']");
        const text = optionInput?.value.trim(); // optional chaining để an toàn

        if (text !== "") {
            if (checkbox && checkbox.checked) {
                correctAnswers.push(visibleIndex);
            }
            visibleIndex++;
        }
    });

    if (questionText.trim() === "" || options.length < 2 || correctAnswers.length === 0) {
        alert("Vui lòng nhập câu hỏi, ít nhất 2 đáp án và chọn đáp án đúng!");
        return;
    }

    questions.push({ questionText, options, correctAnswers });
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
        newOptionDiv.classList.add("input-group", "mb-2");
        newOptionDiv.innerHTML = `
            <input type="text" class="form-control" name="option" value="${option}">
            <div class="input-group-append">
                <div class="input-group-text">
                    <input type="checkbox" name="correctOption" value="${i}" class="form-check-input mt-0" ${question.correctAnswers.includes(i) ? "checked" : ""}>
                </div>
                <button type="button" class="btn btn-danger" onclick="removeOption(this)">🗑️</button>
            </div>
        `;
        optionsContainer.appendChild(newOptionDiv);
    });

    document.getElementById("addBtn").style.display = "none";
    document.getElementById("saveEditBtn").style.display = "inline";
}

function saveEdit() {
    if (editingIndex === -1) return;

    let questionText = document.getElementById("questionText").value;
    let options = [];
    let correctAnswers = [];

    const optionDivs = document.querySelectorAll("#optionsContainer .input-group");

    optionDivs.forEach((div) => {
        const input = div.querySelector("input[name='option']");
        const checkbox = div.querySelector("input[name='correctOption']");
        const text = input?.value.trim();

        if (text !== "") {
            options.push(text);
        }
    });

    let visibleIndex = 0;
    optionDivs.forEach((div) => {
        const input = div.querySelector("input[name='option']");
        const checkbox = div.querySelector("input[name='correctOption']");
        const text = input?.value.trim();

        if (text !== "" && checkbox?.checked) {
            correctAnswers.push(visibleIndex);
        }

        if (text !== "") visibleIndex++;
    });

    if (questionText.trim() === "" || options.length < 2 || correctAnswers.length === 0) {
        alert("Vui lòng nhập đầy đủ thông tin câu hỏi và đáp án.");
        return;
    }

    questions[editingIndex] = { questionText, options, correctAnswers };
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
function validateForm() {
    let fields = ["examName", "grade", "subject", "timeduration", "examType", "city"];
    let isValid = true;

    fields.forEach(id => {
        let field = document.getElementById(id);
        if (field.value.trim() === "") {
            field.classList.add("border-danger");
            isValid = false;
        } else {
            field.classList.remove("border-danger");
        }
    });

    if (!isValid) {
        alert("Please complete all required fields before submitting.");
        return false;
    }

    return true;
}
function submitQuestions() {

    if (!validateForm()) {
        return;
    }
    document.getElementById("messageContainer").innerHTML =
        '<div class="alert alert-warning" style="font-size:0.9rem;">Uploading</div>';

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
        questions: questions,
        timeduration: document.getElementById("timeduration").value
    };


    fetch("/exams/addExam", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(examData)
    })
        .then(response => response.text())
        .then(data => {
            document.getElementById("messageContainer").innerHTML =
                '<div class="alert alert-success" style="font-size:0.9rem;">Đã lưu đề thi thành công!</div>';
            questions = [];
            displayQuestions();
        })
        .catch(error => {
            console.error("Lỗi:", error);
            document.getElementById("messageContainer").innerHTML =
                '<div class="alert alert-danger" style="font-size:0.9rem;">Có lỗi xảy ra khi lưu đề thi!</div>';
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

        let importedCount = 0;

        rows.forEach(row => {
            if (row.length < 3) return; // cần ít nhất: câu hỏi + 1 đáp án + đáp án đúng

            const questionText = String(row[0]).trim();
            const correctAnswerStr = String(row[row.length - 1]).toUpperCase().trim();
            const rawOptions = row.slice(1, row.length - 1); // các đáp án (B1 → Bn)

            const options = rawOptions.map(opt => opt ? String(opt).trim() : "");
            const correctAnswers = [];

            correctAnswerStr.split(',').forEach(ans => {
                const index = ans.trim().charCodeAt(0) - 65; // 'A' = 0, 'B' = 1, ...
                if (index >= 0 && index < options.length) {
                    correctAnswers.push(index);
                } else {
                    console.warn(`Đáp án không hợp lệ: ${ans}`);
                }
            });

            if (questionText && options.every(opt => opt) && correctAnswers.length > 0) {
                questions.push({ questionText, options, correctAnswers });
                importedCount++;
            } else {
                console.warn('Bỏ qua dòng không hợp lệ:', { questionText, options, correctAnswers });
            }
        });

        displayQuestions();
        fileInput.value = ''; // Reset input
        alert(`✅ Đã import ${importedCount} câu hỏi từ file Excel.`);
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

    fetch('/exams/importDocx', {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.questions) {
                questions = data.questions;
                displayQuestions();
                alert(data.message);
            } else {
                alert(data.message || "Không có câu hỏi nào được trả về.");
            }
        })
        .catch(error => {
            console.error('Lỗi:', error);
            alert('Lỗi khi import file DOCX!');
        });
}

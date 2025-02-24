
    let questions = []; // Mảng lưu câu hỏi tạm thời
    let editingIndex = -1;

    function addOptionField() {
    let optionsContainer = document.getElementById("optionsContainer");
    let optionIndex = optionsContainer.children.length;
    let newOptionDiv = document.createElement("div");

    newOptionDiv.innerHTML = `
        <input type="text" name="option" placeholder="Đáp án ${optionIndex + 1}">
        <input type="checkbox" name="correctOption" value="${optionIndex}">
        <button type="button" onclick="removeOption(this)">🗑️</button>
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

    document.querySelectorAll("#optionsContainer div").forEach((div, index) => {
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

    let question = { questionText, options, correctAnswers };
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
    li.innerHTML = `
          <b>${index + 1}. ${question.questionText}</b>
          <br>(${question.options.join(", ")})
          <br>✅ Đáp án đúng: <b>${correctAnswers}</b>
          <button onclick="editQuestion(${index})">✏️ Sửa</button>
          <button onclick="deleteQuestion(${index})">❌ Xóa</button>
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
    headers: { "Content-Type": "application/json" },
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





    document.getElementById("grade").addEventListener("change", function() {
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

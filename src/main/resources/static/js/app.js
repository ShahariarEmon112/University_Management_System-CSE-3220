let currentUser = null;

// Initial check on page load
document.addEventListener("DOMContentLoaded", () => {
  checkAuth();
});

function showMessage(msg, isError = false) {
  const box = document.getElementById("feedback-box");
  box.textContent = msg;
  box.className = "status-box " + (isError ? "error" : "success");
  box.style.display = "block";
  setTimeout(() => {
    box.style.display = "none";
  }, 4000);
}

async function checkAuth() {
  try {
    const res = await fetch("/api/auth/me");
    if (res.ok) {
      currentUser = await res.json();
      renderApp();
    } else {
      currentUser = null;
      renderApp();
    }
  } catch (err) {
    currentUser = null;
    renderApp();
  }
}

async function handleLogin(e) {
  e.preventDefault();
  const username = document.getElementById("login-username").value.trim();
  const password = document.getElementById("login-password").value;

  try {
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    if (res.ok) {
      currentUser = await res.json();
      showMessage("Welcome back, " + currentUser.fullName + "!");
      renderApp();
    } else {
      const err = await res.json();
      showMessage(err.message || "Invalid username or password", true);
    }
  } catch (err) {
    showMessage("Connection failed. Please ensure backend is running.", true);
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const username = document.getElementById("reg-username").value.trim();
  const fullName = document.getElementById("reg-fullname").value.trim();
  const email = document.getElementById("reg-email").value.trim();
  const password = document.getElementById("reg-password").value;
  const studentRoll = document.getElementById("reg-roll").value.trim();
  const series = parseInt(document.getElementById("reg-series").value, 10);

  const payload = {
    username,
    fullName,
    email,
    password,
    role: "ROLE_STUDENT",
    studentRoll,
    series,
    department: "Computer Science & Engineering"
  };

  try {
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      showMessage("Registration successful! Please login with your credentials.");
      document.getElementById("register-form").reset();
    } else {
      const err = await res.json();
      showMessage(err.message || "Registration failed.", true);
    }
  } catch (err) {
    showMessage("Failed to connect to registration service.", true);
  }
}

async function logout() {
  await fetch("/api/auth/logout", { method: "POST" });
  currentUser = null;
  showMessage("Logged out successfully.");
  renderApp();
}

function renderApp() {
  const authView = document.getElementById("auth-view");
  const dashView = document.getElementById("dashboard-view");
  const navPanel = document.getElementById("nav-user-panel");
  const navInfo = document.getElementById("nav-user-info");
  const navRole = document.getElementById("nav-user-role");

  if (!currentUser) {
    authView.style.display = "block";
    dashView.style.display = "none";
    navPanel.style.display = "none";
    return;
  }

  authView.style.display = "none";
  dashView.style.display = "block";
  navPanel.style.display = "flex";
  navInfo.textContent = currentUser.fullName;
  navRole.textContent = currentUser.role.replace("ROLE_", "");
  navRole.className = "badge " + (
    currentUser.role === "ROLE_ADMIN" ? "badge-admin" :
    currentUser.role === "ROLE_TEACHER" ? "badge-teacher" : "badge-student"
  );

  document.getElementById("admin-dashboard").style.display = "none";
  document.getElementById("teacher-dashboard").style.display = "none";
  document.getElementById("student-dashboard").style.display = "none";

  if (currentUser.role === "ROLE_ADMIN") {
    document.getElementById("admin-dashboard").style.display = "block";
    loadAdminData();
  } else if (currentUser.role === "ROLE_TEACHER") {
    document.getElementById("teacher-dashboard").style.display = "block";
    loadTeacherData();
  } else if (currentUser.role === "ROLE_STUDENT") {
    document.getElementById("student-dashboard").style.display = "block";
    loadStudentData();
  }
}

/* ================= ADMIN FUNCTIONS ================= */

function switchAdminTab(tab) {
  document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
  document.getElementById("admin-users-tab").style.display = "none";
  document.getElementById("admin-courses-tab").style.display = "none";
  document.getElementById("admin-enrollments-tab").style.display = "none";

  if (tab === "users") {
    event.target.classList.add("active");
    document.getElementById("admin-users-tab").style.display = "block";
    loadAdminUsers();
  } else if (tab === "courses") {
    event.target.classList.add("active");
    document.getElementById("admin-courses-tab").style.display = "block";
    loadAdminCourses();
  } else if (tab === "enrollments") {
    event.target.classList.add("active");
    document.getElementById("admin-enrollments-tab").style.display = "block";
    loadAdminEnrollments();
  }
}

async function loadAdminData() {
  loadAdminUsers();
}

async function loadAdminUsers() {
  const res = await fetch("/api/users");
  if (!res.ok) return;
  const users = await res.json();
  const tbody = document.getElementById("admin-users-body");
  tbody.innerHTML = users.map(u => `
    <tr>
      <td>${u.id}</td>
      <td><strong>${u.username}</strong></td>
      <td>${u.fullName}</td>
      <td>${u.email}</td>
      <td><span class="badge ${u.role === 'ROLE_ADMIN' ? 'badge-admin' : u.role === 'ROLE_TEACHER' ? 'badge-teacher' : 'badge-student'}">${u.role.replace('ROLE_', '')}</span></td>
      <td>${u.enabled ? 'Active' : 'Disabled'}</td>
    </tr>
  `).join("");
}

async function loadAdminCourses() {
  const res = await fetch("/api/courses");
  if (!res.ok) return;
  const courses = await res.json();
  const tbody = document.getElementById("admin-courses-body");
  tbody.innerHTML = courses.map(c => `
    <tr>
      <td><strong>${c.courseCode}</strong></td>
      <td>${c.courseTitle}</td>
      <td>${c.credits}</td>
      <td>${c.teacherName || '<em>Unassigned</em>'}</td>
      <td>
        <button class="btn btn-danger" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="deleteCourse(${c.id})">Delete</button>
      </td>
    </tr>
  `).join("");

  // Also load teacher dropdown for the course form
  loadTeacherDropdown();
}

async function loadTeacherDropdown() {
  try {
    const res = await fetch("/api/teachers");
    if (!res.ok) return;
    const teachers = await res.json();
    const select = document.getElementById("new-course-teacher");
    if (!select) return;
    const currentVal = select.value;
    select.innerHTML = '<option value="">-- Assign Teacher (Optional) --</option>' +
      teachers.map(t => `<option value="${t.id}">${t.fullName} (${t.designation || 'Teacher'})</option>`).join("");
    select.value = currentVal;
  } catch (err) {
    console.error("Failed to load teachers", err);
  }
}

async function handleCreateCourse(e) {
  e.preventDefault();
  const code = document.getElementById("new-course-code").value.trim();
  const title = document.getElementById("new-course-title").value.trim();
  const credits = parseFloat(document.getElementById("new-course-credits").value);
  const teacherIdVal = document.getElementById("new-course-teacher").value;
  const teacherId = teacherIdVal ? parseInt(teacherIdVal, 10) : null;

  const payload = { courseCode: code, courseTitle: title, credits: credits };
  if (teacherId) payload.teacherId = teacherId;

  const res = await fetch("/api/courses", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  if (res.ok) {
    showMessage("Course created successfully!");
    document.getElementById("create-course-form").reset();
    loadAdminCourses();
  } else {
    const err = await res.json();
    showMessage(err.message || "Failed to create course", true);
  }
}

async function deleteCourse(id) {
  if (!confirm("Are you sure you want to delete this course?")) return;
  const res = await fetch("/api/courses/" + id, { method: "DELETE" });
  if (res.ok) {
    showMessage("Course deleted.");
    loadAdminCourses();
  }
}

async function loadAdminEnrollments() {
  const res = await fetch("/api/enrollments");
  if (!res.ok) return;
  const enrollments = await res.json();
  const tbody = document.getElementById("admin-enrollments-body");
  tbody.innerHTML = enrollments.map(e => `
    <tr>
      <td>${e.id}</td>
      <td><strong>${e.studentRoll}</strong></td>
      <td>${e.studentName}</td>
      <td>${e.courseCode}</td>
      <td>${e.courseTitle}</td>
      <td>${e.semester}</td>
      <td>
        <button class="btn btn-danger" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="deleteEnrollment(${e.id})">Delete</button>
      </td>
    </tr>
  `).join("");

  // Populate students and courses dropdowns in enroll form
  loadEnrollmentDropdowns();
}

async function loadEnrollmentDropdowns() {
  try {
    const [studentsRes, coursesRes] = await Promise.all([
      fetch("/api/students"),
      fetch("/api/courses")
    ]);

    if (studentsRes.ok) {
      const students = await studentsRes.json();
      const studentSelect = document.getElementById("enroll-student-id");
      if (studentSelect) {
        studentSelect.innerHTML = '<option value="">-- Select Student --</option>' +
          students.map(s => `<option value="${s.id}">${s.fullName} (${s.studentRoll})</option>`).join("");
      }
    }

    if (coursesRes.ok) {
      const courses = await coursesRes.json();
      const courseSelect = document.getElementById("enroll-course-id");
      if (courseSelect) {
        courseSelect.innerHTML = '<option value="">-- Select Course --</option>' +
          courses.map(c => `<option value="${c.id}">${c.courseCode} - ${c.courseTitle}</option>`).join("");
      }
    }
  } catch (err) {
    console.error("Failed to load enrollment form options", err);
  }
}

async function handleEnrollStudent(e) {
  e.preventDefault();
  const studentId = parseInt(document.getElementById("enroll-student-id").value, 10);
  const courseId = parseInt(document.getElementById("enroll-course-id").value, 10);
  const semester = document.getElementById("enroll-semester").value.trim();

  if (!studentId || !courseId || !semester) {
    showMessage("Please select a student, course, and semester.", true);
    return;
  }

  const res = await fetch("/api/enrollments", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ studentId, courseId, semester })
  });

  if (res.ok) {
    showMessage("Student enrolled successfully!");
    loadAdminEnrollments();
  } else {
    const err = await res.json();
    showMessage(err.message || "Failed to enroll student", true);
  }
}

async function deleteEnrollment(id) {
  if (!confirm("Are you sure you want to delete this enrollment?")) return;
  const res = await fetch("/api/enrollments/" + id, { method: "DELETE" });
  if (res.ok) {
    showMessage("Enrollment deleted.");
    loadAdminEnrollments();
  } else {
    const err = await res.json();
    showMessage(err.message || "Failed to delete enrollment", true);
  }
}

/* ================= TEACHER FUNCTIONS ================= */

async function loadTeacherData() {
  const res = await fetch("/api/courses/my-courses");
  if (!res.ok) return;
  const courses = await res.json();
  const tbody = document.getElementById("teacher-courses-body");
  tbody.innerHTML = courses.map(c => `
    <tr>
      <td><strong>${c.courseCode}</strong></td>
      <td>${c.courseTitle}</td>
      <td>${c.credits}</td>
      <td>
        <button class="btn btn-primary" style="padding: 0.2rem 0.6rem; font-size: 0.8rem;" onclick="openCourseGrading(${c.id}, '${c.courseCode}')">
          View & Grade Students
        </button>
      </td>
    </tr>
  `).join("");
}

async function openCourseGrading(courseId, courseCode) {
  const enrollmentsRes = await fetch("/api/enrollments/course/" + courseId);
  const resultsRes = await fetch("/api/results/course/" + courseId);
  if (!enrollmentsRes.ok) return;

  const enrollments = await enrollmentsRes.json();
  const results = resultsRes.ok ? await resultsRes.json() : [];

  const gradeCard = document.getElementById("teacher-grade-card");
  gradeCard.style.display = "block";
  document.getElementById("teacher-course-title").textContent = "Grade Students for " + courseCode;

  const tbody = document.getElementById("teacher-grade-body");
  tbody.innerHTML = enrollments.map(e => {
    const existingResult = results.find(r => r.enrollmentId === e.id);
    const marks = existingResult ? existingResult.marks : "";
    const letter = existingResult ? existingResult.letterGrade : "-";
    const gpa = existingResult ? existingResult.gradePoint : "-";

    return `
      <tr>
        <td>${e.id}</td>
        <td><strong>${e.studentRoll}</strong></td>
        <td>${e.studentName}</td>
        <td>
          <input type="number" step="0.5" min="0" max="100" id="marks-${e.id}" class="form-control" style="width: 100px;" value="${marks}">
        </td>
        <td><strong>${letter}</strong></td>
        <td>${gpa}</td>
        <td>
          <button class="btn btn-outline" onclick="submitGrade(${e.id})">Save Grade</button>
        </td>
      </tr>
    `;
  }).join("");
}

async function submitGrade(enrollmentId) {
  const marksInput = document.getElementById("marks-" + enrollmentId);
  const marks = parseFloat(marksInput.value);

  if (isNaN(marks) || marks < 0 || marks > 100) {
    alert("Please enter a valid marks value between 0 and 100.");
    return;
  }

  const res = await fetch("/api/results", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      enrollmentId: enrollmentId,
      marks: marks,
      remarks: "Graded via Teacher Portal"
    })
  });

  if (res.ok) {
    showMessage("Grade successfully saved!");
  } else {
    const err = await res.json();
    showMessage(err.message || "Failed to save grade", true);
  }
}

/* ================= STUDENT FUNCTIONS ================= */

async function loadStudentData() {
  // 1. Profile info
  const profileRes = await fetch("/api/students/me");
  if (profileRes.ok) {
    const p = await profileRes.json();
    document.getElementById("student-profile-info").innerHTML = `
      <strong>Full Name:</strong> ${p.fullName} <br>
      <strong>Roll Number:</strong> ${p.studentRoll} <br>
      <strong>Department:</strong> ${p.department} <br>
      <strong>Series:</strong> ${p.series} <br>
      <strong>Email:</strong> ${p.email}
    `;
  }

  // 2. Enrolled courses and results
  const enrollRes = await fetch("/api/enrollments/my-courses");
  const resultsRes = await fetch("/api/results/my-results");

  if (enrollRes.ok) {
    const enrollments = await enrollRes.json();
    const results = resultsRes.ok ? await resultsRes.json() : [];

    const tbody = document.getElementById("student-results-body");
    tbody.innerHTML = enrollments.map(e => {
      const resData = results.find(r => r.courseId === e.courseId || r.enrollmentId === e.id);
      return `
        <tr>
          <td><strong>${e.courseCode}</strong></td>
          <td>${e.courseTitle}</td>
          <td>${e.semester}</td>
          <td>${resData ? resData.marks : 'Pending'}</td>
          <td><strong>${resData ? resData.letterGrade : 'Pending'}</strong></td>
          <td>${resData ? resData.gradePoint : '-'}</td>
          <td>${resData && resData.remarks ? resData.remarks : '-'}</td>
        </tr>
      `;
    }).join("");
  }
}

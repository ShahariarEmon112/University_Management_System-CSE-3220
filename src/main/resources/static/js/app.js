"use strict";

/* =========================================================
   KUET UMS front-end
   ========================================================= */

let currentUser = null;
const adminData = { users: [], students: [], teachers: [], courses: [], enrollments: [], results: [] };
let adminTab = "overview";
const teacherData = { profile: null, courses: [], enrollmentsByCourse: {}, resultsByCourse: {}, openCourseId: null };

const DEPARTMENTS = [
  "Computer Science & Engineering",
  "Electrical & Electronic Engineering",
  "Electronics & Communication Engineering",
  "Biomedical Engineering",
  "Civil Engineering",
  "Mechanical Engineering",
  "Industrial & Production Engineering",
  "Materials Science & Engineering",
  "Chemical Engineering",
  "Urban & Regional Planning",
  "Architecture"
];

// Must match Result.setMarksAndComputeGrade on the server
const GRADE_SCALE = [
  { min: 80, letter: "A+", gp: 4.0 },
  { min: 75, letter: "A", gp: 3.75 },
  { min: 70, letter: "A-", gp: 3.5 },
  { min: 65, letter: "B+", gp: 3.25 },
  { min: 60, letter: "B", gp: 3.0 },
  { min: 55, letter: "B-", gp: 2.75 },
  { min: 50, letter: "C+", gp: 2.5 },
  { min: 45, letter: "C", gp: 2.25 },
  { min: 40, letter: "D", gp: 2.0 },
  { min: 0, letter: "F", gp: 0.0 }
];

/* ---------------- Helpers ---------------- */

const $ = (id) => document.getElementById(id);

function esc(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function initials(name) {
  return (name || "?").split(/\s+/).filter(w => /[A-Za-z]/.test(w[0] || ""))
    .slice(0, 2).map(w => w[0].toUpperCase()).join("") || "?";
}

function gradeFor(marks) {
  return GRADE_SCALE.find(g => marks >= g.min);
}

function gradeClass(letter) {
  if (!letter) return "grade-pending";
  const c = letter[0].toLowerCase();
  return ["a", "b", "c", "d", "f"].includes(c) ? "grade-" + c : "grade-pending";
}

function gradeBadge(letter) {
  return letter
    ? `<span class="grade ${gradeClass(letter)}">${esc(letter)}</span>`
    : `<span class="grade grade-pending">Pending</span>`;
}

function roleBadge(role) {
  const cls = role === "ROLE_ADMIN" ? "badge-admin" : role === "ROLE_TEACHER" ? "badge-teacher" : "badge-student";
  return `<span class="badge ${cls}">${esc((role || "").replace("ROLE_", ""))}</span>`;
}

function fmt(n, digits = 2) {
  return (n === null || n === undefined || isNaN(n)) ? "–" : Number(n).toFixed(digits);
}

function fmtDate(value) {
  if (!value) return "–";
  const d = new Date(value);
  return isNaN(d) ? esc(value) : d.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

function average(nums) {
  return nums.length ? nums.reduce((a, b) => a + b, 0) / nums.length : null;
}

/** Credit-weighted GPA over graded results. */
function computeCgpa(results) {
  let points = 0, credits = 0;
  results.forEach(r => {
    const cr = r.credits || 0;
    points += (r.gradePoint || 0) * cr;
    credits += cr;
  });
  return credits ? points / credits : null;
}

function semesterOptions() {
  const year = new Date().getFullYear();
  return [year - 1, year, year + 1].flatMap(y => [`Spring ${y}`, `Fall ${y}`]);
}

function currentSemester() {
  const now = new Date();
  return (now.getMonth() < 6 ? "Spring " : "Fall ") + now.getFullYear();
}

function emptyRow(cols, text) {
  return `<tr><td colspan="${cols}" class="empty">${esc(text)}</td></tr>`;
}

/**
 * fetch wrapper: parses JSON, throws Error(message) on failure,
 * and sends the user back to the login screen if the session expired.
 */
async function api(path, options = {}) {
  const opts = { ...options, headers: { ...(options.headers || {}) } };
  if (opts.body && typeof opts.body !== "string") {
    opts.body = JSON.stringify(opts.body);
    opts.headers["Content-Type"] = "application/json";
  }

  let res;
  try {
    res = await fetch(path, opts);
  } catch (e) {
    throw new Error("Cannot reach the server. Is the backend running?");
  }

  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch (e) { data = null; }
  }

  if (!res.ok) {
    if (res.status === 401 && currentUser && !path.startsWith("/api/auth/")) {
      currentUser = null;
      renderApp();
      toast("Your session has expired. Please sign in again.", true);
    }
    const msg = (data && (data.message || data.error)) || `Request failed (${res.status})`;
    const err = new Error(msg);
    err.status = res.status;
    throw err;
  }
  return data;
}

function toast(msg, isError = false) {
  const el = document.createElement("div");
  el.className = "toast" + (isError ? " error" : "");
  el.textContent = msg;
  $("toast-stack").appendChild(el);
  setTimeout(() => el.remove(), isError ? 6000 : 3500);
}

/* ---------------- Modal ---------------- */

let modalResolver = null;

function openModal({ title, html, okText = "OK", danger = false, onOk = null }) {
  $("modal-title").textContent = title;
  $("modal-body").innerHTML = html;
  const ok = $("modal-ok");
  ok.textContent = okText;
  ok.className = "btn " + (danger ? "btn-danger" : "btn-primary");
  ok.disabled = false;
  $("modal").classList.remove("hidden");
  const firstInput = $("modal-body").querySelector("input, select, textarea");
  (firstInput || ok).focus();

  return new Promise(resolve => {
    modalResolver = async (confirmed) => {
      if (confirmed && onOk) {
        ok.disabled = true;
        try {
          const keepOpen = await onOk();
          if (keepOpen === false) { ok.disabled = false; return; }
        } catch (e) {
          toast(e.message, true);
          ok.disabled = false;
          return;
        }
      }
      $("modal").classList.add("hidden");
      modalResolver = null;
      resolve(confirmed);
    };
  });
}

function confirmDanger(title, html, okText = "Delete") {
  return openModal({ title, html, okText, danger: true });
}

/* ---------------- Auth ---------------- */

async function checkAuth() {
  try {
    currentUser = await api("/api/auth/me");
  } catch (e) {
    currentUser = null;
  }
  renderApp();
}

function switchAuthTab(tab) {
  document.querySelectorAll("[data-auth-tab]").forEach(b => b.classList.toggle("active", b.dataset.authTab === tab));
  $("login-form").classList.toggle("hidden", tab !== "login");
  $("register-form").classList.toggle("hidden", tab !== "register");
}

async function handleLogin(e) {
  e.preventDefault();
  const btn = e.submitter || e.target.querySelector("button[type=submit]");
  btn.disabled = true;
  try {
    currentUser = await api("/api/auth/login", {
      method: "POST",
      body: { username: $("login-username").value.trim(), password: $("login-password").value }
    });
    $("login-form").reset();
    toast(`Welcome back, ${currentUser.fullName}!`);
    renderApp();
  } catch (err) {
    toast(err.message || "Invalid username or password", true);
  } finally {
    btn.disabled = false;
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const payload = {
    username: $("reg-username").value.trim(),
    fullName: $("reg-fullname").value.trim(),
    email: $("reg-email").value.trim(),
    password: $("reg-password").value,
    role: "ROLE_STUDENT",
    studentRoll: $("reg-roll").value.trim(),
    series: parseInt($("reg-series").value, 10),
    department: $("reg-department").value,
    phoneNumber: $("reg-phone").value.trim() || null
  };
  try {
    await api("/api/auth/register", { method: "POST", body: payload });
    toast("Account created! You can sign in now.");
    $("register-form").reset();
    $("login-username").value = payload.username;
    switchAuthTab("login");
    $("login-password").focus();
  } catch (err) {
    toast(err.message || "Registration failed.", true);
  }
}

async function logout() {
  try { await api("/api/auth/logout", { method: "POST" }); } catch (e) { /* session may already be gone */ }
  currentUser = null;
  toast("You have been logged out.");
  renderApp();
}

/* ---------------- App shell ---------------- */

function renderApp() {
  const loggedIn = !!currentUser;
  $("auth-view").classList.toggle("hidden", loggedIn);
  $("dashboard-view").classList.toggle("hidden", !loggedIn);
  $("nav-user-panel").classList.toggle("hidden", !loggedIn);
  ["admin-dashboard", "teacher-dashboard", "student-dashboard"].forEach(id => $(id).classList.add("hidden"));
  if (!loggedIn) return;

  $("nav-user-info").textContent = currentUser.fullName;
  $("nav-avatar").textContent = initials(currentUser.fullName);
  const role = currentUser.role;
  $("nav-user-role").textContent = role.replace("ROLE_", "");
  $("nav-user-role").className = "badge " + (role === "ROLE_ADMIN" ? "badge-admin" : role === "ROLE_TEACHER" ? "badge-teacher" : "badge-student");

  const hour = new Date().getHours();
  const part = hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening";
  $("greeting").textContent = `Good ${part}, ${currentUser.fullName.split(" ")[0]}`;
  $("today-line").textContent = new Date().toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long", year: "numeric" })
    + " · " + currentSemester();

  if (role === "ROLE_ADMIN") {
    $("admin-dashboard").classList.remove("hidden");
    switchAdminTab("overview");
    loadAdminData();
  } else if (role === "ROLE_TEACHER") {
    $("teacher-dashboard").classList.remove("hidden");
    loadTeacherData();
  } else {
    $("student-dashboard").classList.remove("hidden");
    loadStudentData();
  }
}

/* =========================================================
   ADMIN
   ========================================================= */

function switchAdminTab(tab) {
  adminTab = tab;
  document.querySelectorAll("#admin-tabs .tab-btn").forEach(b => b.classList.toggle("active", b.dataset.tab === tab));
  ["overview", "users", "courses", "enrollments", "results"].forEach(t => {
    $(`admin-${t}-tab`).classList.toggle("hidden", t !== tab);
  });
}

async function loadAdminData() {
  try {
    const [users, students, teachers, courses, enrollments, results] = await Promise.all([
      api("/api/users"), api("/api/students"), api("/api/teachers"),
      api("/api/courses"), api("/api/enrollments"), api("/api/results")
    ]);
    Object.assign(adminData, { users, students, teachers, courses, enrollments, results });
  } catch (e) {
    toast("Failed to load admin data: " + e.message, true);
    return;
  }
  renderAdminAll();
}

function renderAdminAll() {
  renderAdminStats();
  renderAdminOverview();
  renderAdminUsers();
  renderAdminCourses();
  renderAdminEnrollments();
  renderAdminResults();
  fillAdminSelects();
}

function statCard(label, value, sub, color) {
  return `<div class="stat" style="--stat-color:${color}">
    <div class="stat-label">${esc(label)}</div>
    <div class="stat-value">${esc(value)}</div>
    ${sub ? `<div class="stat-sub">${esc(sub)}</div>` : ""}
  </div>`;
}

function renderAdminStats() {
  const d = adminData;
  const avgGp = average(d.results.map(r => r.gradePoint));
  const passRate = d.results.length ? Math.round(100 * d.results.filter(r => r.letterGrade !== "F").length / d.results.length) : null;
  $("admin-stats").innerHTML = [
    statCard("Users", d.users.length, `${d.users.filter(u => u.role === "ROLE_ADMIN").length} admin(s)`, "#4f46e5"),
    statCard("Students", d.students.length, `${new Set(d.students.map(s => s.series)).size} series`, "#16a34a"),
    statCard("Teachers", d.teachers.length, `${d.courses.filter(c => !c.teacherId).length} unassigned course(s)`, "#0ea5e9"),
    statCard("Courses", d.courses.length, `${fmt(d.courses.reduce((a, c) => a + (c.credits || 0), 0), 1)} total credits`, "#f59e0b"),
    statCard("Enrollments", d.enrollments.length, `${d.enrollments.length - d.results.length} awaiting grade`, "#8b5cf6"),
    statCard("Average GP", fmt(avgGp), passRate === null ? "No results yet" : `${passRate}% pass rate`, "#ec4899")
  ].join("");
}

function barList(rows) {
  const max = Math.max(1, ...rows.map(r => r.value));
  return rows.map(r => `
    <div class="bar-row">
      <span>${r.label}</span>
      <div class="bar-track"><div class="bar-fill" style="width:${(100 * r.value / max).toFixed(1)}%"></div></div>
      <span class="bar-value">${esc(r.value)}</span>
    </div>`).join("") || `<p class="muted small">No data yet.</p>`;
}

function renderAdminOverview() {
  const counts = {};
  adminData.results.forEach(r => { counts[r.letterGrade] = (counts[r.letterGrade] || 0) + 1; });
  $("admin-grade-dist").innerHTML = adminData.results.length
    ? barList(GRADE_SCALE.map(g => ({ label: gradeBadge(g.letter), value: counts[g.letter] || 0 })))
    : `<p class="muted small">No results have been published yet.</p>`;

  const load = adminData.courses.map(c => ({
    label: `<strong>${esc(c.courseCode)}</strong>`,
    value: adminData.enrollments.filter(e => e.courseId === c.id).length
  })).sort((a, b) => b.value - a.value);
  $("admin-course-load").innerHTML = barList(load);
}

/* ----- Users ----- */

function renderAdminUsers() {
  const q = $("user-search").value.trim().toLowerCase();
  const role = $("user-role-filter").value;
  const studentsByUser = Object.fromEntries(adminData.students.map(s => [s.userId, s]));
  const teachersByUser = Object.fromEntries(adminData.teachers.map(t => [t.userId, t]));

  const rows = adminData.users
    .filter(u => !role || u.role === role)
    .filter(u => !q || [u.username, u.fullName, u.email].some(v => (v || "").toLowerCase().includes(q)))
    .map(u => {
      const s = studentsByUser[u.id], t = teachersByUser[u.id];
      const details = s ? `Roll <strong>${esc(s.studentRoll)}</strong> · Series ${esc(s.series)}`
        : t ? `${esc(t.designation)}` : `<span class="muted">—</span>`;
      const isSelf = u.username === currentUser.username;
      return `<tr>
        <td><div style="display:flex;gap:.6rem;align-items:center">
          <div class="avatar" style="width:32px;height:32px;font-size:.75rem">${esc(initials(u.fullName))}</div>
          <div><div class="cell-main">${esc(u.fullName)}</div><div class="cell-sub">@${esc(u.username)}</div></div>
        </div></td>
        <td>${esc(u.email)}</td>
        <td>${roleBadge(u.role)}</td>
        <td class="small">${details}</td>
        <td>${u.enabled ? '<span class="badge badge-ok">Active</span>' : '<span class="badge badge-muted">Disabled</span>'}</td>
        <td class="right">${isSelf
          ? '<span class="cell-sub">You</span>'
          : `<button class="btn btn-sm btn-icon-danger" data-action="delete-user" data-id="${u.id}">Delete</button>`}</td>
      </tr>`;
    });
  $("admin-users-body").innerHTML = rows.join("") || emptyRow(6, "No users match your search.");
}

async function deleteUser(id) {
  const u = adminData.users.find(x => x.id === id);
  if (!u) return;
  const s = adminData.students.find(x => x.userId === id);
  const t = adminData.teachers.find(x => x.userId === id);
  let impact = "";
  if (s) {
    const enr = adminData.enrollments.filter(e => e.studentId === s.id).length;
    const res = adminData.results.filter(r => r.studentId === s.id).length;
    impact = `<ul><li>Student profile (roll ${esc(s.studentRoll)})</li><li>${enr} enrollment(s)</li><li>${res} result(s)</li></ul>`;
  } else if (t) {
    const cs = adminData.courses.filter(c => c.teacherId === t.id).length;
    impact = `<ul><li>Teacher profile</li><li>${cs} course(s) will become unassigned (courses are kept)</li></ul>`;
  }
  const ok = await confirmDanger("Delete user?",
    `<p>This permanently deletes <strong>${esc(u.fullName)}</strong> (@${esc(u.username)})${impact ? " and also removes:" : "."}</p>${impact}`);
  if (!ok) return;
  try {
    await api("/api/users/" + id, { method: "DELETE" });
    toast("User deleted.");
    loadAdminData();
  } catch (e) {
    toast(e.message, true);
  }
}

function toggleNewUserRoleFields() {
  const role = $("nu-role").value;
  document.querySelectorAll("#create-user-form .role-student").forEach(el => el.classList.toggle("hidden", role !== "ROLE_STUDENT"));
  document.querySelectorAll("#create-user-form .role-teacher").forEach(el => el.classList.toggle("hidden", role !== "ROLE_TEACHER"));
}

async function handleCreateUser(e) {
  e.preventDefault();
  const role = $("nu-role").value;
  const payload = {
    role,
    username: $("nu-username").value.trim(),
    fullName: $("nu-fullname").value.trim(),
    email: $("nu-email").value.trim(),
    password: $("nu-password").value,
    department: $("nu-department").value,
    phoneNumber: $("nu-phone").value.trim() || null
  };
  if (role === "ROLE_STUDENT") {
    payload.studentRoll = $("nu-roll").value.trim() || null;
    payload.series = parseInt($("nu-series").value, 10) || null;
  } else if (role === "ROLE_TEACHER") {
    payload.designation = $("nu-designation").value;
  }
  try {
    await api("/api/auth/register", { method: "POST", body: payload });
    toast(`${role.replace("ROLE_", "").toLowerCase()} account created.`);
    $("create-user-form").reset();
    toggleNewUserRoleFields();
    loadAdminData();
  } catch (err) {
    toast(err.message, true);
  }
}

/* ----- Courses ----- */

function teacherOptions(selectedId) {
  return `<option value="">— Unassigned —</option>` + adminData.teachers.map(t =>
    `<option value="${t.id}" ${t.id === selectedId ? "selected" : ""}>${esc(t.fullName)} (${esc(t.designation)})</option>`).join("");
}

function fillAdminSelects() {
  document.querySelectorAll(".teacher-select").forEach(sel => {
    const v = sel.value;
    sel.innerHTML = teacherOptions(null);
    sel.value = v;
  });

  const sSel = $("enroll-student-id"), cSel = $("enroll-course-id");
  const sv = sSel.value, cv = cSel.value;
  sSel.innerHTML = `<option value="">Select student…</option>` + adminData.students
    .slice().sort((a, b) => (a.studentRoll || "").localeCompare(b.studentRoll || ""))
    .map(s => `<option value="${s.id}">${esc(s.studentRoll)} — ${esc(s.fullName)}</option>`).join("");
  cSel.innerHTML = `<option value="">Select course…</option>` + adminData.courses
    .map(c => `<option value="${c.id}">${esc(c.courseCode)} — ${esc(c.courseTitle)}</option>`).join("");
  sSel.value = sv;
  cSel.value = cv;
}

function renderAdminCourses() {
  const rows = adminData.courses.map(c => {
    const enr = adminData.enrollments.filter(e => e.courseId === c.id).length;
    const avg = average(adminData.results.filter(r => r.courseId === c.id).map(r => r.marks));
    return `<tr>
      <td class="cell-main">${esc(c.courseCode)}</td>
      <td><div class="cell-main">${esc(c.courseTitle)}</div>${c.description ? `<div class="cell-sub">${esc(c.description)}</div>` : ""}</td>
      <td>${fmt(c.credits, 2)}</td>
      <td>${c.teacherName ? esc(c.teacherName) : '<span class="badge badge-muted">Unassigned</span>'}</td>
      <td>${enr}</td>
      <td>${avg === null ? "–" : fmt(avg, 1)}</td>
      <td><div class="actions">
        <button class="btn btn-sm btn-ghost" data-action="edit-course" data-id="${c.id}">Edit</button>
        <button class="btn btn-sm btn-icon-danger" data-action="delete-course" data-id="${c.id}">Delete</button>
      </div></td>
    </tr>`;
  });
  $("admin-courses-body").innerHTML = rows.join("") || emptyRow(7, "No courses yet. Add one above.");
}

async function handleCreateCourse(e) {
  e.preventDefault();
  const teacherId = $("new-course-teacher").value;
  const payload = {
    courseCode: $("new-course-code").value.trim(),
    courseTitle: $("new-course-title").value.trim(),
    credits: parseFloat($("new-course-credits").value),
    description: $("new-course-description").value.trim() || null,
    teacherId: teacherId ? Number(teacherId) : null
  };
  try {
    await api("/api/courses", { method: "POST", body: payload });
    toast("Course created.");
    $("create-course-form").reset();
    loadAdminData();
  } catch (err) {
    toast(err.message, true);
  }
}

async function editCourse(id) {
  const c = adminData.courses.find(x => x.id === id);
  if (!c) return;
  await openModal({
    title: `Edit ${c.courseCode}`,
    okText: "Save changes",
    html: `
      <div class="form-row">
        <div class="form-group"><label>Code</label><input id="ec-code" class="form-control" value="${esc(c.courseCode)}"></div>
        <div class="form-group"><label>Credits</label><input id="ec-credits" type="number" step="0.25" min="0.5" class="form-control" value="${esc(c.credits)}"></div>
      </div>
      <div class="form-group"><label>Title</label><input id="ec-title" class="form-control" value="${esc(c.courseTitle)}"></div>
      <div class="form-group"><label>Teacher</label><select id="ec-teacher" class="form-control">${teacherOptions(c.teacherId)}</select></div>
      <div class="form-group"><label>Description</label><input id="ec-desc" class="form-control" maxlength="500" value="${esc(c.description)}"></div>`,
    onOk: async () => {
      const teacherId = $("ec-teacher").value;
      await api("/api/courses/" + id, {
        method: "PUT",
        body: {
          courseCode: $("ec-code").value.trim(),
          courseTitle: $("ec-title").value.trim(),
          credits: parseFloat($("ec-credits").value),
          description: $("ec-desc").value.trim(),
          teacherId: teacherId ? Number(teacherId) : null
        }
      });
      toast("Course updated.");
      loadAdminData();
    }
  });
}

async function deleteCourse(id) {
  const c = adminData.courses.find(x => x.id === id);
  if (!c) return;
  const enr = adminData.enrollments.filter(e => e.courseId === id).length;
  const res = adminData.results.filter(r => r.courseId === id).length;
  const ok = await confirmDanger("Delete course?",
    `<p>This permanently deletes <strong>${esc(c.courseCode)} — ${esc(c.courseTitle)}</strong>.</p>
     ${enr ? `<p>It will also remove:</p><ul><li>${enr} enrollment(s)</li><li>${res} result(s)</li></ul>` : ""}`);
  if (!ok) return;
  try {
    await api("/api/courses/" + id, { method: "DELETE" });
    toast("Course deleted.");
    loadAdminData();
  } catch (e) {
    toast(e.message, true);
  }
}

/* ----- Enrollments ----- */

function renderAdminEnrollments() {
  const q = $("enroll-search").value.trim().toLowerCase();
  const resultByEnrollment = Object.fromEntries(adminData.results.map(r => [r.enrollmentId, r]));
  const rows = adminData.enrollments
    .filter(e => !q || [e.studentName, e.studentRoll, e.courseCode, e.courseTitle, e.semester]
      .some(v => (v || "").toLowerCase().includes(q)))
    .map(e => {
      const r = resultByEnrollment[e.id];
      return `<tr>
        <td class="cell-sub">#${e.id}</td>
        <td><div class="cell-main">${esc(e.studentName)}</div><div class="cell-sub">${esc(e.studentRoll)}</div></td>
        <td><div class="cell-main">${esc(e.courseCode)}</div><div class="cell-sub">${esc(e.courseTitle)}</div></td>
        <td>${esc(e.semester)}</td>
        <td>${fmtDate(e.enrollmentDate)}</td>
        <td>${gradeBadge(r && r.letterGrade)}</td>
        <td><div class="actions">
          <button class="btn btn-sm btn-ghost" data-action="grade-enrollment" data-id="${e.id}">${r ? "Regrade" : "Grade"}</button>
          <button class="btn btn-sm btn-icon-danger" data-action="delete-enrollment" data-id="${e.id}">Delete</button>
        </div></td>
      </tr>`;
    });
  $("admin-enrollments-body").innerHTML = rows.join("") || emptyRow(7, "No enrollments found.");
}

async function handleEnrollStudent(e) {
  e.preventDefault();
  const studentId = Number($("enroll-student-id").value);
  const courseId = Number($("enroll-course-id").value);
  const semester = $("enroll-semester").value.trim();
  if (!studentId || !courseId || !semester) {
    toast("Please select a student, a course and a semester.", true);
    return;
  }
  try {
    await api("/api/enrollments", { method: "POST", body: { studentId, courseId, semester } });
    toast("Student enrolled.");
    $("enroll-student-id").value = "";
    loadAdminData();
  } catch (err) {
    toast(err.message, true);
  }
}

async function deleteEnrollment(id) {
  const en = adminData.enrollments.find(x => x.id === id);
  if (!en) return;
  const graded = adminData.results.some(r => r.enrollmentId === id);
  const ok = await confirmDanger("Delete enrollment?",
    `<p>Remove <strong>${esc(en.studentName)}</strong> from <strong>${esc(en.courseCode)}</strong> (${esc(en.semester)})?</p>
     ${graded ? "<p>This enrollment has a published result, which will also be deleted.</p>" : ""}`);
  if (!ok) return;
  try {
    await api("/api/enrollments/" + id, { method: "DELETE" });
    toast("Enrollment deleted.");
    loadAdminData();
  } catch (e) {
    toast(e.message, true);
  }
}

/* ----- Results ----- */

function renderAdminResults() {
  const q = $("result-search").value.trim().toLowerCase();
  const rows = adminData.results
    .filter(r => !q || [r.studentName, r.studentRoll, r.courseCode, r.courseTitle]
      .some(v => (v || "").toLowerCase().includes(q)))
    .map(r => `<tr>
      <td><div class="cell-main">${esc(r.studentName)}</div><div class="cell-sub">${esc(r.studentRoll)}</div></td>
      <td><div class="cell-main">${esc(r.courseCode)}</div><div class="cell-sub">${esc(r.courseTitle)}</div></td>
      <td>${esc(r.semester || "–")}</td>
      <td>${fmt(r.marks, 1)}</td>
      <td>${gradeBadge(r.letterGrade)}</td>
      <td>${fmt(r.gradePoint)}</td>
      <td class="small">${esc(r.remarks || "–")}</td>
      <td class="cell-sub">${fmtDate(r.gradedAt)}</td>
      <td><div class="actions">
        <button class="btn btn-sm btn-ghost" data-action="grade-enrollment" data-id="${r.enrollmentId}">Edit</button>
        <button class="btn btn-sm btn-icon-danger" data-action="delete-result" data-id="${r.id}">Delete</button>
      </div></td>
    </tr>`);
  $("admin-results-body").innerHTML = rows.join("") || emptyRow(9, "No results found.");
}

async function gradeEnrollmentModal(enrollmentId, label, existing, onSaved) {
  await openModal({
    title: `Grade — ${label}`,
    okText: "Save grade",
    html: `
      <div class="form-row">
        <div class="form-group"><label>Marks (0–100)</label>
          <input id="gm-marks" type="number" step="0.5" min="0" max="100" class="form-control" value="${existing ? esc(existing.marks) : ""}"></div>
        <div class="form-group"><label>Grade preview</label><div id="gm-preview" style="padding-top:.4rem">${gradeBadge(existing && existing.letterGrade)}</div></div>
      </div>
      <div class="form-group"><label>Remarks</label><input id="gm-remarks" class="form-control" maxlength="255" value="${existing ? esc(existing.remarks) : ""}"></div>`,
    onOk: async () => {
      const marks = parseFloat($("gm-marks").value);
      if (isNaN(marks) || marks < 0 || marks > 100) {
        toast("Marks must be a number between 0 and 100.", true);
        return false;
      }
      await api("/api/results", { method: "POST", body: { enrollmentId, marks, remarks: $("gm-remarks").value.trim() || null } });
      toast("Grade saved.");
      onSaved();
    }
  });
}

function adminGradeEnrollment(enrollmentId) {
  const en = adminData.enrollments.find(e => e.id === enrollmentId);
  if (!en) return;
  const existing = adminData.results.find(r => r.enrollmentId === enrollmentId);
  gradeEnrollmentModal(enrollmentId, `${en.studentName} · ${en.courseCode}`, existing, loadAdminData);
}

async function deleteResult(id) {
  const r = adminData.results.find(x => x.id === id);
  if (!r) return;
  const ok = await confirmDanger("Delete result?",
    `<p>Remove the ${esc(r.letterGrade)} grade for <strong>${esc(r.studentName)}</strong> in <strong>${esc(r.courseCode)}</strong>? The enrollment stays.</p>`);
  if (!ok) return;
  try {
    await api("/api/results/" + id, { method: "DELETE" });
    toast("Result deleted.");
    loadAdminData();
  } catch (e) {
    toast(e.message, true);
  }
}

/* =========================================================
   TEACHER
   ========================================================= */

async function loadTeacherData() {
  try {
    const [profile, courses] = await Promise.all([api("/api/teachers/me"), api("/api/courses/my-courses")]);
    teacherData.profile = profile;
    teacherData.courses = courses;
    const perCourse = await Promise.all(courses.map(c => Promise.all([
      api("/api/enrollments/course/" + c.id),
      api("/api/results/course/" + c.id)
    ])));
    courses.forEach((c, i) => {
      teacherData.enrollmentsByCourse[c.id] = perCourse[i][0];
      teacherData.resultsByCourse[c.id] = perCourse[i][1];
    });
  } catch (e) {
    toast("Failed to load teacher data: " + e.message, true);
    return;
  }
  renderTeacher();
  if (teacherData.openCourseId && teacherData.courses.some(c => c.id === teacherData.openCourseId)) {
    renderGradingTable(teacherData.openCourseId);
  }
}

function renderTeacher() {
  const p = teacherData.profile;
  $("teacher-profile").innerHTML = `
    <div class="avatar lg">${esc(initials(p.fullName))}</div>
    <div class="profile-main">
      <h2>${esc(p.fullName)}</h2>
      <div class="profile-meta">
        <span><strong>${esc(p.designation)}</strong></span>
        <span>${esc(p.department)}</span>
        <span>✉ ${esc(p.email)}</span>
        <span>☎ ${esc(p.phoneNumber || "Not set")}</span>
      </div>
    </div>
    <button class="btn btn-ghost" data-action="edit-teacher-profile">Edit contact</button>`;

  const allEnr = Object.values(teacherData.enrollmentsByCourse).flat();
  const allRes = Object.values(teacherData.resultsByCourse).flat();
  const students = new Set(allEnr.map(e => e.studentId)).size;
  $("teacher-stats").innerHTML = [
    statCard("Courses", teacherData.courses.length, `${fmt(teacherData.courses.reduce((a, c) => a + c.credits, 0), 1)} credits`, "#4f46e5"),
    statCard("Students", students, `${allEnr.length} enrollment(s)`, "#16a34a"),
    statCard("Graded", `${allRes.length}/${allEnr.length}`, `${allEnr.length - allRes.length} pending`, "#f59e0b"),
    statCard("Average marks", allRes.length ? fmt(average(allRes.map(r => r.marks)), 1) : "–", "Across all your courses", "#ec4899")
  ].join("");

  $("teacher-courses").innerHTML = teacherData.courses.map(c => {
    const enr = teacherData.enrollmentsByCourse[c.id] || [];
    const res = teacherData.resultsByCourse[c.id] || [];
    const avg = average(res.map(r => r.marks));
    return `<div class="course-card">
      <div class="course-code">${esc(c.courseCode)} · ${fmt(c.credits, 2)} credits</div>
      <div class="course-title">${esc(c.courseTitle)}</div>
      <div class="course-desc">${esc(c.description || "No description provided.")}</div>
      <div class="course-meta">
        <span><strong>${enr.length}</strong> enrolled</span>
        <span><strong>${res.length}</strong> graded</span>
        <span>Avg <strong>${avg === null ? "–" : fmt(avg, 1)}</strong></span>
      </div>
      <button class="btn btn-primary btn-sm" data-action="open-grading" data-id="${c.id}">Grade students</button>
    </div>`;
  }).join("") || `<p class="muted">You have no assigned courses yet. Ask the administrator to assign one.</p>`;
}

function renderGradingTable(courseId) {
  const course = teacherData.courses.find(c => c.id === courseId);
  if (!course) return;
  teacherData.openCourseId = courseId;
  const enrollments = teacherData.enrollmentsByCourse[courseId] || [];
  const results = teacherData.resultsByCourse[courseId] || [];

  $("teacher-grade-card").classList.remove("hidden");
  $("teacher-course-title").textContent = `${course.courseCode} — ${course.courseTitle}`;
  $("teacher-grade-body").innerHTML = enrollments.map(e => {
    const r = results.find(x => x.enrollmentId === e.id);
    return `<tr>
      <td class="cell-main">${esc(e.studentRoll)}</td>
      <td>${esc(e.studentName)}</td>
      <td>${esc(e.semester)}</td>
      <td><input type="number" step="0.5" min="0" max="100" class="form-control sm" style="width:90px"
            id="marks-${e.id}" data-preview="preview-${e.id}" value="${r ? esc(r.marks) : ""}"></td>
      <td id="preview-${e.id}">${gradeBadge(r && r.letterGrade)}</td>
      <td><input class="form-control sm" maxlength="255" id="remarks-${e.id}" placeholder="Optional" value="${r ? esc(r.remarks) : ""}"></td>
      <td class="right"><button class="btn btn-sm btn-primary" data-action="save-grade" data-id="${e.id}">Save</button></td>
    </tr>`;
  }).join("") || emptyRow(7, "No students are enrolled in this course yet.");
  $("teacher-grade-card").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function submitGrade(enrollmentId, btn) {
  const marks = parseFloat($("marks-" + enrollmentId).value);
  if (isNaN(marks) || marks < 0 || marks > 100) {
    toast("Marks must be a number between 0 and 100.", true);
    return;
  }
  btn.disabled = true;
  try {
    await api("/api/results", {
      method: "POST",
      body: { enrollmentId, marks, remarks: $("remarks-" + enrollmentId).value.trim() || null }
    });
    toast("Grade saved.");
    await loadTeacherData();
  } catch (e) {
    toast(e.message, true);
    btn.disabled = false;
  }
}

async function editTeacherProfile() {
  const p = teacherData.profile;
  await openModal({
    title: "Edit contact details",
    okText: "Save",
    html: `<div class="form-group"><label>Full name</label><input id="tp-name" class="form-control" value="${esc(p.fullName)}"></div>
           <div class="form-group"><label>Phone</label><input id="tp-phone" class="form-control" value="${esc(p.phoneNumber)}"></div>`,
    onOk: async () => {
      await api("/api/teachers/" + p.id, {
        method: "PUT",
        body: { fullName: $("tp-name").value.trim(), phoneNumber: $("tp-phone").value.trim(), department: p.department, designation: p.designation }
      });
      toast("Profile updated.");
      loadTeacherData();
    }
  });
}

/* =========================================================
   STUDENT
   ========================================================= */

let studentProfile = null;

async function loadStudentData() {
  let profile, enrollments, results;
  try {
    [profile, enrollments, results] = await Promise.all([
      api("/api/students/me"), api("/api/enrollments/my-courses"), api("/api/results/my-results")
    ]);
  } catch (e) {
    toast("Failed to load your data: " + e.message, true);
    return;
  }
  studentProfile = profile;

  const cgpa = computeCgpa(results);
  const earned = results.filter(r => r.letterGrade !== "F").reduce((a, r) => a + (r.credits || 0), 0);
  const attempted = enrollments.reduce((a, e) => a + (e.credits || 0), 0);
  const pending = enrollments.length - results.length;
  const best = results.slice().sort((a, b) => b.marks - a.marks)[0];

  $("student-profile-info").innerHTML = `
    <div class="avatar lg">${esc(initials(profile.fullName))}</div>
    <div class="profile-main">
      <h2>${esc(profile.fullName)}</h2>
      <div class="profile-meta">
        <span>Roll <strong>${esc(profile.studentRoll)}</strong></span>
        <span>Series <strong>${esc(profile.series)}</strong></span>
        <span>${esc(profile.department)}</span>
        <span>✉ ${esc(profile.email)}</span>
        <span>☎ ${esc(profile.phoneNumber || "Not set")}</span>
      </div>
    </div>
    <button class="btn btn-ghost" data-action="edit-student-profile">Edit contact</button>`;

  $("student-stats").innerHTML = [
    statCard("CGPA", cgpa === null ? "–" : fmt(cgpa), results.length ? `From ${results.length} graded course(s)` : "No results yet", "#4f46e5"),
    statCard("Credits earned", fmt(earned, 2), `of ${fmt(attempted, 2)} enrolled`, "#16a34a"),
    statCard("Courses", enrollments.length, `${new Set(enrollments.map(e => e.semester)).size} semester(s)`, "#0ea5e9"),
    statCard("Pending results", pending, best ? `Best: ${best.courseCode} (${best.letterGrade})` : "", "#f59e0b")
  ].join("");

  const resultByEnrollment = Object.fromEntries(results.map(r => [r.enrollmentId, r]));
  $("student-results-body").innerHTML = enrollments.map(e => {
    const r = resultByEnrollment[e.id];
    return `<tr>
      <td><div class="cell-main">${esc(e.courseCode)}</div><div class="cell-sub">${esc(e.courseTitle)}</div></td>
      <td>${fmt(e.credits, 2)}</td>
      <td class="small">${esc(e.teacherName || "TBA")}</td>
      <td>${esc(e.semester)}</td>
      <td>${r ? fmt(r.marks, 1) : "–"}</td>
      <td>${gradeBadge(r && r.letterGrade)}</td>
      <td>${r ? fmt(r.gradePoint) : "–"}</td>
      <td class="small">${esc((r && r.remarks) || "–")}</td>
    </tr>`;
  }).join("") || emptyRow(8, "You are not enrolled in any courses yet.");
}

async function editStudentProfile() {
  const p = studentProfile;
  await openModal({
    title: "Edit contact details",
    okText: "Save",
    html: `<div class="form-group"><label>Full name</label><input id="sp-name" class="form-control" value="${esc(p.fullName)}"></div>
           <div class="form-group"><label>Phone</label><input id="sp-phone" class="form-control" value="${esc(p.phoneNumber)}"></div>
           <p class="muted small">Roll, series and department can only be changed by the administrator.</p>`,
    onOk: async () => {
      await api("/api/students/" + p.id, {
        method: "PUT",
        body: { fullName: $("sp-name").value.trim(), phoneNumber: $("sp-phone").value.trim(),
                studentRoll: p.studentRoll, department: p.department, series: p.series }
      });
      toast("Profile updated.");
      loadStudentData();
    }
  });
}

function renderGradeScale() {
  $("grade-scale").innerHTML = GRADE_SCALE.map((g, i) => {
    const upper = i === 0 ? 100 : GRADE_SCALE[i - 1].min - 0.01;
    return `<div class="scale-item">${gradeBadge(g.letter)}<div><strong>${fmt(g.gp)}</strong> GP</div>
      <div class="muted">${g.min}–${i === 0 ? 100 : Math.floor(upper)}${i === 0 ? "" : ".99"}</div></div>`;
  }).join("");
}

/* =========================================================
   Wiring
   ========================================================= */

document.addEventListener("DOMContentLoaded", () => {
  // Static selects
  document.querySelectorAll(".dept-select").forEach(sel => {
    sel.innerHTML = DEPARTMENTS.map(d => `<option>${esc(d)}</option>`).join("");
  });
  $("semester-options").innerHTML = semesterOptions().map(s => `<option value="${s}">`).join("");
  $("enroll-semester").value = currentSemester();
  renderGradeScale();

  // Forms
  $("login-form").addEventListener("submit", handleLogin);
  $("register-form").addEventListener("submit", handleRegister);
  $("create-user-form").addEventListener("submit", handleCreateUser);
  $("create-course-form").addEventListener("submit", handleCreateCourse);
  $("enroll-student-form").addEventListener("submit", handleEnrollStudent);
  $("nu-role").addEventListener("change", toggleNewUserRoleFields);
  $("logout-btn").addEventListener("click", logout);
  $("close-grading").addEventListener("click", () => {
    teacherData.openCourseId = null;
    $("teacher-grade-card").classList.add("hidden");
  });

  // Filters
  $("user-search").addEventListener("input", renderAdminUsers);
  $("user-role-filter").addEventListener("change", renderAdminUsers);
  $("enroll-search").addEventListener("input", renderAdminEnrollments);
  $("result-search").addEventListener("input", renderAdminResults);

  // Live grade preview in the teacher grading table and grade modal
  document.addEventListener("input", (e) => {
    const t = e.target;
    let previewEl = null;
    if (t.dataset && t.dataset.preview) previewEl = $(t.dataset.preview);
    else if (t.id === "gm-marks") previewEl = $("gm-preview");
    if (!previewEl) return;
    const m = parseFloat(t.value);
    previewEl.innerHTML = (isNaN(m) || m < 0 || m > 100) ? gradeBadge(null) : gradeBadge(gradeFor(m).letter);
  });

  // Modal
  $("modal-ok").addEventListener("click", () => modalResolver && modalResolver(true));
  document.querySelectorAll("#modal [data-close]").forEach(el =>
    el.addEventListener("click", () => modalResolver && modalResolver(false)));
  document.addEventListener("keydown", (e) => {
    if ($("modal").classList.contains("hidden")) return;
    if (e.key === "Escape") modalResolver && modalResolver(false);
    if (e.key === "Enter" && e.target.tagName === "INPUT") { e.preventDefault(); modalResolver && modalResolver(true); }
  });

  // Delegated clicks
  document.addEventListener("click", (e) => {
    const authTab = e.target.closest("[data-auth-tab]");
    if (authTab) return switchAuthTab(authTab.dataset.authTab);

    const demo = e.target.closest("[data-demo]");
    if (demo) {
      const [u, p] = demo.dataset.demo.split(":");
      $("login-username").value = u;
      $("login-password").value = p;
      return;
    }

    const tab = e.target.closest("#admin-tabs .tab-btn");
    if (tab) return switchAdminTab(tab.dataset.tab);

    const btn = e.target.closest("[data-action]");
    if (!btn) return;
    const id = Number(btn.dataset.id);
    switch (btn.dataset.action) {
      case "delete-user": return deleteUser(id);
      case "edit-course": return editCourse(id);
      case "delete-course": return deleteCourse(id);
      case "delete-enrollment": return deleteEnrollment(id);
      case "grade-enrollment": return adminGradeEnrollment(id);
      case "delete-result": return deleteResult(id);
      case "open-grading": return renderGradingTable(id);
      case "save-grade": return submitGrade(id, btn);
      case "edit-teacher-profile": return editTeacherProfile();
      case "edit-student-profile": return editStudentProfile();
    }
  });

  checkAuth();
});

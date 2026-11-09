# University Management System — CSE 3220 Lab Assignment

A clean, beginner-friendly, and secure **University Management System (UMS)** built with **Java 21**, **Spring Boot 3.3.4**, **Spring Security**, **Spring Data JPA / Hibernate**, and **MySQL 8**, fully containerized using **Docker & Docker Compose**.

Developed for **Khulna University of Engineering & Technology (KUET)** — Department of Computer Science & Engineering.

---

## 1. Architecture Overview

The system uses a classical, layered Spring Boot architecture:

```
[ Web Browser / Postman Client ]
               │ (HTTP / JSON / Session Cookies)
               ▼
┌──────────────────────────────────────────────┐
│  Spring Security Filter Chain                │
│  - BCryptPasswordEncoder                     │
│  - Session Management (JSESSIONID)           │
│  - Role-Based Access Control (@PreAuthorize) │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│  REST Controllers (Presentation Layer)        │
│  - AuthController, UserController            │
│  - StudentController, TeacherController      │
│  - CourseController, EnrollmentController    │
│  - ResultController                          │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│  Service Layer (Business Logic & Privacy)    │
│  - Enforces student data isolation           │
│  - Grades calculation (Marks -> Letter & GPA)│
│  - Input validation & entity mapping         │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│  Spring Data JPA Repositories (Data Access)  │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│  MySQL 8 Database Container (ums_db)         │
│  (Persisted via Docker Named Volume)         │
└──────────────────────────────────────────────┘
```

---

## 2. User Roles & Permission Matrix

The application strictly implements 3 distinct roles:

| Feature / Endpoint | ADMIN | TEACHER | STUDENT | Public (Anonymous) |
|---|:---:|:---:|:---:|:---:|
| User Registration | ✔ (any role) | ❌ | ❌ | ✔ (Student only) |
| User Login / Logout | ✔ | ✔ | ✔ | ✔ |
| View System Users (`/api/users`) | ✔ | ❌ | ❌ | ❌ |
| Create & Delete Courses | ✔ | ❌ | ❌ | ❌ |
| View Course Catalog | ✔ | ✔ | ✔ | ❌ |
| View My Assigned Courses | ❌ | ✔ | ❌ | ❌ |
| Enroll Student into Course | ✔ | ❌ | ❌ | ❌ |
| View Course Enrollment Rosters | ✔ | ✔ (Assigned course only) | ❌ | ❌ |
| Submit/Update Student Grades | ✔ | ✔ (Assigned course only) | ❌ | ❌ |
| View Own Profile & Grades | ✔ | ✔ | ✔ (Own data only) | ❌ |
| Web Portal Dashboard | Admin Dashboard | Teacher Portal | Student Portal | Login / Register |

> **Security Note:** In addition to URL pattern matching, fine-grained service-level checks guarantee that students cannot view other students' private grades or profiles (returns `403 Forbidden`).

---

## 3. Demo Credentials (Development Only)

All accounts are pre-seeded at database startup via `DataInitializer.java` using BCrypt password hashing. Re-running the application will not duplicate records.

| Role | Username | Password | Notes |
|---|---|---|---|
| **ADMIN** | `admin` | `admin123` | System Administrator |
| **TEACHER** | `dr_karim` | `teacher123` | Assigned to CSE 3220 & CSE 3200 |
| **TEACHER** | `prof_rahim` | `teacher123` | Assigned to CSE 3211 |
| **TEACHER** | `dr_nusrat` | `teacher123` | Assigned to CSE 3205 |
| **STUDENT** | `student_sakib` | `student123` | Roll: 2007001 |
| **STUDENT** | `student_tamim` | `student123` | Roll: 2007002 |
| **STUDENT** | `student_mushfiq` | `student123` | Roll: 2007003 |
| **STUDENT** | `student_mahmudullah` | `student123` | Roll: 2007004 |

---

## 4. Running the Application with Docker Compose (Recommended)

### Prerequisites:
- Docker Engine & Docker Compose installed.

### Step 1: Start Services
In the project root directory, run:
```bash
docker compose up -d --build
```
This will:
1. Spin up the **MySQL 8.0** container with persistent volume `mysql_data`.
2. Wait for MySQL to pass its internal health checks (`mysqladmin ping`).
3. Build and launch the **Spring Boot 3.3.4** application container on port `8080`.

### Step 2: Access the Application
- **Web Interface:** Open your browser at [http://localhost:8080](http://localhost:8080)
- **REST API Base URL:** `http://localhost:8080/api`

### Step 3: Check Logs
```bash
# View application logs
docker compose logs -f app

# View MySQL logs
docker compose logs -f mysql-db
```

### Step 4: Stop Services
```bash
# Stop containers while preserving database volume
docker compose down

# To reset database and remove volume:
docker compose down -v
```

---

## 5. Running Locally without Docker (Development Mode)

If you have a local MySQL instance or want to run tests with in-memory H2:

```bash
# Run tests (runs with H2 database automatically)
mvn clean test

# Package the application jar
mvn clean package -DskipTests

# Run the Spring Boot application (ensure MySQL is running on localhost:3306)
mvn spring-boot:run
```

---

## 6. REST API Reference

### Authentication Endpoints
- `POST /api/auth/login` — Sign in with username and password. Sets HTTP session cookie (`JSESSIONID`).
- `POST /api/auth/register` — Register a new account. Anonymous callers can only create `ROLE_STUDENT`; a logged-in admin can create any role.
- `GET /api/auth/me` — Return currently logged-in user profile without password.
- `POST /api/auth/logout` — Invalidate session and clear cookies.

### Courses Endpoints
- `GET /api/courses` — List all courses (Authenticated users).
- `GET /api/courses/my-courses` — List courses assigned to logged-in teacher (`ROLE_TEACHER`).
- `POST /api/courses` — Add new course (`ROLE_ADMIN`).
- `PUT /api/courses/{id}` — Update course (`ROLE_ADMIN`).
- `DELETE /api/courses/{id}` — Delete course and its enrollments/results (`ROLE_ADMIN`).

### Enrollments Endpoints
- `GET /api/enrollments` — List all enrollments (`ROLE_ADMIN`).
- `GET /api/enrollments/my-courses` — List courses enrolled by logged-in student (`ROLE_STUDENT`).
- `GET /api/enrollments/course/{courseId}` — List students enrolled in course (`ROLE_ADMIN`, `ROLE_TEACHER`).
- `POST /api/enrollments` — Enroll student (`ROLE_ADMIN`).
- `DELETE /api/enrollments/{id}` — Remove an enrollment and its result, if any (`ROLE_ADMIN`).

### Users, Students & Teachers (deletion)
- `DELETE /api/users/{id}` — Delete a user. A student's enrollments and results are removed with them; a teacher's courses are kept but become unassigned. Admins cannot delete themselves (`ROLE_ADMIN`).
- `DELETE /api/students/{id}` / `DELETE /api/teachers/{id}` — Same behaviour, by profile id (`ROLE_ADMIN`).

### Results & Grading Endpoints
- `GET /api/results` — All results (`ROLE_ADMIN`).
- `DELETE /api/results/{id}` — Delete a result (`ROLE_ADMIN`).
- `GET /api/results/my-results` — View logged-in student's results (`ROLE_STUDENT`).
- `GET /api/results/course/{courseId}` — View grades for a course (`ROLE_ADMIN`, or the `ROLE_TEACHER` assigned to it).
- `POST /api/results` — Assign or update marks and remarks (`ROLE_ADMIN`, `ROLE_TEACHER`). Automatic calculation of Letter Grade and GPA:
  - $\ge 80 \implies A+$ (4.00)
  - $\ge 75 \implies A$ (3.75)
  - $\ge 70 \implies A-$ (3.50)
  - $\ge 65 \implies B+$ (3.25)
  - $\ge 60 \implies B$ (3.00)
  - $\ge 55 \implies B-$ (2.75)
  - $\ge 50 \implies C+$ (2.50)
  - $\ge 45 \implies C$ (2.25)
  - $\ge 40 \implies D$ (2.00)
  - $< 40 \implies F$ (0.00)

---

## 7. Testing & Continuous Integration

![CI](https://github.com/ShahariarEmon112/University_Management_System-CSE-3220/actions/workflows/ci.yml/badge.svg)

Tests are grouped with JUnit 5 `@Tag`s and each group runs as its own job in GitHub Actions (`.github/workflows/ci.yml`).

| Group | Tag | File | What it proves |
|---|---|---|---|
| **Unit (6)** | `unit` | `unit/ServiceUnitTest.java` | Single classes in isolation with Mockito mocks: grade boundaries, sign-up role rule, password hashing, duplicate course code, duplicate enrollment, teacher grading permission. |
| **Integration (3)** | `integration` | `integration/ApiIntegrationTest.java` | Whole app over HTTP with real login sessions: login/logout, admin creates course & enrolls student, teacher grades and student sees the grade. |
| **Database (5)** | `database` | `database/RepositoryDatabaseTest.java` | Real SQL with `@DataJpaTest`: save/find, UNIQUE username, UNIQUE enrollment, join queries, FOREIGN KEY on graded enrollments. Runs on **MySQL 8** in CI. |

Run a group locally:

```bash
mvn test -Dgroups=unit
mvn test -Dgroups=integration
mvn test -Dgroups=database     # H2 locally; set SPRING_DATASOURCE_URL to use MySQL
mvn test                       # everything
```

### Fail → pass demonstration
Unit test **U5** was written first (test-driven): it expects enrolling a student twice in
"Fall 2026" and "  fall   2026 " to be rejected. The commit
`test: reject duplicate enrollment when semester casing differs` makes CI fail ❌, and the
next commit `fix: normalize semester names before duplicate check` makes it pass ✅.
Both runs are visible in the repository's **Actions** tab.

---

## 8. Lab Viva Guide (Key Questions & Explanations)

### Q1: Why do we use BCrypt instead of MD5 or SHA-256 for passwords?
> **Answer:** BCrypt includes a salt automatically and is an adaptive, computationally slow key-derivation function. As hardware gets faster, the work factor (cost) can be increased to make brute-force and rainbow table attacks infeasible. MD5 and SHA-256 are fast general cryptographic hash functions, making them susceptible to rapid GPU-based dictionary attacks.

### Q2: How does Spring Security handle authentication and session authorization?
> **Answer:**
> 1. When a user submits credentials to `/api/auth/login`, Spring's `AuthenticationManager` verifies the username and password using `CustomUserDetailsService` and `BCryptPasswordEncoder`.
> 2. On success, an `Authentication` token is stored in the `SecurityContext`.
> 3. The `SecurityContext` is attached to the HTTP Session (`JSESSIONID`).
> 4. Subsequent requests pass the `JSESSIONID` cookie. Spring Security's filter chain restores the context and checks `@PreAuthorize` or request matchers for roles.

### Q3: How do you prevent students from seeing each other's grades?
> **Answer:** We enforce data isolation at the service layer in `StudentService.java` and `ResultService.java`. When a non-admin, non-teacher user requests a record, we extract the authenticated username from `SecurityContextHolder.getContext().getAuthentication().getName()` and ensure it matches the owner of that student record. If not, a `403 Forbidden` (`UnauthorizedException`) is thrown.

### Q4: Why can't the app container connect to `localhost:3306` inside Docker Compose?
> **Answer:** In Docker, each container has its own isolated network namespace and its own `localhost` (127.0.0.1 loopback). In `compose.yaml`, containers share a user-defined bridge network (`ums-network`). Docker's embedded DNS resolves the service name `mysql-db` to the MySQL container's IP. Hence, the JDBC URL is `jdbc:mysql://mysql-db:3306/ums_db`.

### Q5: How do we avoid creating duplicate admin accounts on every container restart?
> **Answer:** In `DataInitializer.java`, we execute a conditional check:
> ```java
> if (userRepository.existsByUsername("admin")) {
>     log.info("Database already initialized with admin user. Skipping.");
>     return;
> }
> ```
> This makes the initialization idempotent and safe across restarts.

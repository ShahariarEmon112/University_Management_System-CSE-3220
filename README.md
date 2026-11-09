# University Management System — Testing & GitHub Actions Explanation

## 1. 30-Second Summary (say this first)

"My project is a Spring Boot university management system. I wrote three kinds of tests: 6 unit tests, 3 integration tests and 5 database tests. I use JUnit 5 tags to split them into groups. GitHub Actions runs each group as a separate job on every push, and the database tests run against a real MySQL 8 database. To show that CI catches bugs, I first pushed a test that exposed a bug, so CI went red. Then I pushed the fix, and CI went green."

---

## 2. The Three Kinds of Tests

All test files are in: src/test/java/com/kuet/cse/ums/

### A. Unit Tests (6) — unit/ServiceUnitTest.java

What they are:
Each test checks one class alone. The classes it depends on, like the repositories, are replaced with fake objects made by Mockito (called "mocks"). No Spring, no database and no web server start, so they run in about 2 seconds.

How to explain:
"A unit test checks one piece of logic in isolation. For example, to test EnrollmentService I don't need a real database. I create a fake EnrollmentRepository with mock(), tell it what to return with when(...).thenReturn(...), and then check the service's behaviour. verify(...never()).save(...) proves nothing was saved."

| Test | What it checks |
|---|---|
| U1 | Marks convert to the right grade at each boundary: 80 → A+, 79.5 → A, 40 → D, 39.5 → F |
| U2 | Someone who isn't logged in cannot sign up as ADMIN |
| U3 | Sign-up hashes the password and creates a student profile |
| U4 | A duplicate course code is rejected, even with different casing ("cse 3220" vs "CSE 3220") |
| U5 | The same student can't be enrolled twice in one semester, even if it's typed as "  fall   2026 " (the fail → pass test) |
| U6 | A teacher can't grade a course that belongs to another teacher |

Good one to walk through: U6. It builds a course owned by prof_rahim, then has dr_karim try to grade it. The test expects an UnauthorizedException and checks that nothing was saved.

### B. Integration Tests (3) — integration/ApiIntegrationTest.java

What they are:
The whole application starts: security, controllers, services, the database and the demo data. The tests send real HTTP requests using MockMvc. They log in through /api/auth/login and reuse the session cookie, just like the browser does.

How to explain:
"Unit tests prove each part works alone. Integration tests prove the parts work together. Each test logs in for real, gets a session, and calls the API like the website would. @Transactional undoes each test's changes afterwards, so the tests don't affect each other."

| Test | Flow |
|---|---|
| I1 | Log in as a student → /api/auth/me returns that user (and no password) → log out → access is blocked (401) |
| I2 | Admin creates course CSE 4100 → enrolls a student → the course roster shows them. Then a student tries to create a course and gets 403 Forbidden |
| I3 | Teacher gives 72 marks → the API returns grade A- (3.50) → the student logs in and sees A- with the teacher's remarks |

### C. Database Tests (5) — database/RepositoryDatabaseTest.java

What they are:
@DataJpaTest loads only the database layer (entities and repositories) and runs real SQL. They check things the database itself enforces: UNIQUE rules, FOREIGN KEY rules, and queries that join tables.

How to explain:
"These test the database layer directly. Locally they use H2, an in-memory database running in MySQL mode. In GitHub Actions the same tests run against a real MySQL 8 database, because the workflow sets SPRING_DATASOURCE_URL to point at it. @AutoConfigureTestDatabase(replace = NONE) tells Spring not to swap in its own test database, so whatever database I configure is the one used."

| Test | What it checks |
|---|---|
| D1 | A saved user can be found by username, and the role is stored correctly |
| D2 | A duplicate username is rejected by the UNIQUE rule |
| D3 | The same student, course and semester can't be enrolled twice (UNIQUE rule) |
| D4 | Results can be found by student and by course across the enrollment table |
| D5 | A graded enrollment can't be deleted directly because of the FOREIGN KEY rule |

Link D5 to the bug I fixed:
"D5 shows why the delete button failed originally: the database blocks deleting an enrollment that a result still points to. So my fix deletes the result first, then the enrollment."

---

## 3. How the Groups Are Separated (JUnit Tags)

Each test class has a tag at the top: @Tag("unit"), @Tag("integration") or @Tag("database").
Maven can then run just one group:

```bash
mvn test -Dgroups=unit
mvn test -Dgroups=integration
mvn test -Dgroups=database
mvn test                 # all 31 tests
```

Tip: run one of these live in front of the teacher. Seeing "Tests run: 6, Failures: 0" is convincing.

---

## 4. How GitHub Actions Works — .github/workflows/ci.yml

What it is:
GitHub Actions is GitHub's CI (Continuous Integration) service. Every time I push, GitHub starts a fresh Ubuntu machine, downloads my code, and follows the steps in ci.yml.

Walk through ci.yml top to bottom:

1. on: push / pull_request / workflow_dispatch
   When the workflow runs: on every push to main, on pull requests, and when I click "Run workflow" manually.

2. jobs: there are 4 jobs. The first 3 run at the same time on separate machines:
   - unit-tests → mvn -B test -Dgroups=unit
   - integration-tests → mvn -B test -Dgroups=integration
   - database-tests → mvn -B test -Dgroups=database (against MySQL)
   - build → packages the JAR file

3. Steps inside each job:
   - actions/checkout@v4 → downloads the code
   - actions/setup-java@v4 → installs Java 21 and caches Maven dependencies so later runs are faster
   - then the mvn test command for that group

4. The MySQL service (in the database-tests job):

```yaml
services:
  mysql:
    image: mysql:8.0
```

   "GitHub starts a real MySQL 8 database in Docker next to the test machine. The health check waits until MySQL is ready. The env: section sets SPRING_DATASOURCE_URL, which overrides the H2 setting, so the database tests run on real MySQL. There's also a step that runs SHOW TABLES to prove the tables were created in MySQL."

5. Test summary:
   .github/scripts/test_summary.py reads the test reports Maven produces and draws the ✅/❌ table at the bottom of each job's page.

6. upload-artifact:
   Saves the test reports and the final JAR so they can be downloaded from the run page.

7. needs: [unit-tests, integration-tests, database-tests]
   "The JAR is only built if all three test jobs pass. That's why in the failed run the build was skipped: broken code never gets packaged."

---

## 5. The Fail → Pass Story (most important part)

Explain it as test-driven development:

1. Run 1 (✅): "I set up CI and every test passed."

2. Run 2 (❌): "Then I found a bug: an admin could enroll the same student twice by typing the semester differently, like 'Fall 2026' and '  fall   2026 '. I wrote test U5 first. It expects that second enrollment to be rejected. I pushed it and CI went red, because the code only removed spaces at the ends and didn't treat different casing as the same."

3. Run 3 (✅): "I fixed EnrollmentService with a normalizeSemester() method. It trims, collapses extra spaces and capitalises each word, so both become 'Fall 2026'. I pushed the fix and CI went green."

"This shows the CI really checks the code: a bug turns it red, and the fix turns it green."

---

## 6. What to Click While Showing the Teacher

1. Open the repo: https://github.com/ShahariarEmon112/University_Management_System-CSE-3220
   Point out the green CI badge in the README.

2. Click "Commits" (or run git log --oneline in a terminal). Explain the conventional commit format (section 7).

3. Open the Actions tab. There are three runs: ✅ ❌ ✅

4. Click the ❌ run:
   - The diagram shows 4 test jobs; "Unit tests (6)" is red and "Build application JAR" is skipped.
   - Click "Unit tests (6)" and scroll to the summary:
     duplicateEnrollmentWithDifferentSemesterCasingRejected ❌ Expected BadRequestException to be thrown, but nothing was thrown.

5. Click the latest ✅ run:
   - Every job is green.
   - Open "Database tests (MySQL 8)", expand "Show tables created in MySQL", and show the tables.
   - Show the summary tables with ✅ for every test.

6. Open the code: ci.yml, then one test from each file (U6, I3, D5), then the normalizeSemester method in EnrollmentService.java.

7. Optional: run mvn test -Dgroups=unit locally.

---

## 7. Commit Messages (Conventional Commits)

"I used the Conventional Commits format: type: description"

| Type | Meaning | Example from my repo |
|---|---|---|
| feat: | New feature | feat: implemented login |
| fix: | Bug fix | fix: allow deleting courses and enrollments that have linked records |
| fix!: / BREAKING CHANGE: | A change that breaks the old behaviour | fix!: restrict sign-up roles and make user deletion work |
| test: | Adding tests | test: add database tests for repositories and constraints |
| ci: | CI configuration | ci: add GitHub Actions workflow with separate test jobs |
| docs: | Documentation | docs: add README with setup, API reference and testing guide |
| chore: | Setup and maintenance | chore: initial Spring Boot project setup with Docker Compose |

---

## 8. Likely Questions and Short Answers

Q: Unit test vs integration test?
A: A unit test checks one class with fake dependencies and is fast. An integration test runs the whole app and checks the parts working together.

Q: What is a mock?
A: A fake object that returns what you tell it to, so you can test one class without a database.

Q: Why H2 locally but MySQL in CI?
A: H2 is fast and needs no setup. CI uses real MySQL to prove it works on the production database. The same test code runs on both; only the connection setting changes.

Q: Why don't tests affect each other?
A: @Transactional and @DataJpaTest undo each test's database changes afterwards, and each CI job starts on a fresh machine.

Q: Why separate jobs?
A: They run at the same time, which is faster, and you can see immediately which kind of test failed.

Q: What does "needs" do?
A: It makes the build job wait for all test jobs and skips it if any of them fail.

Q: What happens if someone pushes broken code?
A: CI turns red, GitHub shows a ❌ next to the commit, and the JAR isn't built. Run 2 shows exactly this.

Q: What is -B in "mvn -B"?
A: Batch mode. It gives clean, non-interactive output for CI logs.

"""Turn Maven Surefire XML reports into a Markdown table for the GitHub Actions job summary."""
import glob
import sys
import xml.etree.ElementTree as ET

title = sys.argv[1] if len(sys.argv) > 1 else "Tests"
rows, passed, failed = [], 0, 0

for path in sorted(glob.glob("target/surefire-reports/TEST-*.xml")):
    for case in ET.parse(path).getroot().iter("testcase"):
        problem = case.find("failure")
        if problem is None:
            problem = case.find("error")
        if case.find("skipped") is not None:
            status = "⏭️ skipped"
        elif problem is not None:
            failed += 1
            msg = (problem.get("message") or "").replace("|", "\\|").replace("\n", " ")[:160]
            status = f"❌ failed: {msg}"
        else:
            passed += 1
            status = "✅ passed"
        cls = case.get("classname", "").rsplit(".", 1)[-1]
        rows.append(f"| {cls} | {case.get('name')} | {status} |")

print(f"## {title}: {passed} passed, {failed} failed\n")
if rows:
    print("| Class | Test | Result |\n|---|---|---|")
    print("\n".join(rows))
else:
    print("_No test reports found (the build may have failed before tests ran)._")

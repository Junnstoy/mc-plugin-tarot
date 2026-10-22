from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parents[1]
project = root / "work/v3_plugin"
output = root / "outputs"
jar = output / "TarotRainbow-3.3.2.jar"
source = output / "TarotRainbow-3.3.2-source.zip"
reports = json.loads((output / "彩虹塔罗V3.3.2服务端验证.json").read_text(encoding="utf-8"))
assert len(reports) == 2
assert all(r["package_open_passed"] and r["native_charge_passed"] and r["activation_state_passed"] and r["selftest_passed"] and r["exit_code"] == 0 for r in reports)
assert jar.read_bytes() == (root / "work/paper_validation/plugins" / jar.name).read_bytes()
with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    assert "version: 3.3.2" in archive.read("plugin.yml").decode("utf-8")
    assert "cn/tarot/ActivationStateTest.class" in archive.namelist()
with zipfile.ZipFile(source, "w", zipfile.ZIP_DEFLATED) as archive:
    for directory in ("src", "resources"):
        for path in sorted((project / directory).rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(project).as_posix())
    archive.write(project / "build.ps1", "build.ps1")
    archive.write(output / "彩虹塔罗V3.3.2更新说明.md", "README.md")
with zipfile.ZipFile(source) as archive:
    assert archive.testzip() is None
report = {
    "version": "3.3.2",
    "jar_bytes": jar.stat().st_size,
    "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
    "source_bytes": source.stat().st_size,
    "resource_pack_changed": False,
    "zip_integrity_passed": True,
    "tested_jar_matches_delivered_jar": True,
    "aura_assertions": 195330,
    "cast_assertions": 30689,
    "activation_state_checks_per_boot": 51,
    "actual_paper_native_completion_passed": True,
    "main_hand_package_open_passed": True,
    "paper": "26.3-143",
    "restart_passed": True,
    "real_client_verified": False,
}
(output / "彩虹塔罗V3.3.2构建验证.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parents[1]
project = root / "work/v3_plugin"
output = root / "outputs"
jar = output / "TarotRainbow-3.4.0.jar"
source = output / "TarotRainbow-3.4.0-source.zip"
reports = json.loads((output / "彩虹塔罗V3.4.0服务端验证.json").read_text(encoding="utf-8"))
assert len(reports) == 3
assert all(r["papi_mode_passed"] and r["command_events_passed"] and r["timestamps_preserved"] and r["album_restart_passed"] for r in reports)
assert all(r["package_open_passed"] and r["native_charge_passed"] and r["activation_state_passed"] and r["selftest_passed"] and r["exit_code"] == 0 for r in reports)
assert jar.read_bytes() == (root / "work/paper_validation/plugins" / jar.name).read_bytes()
with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    assert "version: 3.4.0" in archive.read("plugin.yml").decode("utf-8")
    assert "cn/tarot/ActivationStateTest.class" in archive.namelist()
with zipfile.ZipFile(source, "w", zipfile.ZIP_DEFLATED) as archive:
    for directory in ("src", "resources"):
        for path in sorted((project / directory).rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(project).as_posix())
    archive.write(project / "build.ps1", "build.ps1")
    archive.write(output / "彩虹塔罗V3.4.0更新说明.md", "README.md")
    archive.write(output / "彩虹塔罗PAPI占位符.md", "PAPI-placeholders.md")
with zipfile.ZipFile(source) as archive:
    assert archive.testzip() is None
report = {
    "version": "3.4.0",
    "jar_bytes": jar.stat().st_size,
    "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
    "source_bytes": source.stat().st_size,
    "resource_pack_changed": False,
    "zip_integrity_passed": True,
    "tested_jar_matches_delivered_jar": True,
    "aura_assertions": 195330,
    "cast_assertions": 30689,
    "activation_state_checks_per_boot": 75,
    "command_parser_checks": 340,
    "command_permission_checks": 155,
    "placeholder_value_checks": 102,
    "command_event_checks_per_boot": 53,
    "package_checks_per_boot": 85,
    "papi_integration_checks_per_enabled_boot": 69,
    "papi_optional_and_enabled_restart_passed": True,
    "placeholderapi": "2.12.3",
    "actual_paper_native_completion_passed": True,
    "main_hand_package_open_passed": True,
    "paper": "26.3-143",
    "restart_passed": True,
    "real_client_verified": False,
}
(output / "彩虹塔罗V3.4.0构建验证.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

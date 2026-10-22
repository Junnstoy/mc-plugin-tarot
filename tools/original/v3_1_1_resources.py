"""Build the complete 3.1.1 resource pack, preserving all 3.0.1 assets.

Run from any directory: python work/v3_1_1_resources.py
The imagegen PNG is copied byte-for-byte. Pillow is used only to inspect alpha.
"""
from __future__ import annotations

import hashlib
import html
import json
from pathlib import Path
import shutil
import zipfile

from PIL import Image


ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "outputs"
BASE = OUT / "tarot_resources_rainbow_v3_0_1"
PACK = OUT / "tarot_resources_rainbow_v3_1_1"
MANIFEST = ROOT / "work/album_art_manifest.json"
CLIENT_JAR = Path("E:/Minecraft/.minecraft/versions/26.3-Fabric 0.19.5/26.3-Fabric 0.19.5.jar")
BUNDLE_ITEM = "assets/minecraft/items/bundle.json"
ALBUM_TEXTURE = "assets/tarot/textures/item/album.png"
ALBUM_MODEL = {"type": "minecraft:model", "model": "tarot:item/album"}


def write_json(relative: str, value: object) -> None:
    destination = PACK / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def copy_album_art() -> dict:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8-sig"))
    source = Path(manifest["path"])
    if not source.is_absolute():
        source = ROOT / source
    with Image.open(source) as artwork:
        assert artwork.format == "PNG", "Album art must be PNG"
        assert artwork.width == artwork.height, "Album art must be square"
        assert "A" in artwork.getbands(), "Album art must have an alpha channel"
        alpha = artwork.getchannel("A")
        extrema = alpha.getextrema()
        assert extrema == (0, 255), "Album must contain transparent and opaque pixels"
        alpha_histogram = alpha.histogram()
        result = {
            "source": source.relative_to(ROOT).as_posix(),
            "file": ALBUM_TEXTURE,
            "width": artwork.width,
            "height": artwork.height,
            "mode": artwork.mode,
            "alpha_extrema": list(extrema),
            "transparent_pixels": alpha_histogram[0],
            "opaque_pixels": alpha_histogram[255],
            "sha256": sha256(source),
            "unchanged_from_imagegen": True,
        }
    destination = PACK / ALBUM_TEXTURE
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, destination)
    assert sha256(destination) == result["sha256"]
    (OUT / "彩虹塔罗V3.1.1收集册生成提示词.md").write_text(
        "# 彩虹塔罗 V3.1.1 — 收集册原始生成提示词\n\n"
        "图像由内置图像生成器生成，原样复制到资源包，未裁切、缩放或重绘。\n\n"
        + manifest["prompt"] + "\n", encoding="utf-8")
    return result


def validate(native_bundle: dict) -> dict:
    preserved = 0
    for original in BASE.rglob("*"):
        if not original.is_file():
            continue
        relative = original.relative_to(BASE)
        if relative.as_posix() == "pack.mcmeta":
            continue
        assert (PACK / relative).is_file(), f"Missing old asset {relative}"
        assert sha256(original) == sha256(PACK / relative), f"Changed old asset {relative}"
        preserved += 1

    with zipfile.ZipFile(CLIENT_JAR) as vanilla:
        known = set(vanilla.namelist())

        def resource_path(key: str, kind: str, suffix: str) -> str:
            namespace, path = key.split(":", 1) if ":" in key else ("minecraft", key)
            return f"assets/{namespace}/{kind}/{path}.{suffix}"

        def resource_exists(key: str, kind: str, suffix: str) -> bool:
            relative = resource_path(key, kind, suffix)
            return (PACK / relative).is_file() or relative in known

        def read_resource(key: str, kind: str) -> dict:
            relative = resource_path(key, kind, "json")
            local = PACK / relative
            return json.loads(local.read_text(encoding="utf-8") if local.is_file() else vanilla.read(relative))

        def validate_item_node(node: dict) -> None:
            kind = node["type"]
            if kind == "minecraft:model":
                assert resource_exists(node["model"], "models", "json"), node["model"]
                model = read_resource(node["model"], "models")
                for element in model.get("elements", []):
                    for face in element["faces"].values():
                        if "tintindex" in face:
                            assert 0 <= face["tintindex"] < len(node.get("tints", []))
            elif kind == "minecraft:select":
                for case in node["cases"]:
                    validate_item_node(case["model"])
                validate_item_node(node["fallback"])
            elif kind == "minecraft:condition":
                validate_item_node(node["on_true"])
                validate_item_node(node["on_false"])
            elif kind == "minecraft:composite":
                for child in node["models"]:
                    validate_item_node(child)
            else:
                assert kind == "minecraft:bundle/selected_item", kind

        definitions = 0
        for path in PACK.rglob("*.json"):
            definition = json.loads(path.read_text(encoding="utf-8"))
            if "items" in path.parts:
                validate_item_node(definition["model"])
                definitions += 1
            if "models" in path.parts:
                if "parent" in definition:
                    assert resource_exists(definition["parent"], "models", "json"), definition["parent"]
                textures = definition.get("textures", {})
                for texture in textures.values():
                    visited = set()
                    while texture.startswith("#"):
                        assert texture not in visited, f"Circular texture reference: {path}"
                        visited.add(texture)
                        texture = textures[texture[1:]]
                    assert resource_exists(texture, "textures", "png"), texture
                for element in definition.get("elements", []):
                    for face in element["faces"].values():
                        assert face["texture"].startswith("#") and face["texture"][1:] in textures

        bundle = json.loads((PACK / BUNDLE_ITEM).read_text(encoding="utf-8"))["model"]
        assert bundle["type"] == "minecraft:select"
        assert bundle["property"] == "minecraft:custom_model_data"
        assert bundle["index"] == 0
        assert bundle["cases"] == [{"when": "tarot:album", "model": ALBUM_MODEL}]
        assert bundle["fallback"] == native_bundle["model"]
        # Also retain any item-definition-level native settings outside "model".
        emitted = json.loads((PACK / BUNDLE_ITEM).read_text(encoding="utf-8"))
        assert {k: v for k, v in emitted.items() if k != "model"} == {
            k: v for k, v in native_bundle.items() if k != "model"}

        paper = json.loads((PACK / "assets/minecraft/items/paper.json").read_text(encoding="utf-8"))["model"]
        expected = {f"tarot:card_{card}_{level}" for card in range(1, 23) for level in range(1, 6)}
        expected |= {"tarot:pack", "tarot:box", "tarot:case"}
        assert len(paper["cases"]) == 113
        assert {case["when"] for case in paper["cases"]} == expected
        assert paper["fallback"] == json.loads(vanilla.read("assets/minecraft/items/paper.json"))["model"]
        assert definitions == 138, definitions
        assert len(list((PACK / "assets/tarot/models/item").glob("card_*_*.json"))) == 110
        assert json.loads((PACK / "pack.mcmeta").read_text(encoding="utf-8"))["pack"]["min_format"] == [97, 1]

    return {
        "version": "3.1.1",
        "client_version": "26.3",
        "resource_format": [97, 1],
        "verified_against_local_client": str(CLIENT_JAR),
        "base_pack": BASE.name,
        "base_files_preserved_byte_for_byte": preserved,
        "item_definitions": definitions,
        "card_variants": 110,
        "legacy_card_aliases": 22,
        "package_models": 3,
        "paper_selector_cases": 113,
        "paper_selector_unchanged": True,
        "album_selector_cases": 1,
        "album_selector_value": "tarot:album",
        "custom_model_data_string_index": 0,
        "album_unloaded_pack_model": "minecraft:bundle",
        "ordinary_bundle_fallback_exactly_matches_vanilla": True,
        "native_bundle_open_selected_item_states_preserved": True,
        "unresolved_references": 0,
        "runtime_client_render_tested": False,
    }


def gallery() -> None:
    texture = html.escape(PACK.name + "/" + ALBUM_TEXTURE, quote=True)
    page = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>彩虹塔罗 V3.1.1 · 收集册外观</title>
<style>body{margin:0;background:#111323;color:#f4f0ff;font:16px/1.7 system-ui,sans-serif}
main{max-width:900px;margin:auto;padding:32px}h1{font-size:32px}p{color:#bfc4df}
.hero{display:block;width:min(100%,480px);margin:24px auto;image-rendering:pixelated}
.sizes{display:flex;align-items:center;justify-content:center;gap:40px;padding:28px;
background:#20243a;border:1px solid #393554;border-radius:12px}.sizes img{image-rendering:pixelated}
figure{text-align:center;margin:0}figcaption{color:#bfc4df;font-size:12px;margin-top:12px}</style>
<main><h1>彩虹塔罗 · 收藏之书</h1>
<p>深靛紫封面、彩虹像素包边、金色星芒与五色书签。收集册沿用收纳袋物品，启用资源包后显示专属册子外观。</p>
<img class="hero" src="TEXTURE" alt="彩虹塔罗收集册完整贴图">
<section class="sizes">'''.replace("TEXTURE", texture)
    for size in (32, 64, 128):
        page += f'<figure><img src="{texture}" width="{size}" height="{size}" alt="{size}像素显示预览"><figcaption>{size} px</figcaption></figure>'
    page += '''</section><p>这是原始贴图在网页中的尺寸预览，非游戏内截图。未启用资源包时显示原版收纳袋；普通收纳袋保留原有外观及打开状态。收集菜单沿用原版背包界面。</p></main></html>'''
    (OUT / "彩虹塔罗V3.1.1收集册外观.html").write_text(page, encoding="utf-8")


def main() -> None:
    assert BASE.is_dir(), f"Base resource pack missing: {BASE}"
    # No deletion: keep the source pack intact and overlay only known new files.
    shutil.copytree(BASE, PACK, dirs_exist_ok=True)
    metadata = json.loads((BASE / "pack.mcmeta").read_text(encoding="utf-8"))
    metadata["pack"]["description"] = "彩虹塔罗 V3.1.1 · 彩虹收集册 · 22 张 × 5 星 · Minecraft 26.3"
    write_json("pack.mcmeta", metadata)
    with zipfile.ZipFile(CLIENT_JAR) as vanilla:
        native_bundle = json.loads(vanilla.read(BUNDLE_ITEM))
    bundle = dict(native_bundle)
    bundle["model"] = {
        "type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
        "cases": [{"when": "tarot:album", "model": ALBUM_MODEL}],
        "fallback": native_bundle["model"],
    }
    write_json(BUNDLE_ITEM, bundle)
    write_json("assets/tarot/models/item/album.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": "tarot:item/album"}})
    write_json("assets/tarot/items/album.json", {"model": ALBUM_MODEL})
    art = copy_album_art()
    report = validate(native_bundle)
    report["album_artwork"] = art
    gallery()
    destination = OUT / (PACK.name + ".zip")
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in sorted(PACK.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(PACK).as_posix())
    with zipfile.ZipFile(destination) as archive:
        assert archive.testzip() is None
        assert "pack.mcmeta" in archive.namelist()
        assert hashlib.sha256(archive.read(ALBUM_TEXTURE)).hexdigest() == art["sha256"]
        report["zip_crc_valid"] = True
        report["zip_entries"] = len(archive.namelist())
    report["zip_sha1"] = hashlib.sha1(destination.read_bytes()).hexdigest()
    report["zip_sha256"] = sha256(destination)
    report["zip_bytes"] = destination.stat().st_size
    (OUT / "彩虹塔罗V3.1.1资源验证.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

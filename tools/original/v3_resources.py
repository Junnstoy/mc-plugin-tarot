"""Build rainbow tarot resource pack. Generated illustrations are copied byte-for-byte.

Run from project root: python work/v3_resources.py
Build the paper fallback patch: python work/v3_resources.py --version 3.0.1
Use --prepare to generate models before work/rainbow_art_manifest.json is ready.
No generated image is cropped, resized, recolored, composited, or otherwise edited.
"""
from __future__ import annotations

import argparse
import hashlib
import html
import json
from pathlib import Path
import shutil
import struct
import zipfile
import zlib

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "outputs"
PACK = OUT / "tarot_resources_rainbow_v3"
VERSION_LABEL = "V3"
OUTPUT_LABEL = "彩虹塔罗V3"
PAPER_FALLBACK = False
MANIFEST = ROOT / "work/rainbow_art_manifest.json"
CLIENT_JAR = Path("E:/Minecraft/.minecraft/versions/26.3-Fabric 0.19.5/26.3-Fabric 0.19.5.jar")
CARDS = [
    ("愚者", "R"), ("魔术师", "SR"), ("女祭司", "SR"),
    ("女皇", "SSR"), ("皇帝", "SSR"), ("教皇", "SR"),
    ("恋人", "R"), ("战车", "SR"), ("力量", "SSR"),
    ("隐者", "R"), ("命运之轮", "SP"), ("正义", "SR"),
    ("倒吊人", "R"), ("死神", "SP"), ("节制", "R"),
    ("恶魔", "SSR"), ("高塔", "SP"), ("星星", "R"),
    ("月亮", "SR"), ("太阳", "SSR"), ("审判", "UR"), ("世界", "UR"),
]
RARITY = {"R": "ff575f", "SR": "ff963a", "SSR": "ffe460", "SP": "66e395", "UR": "c27aff"}
LEVEL = {1: "ff575f", 2: "ffe460", 3: "58e6ed", 4: "5e92ff", 5: "c27aff"}
COLOR_NAMES = {"R": "红", "SR": "橙", "SSR": "黄", "SP": "绿", "UR": "紫"}


def write_json(relative: str, value: object) -> None:
    p = PACK / relative
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def argb(hex_rgb: str) -> int:
    return int("ff" + hex_rgb, 16) - 2**32


def white_texture() -> bytes:
    # This single white pixel is a code-native material swatch, not an image edit.
    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(b"\x00\xff\xff\xff\xff")) + chunk(b"IEND", b""))


def block(start: list, end: list, tint: int | None, texture: str = "#white", sides: bool = True) -> dict:
    face = {"uv": [0, 0, 16, 16], "texture": texture}
    if tint is not None:
        face["tintindex"] = tint
    directions = ["north", "south", "east", "west", "up", "down"] if sides else ["north", "south"]
    return {"from": start, "to": end, "shade": False,
            "faces": {direction: dict(face) for direction in directions}}


def card_model(card_id: int, level: int) -> dict:
    # Small layer depth differences prevent z-fighting. The face is square (9.8 x 9.8).
    elements = [
        block([2, 0.5, 7.60], [14, 15.5, 8.40], 0),
        block([2.4, 0.9, 7.55], [13.6, 15.1, 8.45], 1),
        block([2.8, 1.3, 7.50], [13.2, 14.7, 8.50], 2),
        block([3.1, 4.5, 7.45], [12.9, 14.3, 8.55], None, "#art", sides=False),
    ]
    # Countable four-point pixel stars, two rectangles each, in the lower panel.
    # They are geometry, so badges remain crisp at any resource texture resolution.
    for star in range(level):
        center = 8 + (star - (level - 1) / 2) * 1.8
        elements += [
            block([round(center - .20, 3), 1.90, 7.43], [round(center + .20, 3), 3.60, 8.57], 1),
            block([round(center - .65, 3), 2.45, 7.41], [round(center + .65, 3), 3.05, 8.59], 1),
        ]
    return {
        "gui_light": "front", "ambientocclusion": False,
        "textures": {"art": f"tarot:item/card_{card_id}", "white": "tarot:item/white", "particle": "#art"},
        "elements": elements,
        "display": {
            "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [.55, .55, .55]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [.55, .55, .55]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [.68, .68, .68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [.68, .68, .68]},
        },
    }


def prepare() -> None:
    PACK.mkdir(parents=True, exist_ok=True)
    write_json("pack.mcmeta", {"pack": {"min_format": [97, 1], "max_format": [97, 1],
                                        "description": f"彩虹塔罗 {VERSION_LABEL} · 22 张 × 5 星 · Paper / Minecraft 26.3"}})
    dest = PACK / "assets/tarot/textures/item/white.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(white_texture())
    cases = []
    for card_id, (_, rarity) in enumerate(CARDS, 1):
        for level in range(1, 6):
            key = f"card_{card_id}_{level}"
            write_json(f"assets/tarot/models/item/{key}.json", card_model(card_id, level))
            definition = {"model": {"type": "minecraft:model", "model": f"tarot:item/{key}",
                "tints": [{"type": "minecraft:constant", "value": argb(color)}
                          for color in [RARITY[rarity], LEVEL[level], "17132d"]]}}
            write_json(f"assets/tarot/items/{key}.json", definition)
            cases.append({"when": f"tarot:{key}", "model": definition["model"]})
            if level == 1:
                write_json(f"assets/tarot/items/card_{card_id}.json", definition)
    for key in ("pack", "box", "case"):
        definition = {"model": {"type": "minecraft:model", "model": f"tarot:item/{key}"}}
        write_json(f"assets/tarot/items/{key}.json", definition)
        cases.append({"when": f"tarot:{key}", "model": definition["model"]})
        write_json(f"assets/tarot/models/item/{key}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"tarot:item/{key}"}})
    if PAPER_FALLBACK:
        # The plugin keeps the vanilla minecraft:paper item model and selects our
        # art with custom_model_data.strings[0]. Without this pack, vanilla paper
        # remains resolvable. The fallback here also preserves ordinary paper.
        write_json("assets/minecraft/items/paper.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
            "cases": cases,
            "fallback": {"type": "minecraft:model", "model": "minecraft:item/paper"},
        }})


def read_manifest() -> dict:
    raw = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if isinstance(raw, list):
        entries = {int(item["index"]): item for item in raw}
    else:
        entries = {int(str(key).removeprefix("index")): item for key, item in raw.items()}
    missing = sorted(set(range(26)) - set(entries))
    if missing:
        raise ValueError(f"Artwork manifest incomplete; missing indices {missing}")
    return entries


def copy_art(entries: dict) -> list:
    records = []
    for index in range(26):
        entry = entries[index]
        source = Path(entry["path"])
        if not source.is_absolute():
            source = ROOT / source
        data = source.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n":
            raise ValueError(f"Artwork {index} is not PNG")
        width, height = struct.unpack(">II", data[16:24])
        if width != height:
            raise ValueError(f"Artwork {index} is not square: {width}x{height}; regenerate as square")
        key = f"card_{index+1}" if index < 22 else ("pack", "box", "case", "luck")[index - 22]
        target = PACK / (f"assets/tarot/textures/item/{key}.png" if index < 25 else "assets/minecraft/textures/mob_effect/luck.png")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        checksum = hashlib.sha256(data).hexdigest()
        assert checksum == hashlib.sha256(target.read_bytes()).hexdigest()
        records.append({"index": index, "file": target.relative_to(PACK).as_posix(),
                        "width": width, "height": height, "sha256": checksum, "unchanged_from_imagegen": True})
    return records


def validate() -> dict:
    vanilla = zipfile.ZipFile(CLIENT_JAR)
    known = set(vanilla.namelist())
    assert "assets/minecraft/textures/mob_effect/luck.png" in known
    sample = json.loads(vanilla.read("assets/minecraft/items/acacia_leaves.json"))
    assert sample["model"]["tints"][0]["type"] == "minecraft:constant"
    def exists(resource: str, kind: str, extension: str) -> bool:
        namespace, key = resource.split(":", 1)
        path = f"assets/{namespace}/{kind}/{key}.{extension}"
        return (PACK / path).is_file() or path in known
    def read_resource(resource: str, kind: str) -> dict:
        namespace, key = resource.split(":", 1)
        relative = f"assets/{namespace}/{kind}/{key}.json"
        local = PACK / relative
        return json.loads(local.read_text(encoding="utf-8") if local.is_file() else vanilla.read(relative))

    def validate_item_model(inner: dict) -> None:
        if inner["type"] == "minecraft:select":
            assert inner["property"] == "minecraft:custom_model_data"
            assert inner["index"] == 0
            expected = {f"tarot:card_{card_id}_{level}" for card_id in range(1, 23) for level in range(1, 6)}
            expected |= {"tarot:pack", "tarot:box", "tarot:case"}
            actual = [case["when"] for case in inner["cases"]]
            assert len(actual) == len(set(actual)) == 113
            assert set(actual) == expected
            for case in inner["cases"]:
                validate_item_model(case["model"])
                # Selection must preserve the exact existing model and rarity/star tints.
                assert case["model"] == read_resource(case["when"], "items")["model"]
            assert inner["fallback"] == {"type": "minecraft:model", "model": "minecraft:item/paper"}
            validate_item_model(inner["fallback"])
        else:
            assert inner["type"] == "minecraft:model"
            assert exists(inner["model"], "models", "json"), inner["model"]
            target_model = read_resource(inner["model"], "models")
            for element in target_model.get("elements", []):
                for face in element["faces"].values():
                    if "tintindex" in face:
                        assert 0 <= face["tintindex"] < len(inner.get("tints", []))

    model_count = 0
    for path in PACK.rglob("*.json"):
        model = json.loads(path.read_text(encoding="utf-8"))
        if "items" in path.parts:
            validate_item_model(model["model"])
            model_count += 1
        if "models" in path.parts:
            if "parent" in model:
                assert exists(model["parent"], "models", "json"), model["parent"]
            textures = model.get("textures", {})
            for texture in textures.values():
                while texture.startswith("#"):
                    texture = textures[texture[1:]]
                assert exists(texture, "textures", "png"), texture
            for element in model.get("elements", []):
                assert all(-16 <= p <= 32 for point in (element["from"], element["to"]) for p in point)
                for face in element["faces"].values():
                    assert face["texture"][1:] in textures
    assert model_count == 135 + int(PAPER_FALLBACK), model_count
    assert len(list((PACK / "assets/tarot/models/item").glob("card_*_*.json"))) == 110
    vanilla_paper = json.loads(vanilla.read("assets/minecraft/items/paper.json"))["model"]
    assert vanilla_paper == {"type": "minecraft:model", "model": "minecraft:item/paper"}
    assert "assets/minecraft/models/item/paper.json" in known
    vanilla.close()
    return {"item_definitions": model_count, "card_variants": 110, "legacy_aliases": 22,
            "package_models": 3, "unresolved_references": 0,
            "verified_against_local_client": str(CLIENT_JAR), "client_version": "26.3",
            "resource_format": [97, 1], "version": VERSION_LABEL,
            "paper_fallback_enabled": PAPER_FALLBACK,
            "custom_model_data_string_index": 0 if PAPER_FALLBACK else None,
            "paper_selector_cases": 113 if PAPER_FALLBACK else 0,
            "ordinary_paper_fallback_matches_vanilla": PAPER_FALLBACK,
            "unloaded_pack_model": "minecraft:paper" if PAPER_FALLBACK else None,
            "runtime_client_render_tested": False}


def gallery() -> None:
    prefix = PACK.name + "/assets/tarot/textures/item/"
    document = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>彩虹塔罗 V3 · 牌面图鉴</title><style>
*{box-sizing:border-box}body{margin:0;background:#0b1020;color:#eef2ff;font:16px/1.6 system-ui,sans-serif}
header,main,footer{max-width:1320px;margin:auto;padding:32px}h1{font-size:clamp(30px,5vw,56px);margin:4px 0;letter-spacing:.04em}
header{border-bottom:1px solid #29304b}.eyebrow{letter-spacing:.2em;color:#72dbea;font-size:12px}p{color:#bbc5df;max-width:850px}
.legend{display:flex;gap:12px;flex-wrap:wrap;margin:20px 0}.chip{border:1px solid var(--rarity);color:var(--rarity);border-radius:6px;padding:5px 12px}
label{display:flex;align-items:center;gap:12px}select{font:inherit;background:#171d32;color:white;border:1px solid #65708f;border-radius:6px;padding:7px 16px}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:24px}.tile{padding:18px;background:#12192b;border:1px solid #25304b;border-radius:12px}
.card{width:100%;aspect-ratio:12/15;background:var(--rarity);position:relative;image-rendering:pixelated;--level:#ff575f}
.rim{position:absolute;inset:2.667% 3.333%;background:var(--level)}.panel{position:absolute;inset:5.333% 6.667%;background:#17132d}
.art{position:absolute;left:9.167%;top:8%;width:81.667%;aspect-ratio:1;display:block;image-rendering:pixelated}
.stars{position:absolute;left:0;right:0;bottom:10.5%;display:flex;justify-content:center;gap:4.167%}.star{width:10.833%;aspect-ratio:1;position:relative}
.star:before,.star:after{content:"";position:absolute;background:var(--level)}.star:before{width:31%;height:100%;left:34.5%}.star:after{width:100%;height:35%;top:32.5%}
.caption{display:flex;justify-content:space-between;align-items:center;margin-top:14px}.caption b{font-size:18px}.caption span{font-size:12px;color:var(--rarity)}
.number{font-size:11px;color:#7d8ba8;letter-spacing:.13em}.packages{display:grid;grid-template-columns:repeat(3,1fr);gap:24px;margin-top:20px}.package{background:#12192b;border:1px solid #25304b;border-radius:12px;padding:20px;text-align:center}
.package img{width:100%;max-height:240px;object-fit:contain;image-rendering:pixelated}.icon{width:48px;height:48px;image-rendering:pixelated;vertical-align:middle;margin-right:12px}
footer{color:#8c9ab8;font-size:13px}@media(max-width:600px){header,main,footer{padding:20px}.grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.tile{padding:10px}.caption b{font-size:15px}.packages{gap:10px}.package{padding:10px}}
</style><header><div class="eyebrow">RAINBOW ARCANA / COLLECTION 01</div><h1>彩虹塔罗 · 全牌图鉴</h1><p>22 张大阿尔卡那，五档稀有度，五个星级。外框表示稀有度，内框与下方星芒表示等级；红、橙、黄、绿、青、蓝、紫组成完整配色。</p><div class="legend">'''
    for rarity, color in RARITY.items():
        document += f'<span class="chip" style="--rarity:#{color}">{rarity} · {COLOR_NAMES[rarity]}框</span>'
    document += '</div><label>预览星级 <select id="level">' + ''.join(f'<option value="{i}">{i} 星</option>' for i in range(1, 6))
    document += '</select><span id="levelLabel">红色内框 · 1 星</span></label><p>这是资源美术与模型正面布局预览，非游戏内截图。实际游戏中有双面卡板与轻微厚度。</p></header><main><section class="grid">'
    for card_id, (name, rarity) in enumerate(CARDS, 1):
        document += f'<article class="tile" style="--rarity:#{RARITY[rarity]}"><div class="card"><div class="rim"></div><div class="panel"></div><img class="art" src="{prefix}card_{card_id}.png" alt="{html.escape(name)}像素牌面"><div class="stars"><i class="star"></i></div></div><div class="caption"><b>{name}</b><span>{rarity}</span></div><div class="number">ARCANA {card_id-1:02d} / 22</div></article>'
    document += '</section><h2>卡包 · 卡盒 · 卡箱</h2><section class="packages">'
    for key, name, description in [("pack", "卡包", "5 张牌 · 至少一张 SR 或以上"), ("box", "卡盒", "8 包 · 40 张牌"), ("case", "卡箱", "48 盒 · 384 包 · 1920 张牌")]:
        document += f'<article class="package"><img src="{prefix}{key}.png" alt="{name}"><h3>{name}</h3><p>{description}</p></article>'
    document += f'</section><h2>状态图标</h2><p><img class="icon" src="{PACK.name}/assets/minecraft/textures/mob_effect/luck.png" alt="塔罗状态图标">通用塔罗星芒图标配合原版状态倒计时。此资源包覆盖原版“幸运”图标，原版幸运效果也会显示这一图标；不会按卡名切换 22 种状态图标。</p></main><footer>插画由内置图像生成器生成并原样封装。边框、卡板和星级徽章为游戏模型几何；没有把边框绘入插画。</footer>'
    document += '<script>const colors=' + json.dumps(LEVEL) + ',names={1:"红",2:"黄",3:"青",4:"蓝",5:"紫"};document.getElementById("level").addEventListener("change",e=>{const l=Number(e.target.value);document.querySelectorAll(".card").forEach(c=>{c.style.setProperty("--level","#"+colors[l]);c.querySelector(".stars").innerHTML=\'<i class="star"></i>\'.repeat(l)});document.getElementById("levelLabel").textContent=names[l]+"色内框 · "+l+" 星"});</script></html>'
    document = document.replace("彩虹塔罗 V3", f"彩虹塔罗 {VERSION_LABEL}")
    if PAPER_FALLBACK:
        document = document.replace("这是资源美术与模型正面布局预览", "V3.0.1：未启用资源包时新版卡牌显示原版纸张，启用后显示彩虹牌面。普通纸张保持原样。这是资源美术与模型正面布局预览")
    (OUT / f"{OUTPUT_LABEL}牌面图鉴.html").write_text(document, encoding="utf-8")


def main() -> None:
    global PACK, VERSION_LABEL, OUTPUT_LABEL, PAPER_FALLBACK
    parser = argparse.ArgumentParser()
    parser.add_argument("--prepare", action="store_true")
    parser.add_argument("--version", choices=("3.0.0", "3.0.1"), default="3.0.0")
    args = parser.parse_args()
    if args.version == "3.0.1":
        PACK = OUT / "tarot_resources_rainbow_v3_0_1"
        VERSION_LABEL = "V3.0.1"
        OUTPUT_LABEL = "彩虹塔罗V3.0.1"
        PAPER_FALLBACK = True
    prepare()
    if args.prepare:
        print("Prepared 110 card models, 22 legacy aliases and 3 package models; art not packaged yet.")
        return
    entries = read_manifest()
    art = copy_art(entries)
    report = validate()
    report["artwork"] = art
    gallery()
    prompts = [f"# 彩虹塔罗 {VERSION_LABEL} — 原始生成提示词", "", "生成图片均逐字节原样复制到资源包。边框与星级是独立模型几何。", ""]
    for i in range(26):
        title = CARDS[i][0] if i < 22 else ("卡包", "卡盒", "卡箱", "通用状态图标")[i - 22]
        prompts += [f"## {i + 1}. {title}", "", entries[i].get("prompt", "（由主流程保存）"), ""]
    (OUT / f"{OUTPUT_LABEL}牌面生成提示词.md").write_text("\n".join(prompts), encoding="utf-8")
    destination = OUT / f"{PACK.name}.zip"
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in sorted(PACK.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(PACK).as_posix())
    with zipfile.ZipFile(destination) as archive:
        assert archive.testzip() is None
        assert "pack.mcmeta" in archive.namelist()
    report["zip_sha1"] = hashlib.sha1(destination.read_bytes()).hexdigest()
    report["zip_bytes"] = destination.stat().st_size
    (OUT / f"{OUTPUT_LABEL}资源验证.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "artwork"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

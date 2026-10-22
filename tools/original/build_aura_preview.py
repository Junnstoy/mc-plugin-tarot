from pathlib import Path
import hashlib
import subprocess

ROOT = Path(__file__).resolve().parent.parent
JAVA = Path('C:/Users/Yuma/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon/bin')
ENGINE = ROOT / 'work/v3_plugin/src/cn/tarot/AlbumAura.java'
CLASSES = ROOT / 'work/aura_preview_classes'
DATA = ROOT / 'work/aura_preview_frames.json'
OUTPUT = ROOT / 'outputs/彩虹塔罗V3.3.1粒子预览.html'
CLASSES.mkdir(exist_ok=True)
subprocess.run([str(JAVA / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(CLASSES), str(ENGINE),
                str(ROOT / 'work/aura_preview_dump.java')], check=True)
subprocess.run([str(JAVA / 'java.exe'), '-cp', str(CLASSES), 'AuraPreviewDump', str(DATA)], check=True)
template = (ROOT / 'work/aura_preview_template.html').read_text(encoding='utf-8')
template = template.replace('__AURA_GEOMETRY_DATA__', DATA.read_text(encoding='utf-8'))
template = template.replace('__ENGINE_HASH__', hashlib.sha256(ENGINE.read_bytes()).hexdigest()[:16])
OUTPUT.write_text(template, encoding='utf-8')
print(f'{OUTPUT}\n{OUTPUT.stat().st_size:,} bytes')

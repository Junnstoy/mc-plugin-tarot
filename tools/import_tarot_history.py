from pathlib import Path, PurePosixPath
import hashlib
import json
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'outputs'
REPO = ROOT / 'mc-plugin-tarot'
STAGES = [
    ('datapack-initial', 'tarot_26_3.zip', None, '最初数据包：22 张卡牌、五级升级与包装抽卡', ['使用说明与数值.md']),
    ('v2.0.0', 'tarot_26_3_v2.zip', 'tarot_resources_26_3.zip', '数据包 V2：牌面贴图、技能粒子与音效', ['V2安装说明.md']),
    ('v2.1.0', 'tarot_26_3_v2_1.zip', None, '数据包 V2.1：26.3 兼容修复', ['V2.1修复说明.md']),
    ('v3.0.0', 'TarotRainbow-3.0.0-source.zip', 'tarot_resources_rainbow_v3.zip', '转为 Paper 插件：彩虹卡牌、定时效果与背包升级', ['彩虹塔罗V3安装与玩法.md']),
    ('v3.0.1', 'TarotRainbow-3.0.1-source.zip', 'tarot_resources_rainbow_v3_0_1.zip', '修复升级、满背包溢出与原版贴图回退', ['彩虹塔罗V3.0.1安装与修复说明.md']),
    ('v3.1.0', 'TarotRainbow-3.1.0-source.zip', None, '收集册：可取回收藏、羁绊奖励与收集特效', ['彩虹塔罗V3.1收集册说明.md']),
    ('v3.1.1', 'TarotRainbow-3.1.1-source.zip', 'tarot_resources_rainbow_v3_1_1.zip', '收集册专属资源与兼容修复', ['彩虹塔罗V3.1.1更新说明.md']),
    ('v3.2.0', 'TarotRainbow-3.2.0-source.zip', None, '重整指令、个人收藏数据与唯一收集册', ['彩虹塔罗V3.2指令与卡册说明.md']),
    ('v3.3.0', 'TarotRainbow-3.3.0-source.zip', None, '头顶光环、挂机翅膀与五秒技能蓄力', ['彩虹塔罗V3.3特效与蓄力说明.md']),
    ('v3.3.1', 'TarotRainbow-3.3.1-source.zip', None, '主手蓄力、降低粒子密度并修复效果切换', ['彩虹塔罗V3.3.1更新说明.md']),
    ('v3.4.0', 'TarotRainbow-3.4.0-source.zip', None, '右键拆包、命令权限、时间绕过与 PAPI', ['彩虹塔罗V3.4.0更新说明.md', '彩虹塔罗PAPI占位符.md']),
]

def git(*args):
    return subprocess.check_output(['git', '-C', str(REPO), *args], text=True, encoding='utf-8').strip()

def write(name, content):
    dest = REPO / name
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(content, encoding='utf-8')

def extract_snapshot(archive, target):
    # Only remove previously imported, known files; never recursively delete a directory.
    base = REPO / target
    if base.exists():
        for path in base.rglob('*'):
            if path.is_file():
                assert path.resolve().is_relative_to(REPO.resolve())
                path.unlink()
    source = OUT / archive
    files = {}
    with zipfile.ZipFile(source) as z:
        assert z.testzip() is None
        for entry in z.infolist():
            if entry.is_dir():
                continue
            name = PurePosixPath(entry.filename)
            assert not name.is_absolute() and '..' not in name.parts
            dest = base.joinpath(*name.parts)
            assert dest.resolve().is_relative_to(base.resolve())
            dest.parent.mkdir(parents=True, exist_ok=True)
            data = z.read(entry)
            dest.write_bytes(data)
            files[name.as_posix()] = hashlib.sha256(data).hexdigest()
    return {'archive': archive, 'archive_sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
            'target': target, 'files': files}

def main():
    if REPO.exists():
        raise SystemExit('Destination already exists; refusing to overwrite history.')
    REPO.mkdir()
    git('init', '-b', 'main')
    git('config', 'user.name', 'Junnstoy')
    git('config', 'user.email', '29830029+Junnstoy@users.noreply.github.com')
    git('config', 'core.autocrlf', 'false')
    git('config', 'core.quotepath', 'false')
    write('.gitignore', '*.class\n*.jar\n*.zip\n*.log\n__pycache__/\n.idea/\n.vscode/\nplugin/classes/\nplugin/tmp/\ndeps/\nserver/\n.env\n.env.*\n')
    write('.gitattributes', '# Preserve imported snapshot bytes, including original line endings.\n* -text\n*.png binary\n*.ogg binary\n')
    imports = []
    for index, (tag, archive, resource, description, docs) in enumerate(STAGES):
        sources = [extract_snapshot(archive, 'legacy/datapack' if index < 3 else 'plugin')]
        if resource:
            sources.append(extract_snapshot(resource, 'resource-pack'))
        for doc in docs:
            write('docs/releases/' + doc, (OUT / doc).read_text(encoding='utf-8'))
        imports.append({'tag': tag, 'description': description, 'sources': sources})
        write('history/snapshots.json', json.dumps(imports, ensure_ascii=False, indent=2) + '\n')
        write('README.md', '# 彩虹塔罗 / mc-plugin-tarot\n\n当前快照：`' + tag + '`。' + description + '。\n\n'
              '此 Git 历史按版本顺序从实际留存的发布文件恢复。提交时间是导入时间，不代表原始开发时间；没有恢复未留存的中间编辑记录。\n\n'
              '- `plugin/`：Paper 插件源码与配置（V3 起）。\n- `legacy/datapack/`：早期数据包源码，仅用于历史参考。\n'
              '- `resource-pack/`：对应资源包的可编辑文件。\n- `docs/releases/`：各版本说明。\n'
              '- `history/snapshots.json`：原始归档及逐文件 SHA-256，可核对导入内容。\n')
        git('add', '--all')
        git('commit', '-m', f'import({tag}): {description}', '-m',
            f'Reconstructed from preserved archive {archive}. Import timestamp, not original development timestamp. See history/snapshots.json for provenance.')
        git('tag', '-a', tag, '-m', f'{tag}: restored release snapshot; source {archive}')
        print(tag, git('rev-parse', '--short', 'HEAD'), flush=True)

    # Preserve supporting scripts as the files currently available; do not imply historical dating.
    for path in sorted((ROOT / 'work').glob('*.py')):
        if path.name == Path(__file__).name:
            continue
        dest = REPO / 'tools/original' / path.name
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(path.read_bytes())
    (REPO / 'tools/import_tarot_history.py').write_bytes(Path(__file__).read_bytes())
    for path in sorted(OUT.glob('*.md')):
        dest = REPO / 'docs/original' / path.name
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(path.read_bytes())
    for path in sorted(OUT.glob('*验证.json')):
        dest = REPO / 'docs/validation' / path.name
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(path.read_bytes())
    write('docs/HISTORY.md', '# 历史导入说明\n\n'
          '本仓库在 2026-10-22 从本地实际留存的发布归档恢复，版本标签顺序如下。提交作者采用仓库所有者 GitHub noreply 地址，未公开个人邮箱。\n\n'
          + '\n'.join(f'- `{tag}`：{desc}（`{archive}`）' for tag, archive, _, desc, _ in STAGES) + '\n\n'
          '初始数据包未标语义版本，使用 `datapack-initial`；V2、V2.1 分别统一标为 `v2.0.0`、`v2.1.0`。\n\n'
          'V3.3.2 只有更新说明和辅助脚本，没有留存独立源码归档，因此不伪造该版本提交或标签；对应拆包改动已在 V3.4.0 中。\n\n'
          'V3.4.0 资源包与 V3.1.1 资源包逐路径、逐字节一致，仅压缩包名称不同，所以继续沿用原资产。\n\n'
          '各 tag 中 plugin、legacy/datapack 和 resource-pack 的导入文件均保留归档原始字节。清单记录原归档 SHA-256 和逐文件 SHA-256；资源包仅在变化的版本记录新的导入。\n\n'
          '`tools/original/` 是导入时保留的辅助脚本，可能含本机路径和早期环境假设；未赋予它们不存在的历史日期。它们用于开发过程存档，并非通用安装器。\n\n'
          '仓库不包含服务端世界、玩家存档、日志、第三方依赖、构建缓存或二进制发布 JAR。源代码历史不能重现没有留存的开发中间步骤。\n')
    write('README.md', '# 彩虹塔罗 · mc-plugin-tarot\n\n'
          '面向 **Paper 26.3 / Java 25** 的塔罗卡牌插件，当前版本 **3.4.0**。\n\n'
          '22 张塔罗牌，每张 1～5 星，R / SR / SSR / SP / UR 五种稀有度；包含抽卡包装、重复卡升级、个人收集册、收藏羁绊、粒子特效、命令权限与 PlaceholderAPI。\n\n'
          '## 当前玩法\n\n'
          '- 主手持牌长按右键 5 秒激活；普通效果持续现实 24 小时，换卡间隔 30 分钟，离线计时。\n'
          '- 背包内将同名同星卡拖到另一张卡上升级，五星满级。\n'
          '- 主手右键打开卡包、卡盒与整箱；每包 5 张且至少一张 SR 或以上，每盒 8 包，每箱 48 盒。\n'
          '- 收集册按玩家保存；卡牌存入才计入进度，取出扣除进度，随身携带收集册获得对应奖励。\n'
          '- `tarot.admin` 管理命令；`tarot.command.<命令>` 单项权限；`tarot.bypass.cd` 单独授予，可免效果期限和换卡冷却。\n'
          '- PAPI 可选：`%tarot_effect_name%`、`%tarot_effect_remaining%`、`%tarot_switch_cd%`、`%tarot_album_progress%`、`%tarot_album_level%` 等。\n\n'
          '## 文件与文档\n\n'
          '- [插件源码与配置](plugin/)\n- [资源包文件](resource-pack/)：将其内容打包，确保 ZIP 根目录包含 pack.mcmeta。\n'
          '- [完整权限、安装与更新说明](docs/releases/彩虹塔罗V3.4.0更新说明.md)\n'
          '- [PAPI 占位符](docs/releases/彩虹塔罗PAPI占位符.md)\n'
          '- [历史说明与版本索引](docs/HISTORY.md)\n- [服务端验证报告](docs/validation/)\n\n'
          '## 构建\n\n'
          '插件使用 Java 25、Paper 26.3 API 和 PlaceholderAPI 2.12.3 编译。原始 `plugin/build.ps1` 保留发布时的环境路径，需要按本机 Java、Paper libraries、PAPI 依赖和输出目录调整后执行。\n\n'
          'PlaceholderAPI 只在使用占位符时需要安装；插件内嵌扩展，无需另下 eCloud 扩展。安装插件请停服替换 JAR 并保留玩家数据，不要与 `legacy/datapack` 同时启用。\n\n'
          '## 版本历史与验证范围\n\n'
          '按顺序恢复了最初数据包、V2、V2.1 及 V3.0.0 至 V3.4.0 共 11 个留存快照；每个版本一个提交和标签。提交时间为本次导入时间，原始开发提交不存在，未伪造日期或缺失版本。完整性依据见 `history/snapshots.json`。\n\n'
          'V3.4.0 已通过隔离 Paper 26.3-143 的无 PAPI、PAPI 2.12.3、重启三轮测试；详情见保留的报告。真人客户端视觉验收尚未覆盖。仓库整理未修改插件源码与资源字节。\n\n'
          '尚未选择开源许可证；公开可见不等于授予额外的再分发许可。\n')
    git('add', '--all')
    git('commit', '-m', 'docs: document recovered version history and preserve development tooling')
    # Verify every imported file against its tagged commit, not just the latest checkout.
    checks = 0
    for stage in imports:
        for source in stage['sources']:
            for name, digest in source['files'].items():
                data = subprocess.check_output(['git', '-C', str(REPO), 'show', f"{stage['tag']}:{source['target']}/{name}"])
                assert hashlib.sha256(data).hexdigest() == digest
                checks += 1
    assert not git('status', '--porcelain')
    report = {'repository': str(REPO), 'commits': int(git('rev-list', '--count', 'HEAD')),
              'tags': git('tag', '--list').splitlines(), 'verified_snapshot_files': checks,
              'head': git('rev-parse', 'HEAD'), 'working_tree_clean': True}
    (OUT / 'github-history-import.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()

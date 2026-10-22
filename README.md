# 彩虹塔罗 · mc-plugin-tarot

面向 **Paper 26.3 / Java 25** 的塔罗卡牌插件，当前版本 **3.4.0**。

22 张塔罗牌，每张 1～5 星，R / SR / SSR / SP / UR 五种稀有度；包含抽卡包装、重复卡升级、个人收集册、收藏羁绊、粒子特效、命令权限与 PlaceholderAPI。

## 当前玩法

- 主手持牌长按右键 5 秒激活；普通效果持续现实 24 小时，换卡间隔 30 分钟，离线计时。
- 背包内将同名同星卡拖到另一张卡上升级，五星满级。
- 主手右键打开卡包、卡盒与整箱；每包 5 张且至少一张 SR 或以上，每盒 8 包，每箱 48 盒。
- 收集册按玩家保存；卡牌存入才计入进度，取出扣除进度，随身携带收集册获得对应奖励。
- `tarot.admin` 管理命令；`tarot.command.<命令>` 单项权限；`tarot.bypass.cd` 单独授予，可免效果期限和换卡冷却。
- PAPI 可选：`%tarot_effect_name%`、`%tarot_effect_remaining%`、`%tarot_switch_cd%`、`%tarot_album_progress%`、`%tarot_album_level%` 等。

## 文件与文档

- [插件源码与配置](plugin/)
- [资源包文件](resource-pack/)：将其内容打包，确保 ZIP 根目录包含 pack.mcmeta。
- [完整权限、安装与更新说明](docs/releases/彩虹塔罗V3.4.0更新说明.md)
- [PAPI 占位符](docs/releases/彩虹塔罗PAPI占位符.md)
- [历史说明与版本索引](docs/HISTORY.md)
- [服务端验证报告](docs/validation/)

## 构建

插件使用 Java 25、Paper 26.3 API 和 PlaceholderAPI 2.12.3 编译。原始 `plugin/build.ps1` 保留发布时的环境路径，需要按本机 Java、Paper libraries、PAPI 依赖和输出目录调整后执行。

PlaceholderAPI 只在使用占位符时需要安装；插件内嵌扩展，无需另下 eCloud 扩展。安装插件请停服替换 JAR 并保留玩家数据，不要与 `legacy/datapack` 同时启用。

## 版本历史与验证范围

按顺序恢复了最初数据包、V2、V2.1 及 V3.0.0 至 V3.4.0 共 11 个留存快照；每个版本一个提交和标签。提交时间为本次导入时间，原始开发提交不存在，未伪造日期或缺失版本。完整性依据见 `history/snapshots.json`。

V3.4.0 已通过隔离 Paper 26.3-143 的无 PAPI、PAPI 2.12.3、重启三轮测试；详情见保留的报告。真人客户端视觉验收尚未覆盖。仓库整理未修改插件源码与资源字节。

尚未选择开源许可证；公开可见不等于授予额外的再分发许可。

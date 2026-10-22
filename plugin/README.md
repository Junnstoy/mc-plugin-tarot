# 彩虹塔罗 V3.4.0

适用于 Paper 26.3。本版增加命令权限、时间绕过权限和 PlaceholderAPI 占位符，并将卡包、卡盒和整箱统一改为主手右键拆开。

## 安装与更新

1. 停止服务器，备份 `plugins/TarotRainbow` 数据目录。
2. 移走旧版 TarotRainbow JAR，将 `TarotRainbow-3.4.0.jar` 放入 `plugins`；保留原来的配置与玩家数据。
3. 如需占位符，安装 [PlaceholderAPI 2.12.3](https://github.com/PlaceholderAPI/PlaceholderAPI/releases/tag/2.12.3)。无需另装 eCloud 扩展；未安装 PAPI 时其他功能仍正常使用。
4. 启动服务器。资源包沿用现有版本，本次无需更新。不要用插件热卸载替代重启。

本次交付未自动替换正式服务器插件。

## 权限

| 权限节点 | 用途 | 默认 |
|---|---|---|
| `tarot.admin` | 全部塔罗命令 | OP |
| `tarot.command.help` | 查看帮助 | 所有人 |
| `tarot.command.status` | 查看当前效果和冷却 | 所有人 |
| `tarot.command.migrate` | 更新背包旧版物品 | 所有人 |
| `tarot.command.album` | 打开个人收集册 | 所有人 |
| `tarot.command.collection` | 查看收集进度 | 所有人 |
| `tarot.command.aura` | 切换收集册粒子 | 所有人 |
| `tarot.command.give` | 发放卡牌、包装与收集册 | 需授予 |
| `tarot.command.reset` | 重置冷却和效果 | 需授予 |
| `tarot.command.reset.cd` | 单独允许重置冷却 | 需授予 |
| `tarot.command.reset.buffer` | 单独允许移除效果 | 需授予 |
| `tarot.command.selftest` | 运行诊断测试 | 需授予 |
| `tarot.bypass.cd` | 卡牌效果永久、换卡无冷却 | 需单独授予 |

帮助与 Tab 补全根据玩家权限过滤。`tarot.admin` 授予所有命令，即使单项命令权限未授予；它不包含 `tarot.bypass.cd`。仅为普通玩家授权某条管理命令时，授予对应节点即可。

拥有 `tarot.bypass.cd` 期间，已经激活的卡牌不会到期，可以立即换卡；原版状态图标显示无限时间。仍需主手长按右键 5 秒完成施法。移除权限后恢复该次激活原本的 24 小时效果期限和 30 分钟换卡期限，时间从原激活时刻计算；已超过期限的效果会清除。重启会保留相关状态，玩家上线时重新核对实际权限。

若已安装 LuckPerms，可参考：

```text
/lp user 玩家名 permission set tarot.admin true
/lp user 玩家名 permission set tarot.bypass.cd true
/lp group default permission set tarot.command.album true
```

## PAPI 占位符

| 占位符 | 含义 |
|---|---|
| `%tarot_effect_name%` | 当前效果卡牌名称 |
| `%tarot_effect_level%` | 当前效果星级 |
| `%tarot_effect_remaining%` | 效果剩余时间，或“永久” |
| `%tarot_switch_cd%` | 换卡冷却剩余时间 |
| `%tarot_album_progress%` | 收集进度，例如 22/110 |
| `%tarot_album_level%` | 收集册等级，0～6；全 110 种为 6 |

完整字段、秒数、各星级进度、羁绊与返回值约定见同目录《彩虹塔罗PAPI占位符.md》。测试示例：`/papi parse me %tarot_effect_name%`。

## 拆包与验证

卡包、卡盒、整箱均为主手右键拆开，无需潜行。最后一个包装被消耗后腾出的槽位可以接收产物；空间不足时整体取消，保留原包装，避免产物消失。旧物品说明会迁移为新操作提示。

已完成隔离 Paper 26.3-143 服务端验证：无 PAPI 启动、安装 PAPI 2.12.3 启动、再次重启三轮均通过。覆盖命令权限、真实 PAPI 解析与异步快照、时间绕过授权及撤销、状态持久化、拆包容量、卡牌充能与既有收集册回归。交付 JAR 与测试 JAR 字节一致。未进行真人客户端视觉验收。

## 源码构建

源码包包含 Java 源码、配置与现有 Windows 构建脚本。脚本使用 Java 25、Paper 26.3 服务端 libraries，并从项目相邻 `deps/PlaceholderAPI-2.12.3.jar` 读取编译依赖；本机 Java、服务端依赖和输出目录路径需要按环境调整。PAPI 编译依赖可从 [官方 Maven 仓库](https://repo.extendedclip.com/content/repositories/placeholderapi/me/clip/placeholderapi/2.12.3/placeholderapi-2.12.3.jar) 获取，运行服务端时使用完整发行版 JAR。

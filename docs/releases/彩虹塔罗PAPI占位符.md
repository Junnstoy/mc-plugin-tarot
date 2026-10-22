# 彩虹塔罗 V3.4.0 · PlaceholderAPI 占位符

服务器安装 PlaceholderAPI 后，塔罗插件自动注册 `%tarot_…%`。不需要下载额外的 eCloud 扩展；没有安装 PlaceholderAPI 时，塔罗插件其他功能仍可使用。可将下列占位符放入支持 PAPI 的计分板、Tab、菜单或全息文字插件。

在游戏中验证：`/papi parse me %tarot_effect_name%`。插件内嵌扩展会保留在 `/papi reload` 后；更换塔罗插件 JAR 请重启服务器。注册方式参考 [PlaceholderAPI 官方扩展文档](https://wiki.placeholderapi.com/developers/creating-a-placeholderexpansion/)。

## 卡牌效果与换卡冷却

| 占位符 | 含义 | 返回示例 |
|---|---|---|
| `%tarot_effect_active%` | 当前是否有有效塔罗效果 | `true` / `false` |
| `%tarot_effect_name%` | 当前效果的卡牌名称，无效果时为“无” | `世界` |
| `%tarot_effect_id%` | 当前卡牌编号，无效果时为 0 | `22` |
| `%tarot_effect_level%` | 当前卡牌星级，无效果时为 0 | `5` |
| `%tarot_effect_rarity%` | 当前卡牌稀有度，无效果时为“无” | `UR` |
| `%tarot_effect_remaining%` | 效果剩余时间，格式为时:分:秒 | `23:59:58` |
| `%tarot_effect_remaining_seconds%` | 效果剩余秒数，向上取整 | `86398` |
| `%tarot_switch_cd%` | 换卡冷却剩余时间 | `00:29:58` |
| `%tarot_switch_cd_seconds%` | 换卡冷却剩余秒数 | `1798` |
| `%tarot_bypass_cd%` | 是否拥有时间与冷却绕过权限 | `true` / `false` |

拥有 `tarot.bypass.cd` 且已有卡牌效果时，`effect_remaining` 返回 `永久`，`effect_remaining_seconds` 返回 `-1`，换卡冷却返回 `00:00:00` / `0`。尚未激活卡牌时，即使拥有绕过权限，效果剩余时间仍为 `00:00:00` / `0`。

兼容别名：`card_name`、`card_id`、`card_level`、`card_rarity` 对应同名 `effect_` 字段；`effect_time` 对应 `effect_remaining`；`effect_seconds` 对应 `effect_remaining_seconds`；`cooldown`、`cooldown_seconds` 对应换卡冷却。

## 收集册整体进度

| 占位符 | 含义 | 返回示例 |
|---|---|---|
| `%tarot_album_count%` | 当前存入册内的不同卡牌与星级组合数量 | `22` |
| `%tarot_album_total%` | 可收藏总数 | `110` |
| `%tarot_album_progress%` | 已收藏 / 总数 | `22/110` |
| `%tarot_album_percent%` | 完成百分比，不带百分号，固定两位小数 | `20.00` |
| `%tarot_album_percentage%` | 完成百分比，带百分号 | `20.00%` |
| `%tarot_album_level%` | 收集册粒子等级，0～6 | `3` |
| `%tarot_album_carried%` | 当前是否随身携带实体收集册 | `true` / `false` |
| `%tarot_album_rewards_active%` | 当前是否携带、满足生效条件且已解锁属性奖励 | `true` / `false` |
| `%tarot_album_particles_enabled%` | 玩家是否打开了收集册粒子开关 | `true` / `false` |
| `%tarot_album_particles_active%` | 携带、存活且非旁观、开关打开，并有已解锁粒子等级 | `true` / `false` |
| `%tarot_album_completed_stars%` | 已集齐 22 张的星级，以逗号分隔；没有时为空 | `1,3,5` |
| `%tarot_album_milestones%` | 已达成的收藏数量奖励档数，0～6 | `2` |
| `%tarot_album_bonds%` | 已达成的羁绊种数，0～6 | `6` |

`album_level` 与 `album_particle_level` 含义相同：0 表示尚未集齐任何星级；1～5 表示已集齐全套的最高星级；6 表示全部 110 种集齐。比如只集齐三星全套时为 3，并不要求先集齐一、二星。取出卡牌后，进度及等级重新计算。

`album_particles_active` 表示具备显示资格；隐身、正在释放卡牌技能、圆环停顿等阶段仍可能暂时不绘制粒子。`album_particles_enabled` 仅表示个人开关，不能单独用来判断奖励或粒子是否生效。

## 各星级进度

把下面示例的 `1` 替换为 `1`～`5`：

| 占位符 | 含义 | 返回示例 |
|---|---|---|
| `%tarot_album_star_1_count%` | 一星已收藏数量 | `15` |
| `%tarot_album_star_1_progress%` | 一星收藏进度 | `15/22` |
| `%tarot_album_star_1_percent%` | 一星完成百分比，不带百分号 | `68.18` |
| `%tarot_album_star_1_complete%` | 是否集齐一星 22 张 | `false` |

## 羁绊等级

| 占位符 | 羁绊 | 数值范围 |
|---|---|---|
| `%tarot_album_bond_heart_level%` | 心之守护 | 0～5 |
| `%tarot_album_bond_guidance_level%` | 旅途启示 | 0～5 |
| `%tarot_album_bond_resolve_level%` | 勇气与裁决 | 0～5 |
| `%tarot_album_bond_balance_level%` | 静心修行 | 0～5 |
| `%tarot_album_bond_change_level%` | 命运蜕变 | 0～5 |
| `%tarot_album_bond_cosmos_level%` | 星辰终章 | 0～5 |

等级取羁绊所需卡牌各自“已收藏最高星级”中的最低值；0 表示尚未集齐。也可使用短别名 `%tarot_bond_heart_level%` 等。这里显示已收藏进度，是否真正获得属性还需结合 `%tarot_album_rewards_active%`。

## 计分板文字示例

```text
当前塔罗：%tarot_effect_name% · %tarot_effect_level% 星
效果剩余：%tarot_effect_remaining%
换卡冷却：%tarot_switch_cd%
收藏进度：%tarot_album_progress%（%tarot_album_percentage%）
收集册等级：%tarot_album_level%
五星进度：%tarot_album_star_5_progress%
星辰终章：%tarot_album_bond_cosmos_level% 级
```

占位符不带颜色，颜色可在使用它的插件中配置。布尔字段统一返回英文小写 `true` / `false`。没有玩家上下文时返回空状态；未定义的占位符保持原样。异步请求使用线程安全的快照，首次尚未缓存的玩家先返回空状态并安排加载；后续刷新显示实际数据。离线不携带收集册，进度仍可查询，常规效果与冷却按现实时间递减。

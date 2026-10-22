function tarot:clear
item replace entity @s weapon.mainhand with minecraft:air
item replace entity @s weapon.offhand with minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{card:22,level:4}},minecraft:item_name={"text":"UR · 世界 ★4","color":"gold","italic":false},minecraft:lore=[{"text":"副手潜行激活 | 持续15秒","color":"gray","italic":false},{"text":"冷却45秒 | 重复卡可升级","color":"gray","italic":false}]]
tellraw @s {"text":"升级成功：世界 ★4","color":"gold","italic":false}

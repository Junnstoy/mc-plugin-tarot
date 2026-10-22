function tarot:clear
item replace entity @s weapon.mainhand with minecraft:air
item replace entity @s weapon.offhand with minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{card:21,level:2}},minecraft:item_name={"text":"UR · 审判 ★2","color":"gold","italic":false},minecraft:lore=[{"text":"副手潜行激活 | 持续10秒","color":"gray","italic":false},{"text":"冷却55秒 | 重复卡可升级","color":"gray","italic":false}]]
tellraw @s {"text":"升级成功：审判 ★2","color":"gold","italic":false}

function tarot:clear
item replace entity @s weapon.mainhand with minecraft:air
item replace entity @s weapon.offhand with minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{card:17,level:2}},minecraft:item_name={"text":"SP · 高塔 ★2","color":"light_purple","italic":false},minecraft:lore=[{"text":"副手潜行激活 | 持续10秒","color":"gray","italic":false},{"text":"冷却55秒 | 重复卡可升级","color":"gray","italic":false}]]
tellraw @s {"text":"升级成功：高塔 ★2","color":"gold","italic":false}

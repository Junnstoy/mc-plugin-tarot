function tarot:clear
item replace entity @s weapon.mainhand with minecraft:air
item replace entity @s weapon.offhand with minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{card:12,level:5}},minecraft:item_name={"text":"SR · 正义 ★5","color":"green","italic":false},minecraft:lore=[{"text":"副手潜行激活 | 持续18秒","color":"gray","italic":false},{"text":"冷却40秒 | 重复卡可升级","color":"gray","italic":false}]]
tellraw @s {"text":"升级成功：正义 ★5","color":"gold","italic":false}

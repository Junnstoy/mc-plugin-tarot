function tarot:clear
item replace entity @s weapon.mainhand with minecraft:air
item replace entity @s weapon.offhand with minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{card:10,level:3}},minecraft:item_name={"text":"R · 隐者 ★3","color":"white","italic":false},minecraft:lore=[{"text":"副手潜行激活 | 持续12秒","color":"gray","italic":false},{"text":"冷却50秒 | 重复卡可升级","color":"gray","italic":false}]]
tellraw @s {"text":"升级成功：隐者 ★3","color":"gold","italic":false}

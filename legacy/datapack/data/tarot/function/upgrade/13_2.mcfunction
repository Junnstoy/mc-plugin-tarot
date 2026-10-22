function tarot:clear
item replace entity @s weapon.mainhand with minecraft:air
item replace entity @s weapon.offhand with minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{card:13,level:3}},minecraft:item_model="tarot:card_13",minecraft:enchantment_glint_override=false,minecraft:item_name={"text":"R · 倒吊人 ★3","color":"white","italic":false},minecraft:lore=[{"text":"副手潜行激活 | 持续12秒","color":"gray","italic":false},{"text":"冷却50秒 | 重复卡可升级","color":"gray","italic":false}]]
tellraw @s {"text":"升级成功：倒吊人 ★3","color":"gold","italic":false}
playsound minecraft:block.enchantment_table.use player @s ~ ~ ~ 0.6 1.2
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.6 1.4
particle minecraft:enchanted_hit ~ ~1 ~ .4 .6 .4 .1 32 normal @a[distance=..24]

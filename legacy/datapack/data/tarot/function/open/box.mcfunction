item replace entity @s weapon.offhand with minecraft:air
give @s minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{package:"pack"}},minecraft:item_model="tarot:pack",minecraft:item_name={"text":"塔罗卡包 · 5张","color":"gold","italic":false}] 8
playsound minecraft:item.bundle.drop_contents player @s ~ ~ ~ 0.6 0.8
particle minecraft:enchant ~ ~1 ~ .4 .4 .4 .1 20 normal @s

item replace entity @s weapon.offhand with minecraft:air
give @s minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{package:"box"}},minecraft:item_model="tarot:box",minecraft:item_name={"text":"塔罗卡盒 · 8包","color":"gold","italic":false}] 48
playsound minecraft:item.bundle.drop_contents player @s ~ ~ ~ 0.6 0.8
particle minecraft:enchant ~ ~1 ~ .4 .4 .4 .1 20 normal @s

function tarot:clear
attribute @s minecraft:luck modifier add tarot:luck 4.0000 add_value
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.2000 add_multiplied_base
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 11
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"命运之轮 · 技能已激活","color":"light_purple","italic":false}

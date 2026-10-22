function tarot:clear
attribute @s minecraft:luck modifier add tarot:luck 8.4000 add_value
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.4200 add_multiplied_base
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 11
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"命运之轮 · 技能已激活","color":"light_purple","italic":false}

function tarot:clear
attribute @s minecraft:sneaking_speed modifier add tarot:sneaking_speed 0.5250 add_value
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 2.1000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 19
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"月亮 · 技能已激活","color":"green","italic":false}

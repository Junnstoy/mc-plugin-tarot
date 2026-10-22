function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 3.0000 add_value
attribute @s minecraft:armor modifier add tarot:armor 4.0000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.2000 add_multiplied_base
attribute @s minecraft:max_health modifier add tarot:max_health 6.0000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 22
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"世界 · 技能已激活","color":"gold","italic":false}
function tarot:fx/22_1

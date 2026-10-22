function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 5.2500 add_value
attribute @s minecraft:armor modifier add tarot:armor 7.0000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.3500 add_multiplied_base
attribute @s minecraft:max_health modifier add tarot:max_health 10.5000 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 22
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"世界 · 技能已激活","color":"gold","italic":false}
function tarot:fx/22_4

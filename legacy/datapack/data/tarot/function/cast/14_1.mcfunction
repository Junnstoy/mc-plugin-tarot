function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 5.0000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.1000 add_multiplied_base
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 14
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"死神 · 技能已激活","color":"light_purple","italic":false}
function tarot:fx/14_1

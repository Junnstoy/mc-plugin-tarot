function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 8.7500 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.1750 add_multiplied_base
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 14
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"死神 · 技能已激活","color":"light_purple","italic":false}
function tarot:fx/14_4

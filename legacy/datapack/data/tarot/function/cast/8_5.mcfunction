function tarot:clear
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.4200 add_multiplied_base
attribute @s minecraft:step_height modifier add tarot:step_height 1.0500 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 8
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"战车 · 技能已激活","color":"green","italic":false}
function tarot:fx/8_5

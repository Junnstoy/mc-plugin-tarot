function tarot:clear
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.3500 add_multiplied_base
attribute @s minecraft:step_height modifier add tarot:step_height 0.8750 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 8
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"战车 · 技能已激活","color":"green","italic":false}
function tarot:fx/8_4

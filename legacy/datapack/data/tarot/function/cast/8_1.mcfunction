function tarot:clear
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.2000 add_multiplied_base
attribute @s minecraft:step_height modifier add tarot:step_height 0.5000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 8
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"战车 · 技能已激活","color":"green","italic":false}

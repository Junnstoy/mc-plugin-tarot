function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 4.2000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.1050 add_multiplied_base
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 7
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"恋人 · 技能已激活","color":"white","italic":false}
function tarot:fx/7_5

function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 2.9000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.0725 add_multiplied_base
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 7
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"恋人 · 技能已激活","color":"white","italic":false}

function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 3.5000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.0875 add_multiplied_base
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 7
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"恋人 · 技能已激活","color":"white","italic":false}

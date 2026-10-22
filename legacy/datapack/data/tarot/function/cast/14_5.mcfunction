function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 10.5000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.2100 add_multiplied_base
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 14
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"死神 · 技能已激活","color":"light_purple","italic":false}

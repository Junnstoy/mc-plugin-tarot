function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 3.6000 add_value
attribute @s minecraft:armor modifier add tarot:armor 4.8000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.2400 add_multiplied_base
attribute @s minecraft:max_health modifier add tarot:max_health 7.2000 add_value
scoreboard players set @s tr_cd 1100
scoreboard players set @s tr_time 200
scoreboard players set @s tr_active 22
scoreboard players set @s tr_alvl 2
tellraw @s {"text":"世界 · 技能已激活","color":"gold","italic":false}

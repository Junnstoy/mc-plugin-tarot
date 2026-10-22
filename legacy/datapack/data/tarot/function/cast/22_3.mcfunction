function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 4.3500 add_value
attribute @s minecraft:armor modifier add tarot:armor 5.8000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.2900 add_multiplied_base
attribute @s minecraft:max_health modifier add tarot:max_health 8.7000 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 22
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"世界 · 技能已激活","color":"gold","italic":false}

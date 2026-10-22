function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 6.3000 add_value
attribute @s minecraft:armor modifier add tarot:armor 8.4000 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.4200 add_multiplied_base
attribute @s minecraft:max_health modifier add tarot:max_health 12.6000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 22
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"世界 · 技能已激活","color":"gold","italic":false}

function tarot:clear
attribute @s minecraft:sneaking_speed modifier add tarot:sneaking_speed 0.4375 add_value
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 1.7500 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 19
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"月亮 · 技能已激活","color":"green","italic":false}

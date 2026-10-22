function tarot:clear
attribute @s minecraft:sneaking_speed modifier add tarot:sneaking_speed 0.3000 add_value
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 1.2000 add_value
scoreboard players set @s tr_cd 1100
scoreboard players set @s tr_time 200
scoreboard players set @s tr_active 19
scoreboard players set @s tr_alvl 2
tellraw @s {"text":"月亮 · 技能已激活","color":"green","italic":false}

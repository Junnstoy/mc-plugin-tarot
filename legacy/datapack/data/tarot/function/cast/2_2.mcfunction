function tarot:clear
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.1800 add_multiplied_base
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 1.2000 add_value
scoreboard players set @s tr_cd 1100
scoreboard players set @s tr_time 200
scoreboard players set @s tr_active 2
scoreboard players set @s tr_alvl 2
tellraw @s {"text":"魔术师 · 技能已激活","color":"green","italic":false}

function tarot:clear
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.2625 add_multiplied_base
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 1.7500 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 2
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"魔术师 · 技能已激活","color":"green","italic":false}

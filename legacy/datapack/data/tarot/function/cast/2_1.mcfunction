function tarot:clear
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.1500 add_multiplied_base
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 1.0000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 2
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"魔术师 · 技能已激活","color":"green","italic":false}

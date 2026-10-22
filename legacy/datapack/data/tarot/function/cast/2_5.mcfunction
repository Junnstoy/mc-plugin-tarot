function tarot:clear
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.3150 add_multiplied_base
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 2.1000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 2
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"魔术师 · 技能已激活","color":"green","italic":false}
function tarot:fx/2_5

function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 3.0000 add_value
attribute @s minecraft:attack_knockback modifier add tarot:attack_knockback 0.3000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 9
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"力量 · 技能已激活","color":"aqua","italic":false}
function tarot:fx/9_1

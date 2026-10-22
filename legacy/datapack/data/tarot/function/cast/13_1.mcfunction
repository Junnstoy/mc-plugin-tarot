function tarot:clear
attribute @s minecraft:safe_fall_distance modifier add tarot:safe_fall_distance 4.0000 add_value
attribute @s minecraft:jump_strength modifier add tarot:jump_strength 0.1500 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 13
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"倒吊人 · 技能已激活","color":"white","italic":false}
function tarot:fx/13_1

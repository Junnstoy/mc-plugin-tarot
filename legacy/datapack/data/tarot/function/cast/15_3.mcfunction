function tarot:clear
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.1450 add_multiplied_base
attribute @s minecraft:armor modifier add tarot:armor 1.4500 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 15
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"节制 · 技能已激活","color":"white","italic":false}
function tarot:fx/15_3

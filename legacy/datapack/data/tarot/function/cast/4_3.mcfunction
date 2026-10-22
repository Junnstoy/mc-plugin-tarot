function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 8.7000 add_value
attribute @s minecraft:armor modifier add tarot:armor 2.9000 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 4
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"女皇 · 技能已激活","color":"aqua","italic":false}
function tarot:fx/4_3

function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 8.0000 add_value
attribute @s minecraft:armor modifier add tarot:armor 2.0000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 20
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"太阳 · 技能已激活","color":"aqua","italic":false}
function tarot:fx/20_1

function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 14.0000 add_value
attribute @s minecraft:armor modifier add tarot:armor 3.5000 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 20
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"太阳 · 技能已激活","color":"aqua","italic":false}
function tarot:fx/20_4

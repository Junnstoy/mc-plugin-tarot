function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 16.8000 add_value
attribute @s minecraft:armor modifier add tarot:armor 4.2000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 20
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"太阳 · 技能已激活","color":"aqua","italic":false}
function tarot:fx/20_5

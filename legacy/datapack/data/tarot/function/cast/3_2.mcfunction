function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 4.8000 add_value
attribute @s minecraft:luck modifier add tarot:luck 1.2000 add_value
scoreboard players set @s tr_cd 1100
scoreboard players set @s tr_time 200
scoreboard players set @s tr_active 3
scoreboard players set @s tr_alvl 2
tellraw @s {"text":"女祭司 · 技能已激活","color":"green","italic":false}
function tarot:fx/3_2

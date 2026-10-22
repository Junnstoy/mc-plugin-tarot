function tarot:clear
attribute @s minecraft:armor modifier add tarot:armor 8.7000 add_value
attribute @s minecraft:attack_knockback modifier add tarot:attack_knockback 1.1600 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 17
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"高塔 · 技能已激活","color":"light_purple","italic":false}
function tarot:fx/17_3

function tarot:clear
attribute @s minecraft:armor modifier add tarot:armor 12.6000 add_value
attribute @s minecraft:attack_knockback modifier add tarot:attack_knockback 1.6800 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 17
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"高塔 · 技能已激活","color":"light_purple","italic":false}
function tarot:fx/17_5

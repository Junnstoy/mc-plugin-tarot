function tarot:clear
attribute @s minecraft:armor modifier add tarot:armor 7.2000 add_value
attribute @s minecraft:attack_knockback modifier add tarot:attack_knockback 0.9600 add_value
scoreboard players set @s tr_cd 1100
scoreboard players set @s tr_time 200
scoreboard players set @s tr_active 17
scoreboard players set @s tr_alvl 2
tellraw @s {"text":"高塔 · 技能已激活","color":"light_purple","italic":false}

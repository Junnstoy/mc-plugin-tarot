function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 7.2000 add_value
attribute @s minecraft:armor modifier add tarot:armor 2.4000 add_value
scoreboard players set @s tr_cd 1100
scoreboard players set @s tr_time 200
scoreboard players set @s tr_active 4
scoreboard players set @s tr_alvl 2
tellraw @s {"text":"女皇 · 技能已激活","color":"aqua","italic":false}

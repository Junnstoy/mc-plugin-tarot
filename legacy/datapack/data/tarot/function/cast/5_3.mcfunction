function tarot:clear
attribute @s minecraft:armor modifier add tarot:armor 7.2500 add_value
attribute @s minecraft:knockback_resistance modifier add tarot:knockback_resistance 0.1450 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 5
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"皇帝 · 技能已激活","color":"aqua","italic":false}

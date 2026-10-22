function tarot:clear
attribute @s minecraft:armor modifier add tarot:armor 10.5000 add_value
attribute @s minecraft:knockback_resistance modifier add tarot:knockback_resistance 0.2100 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 5
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"皇帝 · 技能已激活","color":"aqua","italic":false}

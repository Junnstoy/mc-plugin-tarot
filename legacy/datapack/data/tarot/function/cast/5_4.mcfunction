function tarot:clear
attribute @s minecraft:armor modifier add tarot:armor 8.7500 add_value
attribute @s minecraft:knockback_resistance modifier add tarot:knockback_resistance 0.1750 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 5
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"皇帝 · 技能已激活","color":"aqua","italic":false}

function tarot:clear
attribute @s minecraft:armor modifier add tarot:armor 5.0000 add_value
attribute @s minecraft:knockback_resistance modifier add tarot:knockback_resistance 0.1000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 5
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"皇帝 · 技能已激活","color":"aqua","italic":false}

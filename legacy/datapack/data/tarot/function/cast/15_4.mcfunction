function tarot:clear
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.1750 add_multiplied_base
attribute @s minecraft:armor modifier add tarot:armor 1.7500 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 15
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"节制 · 技能已激活","color":"white","italic":false}

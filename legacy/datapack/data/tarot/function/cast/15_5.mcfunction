function tarot:clear
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.2100 add_multiplied_base
attribute @s minecraft:armor modifier add tarot:armor 2.1000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 15
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"节制 · 技能已激活","color":"white","italic":false}

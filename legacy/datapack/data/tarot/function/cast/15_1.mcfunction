function tarot:clear
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.1000 add_multiplied_base
attribute @s minecraft:armor modifier add tarot:armor 1.0000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 15
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"节制 · 技能已激活","color":"white","italic":false}

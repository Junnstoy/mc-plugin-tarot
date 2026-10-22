function tarot:clear
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.1450 add_multiplied_base
attribute @s minecraft:jump_strength modifier add tarot:jump_strength 0.1450 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 1
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"愚者 · 技能已激活","color":"white","italic":false}

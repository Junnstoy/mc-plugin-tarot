function tarot:clear
attribute @s minecraft:sneaking_speed modifier add tarot:sneaking_speed 0.1500 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.0500 add_multiplied_base
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 10
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"隐者 · 技能已激活","color":"white","italic":false}

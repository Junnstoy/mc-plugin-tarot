function tarot:clear
attribute @s minecraft:sneaking_speed modifier add tarot:sneaking_speed 0.3150 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.1050 add_multiplied_base
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 10
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"隐者 · 技能已激活","color":"white","italic":false}
function tarot:fx/10_5

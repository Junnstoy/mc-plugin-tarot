function tarot:clear
attribute @s minecraft:sneaking_speed modifier add tarot:sneaking_speed 0.2625 add_value
attribute @s minecraft:movement_speed modifier add tarot:movement_speed 0.0875 add_multiplied_base
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 10
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"隐者 · 技能已激活","color":"white","italic":false}
function tarot:fx/10_4

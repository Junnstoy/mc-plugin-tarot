function tarot:clear
attribute @s minecraft:luck modifier add tarot:luck 7.0000 add_value
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.3500 add_multiplied_base
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 11
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"命运之轮 · 技能已激活","color":"light_purple","italic":false}
function tarot:fx/11_4

function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 7.0000 add_value
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.1750 add_multiplied_base
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 16
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"恶魔 · 技能已激活","color":"aqua","italic":false}
function tarot:fx/16_4

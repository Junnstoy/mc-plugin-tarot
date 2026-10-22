function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 8.7500 add_value
attribute @s minecraft:armor modifier add tarot:armor 10.5000 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 21
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"审判 · 技能已激活","color":"gold","italic":false}
function tarot:fx/21_4

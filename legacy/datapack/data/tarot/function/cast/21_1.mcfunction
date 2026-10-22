function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 5.0000 add_value
attribute @s minecraft:armor modifier add tarot:armor 6.0000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 21
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"审判 · 技能已激活","color":"gold","italic":false}
function tarot:fx/21_1

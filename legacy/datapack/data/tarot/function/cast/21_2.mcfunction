function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 6.0000 add_value
attribute @s minecraft:armor modifier add tarot:armor 7.2000 add_value
scoreboard players set @s tr_cd 1100
scoreboard players set @s tr_time 200
scoreboard players set @s tr_active 21
scoreboard players set @s tr_alvl 2
tellraw @s {"text":"审判 · 技能已激活","color":"gold","italic":false}

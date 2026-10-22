function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 7.2500 add_value
attribute @s minecraft:armor modifier add tarot:armor 8.7000 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 21
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"审判 · 技能已激活","color":"gold","italic":false}

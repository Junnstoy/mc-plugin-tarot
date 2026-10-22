function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 10.5000 add_value
attribute @s minecraft:armor modifier add tarot:armor 12.6000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 21
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"审判 · 技能已激活","color":"gold","italic":false}

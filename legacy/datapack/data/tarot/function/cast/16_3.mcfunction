function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 5.8000 add_value
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.1450 add_multiplied_base
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 16
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"恶魔 · 技能已激活","color":"aqua","italic":false}

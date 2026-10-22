function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 4.0000 add_value
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.1000 add_multiplied_base
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 16
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"恶魔 · 技能已激活","color":"aqua","italic":false}

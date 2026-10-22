function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 8.4000 add_value
attribute @s minecraft:attack_speed modifier add tarot:attack_speed 0.2100 add_multiplied_base
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 16
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"恶魔 · 技能已激活","color":"aqua","italic":false}

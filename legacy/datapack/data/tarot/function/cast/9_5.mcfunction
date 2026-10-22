function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 6.3000 add_value
attribute @s minecraft:attack_knockback modifier add tarot:attack_knockback 0.6300 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 9
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"力量 · 技能已激活","color":"aqua","italic":false}

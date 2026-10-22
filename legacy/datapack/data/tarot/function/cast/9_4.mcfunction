function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 5.2500 add_value
attribute @s minecraft:attack_knockback modifier add tarot:attack_knockback 0.5250 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 9
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"力量 · 技能已激活","color":"aqua","italic":false}

function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 1.7500 add_value
attribute @s minecraft:armor modifier add tarot:armor 3.5000 add_value
scoreboard players set @s tr_cd 900
scoreboard players set @s tr_time 300
scoreboard players set @s tr_active 12
scoreboard players set @s tr_alvl 4
tellraw @s {"text":"正义 · 技能已激活","color":"green","italic":false}

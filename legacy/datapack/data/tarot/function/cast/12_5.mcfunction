function tarot:clear
attribute @s minecraft:attack_damage modifier add tarot:attack_damage 2.1000 add_value
attribute @s minecraft:armor modifier add tarot:armor 4.2000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 12
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"正义 · 技能已激活","color":"green","italic":false}
function tarot:fx/12_5

function tarot:clear
attribute @s minecraft:luck modifier add tarot:luck 2.9000 add_value
attribute @s minecraft:safe_fall_distance modifier add tarot:safe_fall_distance 4.3500 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 18
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"星星 · 技能已激活","color":"white","italic":false}

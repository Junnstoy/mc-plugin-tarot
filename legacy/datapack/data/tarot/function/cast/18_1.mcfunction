function tarot:clear
attribute @s minecraft:luck modifier add tarot:luck 2.0000 add_value
attribute @s minecraft:safe_fall_distance modifier add tarot:safe_fall_distance 3.0000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 18
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"星星 · 技能已激活","color":"white","italic":false}

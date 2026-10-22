function tarot:clear
attribute @s minecraft:luck modifier add tarot:luck 4.2000 add_value
attribute @s minecraft:safe_fall_distance modifier add tarot:safe_fall_distance 6.3000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 18
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"星星 · 技能已激活","color":"white","italic":false}

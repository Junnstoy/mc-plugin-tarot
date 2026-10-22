function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 8.4000 add_value
attribute @s minecraft:luck modifier add tarot:luck 2.1000 add_value
scoreboard players set @s tr_cd 800
scoreboard players set @s tr_time 360
scoreboard players set @s tr_active 3
scoreboard players set @s tr_alvl 5
tellraw @s {"text":"女祭司 · 技能已激活","color":"green","italic":false}

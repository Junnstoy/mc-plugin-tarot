function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 5.8000 add_value
attribute @s minecraft:luck modifier add tarot:luck 1.4500 add_value
scoreboard players set @s tr_cd 1000
scoreboard players set @s tr_time 240
scoreboard players set @s tr_active 3
scoreboard players set @s tr_alvl 3
tellraw @s {"text":"女祭司 · 技能已激活","color":"green","italic":false}

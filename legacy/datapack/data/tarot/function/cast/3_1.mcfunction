function tarot:clear
attribute @s minecraft:max_health modifier add tarot:max_health 4.0000 add_value
attribute @s minecraft:luck modifier add tarot:luck 1.0000 add_value
scoreboard players set @s tr_cd 1200
scoreboard players set @s tr_time 160
scoreboard players set @s tr_active 3
scoreboard players set @s tr_alvl 1
tellraw @s {"text":"女祭司 · 技能已激活","color":"green","italic":false}

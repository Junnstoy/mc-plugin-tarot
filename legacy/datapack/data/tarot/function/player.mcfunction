scoreboard players add @s tr_cd 0
scoreboard players add @s tr_edge 0
scoreboard players add @s tr_time 0
scoreboard players enable @s tarot_upgrade
execute if score @s tr_cd matches 1.. run scoreboard players remove @s tr_cd 1
execute if score @s tr_time matches 1.. run scoreboard players remove @s tr_time 1
scoreboard players set @s tr_card 0
scoreboard players set @s tr_level 0
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:1}}] run scoreboard players set @s tr_card 1
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:2}}] run scoreboard players set @s tr_card 2
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:3}}] run scoreboard players set @s tr_card 3
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:4}}] run scoreboard players set @s tr_card 4
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:5}}] run scoreboard players set @s tr_card 5
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:6}}] run scoreboard players set @s tr_card 6
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:7}}] run scoreboard players set @s tr_card 7
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:8}}] run scoreboard players set @s tr_card 8
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:9}}] run scoreboard players set @s tr_card 9
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:10}}] run scoreboard players set @s tr_card 10
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:11}}] run scoreboard players set @s tr_card 11
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:12}}] run scoreboard players set @s tr_card 12
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:13}}] run scoreboard players set @s tr_card 13
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:14}}] run scoreboard players set @s tr_card 14
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:15}}] run scoreboard players set @s tr_card 15
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:16}}] run scoreboard players set @s tr_card 16
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:17}}] run scoreboard players set @s tr_card 17
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:18}}] run scoreboard players set @s tr_card 18
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:19}}] run scoreboard players set @s tr_card 19
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:20}}] run scoreboard players set @s tr_card 20
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:21}}] run scoreboard players set @s tr_card 21
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{card:22}}] run scoreboard players set @s tr_card 22
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{level:1}}] run scoreboard players set @s tr_level 1
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{level:2}}] run scoreboard players set @s tr_level 2
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{level:3}}] run scoreboard players set @s tr_level 3
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{level:4}}] run scoreboard players set @s tr_level 4
execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{tarot:{level:5}}] run scoreboard players set @s tr_level 5
execute if score @s tr_active matches 1.. unless score @s tr_active = @s tr_card run function tarot:clear
execute if score @s tr_active matches 1.. unless score @s tr_alvl = @s tr_level run function tarot:clear
execute if score @s tr_active matches 1.. if score @s tr_time matches 0 run function tarot:clear
execute if score @s tarot_upgrade matches 1.. run function tarot:upgrade
execute if predicate tarot:sneaking if score @s tr_edge matches 0 run function tarot:press
execute if predicate tarot:sneaking run scoreboard players set @s tr_edge 1
execute unless predicate tarot:sneaking run scoreboard players set @s tr_edge 0

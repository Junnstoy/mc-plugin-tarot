execute store result score @s tr_pick run random value 1..2
execute if score @s tr_pick matches 1 run function tarot:give/21_1
execute if score @s tr_pick matches 2 run function tarot:give/22_1
execute unless score @s tr_best matches 5.. run scoreboard players set @s tr_best 5

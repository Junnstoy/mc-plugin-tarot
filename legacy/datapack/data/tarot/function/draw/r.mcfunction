execute store result score @s tr_pick run random value 1..6
execute if score @s tr_pick matches 1 run function tarot:give/1_1
execute if score @s tr_pick matches 2 run function tarot:give/7_1
execute if score @s tr_pick matches 3 run function tarot:give/10_1
execute if score @s tr_pick matches 4 run function tarot:give/13_1
execute if score @s tr_pick matches 5 run function tarot:give/15_1
execute if score @s tr_pick matches 6 run function tarot:give/18_1
execute unless score @s tr_best matches 1.. run scoreboard players set @s tr_best 1

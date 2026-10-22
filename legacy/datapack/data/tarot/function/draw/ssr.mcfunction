execute store result score @s tr_pick run random value 1..5
execute if score @s tr_pick matches 1 run function tarot:give/4_1
execute if score @s tr_pick matches 2 run function tarot:give/5_1
execute if score @s tr_pick matches 3 run function tarot:give/9_1
execute if score @s tr_pick matches 4 run function tarot:give/16_1
execute if score @s tr_pick matches 5 run function tarot:give/20_1
execute unless score @s tr_best matches 3.. run scoreboard players set @s tr_best 3

execute store result score @s tr_pick run random value 1..3
execute if score @s tr_pick matches 1 run function tarot:give/11_1
execute if score @s tr_pick matches 2 run function tarot:give/14_1
execute if score @s tr_pick matches 3 run function tarot:give/17_1

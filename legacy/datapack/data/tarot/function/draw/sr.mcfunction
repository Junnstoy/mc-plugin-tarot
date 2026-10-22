execute store result score @s tr_pick run random value 1..6
execute if score @s tr_pick matches 1 run function tarot:give/2_1
execute if score @s tr_pick matches 2 run function tarot:give/3_1
execute if score @s tr_pick matches 3 run function tarot:give/6_1
execute if score @s tr_pick matches 4 run function tarot:give/8_1
execute if score @s tr_pick matches 5 run function tarot:give/12_1
execute if score @s tr_pick matches 6 run function tarot:give/19_1

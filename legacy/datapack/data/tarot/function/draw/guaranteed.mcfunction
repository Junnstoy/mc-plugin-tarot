execute store result score @s tr_roll run random value 1..10000
execute if score @s tr_roll matches 1..9400 run function tarot:draw/sr
execute if score @s tr_roll matches 9401..9900 run function tarot:draw/ssr
execute if score @s tr_roll matches 9901..9990 run function tarot:draw/sp
execute if score @s tr_roll matches 9991..10000 run function tarot:draw/ur

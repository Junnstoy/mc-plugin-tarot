execute store result score @s tr_roll run random value 1..10000
execute if score @s tr_roll matches 1..8000 run function tarot:draw/r
execute if score @s tr_roll matches 8001..9800 run function tarot:draw/sr
execute if score @s tr_roll matches 9801..9970 run function tarot:draw/ssr
execute if score @s tr_roll matches 9971..9995 run function tarot:draw/sp
execute if score @s tr_roll matches 9996..10000 run function tarot:draw/ur

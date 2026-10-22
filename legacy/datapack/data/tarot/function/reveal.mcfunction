playsound minecraft:item.bundle.drop_contents player @s ~ ~ ~ 0.6 1.2
execute if score @s tr_best matches 1 run function tarot:reveal/r
execute if score @s tr_best matches 2 run function tarot:reveal/sr
execute if score @s tr_best matches 3 run function tarot:reveal/ssr
execute if score @s tr_best matches 4 run function tarot:reveal/sp
execute if score @s tr_best matches 5 run function tarot:reveal/ur

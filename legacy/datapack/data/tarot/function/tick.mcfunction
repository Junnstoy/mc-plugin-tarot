execute if score #enabled tr_on matches 1 as @a at @s run function tarot:player
execute if score #enabled tr_on matches 0 as @a[scores={tr_active=1..}] run function tarot:clear

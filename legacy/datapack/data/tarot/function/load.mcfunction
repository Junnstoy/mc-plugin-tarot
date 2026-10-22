scoreboard objectives add tr_cd dummy
scoreboard objectives add tr_time dummy
scoreboard objectives add tr_edge dummy
scoreboard objectives add tr_card dummy
scoreboard objectives add tr_level dummy
scoreboard objectives add tr_active dummy
scoreboard objectives add tr_alvl dummy
scoreboard objectives add tr_roll dummy
scoreboard objectives add tr_pick dummy
scoreboard objectives add tr_on dummy
scoreboard objectives add tarot_upgrade trigger
execute unless score #enabled tr_on matches 0..1 run scoreboard players set #enabled tr_on 1
scoreboard objectives add tr_best dummy

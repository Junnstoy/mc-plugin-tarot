import pathlib,json,shutil,zipfile,math,re,hashlib
R=pathlib.Path('outputs');D=R/'tarot_26_3_v2';P=R/'tarot_resources_26_3'
shutil.copytree(R/'tarot_26_3',D,dirs_exist_ok=True)
F=D/'data/tarot/function'
def fn(n,s):
 p=F/(n+'.mcfunction');p.parent.mkdir(parents=True,exist_ok=True);p.write_text('\n'.join(s)+'\n',encoding='utf-8')
def append(n,s):
 p=F/(n+'.mcfunction');p.write_text(p.read_text(encoding='utf-8')+'\n'.join(s)+'\n',encoding='utf-8')
def sound(n,pitch=1,volume=.6):return f'playsound minecraft:{n} player @s ~ ~ ~ {volume} {pitch}'
rarities=['R','SR','SR','SSR','SSR','SR','R','SR','SSR','R','SP','SR','R','SP','R','SSR','SP','R','SR','SSR','UR','UR']
rgb={'R':[.8,.8,.9],'SR':[.25,1,.5],'SSR':[.2,.7,1],'SP':[.75,.25,1],'UR':[1,.75,.15]}
particles=['cloud','enchant','end_rod','heart','crit','enchant','heart','cloud','crit','ash','enchant','crit','cloud','soul','end_rod','flame','electric_spark','end_rod','enchant','flame','end_rod','enchant']
sounds=['block.amethyst_block.chime','block.enchantment_table.use','block.beacon.activate','block.amethyst_block.resonate','item.armor.equip_netherite','block.beacon.activate','block.amethyst_block.chime','entity.player.attack.sweep','entity.player.attack.strong','block.amethyst_block.chime','block.enchantment_table.use','block.anvil.place','entity.breeze.jump','entity.evoker.cast_spell','block.brewing_stand.brew','item.firecharge.use','entity.lightning_bolt.impact','block.amethyst_block.resonate','block.enchantment_table.use','block.beacon.activate','block.end_portal_frame.fill','block.beacon.activate']
for p in F.rglob('*.mcfunction'):
 s=p.read_text(encoding='utf-8')
 s=re.sub(r'minecraft:paper\[minecraft:max_stack_size=1,minecraft:custom_data=\{tarot:\{card:(\d+),level:(\d+)\}\}',lambda m:m.group(0)+f',minecraft:item_model="tarot:card_{m[1]}",minecraft:enchantment_glint_override='+('true' if int(m[2])==5 else 'false'),s)
 s=re.sub(r'minecraft:paper\[minecraft:max_stack_size=1,minecraft:custom_data=\{tarot:\{package:"(\w+)"\}\}',lambda m:m.group(0)+f',minecraft:item_model="tarot:{m[1]}"',s)
 p.write_text(s,encoding='utf-8')
for i in range(22):
 for l in range(1,6):
  lines=[sound(sounds[i],.8+l*.08),f'particle minecraft:{particles[i]} ~ ~1 ~ .35 .45 .35 .02 {8+l*4} normal @a[distance=..24]']
  for n in range(8+l*4):
   angle=n*math.tau/(8+l*4);x=math.cos(angle)*(0.65+l*.08);z=math.sin(angle)*(0.65+l*.08)
   lines.append(f'particle minecraft:dust{{color:{json.dumps(rgb[rarities[i]])},scale:0.8}} ~{x:.3f} ~0.15 ~{z:.3f} 0 0 0 0 1 normal @a[distance=..24]')
  fn(f'fx/{i+1}_{l}',lines);append(f'cast/{i+1}_{l}',[f'function tarot:fx/{i+1}_{l}'])
  if l<5:append(f'upgrade/{i+1}_{l}',[sound('block.enchantment_table.use',1.2),sound('entity.player.levelup',1.4), 'particle minecraft:enchanted_hit ~ ~1 ~ .4 .6 .4 .1 32 normal @a[distance=..24]'])
# Reveal once per pack according to highest rarity, rather than 5 overlapping sounds.
for rank,rar in enumerate(rgb,1):append('draw/'+rar.lower(),[f'execute unless score @s tr_best matches {rank}.. run scoreboard players set @s tr_best {rank}'])
for k in ['box','case']:append('open/'+k,[sound('item.bundle.drop_contents',.8),'particle minecraft:enchant ~ ~1 ~ .4 .4 .4 .1 20 normal @s'])
p=F/'open/pack.mcfunction';s=p.read_text(encoding='utf-8');p.write_text('scoreboard players set @s tr_best 0\n'+s+'function tarot:reveal\n',encoding='utf-8')
reveal=[sound('item.bundle.drop_contents',1.2)]
for rank,rar in enumerate(rgb,1):
 fx=[sound('block.amethyst_block.chime',.8+rank*.15),f'particle minecraft:dust{{color:{json.dumps(rgb[rar])},scale:1.3}} ~ ~1 ~ .5 .7 .5 .05 {rank*12} normal @a[distance=..24]',f'title @s subtitle {{"text":"最高稀有度 · {rar}","color":"'+{'R':'white','SR':'green','SSR':'aqua','SP':'light_purple','UR':'gold'}[rar]+'"}','title @s times 5 35 10','title @s title {"text":"秘仪揭晓","color":"gold"}']
 if rank>=3:fx+=[sound('entity.player.levelup',1.5-rank*.1)]
 if rank==5:fx+=['particle minecraft:firework ~ ~1 ~ .6 .7 .6 .07 45 normal @a[distance=..24]']
 fn('reveal/'+rar.lower(),fx);reveal.append(f'execute if score @s tr_best matches {rank} run function tarot:reveal/{rar.lower()}')
fn('reveal',reveal)
append('load',['scoreboard objectives add tr_best dummy'])
p=F/'player.mcfunction';s=p.read_text(encoding='utf-8');s=s.replace('scoreboard players add @s tr_cd 0','execute if score @s tr_cd matches 1 run function tarot:ready\nscoreboard players add @s tr_cd 0');p.write_text(s,encoding='utf-8')
fn('ready',[sound('block.note_block.chime',1.6,.35),'title @s actionbar {"text":"塔罗牌技能已就绪","color":"green"}'])
# While softly disabled, remove outstanding modifiers on returning players.
append('tick',['execute if score #enabled tr_on matches 0 as @a[scores={tr_active=1..}] run function tarot:clear'])
def js(path,obj):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf-8')
js(P/'pack.mcmeta',{'pack':{'min_format':[97,1],'max_format':[97,1],'description':'塔罗秘仪 · 黑金牌面 · 26.3'}})
for name in [f'card_{i}' for i in range(1,23)]+['pack','box','case']:
 js(P/f'assets/tarot/items/{name}.json',{'model':{'type':'minecraft:model','model':f'tarot:item/{name}'}})
 js(P/f'assets/tarot/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'tarot:item/{name}'}})
print('V2 effects, models and audio commands prepared.')

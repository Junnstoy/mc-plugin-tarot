import json, pathlib, zipfile, re
ROOT=pathlib.Path('outputs'); PACK=ROOT/'tarot_26_3'
def write(path,s):
 p=PACK/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def js(path,x):write(path,json.dumps(x,ensure_ascii=False,indent=2))
def fn(name,lines):write(pathlib.Path('data/tarot/function')/(name+'.mcfunction'),'\n'.join(lines)+'\n')
# Attribute amounts are additions, except movement/attack speed which multiply the base.
cards=[
('愚者','R', [('movement_speed',.10),('jump_strength',.10)]),
('魔术师','SR',[('attack_speed',.15),('attack_damage',1)]),
('女祭司','SR',[('max_health',4),('luck',1)]),
('女皇','SSR',[('max_health',6),('armor',2)]),
('皇帝','SSR',[('armor',5),('knockback_resistance',.10)]),
('教皇','SR',[('armor',2),('max_health',2)]),
('恋人','R',[('max_health',2),('movement_speed',.05)]),
('战车','SR',[('movement_speed',.20),('step_height',.5)]),
('力量','SSR',[('attack_damage',3),('attack_knockback',.3)]),
('隐者','R',[('sneaking_speed',.15),('movement_speed',.05)]),
('命运之轮','SP',[('luck',4),('attack_speed',.20)]),
('正义','SR',[('attack_damage',1),('armor',2)]),
('倒吊人','R',[('safe_fall_distance',4),('jump_strength',.15)]),
('死神','SP',[('attack_damage',5),('movement_speed',.10)]),
('节制','R',[('attack_speed',.10),('armor',1)]),
('恶魔','SSR',[('attack_damage',4),('attack_speed',.10)]),
('高塔','SP',[('armor',6),('attack_knockback',.8)]),
('星星','R',[('luck',2),('safe_fall_distance',3)]),
('月亮','SR',[('sneaking_speed',.25),('attack_damage',1)]),
('太阳','SSR',[('max_health',8),('armor',2)]),
('审判','UR',[('attack_damage',5),('armor',6)]),
('世界','UR',[('attack_damage',3),('armor',4),('movement_speed',.20),('max_health',6)])]
strength=[1,1.2,1.45,1.75,2.1];duration=[8,10,12,15,18];cooldown=[60,55,50,45,40]
colors={'R':'white','SR':'green','SSR':'aqua','SP':'light_purple','UR':'gold'}
def txt(s,color='gray'):return json.dumps({'text':s,'color':color,'italic':False},ensure_ascii=False,separators=(',',':'))
def item(i,l):
 name,rar,_=cards[i]
 return 'minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{card:%d,level:%d}},minecraft:item_name=%s,minecraft:lore=[%s,%s]]'%(i+1,l,txt(f'{rar} · {name} ★{l}',colors[rar]),txt(f'副手潜行激活 | 持续{duration[l-1]}秒'),txt(f'冷却{cooldown[l-1]}秒 | 重复卡可升级'))
def container(kind):return 'minecraft:paper[minecraft:max_stack_size=1,minecraft:custom_data={tarot:{package:"%s"}},minecraft:item_name=%s]'%(kind,txt({'pack':'塔罗卡包 · 5张','box':'塔罗卡盒 · 8包','case':'塔罗卡箱 · 48盒'}[kind],'gold'))
js(pathlib.Path('pack.mcmeta'),{'pack':{'min_format':[121,0],'max_format':[121,0],'description':'塔罗秘仪 · 22张 × 5级 · Paper 26.3 测试版'}})
for tag in ['load','tick']:js(pathlib.Path(f'data/minecraft/tags/function/{tag}.json'),{'values':['tarot:'+tag]})
js(pathlib.Path('data/tarot/predicate/sneaking.json'),{'type':'minecraft:entity_properties','entity':'this','predicate':{'flags':{'is_sneaking':True}}})
objs=['tr_cd','tr_time','tr_edge','tr_card','tr_level','tr_active','tr_alvl','tr_roll','tr_pick','tr_on','tarot_upgrade']
fn('load',[f'scoreboard objectives add {o} '+('trigger' if o=='tarot_upgrade' else 'dummy') for o in objs]+['execute unless score #enabled tr_on matches 0..1 run scoreboard players set #enabled tr_on 1'])
fn('tick',['execute if score #enabled tr_on matches 1 as @a at @s run function tarot:player'])
attrs=sorted({a for _,_,v in cards for a,_ in v})
fn('clear',[f'attribute @s minecraft:{a} modifier remove tarot:{a}' for a in attrs]+['scoreboard players set @s tr_time 0','scoreboard players set @s tr_active 0','scoreboard players set @s tr_alvl 0'])
player=['scoreboard players add @s tr_cd 0','scoreboard players add @s tr_edge 0','scoreboard players add @s tr_time 0','scoreboard players enable @s tarot_upgrade','execute if score @s tr_cd matches 1.. run scoreboard players remove @s tr_cd 1','execute if score @s tr_time matches 1.. run scoreboard players remove @s tr_time 1','scoreboard players set @s tr_card 0','scoreboard players set @s tr_level 0']
for i in range(22):
 player.append(f'execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{{tarot:{{card:{i+1}}}}}] run scoreboard players set @s tr_card {i+1}')
for l in range(1,6):player.append(f'execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{{tarot:{{level:{l}}}}}] run scoreboard players set @s tr_level {l}')
player+=['execute if score @s tr_active matches 1.. unless score @s tr_active = @s tr_card run function tarot:clear','execute if score @s tr_active matches 1.. unless score @s tr_alvl = @s tr_level run function tarot:clear','execute if score @s tr_active matches 1.. if score @s tr_time matches 0 run function tarot:clear','execute if score @s tarot_upgrade matches 1.. run function tarot:upgrade','execute if predicate tarot:sneaking if score @s tr_edge matches 0 run function tarot:press','execute if predicate tarot:sneaking run scoreboard players set @s tr_edge 1','execute unless predicate tarot:sneaking run scoreboard players set @s tr_edge 0']
fn('player',player)
press=[]
for k in ['pack','box','case']:press.append(f'execute if items entity @s weapon.offhand minecraft:paper[minecraft:custom_data~{{tarot:{{package:"{k}"}}}}] run return run function tarot:open/{k}')
press+=['execute if score @s tr_cd matches 1.. run return 0']
for i in range(22):
 for l in range(1,6):
  press.append(f'execute if score @s tr_card matches {i+1} if score @s tr_level matches {l} run return run function tarot:cast/{i+1}_{l}')
  lines=['function tarot:clear']
  for a,amount in cards[i][2]:
   op='add_multiplied_base' if a in ['movement_speed','attack_speed'] else 'add_value'
   lines.append(f'attribute @s minecraft:{a} modifier add tarot:{a} {amount*strength[l-1]:.4f} {op}')
  lines += [f'scoreboard players set @s tr_cd {cooldown[l-1]*20}',f'scoreboard players set @s tr_time {duration[l-1]*20}',f'scoreboard players set @s tr_active {i+1}',f'scoreboard players set @s tr_alvl {l}',f'tellraw @s {txt(cards[i][0]+" · 技能已激活",colors[cards[i][1]])}']
  fn(f'cast/{i+1}_{l}',lines);fn(f'give/{i+1}_{l}',['give @s '+item(i,l)])
fn('press',press)
upgrade=['scoreboard players set @s tarot_upgrade 0']
for i in range(22):
 for l in range(1,5):
  pred=f'minecraft:paper[minecraft:custom_data~{{tarot:{{card:{i+1},level:{l}}}}}]'
  upgrade.append(f'execute if items entity @s weapon.mainhand {pred} if items entity @s weapon.offhand {pred} run return run function tarot:upgrade/{i+1}_{l}')
  fn(f'upgrade/{i+1}_{l}',['function tarot:clear','item replace entity @s weapon.mainhand with minecraft:air','item replace entity @s weapon.offhand with '+item(i,l+1),f'tellraw @s {txt("升级成功："+cards[i][0]+" ★"+str(l+1),"gold")}'])
upgrade.append('tellraw @s '+txt('升级需要主手和副手各持一张同名同等级卡，最高5级。','red'));fn('upgrade',upgrade)
for k in ['pack','box','case']:fn('give/'+k,['give @s '+container(k)])
fn('open/case',['item replace entity @s weapon.offhand with minecraft:air','give @s '+container('box')+' 48'])
fn('open/box',['item replace entity @s weapon.offhand with minecraft:air','give @s '+container('pack')+' 8'])
fn('open/pack',['item replace entity @s weapon.offhand with minecraft:air']+['function tarot:draw/normal']*4+['function tarot:draw/guaranteed'])
weights={'normal':[8000,1800,170,25,5],'guaranteed':[0,9400,500,90,10]}
for pool,ww in weights.items():
 lines=['execute store result score @s tr_roll run random value 1..10000'];start=1
 for rarity,w in zip(colors,ww):
  if w:lines.append(f'execute if score @s tr_roll matches {start}..{start+w-1} run function tarot:draw/{rarity.lower()}')
  start+=w
 assert start==10001
 fn('draw/'+pool,lines)
for rarity in colors:
 ids=[i for i,c in enumerate(cards) if c[1]==rarity]
 fn('draw/'+rarity.lower(),[f'execute store result score @s tr_pick run random value 1..{len(ids)}']+[f'execute if score @s tr_pick matches {n} run function tarot:give/{i+1}_1' for n,i in enumerate(ids,1)])
fn('disable',['scoreboard players set #enabled tr_on 0','execute as @a run function tarot:clear'])
fn('enable',['scoreboard players set #enabled tr_on 1','execute as @a run function tarot:clear'])
fn('reset_player',['function tarot:clear','scoreboard players set @s tr_cd 0'])
doc=['# 塔罗秘仪 — Paper 26.3 数据包测试版','', '实现：22张固定稀有度卡，每张5级；副手持牌按下潜行激活，松开再按才会再次触发。开包使用同样操作。原版纸外观，中文名称区分；尚未包含自定义牌面资源包。','', '## 等级数值','', '|等级|强度倍率|持续秒数|冷却秒数|累计1级卡成本|','|---|---|---|---|---|']
for l in range(5):doc.append(f'|{l+1}|{strength[l]}|{duration[l]}|{cooldown[l]}|{2**l}|')
doc+=['','冷却从激活开始，所有塔罗卡共享。冷却和持续计时仅在线推进，按20TPS计秒；重载不重置冷却。卸下、换卡或到期移除属性。生命上限提高不等于立即治疗。','', '## 全卡清单','', '|编号|卡名|稀有度|1级效果（高等级按倍率增加）|','|---|---|---|---|']
labels={'movement_speed':'移速（基础倍率）','jump_strength':'跳跃力度','attack_speed':'攻速（基础倍率）','attack_damage':'攻击伤害','max_health':'生命上限（点）','luck':'幸运','armor':'护甲','knockback_resistance':'击退抗性','step_height':'跨步高度','attack_knockback':'攻击击退','sneaking_speed':'潜行速度','safe_fall_distance':'安全跌落距离'}
for i,(name,r,v) in enumerate(cards):doc.append(f'|{i}|{name}|{r}|'+ '；'.join(f'{labels[a]} +{x:g}' for a,x in v)+'|')
doc+=['','幸运属性仅对使用该属性的战利品生效，不会提高本系统卡包出率。属性受原版上下限限制。各卡定位和数值为初始平衡方案，可后续修改。','', '## 卡包概率','', '每包4个普通位加1个保底位。所有牌均1级；同档卡等概率，允许重复。不设盒保底或箱保底。','', '|稀有度|普通位|保底位|单包期望张数|','|---|---|---|---|']
for r,a,b in zip(colors,weights['normal'],weights['guaranteed']):doc.append(f'|{r}|{a/100:g}%|{b/100:g}%|{(4*a+b)/10000:g}|')
doc+=['','每盒8包=40张；每箱48盒=384包=1920张。各位置独立抽取，期望不等于每盒或每箱必出。','', '## 安装与使用','', '将 tarot_26_3.zip 放入世界 datapacks 文件夹，运行 `/minecraft:reload`，检查日志。','获取一个卡包：`/function tarot:give/pack`；一盒：`/function tarot:give/box`；一箱：`/function tarot:give/case`。需管理员执行；控制台用 `execute as 玩家名 run function ...`。','把包装放副手，按下潜行打开；背包不足时 /give 的物品会掉在地面，拆箱前建议清空背包或预备存储空间。','升级：主副手各放一张同名同等级卡，执行 `/trigger tarot_upgrade`；消耗两张，副手得到高一级卡。普通玩家可用。','测试指定卡：`/function tarot:give/1_1`，第一个数字为清单编号+1，第二个为等级1–5。','', '## 热停用与卸载','', '优先 `/function tarot:disable`：停止运行并清理在线玩家属性；保留包使后续清理仍可用。恢复运行：`/function tarot:enable`。','彻底删除前，应让曾使用技能的玩家上线执行 `/function tarot:reset_player`，再禁用数据包和删除文件。离线玩家属性无法由普通命令直接清理，不能承诺任意时刻无残留卸载。实体卡和包装不会自动消失。','', '## 验证状态','', '已自动检查 JSON、内部函数引用、110个卡等级函数及概率权重。当前机器只有Java17，没有启动需要新版Java的Paper26.3；尚未实服验证，不能标为生产可用。','建议测试：加载日志无错误、各卡属性、取下清理、切牌共享冷却、升级消耗、每包5张且SR+保底、盒箱数量、重载和重登状态。','版本格式根据官方26.3发布说明设为121.0：https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3']
ROOT.mkdir(exist_ok=True);(ROOT/'使用说明与数值.md').write_text('\n'.join(doc),encoding='utf-8')
# Meaningful structural checks: all references resolve and draw pools cannot escape ranges.
for p in PACK.rglob('*.json'):json.loads(p.read_text(encoding='utf-8'))
for p in PACK.rglob('*.mcfunction'):
 for ref in re.findall(r'\bfunction tarot:([\w/]+)',p.read_text(encoding='utf-8')):assert (PACK/'data/tarot/function'/f'{ref}.mcfunction').exists(),ref
assert len(list((PACK/'data/tarot/function/cast').glob('*.mcfunction')))==110
assert weights['guaranteed'][0]==0
with zipfile.ZipFile(ROOT/'tarot_26_3.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in PACK.rglob('*'):
  if p.is_file():z.write(p,p.relative_to(PACK))
print('Built 110 card variants; JSON, references, and weight checks passed.')

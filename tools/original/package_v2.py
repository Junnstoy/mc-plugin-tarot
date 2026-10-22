import pathlib,json,shutil,zipfile,re,hashlib
from PIL import Image
R=pathlib.Path('outputs');D=R/'tarot_26_3_v2';P=R/'tarot_resources_26_3'
manifest=json.loads(pathlib.Path('work/art_manifest.json').read_text(encoding='utf-8'))
prompts=[]
for a in manifest:
 i=a['index'];name=f'card_{i+1}' if i<22 else ['pack','box','case'][i-22]
 dest=P/f'assets/tarot/textures/item/{name}.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(a['path'],dest)
 with Image.open(dest) as img:
  assert img.mode=='RGBA',(name,img.mode)
  assert img.getchannel('A').getextrema()[0]==0,name
 prompts.append(f"## {name}\n{a['prompt']}\n")
(R/'牌面生成提示词.md').write_text('# 内置 imagegen 生成记录\n\n'+'\n'.join(prompts),encoding='utf-8')
names=['愚者','魔术师','女祭司','女皇','皇帝','教皇','恋人','战车','力量','隐者','命运之轮','正义','倒吊人','死神','节制','恶魔','高塔','星星','月亮','太阳','审判','世界','卡包','卡盒','卡箱']
gallery='<!doctype html><html lang="zh"><meta charset="utf-8"><title>塔罗秘仪 · 牌面图鉴</title><style>body{background:#101116;color:#ead8a6;font-family:system-ui;margin:40px}h1{font-weight:500}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:20px}figure{margin:0;border:1px solid #53452b;border-radius:12px;padding:12px;background:#191a20}img{width:100%;display:block}figcaption{text-align:center;padding:10px}p{color:#b5b3ae}</style><h1>塔罗秘仪 · 黑金典藏</h1><p>22张牌与3种包装 · 图片预览，非游戏运行截图</p><main>'
for i,name in enumerate(names):
 key=f'card_{i+1}' if i<22 else ['pack','box','case'][i-22]
 gallery+=f'<figure><img src="tarot_resources_26_3/assets/tarot/textures/item/{key}.png"><figcaption>{name}</figcaption></figure>'
(R/'牌面图鉴.html').write_text(gallery+'</main></html>',encoding='utf-8')
for base in [D,P]:
 for p in base.rglob('*.json'):json.loads(p.read_text(encoding='utf-8'))
 json.loads((base/'pack.mcmeta').read_text(encoding='utf-8'))
for p in (D/'data/tarot/function').rglob('*.mcfunction'):
 s=p.read_text(encoding='utf-8')
 for ref in re.findall(r'\bfunction tarot:([\w/]+)',s):assert (D/f'data/tarot/function/{ref}.mcfunction').exists(),ref
 for name in re.findall(r'minecraft:item_model="tarot:(\w+)"',s):assert (P/f'assets/tarot/items/{name}.json').exists(),name
for p in (P/'assets/tarot/items').glob('*.json'):
 model=json.loads(p.read_text(encoding='utf-8'))['model']['model'].split(':')[1]
 mp=P/f'assets/tarot/models/{model}.json';assert mp.exists()
 tex=json.loads(mp.read_text(encoding='utf-8'))['textures']['layer0'].split(':')[1]
 assert (P/f'assets/tarot/textures/{tex}.png').exists()
for base in [D,P]:
 with zipfile.ZipFile(R/(base.name+'.zip'),'w',zipfile.ZIP_DEFLATED) as z:
  for p in base.rglob('*'):
   if p.is_file():z.write(p,p.relative_to(base))
sha=hashlib.sha1((R/'tarot_resources_26_3.zip').read_bytes()).hexdigest()
(R/'V2安装说明.md').write_text('''# 塔罗秘仪 V2 — 贴图、特效与音效

包含22张独立牌面、卡包/盒/箱三种贴图，共25张；黑金风格，满级卡附加附魔闪光。各等级共用该卡牌面，通过物品名称区分等级与稀有度；没有110张不同插画或五色边框。

发动：各牌采用不同组合的原版粒子和声音，并生成稀有度颜色的粒子环，等级越高环越大、粒子越多。升级有附魔音、升级音和粒子；冷却结束有提示音和行动栏提醒。拆包按本包最高稀有度播放标题、彩色粒子和音效；SSR及以上增加升级音，UR增加烟花粒子。技能音效仅本人听见，粒子附近24格玩家可见；所有声音为原版音源，无需额外音频文件。

## 安装
1. 备份世界。在世界 datapacks 文件夹中，以 tarot_26_3_v2.zip 替换旧 tarot_26_3.zip，不要同时启用两版。
2. 执行 /minecraft:reload 并检查日志。
3. 客户端将 tarot_resources_26_3.zip 放入 resourcepacks，在游戏资源包菜单中启用。
4. 测试 /function tarot:give/pack；副手持卡包，松开再按潜行拆包。卡牌发动方式相同。

旧版已发出的卡片没有新模型组件，重载不会自动更新其外观。管理员可按旧牌的编号与等级用 tarot:give/编号_等级 重新发放并回收旧牌；旧牌仍能触发技能。

## 服务器下发资源包
将资源包上传到你自己的可直接下载的HTTPS地址，再在 server.properties 填写 resource-pack 和 resource-pack-sha1；本次未上传或修改服务器配置。资源包SHA-1：
'''+sha+'''

## 保留的玩法
22张固定稀有度卡，每张5级，所有抽出卡为1级；两张同名同等级卡用 /trigger tarot_upgrade 合成高一级。每包4个普通位和1个SR+保底位，每盒8包，每箱48盒。基础效果、概率与数值见《使用说明与数值》，其中旧版“未包含资源包”的描述已由本V2取代。

## 验证范围
已检查25张图的透明通道、JSON、模型/贴图和函数引用。未启动Paper26.3实服，粒子、声音和新版命令兼容性仍需在测试服确认；图片预览不等于游戏运行截图。

先用 /function tarot:disable 停用可清理在线玩家，保留数据包时也会清理后来上线的激活状态玩家。彻底删包前仍需让有关离线玩家上线清理；不要将软停用视为已完成全部离线数据卸载。
''',encoding='utf-8')
print('All assets and references passed; ZIPs packaged. Resource SHA1:',sha)

import pathlib,shutil,json,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
pack=root/'outputs/tarot_26_3_v2_1'
shutil.copytree(root/'outputs/tarot_26_3_v2',pack,dirs_exist_ok=True)
predicate=pack/'data/tarot/predicate/sneaking.json'
obj=json.loads(predicate.read_text(encoding='utf-8'))
obj['type']=obj.pop('condition')
predicate.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
assert obj=={'type':'minecraft:entity_properties','entity':'this','predicate':{'flags':{'is_sneaking':True}}}
for p in pack.rglob('*.json'):json.loads(p.read_text(encoding='utf-8'))
dest=root/'outputs/tarot_26_3_v2_1.zip'
with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED) as z:
 for p in pack.rglob('*'):
  if p.is_file():z.write(p,p.relative_to(pack))
print(dest)

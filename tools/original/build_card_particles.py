from pathlib import Path
from PIL import Image

root=Path(__file__).resolve().parents[1]
source=root/'outputs/tarot_resources_rainbow_v3_1_1/assets/tarot/textures/item'
out=root/'work/v3_plugin/resources/card-particles.csv'
palette=[0xff5252,0xff983e,0xffdc40,0x4bdf7f,0x46e1e8,0x568bff,0xb478ff]
rows=['# id,right,up,rgb; sampled from the existing 22 tarot textures; back=1.05']
for card in range(1,23):
    points=[]
    # 58 border points, followed by 121 image points revealed from bottom to top.
    for side in [-1,1]:
        for i in range(16):points.append((side*1.05,.65+2.75*i/15,palette[(i+card)%7]))
    for y in [.65,3.4]:
        for i in range(13):points.append((-1.05+2.1*i/12,y,palette[(i+card)%7]))
    with Image.open(source/f'card_{card}.png') as original:
        pixels=original.convert('RGB').resize((11,11),Image.Resampling.BOX)
        for y in reversed(range(11)):
            for x in range(11):
                r,g,b=pixels.getpixel((x,y))
                points.append((-.9+1.8*x/10,1.15+1.8*(10-y)/10,(r<<16)|(g<<8)|b))
    assert len(points)==179
    rows.extend(f'{card},{x:.3f},{y:.3f},{color:06x}' for x,y,color in points)
out.write_text('\n'.join(rows)+'\n',encoding='utf-8')
print(f'Wrote {22*179} card artwork points: {out}')

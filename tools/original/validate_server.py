import pathlib,shutil,subprocess,threading,time
root=pathlib.Path(__file__).resolve().parents[1]
test=root/'work/paper_validation'
source=pathlib.Path('E:/Minecraft/server')
test.mkdir(exist_ok=True)
for name in ['libraries','versions','cache']:
 shutil.copytree(source/name,test/name,dirs_exist_ok=True)
for name in ['paper-26.3-143.jar','eula.txt']:shutil.copyfile(source/name,test/name)
world_name='validation_'+str(time.time_ns())
(test/world_name/'datapacks').mkdir(parents=True,exist_ok=True)
shutil.copyfile(root/'outputs/tarot_26_3_v2_1.zip',test/world_name/'datapacks/tarot_26_3_v2_1.zip')
(test/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25587\nonline-mode=false\nenable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\nmax-players=1\nlevel-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains","structures":{}}\ninitial-enabled-packs=vanilla,file/tarot_26_3_v2_1.zip\n',encoding='utf-8')
java=pathlib.Path('C:/Users/Yuma/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon/bin/java.exe')
cmd=[str(java),'-Xms512M','-Xmx2G','-jar','paper-26.3-143.jar','--nogui','--world',world_name]
lines=[];ready=threading.Event();reloaded=threading.Event()
proc=subprocess.Popen(cmd,cwd=test,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
def reader():
 with (root/'work/paper_validation_output.log').open('w',encoding='utf-8') as out:
  for line in proc.stdout:
   lines.append(line);out.write(line);out.flush()
   if 'Done (' in line:ready.set()
   if 'Loaded' in line or 'ERROR' in line or 'Done (' in line or 'Failed' in line:print(line.strip(),flush=True)
thread=threading.Thread(target=reader);thread.start()
try:
 deadline=time.monotonic()+150
 while not ready.is_set() and proc.poll() is None and time.monotonic()<deadline:ready.wait(.5)
 if ready.is_set():
  proc.stdin.write('datapack list enabled\nfunction tarot:disable\nfunction tarot:enable\nminecraft:reload\n');proc.stdin.flush()
  time.sleep(12)
  proc.stdin.write('stop\n');proc.stdin.flush()
 else:print('FAILED: server did not reach ready state',flush=True)
 try:proc.wait(timeout=35)
 except subprocess.TimeoutExpired:proc.terminate();proc.wait(timeout=10)
finally:
 if proc.poll() is None:proc.kill()
 thread.join(timeout=10)
print('Ready:',ready.is_set(),'Exit:',proc.returncode,flush=True)

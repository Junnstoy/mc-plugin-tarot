import pathlib,shutil,subprocess,threading,time,json
root=pathlib.Path(__file__).resolve().parents[1];test=root/'work/paper_validation'
plugin_dir=test/'plugins';plugin_dir.mkdir(exist_ok=True)
shutil.copyfile(root/'outputs/TarotRainbow-3.4.0.jar',plugin_dir/'TarotRainbow-3.4.0.jar')
world='rainbow_validation_'+str(time.time_ns())
album_restart=root/'work'/('album_restart_'+str(time.time_ns()))
(test/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25587\nonline-mode=false\nenable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\nmax-players=1\nlevel-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains","structures":{}}\n',encoding='utf-8')
data=plugin_dir/'TarotRainbow';data.mkdir(exist_ok=True)
now=int(time.time()*1000);expires=now+86400000;unlocks=now+1800000
uid='00000000-0000-0000-0000-000000000001';expired_uid='00000000-0000-0000-0000-000000000002'
(data/'players.yml').write_text(f'players:\n  {uid}:\n    card: 8\n    level: 3\n    activated: {now}\n    expires: {expires}\n    unlocks: {unlocks}\n  {expired_uid}:\n    card: 1\n    level: 1\n    activated: 1\n    expires: 2\n    unlocks: 2\n',encoding='utf-8')
cooldown_uid='00000000-0000-0000-0000-000000000003'
with (data/'players.yml').open('a',encoding='utf-8') as file:
 file.write(f'cooldowns:\n  {cooldown_uid}: {unlocks}\n')
java='C:/Users/Yuma/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon/bin/java.exe'
reports=[]
for run in range(3):
 if run>0:shutil.copyfile(root/'work/deps/PlaceholderAPI-2.12.3-runtime.jar',plugin_dir/'PlaceholderAPI.jar')
 ready=threading.Event();tested=threading.Event();deferred=threading.Event();lines=[]
 proc=subprocess.Popen([java,'-Xms512M','-Xmx2G','-Dtarot.validation.album-dir='+str(album_restart),'-jar','paper-26.3-143.jar','--nogui','--world',world],cwd=test,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace',creationflags=subprocess.CREATE_NO_WINDOW)
 def read():
  with (root/f'work/v3_4_0_server_run{run+1}.log').open('w',encoding='utf-8') as out:
   for line in proc.stdout:
    lines.append(line);out.write(line);out.flush()
    if 'Done (' in line:ready.set()
    if 'TAROT SELFTEST PASS' in line:tested.set()
    if 'TAROT ALBUM MENU DEFERRED SELFTEST PASS' in line:deferred.set()
    if 'TarotRainbow' in line or 'TAROT ' in line or 'Done (' in line or 'Error occurred while' in line:print(line.strip(),flush=True)
 thread=threading.Thread(target=read);thread.start()
 try:
  deadline=time.monotonic()+100
  while not ready.is_set() and proc.poll() is None and time.monotonic()<deadline:ready.wait(.5)
  if ready.is_set():
   proc.stdin.write('tarot help\ntarot selftest\ndatapack list enabled\n');proc.stdin.flush();tested.wait(15)
   if tested.is_set():deferred.wait(3)
   proc.stdin.write('stop\n');proc.stdin.flush()
  try:proc.wait(timeout=30)
  except subprocess.TimeoutExpired:proc.terminate();proc.wait(timeout=10)
 finally:
  if proc.poll() is None:proc.kill()
  thread.join(timeout=10)
 content=(data/'players.yml').read_text(encoding='utf-8')
 report={'run':run+1,'ready':ready.is_set(),'selftest_passed':tested.is_set(),'upgrade_events_passed':any('TAROT UPGRADE EVENT SELFTEST PASS' in line for line in lines),'album_service_passed':any('TAROT ALBUM SERVICE SELFTEST PASS' in line for line in lines),'album_menu_passed':any('TAROT ALBUM MENU SELFTEST PASS' in line for line in lines),'timestamps_preserved':all(str(v) in content for v in [now,expires,unlocks]),'expired_state_removed':expired_uid not in content,'exit_code':proc.returncode}
 reports.append(report);print(json.dumps(report),flush=True)
 report['album_menu_deferred_passed']=deferred.is_set()
 report['album_restart_passed']=run==0 or any('TAROT ALBUM RESTART SELFTEST PASS' in line for line in lines)
 report['command_events_passed']=any('TAROT COMMAND EVENT SELFTEST PASS' in line for line in lines)
 report['package_open_passed']=any('TAROT PACKAGE OPEN SELFTEST PASS' in line for line in lines)
 report['independent_cooldown_preserved']=f'{cooldown_uid}: {unlocks}' in content
 report['charge_passed']=any('TAROT CHARGE SELFTEST PASS' in line for line in lines)
 report['native_charge_passed']=any('TAROT NATIVE CHARGE SELFTEST PASS' in line for line in lines)
 report['activation_state_passed']=any('TAROT ACTIVATION STATE SELFTEST PASS' in line for line in lines)
 report['particles_passed']=any('TAROT PARTICLE SELFTEST PASS' in line for line in lines)
 report['papi_mode_passed']=any('TAROT PAPI INTEGRATION SELFTEST PASS' in line for line in lines) if run>0 else not any('PlaceholderAPI registered' in line for line in lines)
 if not ready.is_set() or not tested.is_set():break
(root/'outputs/彩虹塔罗V3.4.0服务端验证.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2),encoding='utf-8')
assert len(reports)==3 and all(all(r[k] for k in ['ready','selftest_passed','upgrade_events_passed','album_service_passed','album_menu_passed','album_menu_deferred_passed','album_restart_passed','timestamps_preserved','expired_state_removed','command_events_passed','package_open_passed','independent_cooldown_preserved','charge_passed','native_charge_passed','activation_state_passed','particles_passed','papi_mode_passed']) and r['exit_code']==0 for r in reports),reports

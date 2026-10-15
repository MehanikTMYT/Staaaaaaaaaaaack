#!/usr/bin/env python3
import pathlib,subprocess,time,os,shutil,sys
root=pathlib.Path(__file__).resolve().parents[1];out=root/'verification/optimization-evidence';out.mkdir(exist_ok=True)
def run(mode,directory,name,limit=240):
 with (out/name).open('w') as log:subprocess.run(['python3','verification/mixed_launch.py',mode,directory],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=limit,check=True)
def require(directory,file):
 text=(root/'neoforge'/directory/file).read_text();assert text.startswith('PASS'),text;shutil.copy2(root/'neoforge'/directory/file,out/file)
(root/'neoforge/run-mixed-opt-client').mkdir(exist_ok=True)
(root/'neoforge/run-mixed-opt-client/mixed-client.txt').write_text('RUNNING')
with (out/'client-server.log').open('w') as log:
 server=subprocess.Popen(['python3','verification/mixed_launch.py','mixedClientServer','run-mixed-opt-client-server'],cwd=root,stdout=log,stderr=subprocess.STDOUT)
 try:
  deadline=time.time()+120
  while time.time()<deadline:
   if 'Done (' in (out/'client-server.log').read_text():break
   if server.poll() is not None:raise RuntimeError('Server stopped before Done')
   time.sleep(1)
  else:raise TimeoutError('Server startup')
  with (out/'client.log').open('w') as clientlog:subprocess.run(['xvfb-run','-a','python3','verification/mixed_launch.py','mixedClient','run-mixed-opt-client'],cwd=root,stdout=clientlog,stderr=subprocess.STDOUT,timeout=240,check=True,env={**os.environ,'LIBGL_ALWAYS_SOFTWARE':'1'})
  text=(root/'neoforge/run-mixed-opt-client/mixed-client.txt').read_text();assert 'count_sync=true inventory=1000' in text,text;shutil.copy2(root/'neoforge/run-mixed-opt-client/mixed-client.txt',out/'mixed-client.txt')
  server.wait(timeout=60)
 finally:
  if server.poll() is None:server.terminate();server.wait(timeout=30)
print('PASS optimized Mixed real client synchronization and pickup')

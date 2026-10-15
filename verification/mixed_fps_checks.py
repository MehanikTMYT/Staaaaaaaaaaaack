#!/usr/bin/env python3
import pathlib,subprocess,time,os,shutil
root=pathlib.Path(__file__).resolve().parents[1];out=root/'verification/mixed-evidence'
with (out/'fps-server.log').open('w') as log:
 server=subprocess.Popen(['python3','verification/mixed_launch.py','mixedFpsServer','run-mixed-fps-server'],cwd=root,stdout=log,stderr=subprocess.STDOUT)
 try:
  deadline=time.time()+120
  while 'Done (' not in (out/'fps-server.log').read_text():
   if server.poll() is not None or time.time()>deadline:raise RuntimeError('FPS server startup failed')
   time.sleep(1)
  with (out/'fps-client.log').open('w') as clientlog:subprocess.run(['xvfb-run','-a','python3','verification/mixed_launch.py','mixedFpsClient','run-mixed-fps-client'],cwd=root,stdout=clientlog,stderr=subprocess.STDOUT,timeout=300,check=True,env={**os.environ,'LIBGL_ALWAYS_SOFTWARE':'1'})
  result=root/'neoforge/run-mixed-fps-client/mixed-fps.csv';assert len(result.read_text().splitlines())==7,result.read_text();shutil.copy2(result,out/'mixed-fps.csv');server.wait(timeout=60)
 finally:
  if server.poll() is None:server.terminate();server.wait(timeout=30)
print('PASS six steady-state FPS scenarios')

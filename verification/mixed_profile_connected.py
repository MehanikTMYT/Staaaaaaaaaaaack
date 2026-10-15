#!/usr/bin/env python3
"""Profile an artifact with a real connected client, in an isolated test world."""
import pathlib,subprocess,os,time,sys,shutil
root=pathlib.Path(__file__).resolve().parents[1];label=sys.argv[1];out=root/'verification/optimization-evidence';out.mkdir(exist_ok=True)
serverdir='run-mixed-profile-'+label+'-server';clientdir='run-mixed-profile-'+label+'-client'
with (out/(label+'-server.log')).open('w') as log:
 server=subprocess.Popen(['python3','verification/mixed_launch.py','mixedProfile',serverdir],cwd=root,stdout=log,stderr=subprocess.STDOUT,env={**os.environ,'STXCK_PROFILE_CONNECTED':'true'})
 try:
  deadline=time.time()+120
  while 'Done (' not in (out/(label+'-server.log')).read_text():
   if server.poll() is not None or time.time()>deadline:raise RuntimeError('Profile server startup failed')
   time.sleep(1)
  with (out/(label+'-client.log')).open('w') as clientlog:
   subprocess.run(['xvfb-run','-a','python3','verification/mixed_launch.py','mixedProfileClient',clientdir],cwd=root,stdout=clientlog,stderr=subprocess.STDOUT,timeout=600,check=True,env={**os.environ,'LIBGL_ALWAYS_SOFTWARE':'1','STXCK_PROFILE_PHASE':str(root/'neoforge'/serverdir/'profile-phase.txt')})
  server.wait(timeout=120)
  for directory,file in [(serverdir,'profile-result.txt'),(clientdir,'profile-client.txt')]:
   data=(root/'neoforge'/directory/file).read_text();assert data.startswith('PASS'),data;shutil.copy2(root/'neoforge'/directory/file,out/(label+'-'+file))
  shutil.copy2(root/'neoforge'/serverdir/'profile-ticks.csv',out/(label+'-ticks.csv'))
 finally:
  if server.poll() is None:server.terminate();server.wait(timeout=30)
print('PASS connected profile '+label)

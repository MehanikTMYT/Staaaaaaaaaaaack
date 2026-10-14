#!/usr/bin/env python3
"""Run real Create/Gacha callbacks and the client FPS matrix after release smoke checks."""
import pathlib,subprocess,os,shutil
r=pathlib.Path(__file__).resolve().parents[1];ev=r/'verification/evidence'
for mode in ['create','gacha']:
 d=r/'neoforge'/('run-'+mode);result=d/(mode+'-results.txt');result.write_text('RUNNING')
 env=os.environ.copy();env['JAVA_TOOL_OPTIONS']='-Dstxck.'+mode+'=true'
 with (ev/(mode+'-nl114.log')).open('w') as out:
  subprocess.run(['python3','verification/launch.py','runSmokeServer','--artifact','--directory','run-'+mode],cwd=r,env=env,stdout=out,stderr=subprocess.STDOUT,timeout=180,check=True)
 text=result.read_text();assert text.startswith('PASS') and 'FAIL' not in text,text
 shutil.copy(result,ev/(mode+'-nl114-results.txt'));print(text,flush=True)
subprocess.run(['python3','verification/client_matrix.py'],cwd=r,check=True)

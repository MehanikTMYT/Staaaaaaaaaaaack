#!/usr/bin/env python3
"""Sequential checks on NL; integration JARs must already exist in isolated run directories."""
import os,pathlib,shutil,subprocess,sys
r=pathlib.Path(__file__).resolve().parents[1];n=r/'neoforge';ev=r/'verification/evidence'
jar=next((n/'build/libs').glob('*.jar'))
for name in ['run-create','run-ae2','run-gacha','run-verify']:
 (n/name/'mods').mkdir(exist_ok=True);shutil.copy(jar,n/name/'mods'/jar.name)
def run(task,directory,log,prop=None,extra=()):
 env=os.environ.copy()
 if prop:env['JAVA_TOOL_OPTIONS']='-Dstxck.'+prop+'=true'
 with (ev/log).open('w') as out:
  subprocess.run(['python3','verification/launch.py',task,'--directory',directory,*extra],cwd=r,env=env,stdout=out,stderr=subprocess.STDOUT,timeout=240,check=True)
def check(directory,result):
 text=(n/directory/result).read_text();assert 'PASS' in text and 'FAIL' not in text,text
 if result=='verification-results.csv':
  assert sum(line.startswith(('immediate,','deferred10,')) for line in text.splitlines())==6, 'incomplete benchmark'
 shutil.copy(n/directory/result,ev/(directory[4:]+'-nl114-'+result));print(directory+' PASS',flush=True)
for directory,result,prop in [('run-create','create-results.txt','create'),('run-ae2','ae2-results.txt','ae2')]:
 (n/directory/result).write_text('RUNNING')
 run('runSmokeServer',directory,prop+'-nl114.log',prop,('--artifact',));check(directory,result)
(n/'run-verify/verification-results.csv').write_text('RUNNING')
run('runVerifyServer','run-verify','server-functional-nl114.log',extra=('--artifact',));check('run-verify','verification-results.csv')
if '--reuse-baseline' not in sys.argv:
 run('runVanillaBenchmark','run-vanilla','vanilla-nl114.log',extra=('--without-mod',))
shutil.copy(n/'run-vanilla/baseline.csv',ev/'vanilla-nl114.csv')
subprocess.run(['python3','verification/check_release.py'],cwd=r,check=True)
(n/'run-gacha/gacha-results.txt').write_text('RUNNING')
run('runSmokeServer','run-gacha','gacha-nl114.log','gacha',('--artifact',));check('run-gacha','gacha-results.txt')
subprocess.run(['python3','verification/client_matrix.py'],cwd=r,check=True)

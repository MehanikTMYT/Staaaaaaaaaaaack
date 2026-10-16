#!/usr/bin/env python3
"""Fresh-world regressions for the optimized artifact; historical evidence is preserved."""
import pathlib,subprocess,shutil,os,time,hashlib
root=pathlib.Path(__file__).resolve().parents[1]
out=root/'verification/optimization-evidence';out.mkdir(exist_ok=True)
stamp=str(int(time.time()))
props=dict(line.split('=',1) for line in (root/'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
jar=pathlib.Path(os.environ['STXCK_JAR']) if 'STXCK_JAR' in os.environ else next((root/'neoforge/build/libs').glob('*-'+props['mod_version']+'.jar'))
label=os.environ.get('STXCK_LABEL','')
if label:out=root/'verification/integration-evidence'/label;out.mkdir(parents=True,exist_ok=True)
(out/'artifact.sha256').write_text(hashlib.sha256(jar.read_bytes()).hexdigest()+'  '+jar.name+'\n')
cases=[('mixedKey','key','mixed-key.txt',240),('mixedCollision','collision','mixed-collision.txt',240),('mixedLifecycle','lifecycle','mixed-lifecycle.txt',240),('mixedGround','ground','mixed-ground.txt',240),('mixed','core','mixed-results.txt',240),('mixedSafety','safety','mixed-safety.txt',180),('mixedHoppers','hoppers','mixed-hoppers.txt',180),('mixedPersistence','persistence',None,180),('mixedPersistence','persistence','mixed-persistence.txt',180),('verify','same','verification-results.csv',300),('mixedBenchmark','matrix','mixed-matrix.txt',900)]
only=os.environ.get('STXCK_CASES','').split()
if only:cases=[c for c in cases if c[1] in only]
for mode,name,result,limit in cases:
 directory='run-mixed-optimization-'+stamp+'-'+(label+'-' if label else '')+name
 if mode=='verify':
  config=root/'neoforge'/directory/'config';config.mkdir(parents=True,exist_ok=True)
  (config/'staaaaaaaaaaaack-common.toml').write_text((root/'neoforge/run-mixed/config/staaaaaaaaaaaack-common.toml').read_text().replace('enabled = true','enabled = false'))
 with (out/(mode+('-save' if result is None else '')+'.log')).open('w') as log:
  subprocess.run(['python3','verification/mixed_launch.py',mode,directory],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=limit*float(os.environ.get('STXCK_TIMEOUT_SCALE','1')),check=True,env={**os.environ,'STXCK_MATRIX_START':'4'})
 if result:
  text=(root/'neoforge'/directory/result).read_text();assert 'PASS' in text and 'FAIL' not in text,text
  shutil.copy2(root/'neoforge'/directory/result,out/result)
 if mode=='mixedBenchmark':shutil.copy2(root/'neoforge'/directory/'mixed-matrix.csv',out/'mixed-matrix.csv')
 print('PASS '+mode+(' save' if result is None else ''),flush=True)

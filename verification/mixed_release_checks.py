#!/usr/bin/env python3
"""Final artifact regression run. Always use fresh isolated worlds for persistence."""
import pathlib,subprocess,shutil,os,time,hashlib
root=pathlib.Path(__file__).resolve().parents[1];out=root/'verification/mixed-evidence';stamp=str(int(time.time()))
jar=next((root/'neoforge/build/libs').glob('*.jar'))
(out/'release-artifact.sha256').write_text(hashlib.sha256(jar.read_bytes()).hexdigest()+'  '+jar.name+'\n')
cases=[('mixedLifecycle','lifecycle','mixed-lifecycle.txt',240),('mixedGround','ground','mixed-ground.txt',240),('mixed','core','mixed-results.txt',240),('mixedSafety','safety','mixed-safety.txt',180),('mixedHoppers','hoppers','mixed-hoppers.txt',180),('mixedPersistence','persistence',None,180),('mixedPersistence','persistence','mixed-persistence.txt',180),('verify','same','verification-results.csv',300),('mixedBenchmark','matrix','mixed-matrix.txt',600)]
for mode,name,result,limit in cases:
 directory='run-mixed-release-'+stamp+'-'+name
 if mode=='verify':
  config=root/'neoforge'/directory/'config';config.mkdir(parents=True,exist_ok=True)
  (config/'staaaaaaaaaaaack-common.toml').write_text((root/'neoforge/run-mixed/config/staaaaaaaaaaaack-common.toml').read_text().replace('enabled = true','enabled = false'))
 with (out/('release-'+mode+('-save' if result is None else '')+'.log')).open('w') as log:
  subprocess.run(['python3','verification/mixed_launch.py',mode,directory],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=limit,check=True,env={**os.environ,'STXCK_MATRIX_START':'8'})
 if result:
  text=(root/'neoforge'/directory/result).read_text();assert 'PASS' in text and 'FAIL' not in text,text
  shutil.copy2(root/'neoforge'/directory/result,out/('release-'+result))
 if mode=='mixedBenchmark':shutil.copy2(root/'neoforge'/directory/'mixed-matrix.csv',out/'release-mixed-matrix.csv')
 print('PASS '+mode+(' save' if result is None else ''),flush=True)
subprocess.run(['python3','verification/mixed_checks.py','--client-only'],cwd=root,check=True,timeout=420)
subprocess.run(['python3','verification/mixed_remaining_checks.py'],cwd=root,check=True,timeout=1500)

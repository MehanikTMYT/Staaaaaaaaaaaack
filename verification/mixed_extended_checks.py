#!/usr/bin/env python3
import pathlib,subprocess,shutil
root=pathlib.Path(__file__).resolve().parents[1];out=root/'verification/mixed-evidence';out.mkdir(exist_ok=True)
cases=[('mixed','run-mixed','mixed-results.txt',240),('mixedSafety','run-mixed-safety','mixed-safety.txt',180),('mixedHoppers','run-mixed-hoppers','mixed-hoppers.txt',180),('verify','run-mixed-same-regression','verification-results.csv',300),('mixedBenchmark','run-mixed-matrix','mixed-matrix.txt',900)]
for mode,directory,result,timeout in cases:
 if mode=='verify':
  config=root/'neoforge'/directory/'config';config.mkdir(parents=True,exist_ok=True)
  (config/'staaaaaaaaaaaack-common.toml').write_text((root/'neoforge/run-mixed/config/staaaaaaaaaaaack-common.toml').read_text().replace('enabled = true','enabled = false'))
 with (out/(mode+'.log')).open('w') as log:subprocess.run(['python3','verification/mixed_launch.py',mode,directory],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=timeout,check=True)
 text=(root/'neoforge'/directory/result).read_text();assert 'FAIL' not in text and 'PASS' in text,text
 shutil.copy2(root/'neoforge'/directory/result,out/result)
 if mode=='mixedBenchmark':shutil.copy2(root/'neoforge'/directory/'mixed-matrix.csv',out/'mixed-matrix.csv')
 print('PASS '+mode,flush=True)

#!/usr/bin/env python3
import pathlib,subprocess,shutil
root=pathlib.Path(__file__).resolve().parents[1];out=root/'verification/mixed-evidence'
for mode,source,directory,result in [('mixedCreate','run-create','run-mixed-create','mixed-create-results.txt'),('gacha','run-gacha','run-mixed-gacha','gacha-results.txt'),('mixedGacha','run-gacha','run-mixed-gacha-aggregation','mixed-gacha-results.txt')]:
 mods=root/'neoforge'/directory/'mods';mods.mkdir(parents=True,exist_ok=True)
 for jar in (root/'neoforge'/source/'mods').glob('*.jar'):
  if not jar.name.startswith('staaaaaaaaaaaack-'):shutil.copy2(jar,mods/jar.name)
 (root/'neoforge'/directory/result).write_text('RUNNING')
 with (out/(mode+'-114.log')).open('w') as log:subprocess.run(['python3','verification/mixed_launch.py',mode,directory],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=240,check=True)
 text=(root/'neoforge'/directory/result).read_text();assert 'PASS' in text and 'FAIL' not in text,text;shutil.copy2(root/'neoforge'/directory/result,out/result);print('PASS '+mode,flush=True)

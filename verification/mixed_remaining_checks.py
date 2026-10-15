#!/usr/bin/env python3
"""Additional .109 release checks; run after client smoke test finishes."""
import pathlib,subprocess,os,shutil
root=pathlib.Path(__file__).resolve().parents[1];out=root/'verification/mixed-evidence'
for mode,directory,result,env,timeout in [
 ('mixedBenchmark','run-mixed-final-same-matrix','mixed-matrix.csv',{'STXCK_MATRIX_START':'4','STXCK_MATRIX_END':'7'},300),
 ('mixedVanilla','run-mixed-final-vanilla-isolated','mixed-vanilla.csv',{},600)]:
 with (out/('control-'+mode+'.log')).open('w') as log:
  subprocess.run(['python3','verification/mixed_launch.py',mode,directory],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=timeout,check=True,env={**os.environ,**env})
 data=(root/'neoforge'/directory/result).read_text()
 assert 'FAIL' not in data and (data.startswith('PASS') or len(data.splitlines())>=2),data
 shutil.copy2(root/'neoforge'/directory/result,out/(('same-' if mode=='mixedBenchmark' else 'release-')+result));print('PASS '+mode,flush=True)
subprocess.run(['python3','verification/mixed_fps_checks.py'],cwd=root,check=True,timeout=420)

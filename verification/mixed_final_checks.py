#!/usr/bin/env python3
import pathlib,subprocess,shutil,os
root=pathlib.Path(__file__).resolve().parents[1];out=root/'verification/mixed-evidence'
cases=[('mixed','run-mixed-final-core','mixed-results.txt',240),('mixedSafety','run-mixed-final-safety','mixed-safety.txt',180),('mixedHoppers','run-mixed-final-hoppers','mixed-hoppers.txt',180),('mixedPersistence','run-mixed-final-persistence',None,180),('mixedPersistence','run-mixed-final-persistence','mixed-persistence.txt',180),('mixedBenchmark','run-mixed-final-matrix','mixed-matrix.txt',600),('mixedVanilla','run-mixed-final-vanilla','mixed-vanilla.csv',600)]
for index,(mode,directory,result,limit) in enumerate(cases):
 if result:(root/'neoforge'/directory).mkdir(exist_ok=True);(root/'neoforge'/directory/result).write_text('RUNNING')
 with (out/('final-'+mode+('-save' if result is None else '')+'.log')).open('w') as log:
  subprocess.run(['python3','verification/mixed_launch.py',mode,directory],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=limit,check=True,env={**os.environ,'STXCK_MATRIX_START':'8'})
 if result:
  text=(root/'neoforge'/directory/result).read_text();assert 'FAIL' not in text and 'RUNNING' not in text,text
  assert ('PASS' in text if mode!='mixedVanilla' else len(text.splitlines())==2),text
  shutil.copy2(root/'neoforge'/directory/result,out/('final-'+result))
 if mode=='mixedBenchmark':shutil.copy2(root/'neoforge'/directory/'mixed-matrix.csv',out/'final-mixed-matrix.csv')
 print('PASS '+mode+(' save' if result is None else ''),flush=True)
subprocess.run(['python3','verification/mixed_checks.py','--client-only'],cwd=root,check=True,timeout=420)

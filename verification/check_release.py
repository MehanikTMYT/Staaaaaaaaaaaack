#!/usr/bin/env python3
"""Check final-jar persistence and client pickup; optionally check local MineColonies JARs."""
import os, pathlib, shutil, subprocess, time
root=pathlib.Path(__file__).resolve().parents[1]
neo=root/'neoforge'; evidence=root/'verification/evidence'
jar=next((neo/'build/libs').glob('*.jar'))
for name in ['run-smoke','run-client','run-persistence','run-integration']:
    directory=neo/name
    (directory/'mods').mkdir(exist_ok=True,parents=True)
    shutil.copy(jar,directory/'mods'/jar.name)
def run(name,log,env=None,extra=()):
    with open(evidence/log,'w') as out:
        subprocess.run(['python3','verification/launch.py',name,'--artifact',*extra],cwd=root,stdout=out,stderr=subprocess.STDOUT,env=env,timeout=180,check=True)
# This world was saved in a previous process; do not replace it.
(neo/'run-persistence/persistence-results.txt').write_text('RUNNING')
run('runPersistenceServer','persistence-final.log')
assert (neo/'run-persistence/persistence-results.txt').read_text().startswith('PASS')
print('Final JAR persistence PASS',flush=True)
(neo/'run-client/client-verification.txt').write_text('RUNNING')
with open(evidence/'server-final.log','w') as out:
    p=subprocess.Popen(['python3','verification/launch.py','runSmokeServer','--artifact'],cwd=root,stdout=out,stderr=subprocess.STDOUT)
    try:
        deadline=time.monotonic()+90
        while 'Done (' not in (evidence/'server-final.log').read_text():
            if p.poll() is not None or time.monotonic()>deadline:raise RuntimeError('smoke server failed')
            time.sleep(.5)
        env=os.environ.copy();env['LIBGL_ALWAYS_SOFTWARE']='1';env['ALSOFT_DRIVERS']='null'
        with open(evidence/'client-final.log','w') as log:
            subprocess.run(['xvfb-run','-a','-s','-screen 0 1024x768x24','python3','verification/launch.py','runVerifyClient','--artifact'],cwd=root,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=150,check=True)
        assert 'count_sync=true inventory=1000' in (neo/'run-client/client-verification.txt').read_text()
        p.wait(timeout=45)
        assert 'pickup=1000' in (evidence/'server-final.log').read_text()
    finally:
        if p.poll() is None:p.terminate();p.wait(timeout=30)
print('Final JAR client/server pickup PASS',flush=True)
if list((neo/'run-integration/mods').glob('minecolonies-*.jar')):
    env=os.environ.copy();env['JAVA_TOOL_OPTIONS']='-Dstxck.minecolonies=true'
    (neo/'run-integration/minecolonies-results.txt').write_text('RUNNING')
    run('runSmokeServer','minecolonies-final.log',env,('--directory','run-integration'))
    assert (neo/'run-integration/minecolonies-results.txt').read_text().startswith('PASS')
    print('Final JAR MineColonies pickup component PASS',flush=True)

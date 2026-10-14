#!/usr/bin/env python3
"""Steady-state FPS on a real software-rendered client and separate dedicated server."""
import os, pathlib, shutil, subprocess, time
root=pathlib.Path(__file__).resolve().parents[1]
neo=root/'neoforge'
for mode in ['vanilla','stxck']:
    server=neo/f'run-fps-{mode}-server';client=neo/f'run-fps-{mode}-client'
    server.mkdir(exist_ok=True);client.mkdir(exist_ok=True)
    (client/'fps-results.csv').unlink(missing_ok=True)
    (server/'eula.txt').write_text('eula=true\n')
    props=(neo/'run-smoke/server.properties').read_text()
    props+='\nlevel-seed=18261\ngenerator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}]}\n'
    (server/'server.properties').write_text(props)
    shutil.copy(neo/'run-client/options.txt',client/'options.txt')
    if mode=='stxck':
        for directory in [server,client]:
            (directory/'mods').mkdir(exist_ok=True)
            for jar in (neo/'build/libs').glob('*.jar'):shutil.copy(jar,directory/'mods'/jar.name)
    switch='--artifact' if mode=='stxck' else '--without-mod'
    server_log=open(root/f'verification/evidence/fps-{mode}-server.log','w')
    client_log=open(root/f'verification/evidence/fps-{mode}-client.log','w')
    env=os.environ.copy();env['JAVA_TOOL_OPTIONS']='-Dstxck.fpsServer=true'
    p=subprocess.Popen(['python3','verification/launch.py','runSmokeServer',switch,'--directory',server.name],cwd=root,env=env,stdout=server_log,stderr=subprocess.STDOUT)
    try:
        deadline=time.monotonic()+100
        while 'Done (' not in (root/f'verification/evidence/fps-{mode}-server.log').read_text():
            if p.poll() is not None or time.monotonic()>deadline:raise RuntimeError(f'{mode} server failed to start')
            time.sleep(.5)
        env=os.environ.copy();env['JAVA_TOOL_OPTIONS']=f'-Dstxck.fpsClient=true -Dstxck.fpsPhase={server}/fps-phase.txt'
        env['LIBGL_ALWAYS_SOFTWARE']='1';env['ALSOFT_DRIVERS']='null'
        c=subprocess.run(['xvfb-run','-a','-s','-screen 0 1024x768x24','python3','verification/launch.py','runVerifyClient',switch,'--directory',client.name],cwd=root,env=env,stdout=client_log,stderr=subprocess.STDOUT,timeout=200)
        if c.returncode:raise RuntimeError(f'{mode} client exited {c.returncode}')
        result=client/'fps-results.csv'
        if not result.exists():raise RuntimeError(f'{mode} FPS result missing')
        assert len(result.read_text().strip().splitlines()) == (4 if mode=='vanilla' else 7), 'incomplete FPS matrix'
        shutil.copy(result,root/f'verification/evidence/fps-{mode}.csv')
        print(result.read_text(),flush=True)
        p.wait(timeout=25)
    finally:
        if p.poll() is None:p.terminate();p.wait(timeout=30)
        server_log.close();client_log.close()

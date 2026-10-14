#!/usr/bin/env python3
"""Run an exported development launch without retaining a Gradle daemon."""
import json, os, pathlib, shutil, sys
root = pathlib.Path(__file__).resolve().parents[1]
spec = json.loads((root / 'neoforge/build/verification-launch.json').read_text())[sys.argv[1]]
args = spec['argv']
args[0] = args[0] or shutil.which('java')
if '--artifact' in sys.argv or '--without-mod' in sys.argv:
    for i, arg in enumerate(args):
        if isinstance(arg, str) and arg.startswith('-Dfml.modFolders='):
            args[i] = '-Dfml.modFolders=' + ':'.join(x for x in arg.split('=',1)[1].split(':') if x.startswith('stxck_verification%%'))
devlaunch = list(pathlib.Path.home().glob('.gradle/caches/modules-2/files-2.1/net.neoforged/DevLaunch/*/*/DevLaunch-*.jar'))
args[args.index('-cp') + 1] += ':' + str(devlaunch[-1])
directories = {'runPersistenceServer':'run-persistence', 'runVerifyServer':'run-verify',
               'runVanillaBenchmark':'run-vanilla', 'runSmokeServer':'run-smoke', 'runVerifyClient':'run-client'}
os.chdir(root / 'neoforge' / (sys.argv[sys.argv.index('--directory')+1] if '--directory' in sys.argv else directories[sys.argv[1]]))
os.execv(args[0], args)

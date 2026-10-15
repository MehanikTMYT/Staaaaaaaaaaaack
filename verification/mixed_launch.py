#!/usr/bin/env python3
"""Isolated artifact launch; modes mixed and mixedPersistence."""
import json,os,pathlib,shutil,sys,shlex
root=pathlib.Path(__file__).resolve().parents[1]
mode=sys.argv[1];directory=root/'neoforge'/sys.argv[2]
directory.mkdir(exist_ok=True);(directory/'mods').mkdir(exist_ok=True)
if mode in ('mixedClient','mixedFpsClient'):
 shutil.copy2(root/'neoforge/run-client/options.txt',directory/'options.txt')
 os.environ['ALSOFT_DRIVERS']='null'
jar=next((root/'neoforge/build/libs').glob('*.jar'))
for old in (directory/'mods').glob('staaaaaaaaaaaack-*.jar'):
 if old.name!=jar.name:old.unlink()
if mode!='mixedVanilla':shutil.copy2(jar,directory/'mods'/jar.name)
elif (directory/'mods'/jar.name).exists():(directory/'mods'/jar.name).unlink()
(directory/'eula.txt').write_text('eula=true\n')
if not (directory/'server.properties').exists():(directory/'server.properties').write_text('pause-when-empty-seconds=0\nserver-ip=127.0.0.1\nonline-mode=false\nserver-port=25584\nlevel-type=minecraft:flat\ngenerate-structures=false\nspawn-protection=0\nview-distance=2\nsimulation-distance=2\n')
if mode in ('mixedBenchmark','mixedVanilla'):
 prop=directory/'server.properties'
 prop.write_text(prop.read_text()+'\nmax-tick-time=-1\npause-when-empty-seconds=0\n')
args=json.loads((root/'neoforge/build/verification-launch.json').read_text())['runVerifyClient' if mode in ('mixedClient','mixedFpsClient') else 'runSmokeServer']['argv']
args=[part for arg in args for part in (shlex.split(pathlib.Path(arg[1:]).read_text(),comments=True) if isinstance(arg,str) and arg.startswith('@') else [arg])]
args=[a for a in args if not isinstance(a,str) or not a.startswith('-Dstxck.')]
args=[a.replace('127.0.0.1:25580','127.0.0.1:25584') if isinstance(a,str) else a for a in args]
args.insert(1,'-Dstxck.'+mode+'=true')
if mode=='mixedBenchmark':
 args.insert(1,'-Dstxck.matrixStart='+os.environ.get('STXCK_MATRIX_START','0'))
 args.insert(1,'-Dstxck.matrixEnd='+os.environ.get('STXCK_MATRIX_END','14'))
if mode=='mixedFpsClient':args.insert(1,'-Dstxck.mixedFpsPhase='+str(root/'neoforge/run-mixed-fps-server/fps-phase.txt'))
args[0]=args[0] or shutil.which('java')
for i,a in enumerate(args):
 if isinstance(a,str) and a.startswith('-Dfml.modFolders='):args[i]='-Dfml.modFolders='+':'.join(x for x in a.split('=',1)[1].split(':') if x.startswith('stxck_verification%%'))
if mode=='mixedVanilla':
 isolated=directory/'vanilla-verification';isolated.mkdir(exist_ok=True)
 for i,a in enumerate(args):
  if isinstance(a,str) and a.startswith('-Dfml.modFolders='):
   folders=[]
   for entry in a.split('=',1)[1].split(':'):
    name,path=entry.split('%%',1);source=pathlib.Path(path)
    if (source/'me').exists():
     target=isolated/'me/frankv/staaaaaaaaaaaack/verify';target.mkdir(parents=True,exist_ok=True)
     for cls in source.rglob('MixedVanillaBenchmark*.class'):shutil.copy2(cls,target/cls.name)
     folders.append(name+'%%'+str(isolated))
    else:folders.append(entry)
   args[i]='-Dfml.modFolders='+':'.join(folders)
devlaunch=list(pathlib.Path.home().glob('.gradle/caches/modules-2/files-2.1/net.neoforged/DevLaunch/*/*/DevLaunch-*.jar'))
args[args.index('-cp')+1]+=':'+str(devlaunch[-1]);os.chdir(directory);os.execv(args[0],args)

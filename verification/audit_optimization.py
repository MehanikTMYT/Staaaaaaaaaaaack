#!/usr/bin/env python3
"""Audit final runtime evidence without treating isolated flat-world preset warnings as mod errors."""
from pathlib import Path
import re,zipfile,hashlib
root=Path(__file__).resolve().parents[1];out=root/'verification/optimization-evidence'
pattern=re.compile(r'MixinApplyError|ClassNotFoundException|NoSuchMethodError|DecoderException|EncoderException|Exception.*(?:serializ|packet)|FAIL |AssertionError')
logs=list(out.glob('*.log'));bad=[]
for log in logs:
 for i,line in enumerate(log.read_text(errors='replace').splitlines(),1):
  if pattern.search(line):bad.append(f'{log.name}:{i}: {line}')
assert not bad,'\n'.join(bad)
jar=root/'artifacts/mixed/staaaaaaaaaaaack-neoforge-26.1.2-1.8-port.2-mixed.2.jar'
with zipfile.ZipFile(jar) as z:
 names=z.namelist();assert any('LICENSE' in n for n in names)
 assert not any('verify/' in n or n=='fabric.mod.json' or n.startswith('net/fabricmc/') for n in names)
report=f'PASS {len(logs)} logs: no targeted mixin/linkage/packet/serialization/assertion failures. Artifact contains license, no verification classes or Fabric loader metadata.\nSHA256 {hashlib.sha256(jar.read_bytes()).hexdigest()}\n'
(out/'audit.txt').write_text(report);print(report)

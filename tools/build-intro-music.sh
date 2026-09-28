#!/usr/bin/env bash
set -euo pipefail
reference_zip=${1:?Pass the supplied Air_Defense_Digital7_Intro.zip path}
project_dir=$(cd "$(dirname "$0")/.." && pwd)
python3 - "$reference_zip" "$project_dir" <<'PY'
import sys
from pathlib import Path
from zipfile import ZipFile
with ZipFile(sys.argv[1]) as z:
 for source,target in [('intro-music.mp3','intro_music.mp3'),('digital-7.ttf','fonts/digital-7.ttf'),('FONT-LICENSE.txt','fonts/Digital-7-LICENSE.txt')]:
  dest=Path(sys.argv[2])/'assets'/target
  dest.parent.mkdir(parents=True,exist_ok=True)
  dest.write_bytes(z.read('air-defense-digital7/assets/'+source))
PY

#!/usr/bin/env bash
set -euo pipefail
source_track=${1:?Pass the supplied the_watcher_s_desk.mp3 path}
project_dir=$(cd "$(dirname "$0")/.." && pwd)
ffmpeg -y -v error -i "$source_track" -filter_complex '[0:a]asplit=2[a][b];[a][b]acrossfade=d=3:c1=tri:c2=tri,atrim=duration=76,afade=t=in:st=0:d=0.8,afade=t=out:st=73.5:d=2.5[out]' -map '[out]' -c:a libmp3lame -b:a 192k "$project_dir/assets/intro_music.mp3"

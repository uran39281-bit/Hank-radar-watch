package com.jb.radar;
import android.content.res.AssetManager;import android.content.res.AssetFileDescriptor;import android.media.*;

/** The supplied track is crossfaded into its repeat in the bundled 76s mix. */
final class IntroAudio {
 MediaPlayer player;boolean ready,failed,wanted,muted;
 void open(AssetManager assets){close();failed=false;try{final MediaPlayer current=new MediaPlayer();player=current;current.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());try(AssetFileDescriptor fd=assets.openFd("intro_music.mp3")){current.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}current.setLooping(false);current.setVolume(.65f,.65f);current.setOnPreparedListener(m->{if(player!=m)return;ready=true;sync(wanted,!muted);});current.setOnErrorListener((m,w,e)->{if(player==m){failed=true;ready=false;}return true;});current.prepareAsync();}catch(Exception e){failed=true;if(player!=null)player.release();player=null;}}
 void sync(boolean play,boolean music){wanted=play;muted=!music;if(player==null||!ready)return;try{player.setVolume(music?.65f:0,music?.65f:0);if(play&&!player.isPlaying())player.start();else if(!play&&player.isPlaying())player.pause();}catch(IllegalStateException e){failed=true;ready=false;}}
 double position(){if(player==null||!ready)return -1;try{return player.getCurrentPosition()/1000.0;}catch(IllegalStateException e){failed=true;ready=false;return -1;}}
 void close(){wanted=false;ready=false;if(player!=null){player.release();player=null;}}
}

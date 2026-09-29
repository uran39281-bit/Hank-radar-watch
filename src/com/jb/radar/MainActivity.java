package com.jb.radar;
import android.app.*;import android.os.*;import android.view.*;import android.graphics.*;import android.media.*;import java.util.*;
public class MainActivity extends Activity {
 Radar view; MediaPlayer menuMusic;boolean foreground,musicReady,hasFocus;AudioManager audioManager;
 final IntroAudio introAudio=new IntroAudio();
 final AudioManager.OnAudioFocusChangeListener focusListener=change->{
  hasFocus=change==AudioManager.AUDIOFOCUS_GAIN;
  if(view!=null&&view.intro.active){if(!hasFocus)view.captureIntro();view.last=System.nanoTime();introAudio.sync(hasFocus&&foreground&&!view.intro.paused,view.intro.music);}
  else if(hasFocus){if(foreground&&view!=null&&!view.started&&musicReady)menuMusic.start();}
  else if(menuMusic!=null&&musicReady&&menuMusic.isPlaying())menuMusic.pause();
 };
 void syncMusic(){
  boolean cinematic=view!=null&&view.intro.active;
  boolean wanted=foreground&&view!=null&&(!view.started||cinematic);
  if(audioManager==null)audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
  if(cinematic){if(menuMusic!=null&&musicReady&&menuMusic.isPlaying())menuMusic.pause();if(wanted&&!hasFocus)hasFocus=audioManager.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;introAudio.sync(wanted&&hasFocus&&!view.intro.paused,view.intro.music);return;}
  introAudio.sync(false,false);
  if(!wanted){if(menuMusic!=null&&musicReady&&menuMusic.isPlaying())menuMusic.pause();if(hasFocus)audioManager.abandonAudioFocus(focusListener);hasFocus=false;return;}
  if(menuMusic==null){try{menuMusic=new MediaPlayer();menuMusic.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());try(android.content.res.AssetFileDescriptor fd=getAssets().openFd("menu_theme.mp3")){menuMusic.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}menuMusic.setLooping(true);menuMusic.setVolume(.65f,.65f);menuMusic.setOnPreparedListener(m->{musicReady=true;syncMusic();});menuMusic.setOnErrorListener((m,w,e)->{musicReady=false;return true;});menuMusic.prepareAsync();}catch(Exception e){if(menuMusic!=null)menuMusic.release();menuMusic=null;musicReady=false;}}
  if(musicReady){if(!hasFocus)hasFocus=audioManager.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(hasFocus&&!menuMusic.isPlaying())menuMusic.start();}
 }

 public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(1024,1024);getWindow().addFlags(128);view=new Radar();setContentView(view);view.setOnApplyWindowInsetsListener((v,i)->{int l=i.getSystemWindowInsetLeft(),r=i.getSystemWindowInsetRight(),t=i.getSystemWindowInsetTop(),bt=i.getSystemWindowInsetBottom();if(Build.VERSION.SDK_INT>=28&&i.getDisplayCutout()!=null){DisplayCutout d=i.getDisplayCutout();l=Math.max(l,d.getSafeInsetLeft());r=Math.max(r,d.getSafeInsetRight());t=Math.max(t,d.getSafeInsetTop());bt=Math.max(bt,d.getSafeInsetBottom());}v.setPadding(l,t,r,bt);return i;});}
 public void onResume(){super.onResume();foreground=true;syncMusic();if(view!=null){if(view.economy.saveFailed)view.economy.flush();view.last=System.nanoTime();view.invalidate();}}
 public void onPause(){if(view!=null)view.captureIntro();foreground=false;syncMusic();super.onPause();if(view!=null){view.economy.flush();view.paused=true;view.last=0;}}
 public void onDestroy(){introAudio.close();if(menuMusic!=null){menuMusic.release();menuMusic=null;}if(view!=null&&view.tone!=null)view.tone.release();super.onDestroy();}
 @Override public void onBackPressed(){if(view.intro.active){view.act("intropause");return;}if(view.treeOpen){view.treeOpen=false;view.invalidate();return;}if(view.equipmentOpen){view.equipmentOpen=false;view.invalidate();return;}if(view.economyOpen){view.economyOpen=false;view.invalidate();return;}if(!view.started&&view.selectionScreen>0){view.act("selectionback");view.invalidate();return;}view.statusOpen=false;view.paused=true;view.help=false;view.invalidate();}
 class Radar extends View {
  final IntroSequence intro=new IntroSequence();final IntroScene introScene=new IntroScene(Typeface.createFromAsset(getAssets(),"fonts/digital-7.ttf"));
  final int green=0xff00ff00,muted=0xff70b870,amber=0xffeeeeee,red=0xffffffff,bg=0xff000000,border=0xff164516,panel=0xff020a02;
  Economy economy=new Economy(new EconomyStore(getPreferences(0)));boolean economyOpen,equipmentOpen,treeOpen,profileFallback,statusOpen;String treeSelected="stonebolt",treeNotice="SELECT EQUIPMENT / RESEARCH > BUY > EQUIP";int equipmentIndex;Equipment equipment=loadEquipment();CombatRules combatRules=loadCombatRules();Paint p=new Paint(3);Game game=new Game(System.nanoTime(),economy,equipment,combatRules);Game.Contact selected;boolean guidanceLines=true;ArrayList<Btn> buttons=new ArrayList<>();long last,lastSeekerTone;float scale,ox,oy;boolean paused=false,started=false,sound=false,ended=false,help=false;int speed=1,best=getPreferences(0).getInt("hawk_best",0);String notice="MISSION 1 / ADS-201 WATCHPOST";ToneGenerator tone;Bitmap launcherIcon;
  class Btn{RectF r;String id;boolean enabled;Btn(float x,float y,float w,float h,String i,boolean e){r=new RectF(x,y,x+w,y+h);id=i;enabled=e;}}
  Equipment loadEquipment(){try(java.io.InputStream in=getAssets().open("equipment.properties")){return Equipment.load(in);}catch(java.io.IOException|IllegalArgumentException ex){profileFallback=true;return Equipment.defaults();}}
  CombatRules loadCombatRules(){try(java.io.InputStream in=getAssets().open("combat.properties")){return CombatRules.load(in);}catch(java.io.IOException|IllegalArgumentException ex){profileFallback=true;return CombatRules.defaults();}}
  Radar(){super(MainActivity.this);setFocusable(true);try(java.io.InputStream stream=getAssets().open("hawk_launcher.png")){BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=4;launcherIcon=BitmapFactory.decodeStream(stream,null,opts);}catch(java.io.IOException e){launcherIcon=null;}try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,30);}catch(Exception e){}}
  void text(Canvas c,String s,float x,float y,float size,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.MONOSPACE);p.setTextSize(size);c.drawText(s,x,y,p);}
  void rect(Canvas c,float x,float y,float w,float h,int col,boolean fill){p.setColor(col);p.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRoundRect(new RectF(x,y,x+w,y+h),7,7,p);p.setStyle(Paint.Style.FILL);}
  void line(Canvas c,float x,float y,float xx,float yy,int col){p.setColor(col);p.setStrokeWidth(1);c.drawLine(x,y,xx,yy,p);}
  void circle(Canvas c,float x,float y,float r,int col,boolean fill){p.setColor(col);p.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);p.setStrokeWidth(1);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);}
  void button(Canvas c,String id,String label,float x,float y,float w,boolean enabled){rect(c,x,y,w,48,enabled?border:0xff061006,true);rect(c,x,y,w,48,enabled?muted:border,false);text(c,label,x+12,y+30,15,enabled?green:muted);buttons.add(new Btn(x,y,w,48,id,enabled));}
  String fmt(String s,Object...o){return String.format(Locale.US,s,o);}void beep(){if(sound&&tone!=null)tone.startTone(ToneGenerator.TONE_PROP_BEEP,60);}
  protected void onDraw(Canvas c){super.onDraw(c);long now=System.nanoTime();if(intro.active&&foreground&&!intro.paused&&hasFocus){double audioTime=introAudio.position();if(audioTime>=0)intro.sync(audioTime);else if(introAudio.failed&&last!=0)intro.advance((now-last)/1e9);if(intro.done())finishIntro();}if(last!=0&&!intro.active&&!paused&&started){double remaining=Math.min(.15,(now-last)/1e9)*speed;while(remaining>0){double d=Math.min(.05,remaining);game.tick(d);remaining-=d;}}last=now;
   if(game.finished&&!ended){ended=true;if(game.score>best){best=game.score;getPreferences(0).edit().putInt("hawk_best",best).apply();}}
   float aw=getWidth()-getPaddingLeft()-getPaddingRight(),ah=getHeight()-getPaddingTop()-getPaddingBottom();scale=Math.min(aw/1280f,ah/720f);ox=getPaddingLeft()+(aw-1280*scale)/2;oy=getPaddingTop()+(ah-720*scale)/2;c.drawColor(bg);c.save();c.translate(ox,oy);c.scale(scale,scale);buttons.clear();if(intro.active){introScreen(c);c.restore();if(isShown()&&foreground)postInvalidateDelayed(16);return;}if(!started){if(treeOpen)techTreeScreen(c);else if(equipmentOpen)equipmentScreen(c);else if(economyOpen)economyScreen(c);else if(selectionScreen>0)selectionMenu(c);else mainMenu(c);c.restore();if(isShown())postInvalidateDelayed(33);return;}
   text(c,"CMD "+game.battery.parts[Battery.COMMAND].percent()+"% / RADAR "+game.battery.parts[Battery.RADAR].percent()+"%",20,31,18,green);text(c,game.battery.warning(game.elapsed),20,54,12,muted);
   Economy.State wallet=economy.snapshot();text(c,"$"+compact(wallet.dollars)+"   BP "+compact(wallet.bp),405,33,18,green);if(economy.saveFailed)text(c,"SAVE PENDING",665,54,11,amber);
   button(c,"status","BATTERY",705,8,140,true);button(c,"speed",speed+"x TIME",855,8,120,true);button(c,"help","GUIDE",985,8,120,true);button(c,"pause","PAUSE",1115,8,145,!game.finished);
   telemetry(c);scope(c);engagement(c);launchers(c);controlBar(c);seekerPanel(c);if(game.elapsed<game.impactUntil){rect(c,405,180,390,130,bg,true);rect(c,405,180,390,130,green,false);int flash=((int)(game.elapsed*5)%2==0)?green:amber;text(c,"BATTERY IMPACT",430,220,27,flash);text(c,game.battery.warning(game.elapsed),430,254,17,flash);text(c,fmt("CONDITION -%.1f%% / CHECK BATTERY",game.lastDamage),430,284,15,flash);}
   text(c,notice,20,717,11,amber);
   if(statusOpen)batteryScreen(c);else if(!started||paused||game.finished)overlay(c);c.restore();if(isShown())postInvalidateDelayed(33);
  }
  void captureIntro(){if(intro.active&&!intro.paused){double t=introAudio.position();if(t>=0)intro.sync(t);}}
  void beginIntro(){selectionScreen=0;game.running=false;game.ir.cancel();intro.start();introAudio.open(getAssets());started=false;paused=help=statusOpen=economyOpen=equipmentOpen=treeOpen=false;selected=null;last=System.nanoTime();syncMusic();}
  void finishIntro(){intro.stop();introAudio.close();last=0;if(!game.start()){started=false;paused=false;economyOpen=true;notice="SAVE FAILED / RETRY IN ECONOMY";syncMusic();return;}selected=null;started=true;ended=false;speed=1;paused=help=statusOpen=false;notice="SEARCH ACTIVE / AUTOMATIC TRACKING ENABLED";syncMusic();}
  void introButton(Canvas c,String id,String label,float x,float y,float w,boolean enabled){introScene.control(c,label,x,y,w);buttons.add(new Btn(x,y,w,56,id,true));}
  void introScreen(Canvas c){introScene.draw(c,intro);introButton(c,"intropause",intro.paused?"RESUME":"PAUSE",55,640,230,true);introButton(c,"intromusic",intro.music?"MUSIC ON":"MUSIC OFF",517,640,245,true);introButton(c,"introskip","SKIP INTRO >",995,640,230,true);if(introAudio.failed)text(c,"MUSIC UNAVAILABLE",55,610,14,IntroScene.GREEN);}
  int selectionScreen;String loadoutNotice="SELECT YOUR MISSILE / LOADOUT APPLIES TO ALL LAUNCHERS";final MissionScene missionScene=new MissionScene();
  final MenuScene menuScene=new MenuScene();
  void mainMenu(Canvas c){Economy.State wallet=economy.snapshot();menuScene.draw(c,System.nanoTime()/1e9,"$"+compact(wallet.dollars),compact(wallet.bp),economy.saveFailed,profileFallback);buttons.add(new Btn(52,309,554,91,"playmodes",true));buttons.add(new Btn(52,418,554,71,"techtree",true));buttons.add(new Btn(52,504,554,71,"economy",true));buttons.add(new Btn(52,590,554,71,"equipment",true));}
  void selectionMenu(Canvas c){double time=System.nanoTime()/1e9;
   if(selectionScreen==1){missionScene.modes(c,time);buttons.add(new Btn(150,202,445,263,"storymode",true));buttons.add(new Btn(685,202,445,263,"survival",false));buttons.add(new Btn(28,640,200,64,"selectionback",true));}
   else if(selectionScreen==2){missionScene.missions(c,time);buttons.add(new Btn(28,62,210,64,"selectionback",true));for(int i=0;i<8;i++)buttons.add(new Btn(115+(i%4)*284,178+(i/4)*209,195,140,"mission:"+i,i==0));buttons.add(new Btn(36,594,280,76,"missionstore",true));buttons.add(new Btn(964,594,280,76,"missionloadout",true));}
   else{Economy.State w=economy.snapshot();missionScene.loadout(c,time,w,equipment,economy.saveFailed?"SAVE FAILED / RETRY IN STORE":loadoutNotice);buttons.add(new Btn(36,594,280,76,"selectionback",true));buttons.add(new Btn(876,594,368,76,"missiondeploy",!economy.saveFailed));Equipment.Weapon[] choices=playerWeapons();for(int i=0;i<choices.length;i++)buttons.add(new Btn(64+i%2*592,314+i/2*112,560,98,"loadout:"+choices[i].id,w.owned.contains(choices[i].id)&&!economy.saveFailed));}
  }
  String compact(long n){if(n<1000000)return fmt("%,d",n);if(n<1000000000L)return fmt("%.1fM",n/1000000.0);if(n<1000000000000L)return fmt("%.1fB",n/1000000000.0);return fmt("%.1fT",n/1000000000000.0);}
  void rewardRow(Canvas c,String label,long dollars,long bp,float y){text(c,label,245,y,14,muted);text(c,"$"+compact(dollars),483,y,15,green);text(c,"+"+compact(bp),599,y,15,amber);}
  void rewardReceipt(Canvas c){Economy.State w=economy.snapshot();rect(c,225,175,460,320,border,false);text(c,"MISSION REWARDS",245,202,16,green);text(c,"DOLLARS",483,202,12,muted);text(c,"BP",599,202,12,muted);
   rewardRow(c,"AIRCRAFT x "+w.aircraft,(long)w.aircraft*Economy.AIR_DOLLARS,(long)w.aircraft*Economy.AIR_BP,239);
   rewardRow(c,"MISSILES x "+w.missiles,(long)w.missiles*Economy.MISSILE_DOLLARS,(long)w.missiles*Economy.MISSILE_BP,278);
   boolean win="VICTORY".equals(w.result);rewardRow(c,"VICTORY BONUS",win?Economy.WIN_DOLLARS:0,win?Economy.WIN_BP:0,317);rewardRow(c,"INTEGRITY BONUS x "+w.remainingHits,(long)w.remainingHits*Economy.HIT_DOLLARS,(long)w.remainingHits*Economy.HIT_BP,356);line(c,245,375,665,375,border);rewardRow(c,"TOTAL EARNED",w.missionDollars,w.missionBP,409);text(c,economy.saveFailed?"SAVE PENDING / RETRY IN ECONOMY":"REWARDS SAVED",245,450,13,amber);text(c,"Combat earnings are kept after defeat.",245,478,11,muted);}
  void economyScreen(Canvas c){Economy.State w=economy.snapshot();text(c,"ECONOMY",35,55,30,green);button(c,"economyback","BACK",1095,20,150,true);
   rect(c,35,90,595,143,panel,true);rect(c,650,90,595,143,panel,true);text(c,"DOLLARS",57,120,16,muted);text(c,"$"+fmt("%,d",w.dollars),57,164,30,green);text(c,"Buy researched equipment in the USA tech tree.",57,207,14,muted);text(c,"BP / BATTLE POINTS = XP",673,120,16,muted);text(c,fmt("%,d",w.bp)+" BP",673,164,30,green);text(c,"Spend earned BP to research new equipment.",673,207,14,muted);
   text(c,"EARN REWARDS",55,280,19,green);text(c,"ACTION",55,320,14,muted);text(c,"DOLLARS",408,320,14,muted);text(c,"BP",544,320,14,muted);
   String[] names={"Aircraft destroyed","Incoming missile intercepted","Mission victory","Each 25% condition tier on victory"};int[] cash={Economy.AIR_DOLLARS,Economy.MISSILE_DOLLARS,Economy.WIN_DOLLARS,Economy.HIT_DOLLARS};int[] points={Economy.AIR_BP,Economy.MISSILE_BP,Economy.WIN_BP,Economy.HIT_BP};for(int i=0;i<4;i++){float y=361+i*47;text(c,names[i],55,y,13,muted);text(c,"$"+cash[i],408,y,17,green);text(c,"+"+points[i],544,y,17,amber);}
   rect(c,650,260,595,298,border,false);text(c,"LAST MISSION / "+w.result,673,295,18,green);text(c,"EARNED $"+compact(w.missionDollars)+"  /  "+compact(w.missionBP)+" BP",673,343,20,amber);text(c,w.aircraft+" aircraft / "+w.missiles+" incoming missiles",673,382,15,muted);text(c,"VICTORIES "+w.wins+" / COMPLETED "+w.completed,673,423,14,muted);text(c,"LIFETIME $"+compact(w.lifetimeDollars)+" / "+compact(w.lifetimeBP)+" BP",673,464,14,muted);text(c,economy.saveFailed?"SAVE PENDING":"PROGRESS SAVED ON THIS DEVICE",673,518,13,amber);
   text(c,"Combat rewards save immediately. Victory bonuses are paid once at mission end.",55,601,14,muted);text(c,"Use USA TECH TREE to research, buy and equip. Ammo and repairs are free.",55,630,14,muted);text(c,"Progress survives app updates; uninstalling or clearing app data removes it.",55,659,13,muted);if(economy.saveFailed)button(c,"retrysave","RETRY SAVE",1015,574,230,true);
  }
  Equipment.Weapon[] playerWeapons(){return new Equipment.Weapon[]{equipment.primary,equipment.stonebolt,equipment.active,equipment.infrared};}
  void stat(Canvas c,String label,String value,float x,float y){text(c,label,x,y,13,muted);text(c,value,x+277,y,15,green);}
  void equipmentScreen(Canvas c){Equipment.Radar r=equipment.radar;Equipment.Weapon w=playerWeapons()[equipmentIndex];
   text(c,"EQUIPMENT",35,55,30,green);button(c,"equipmentback","BACK",1095,20,150,true);
   rect(c,35,90,595,488,panel,true);rect(c,35,90,595,488,border,false);rect(c,650,90,595,488,panel,true);rect(c,650,90,595,488,border,false);
   text(c,"RADAR SYSTEM",57,120,13,muted);text(c,r.name,57,154,24,green);
   stat(c,"Detect effective / limit",fmt("%d / %.0f km",r.detectionKm,r.detectionAbsoluteKm),57,197);stat(c,"Track effective / limit",fmt("%.0f / %.0f km",r.trackingKm,r.trackingAbsoluteKm),57,235);stat(c,"Lock effective / limit",fmt("%.0f / %.0f km",r.lockKm,r.lockAbsoluteKm),57,273);stat(c,"Simultaneous tracks",""+r.tracks,57,311);stat(c,"Illumination channels",""+r.illuminationChannels,57,349);stat(c,"Midcourse channels",""+r.midcourseChannels,57,383);stat(c,"S-A supported missiles",""+r.supportedMissiles,57,417);stat(c,"Full sweep",fmt("%.1f s / SPEED %.2f",r.sweepSeconds(),r.scanSpeed),57,451);stat(c,"IRST sensor",r.infrared?"YES":"NOT FITTED",57,485);text(c,"IR missiles use their own seeker; IRST is optional.",57,522,12,muted);text(c,"Radar damage reduces available range.",57,551,12,muted);
   text(c,"USA MISSILES / "+(equipmentIndex+1)+" OF "+playerWeapons().length,673,120,13,muted);text(c,w.name,673,154,24,green);
   stat(c,"Guidance",w.guidance.toString(),673,193);stat(c,"Radar mode",w.radarMode.toString().replace('_',' '),673,225);stat(c,"Maximum guidance",fmt("%.0f s",w.guidanceSeconds),673,257);stat(c,"Maximum speed",fmt("%,.0f km/h",w.maxSpeedKmh),673,289);stat(c,"Maximum G load",fmt("%.1f G",w.maxG),673,321);stat(c,"Mass",fmt("%,.0f kg",w.massKg),673,353);stat(c,"Maximum AOA",fmt("%.0f deg",w.maxAoADeg),673,385);stat(c,"Maximum thrust",fmt("%,.0f N",w.thrustN),673,417);stat(c,"Motor burn",fmt("%.1f s",w.burnSeconds),673,449);stat(c,"Flight range",fmt("%.1f km",w.rangeKm),673,481);stat(c,"Seeker / ceiling",(w.guidance==Equipment.Guidance.INFRARED||w.radarMode==Equipment.RadarMode.ACTIVE?fmt("%.1f km",w.seekerKm):"--")+fmt(" / %.0f m",w.ceilingM),673,513);text(c,fmt("WARHEAD %.1f kg / BLAST %.2f km",w.explosiveKg,w.blastRadiusKm),673,551,12,muted);
   text(c,"INTRO FONT: DIGITAL-7 / SIZENKO ALEXANDER, STYLE-7",55,706,12,muted);button(c,"equipprev","< PREVIOUS",650,596,280,true);button(c,"equipnext","NEXT >",950,596,295,true);
   text(c,"FICTIONAL GAME BALANCE",55,617,15,green);text(c,"Thrust and mass control acceleration; turns cost speed.",55,644,13,muted);text(c,"Research, buy and equip your missiles in the USA TECH TREE.",55,686,14,muted);
  }
  void techCard(Canvas c,TechTree.Node n,float x,float y,Economy.State w){boolean selected=n.id.equals(treeSelected),owned=w.owned.contains(n.id);rect(c,x,y,304,91,selected?0xff082208:panel,true);rect(c,x,y,304,91,selected?amber:owned?green:border,false);
   text(c,n.name,x+14,y+25,18,green);text(c,TechTree.status(w,n),x+14,y+48,12,selected?amber:green);
   text(c,n.starter()?"STARTER / FREE":TechTree.progress(w,n)+" / "+n.bp+" BP   $"+compact(n.dollars),x+14,y+73,12,muted);buttons.add(new Btn(x,y,304,91,"node:"+n.id,true));
  }
  void treeStat(Canvas c,String label,String value,float y){text(c,label,780,y,13,muted);text(c,value,1055,y,15,green);}
  void techTreeScreen(Canvas c){Economy.State w=economy.snapshot();TechTree.Node node=TechTree.get(treeSelected);if(node==null)node=TechTree.STONEBOLT;
   text(c,"RESEARCH / USA",35,53,30,green);text(c,"$"+compact(w.dollars)+"    BP "+compact(w.bp),605,50,21,green);button(c,"treeback","BACK",1095,20,150,true);
   rect(c,35,87,118,42,border,true);text(c,"USA",75,115,19,green);text(c,"RANK I",190,113,17,amber);text(c,"RESEARCH > PURCHASE > EQUIP",360,112,15,muted);
   text(c,"BATTERIES",68,173,16,muted);text(c,"MISSILES",414,173,16,muted);
   techCard(c,TechTree.WATCHPOST,68,195,w);Equipment.Weapon[] choices=playerWeapons();for(int i=0;i<choices.length;i++){TechTree.Node n=TechTree.get(choices[i].id);if(n!=null)techCard(c,n,414,195+i*101,w);}
   text(c,"YOUR BATTERY",68,335,12,muted);text(c,equipment.radar.name,68,362,16,green);text(c,"MISSION LOADOUT",68,409,12,muted);text(c,equipment.playerWeapon(w.equippedMissile).name,68,435,16,green);text(c,"Loadout applies next mission.",68,472,12,muted);text(c,"S-A: maintained illumination",68,511,12,muted);text(c,"A-H: link, then own seeker",68,535,12,muted);text(c,"IR: own seeker lock",68,559,12,muted);
   rect(c,754,151,492,432,panel,true);rect(c,754,151,492,432,border,false);text(c,node.name,780,190,24,green);text(c,TechTree.status(w,node)+" / USA",780,219,13,amber);
   if(node.battery){Equipment.Radar r=equipment.radar;treeStat(c,"Detect effective / limit",fmt("%d / %.0f km",r.detectionKm,r.detectionAbsoluteKm),259);treeStat(c,"Track effective / limit",fmt("%.0f / %.0f km",r.trackingKm,r.trackingAbsoluteKm),293);treeStat(c,"Lock effective / limit",fmt("%.0f / %.0f km",r.lockKm,r.lockAbsoluteKm),327);treeStat(c,"Simultaneous tracks",""+r.tracks,361);treeStat(c,"Illumination channels",""+r.illuminationChannels,395);treeStat(c,"Midcourse channels",""+r.midcourseChannels,429);treeStat(c,"S-A supported missiles",""+r.supportedMissiles,463);treeStat(c,"Full sweep",fmt("%.1f s",r.sweepSeconds()),497);treeStat(c,"IRST sensor",r.infrared?"FITTED":"NOT FITTED",531);}
   else{Equipment.Weapon m=equipment.playerWeapon(node.id);treeStat(c,"Guidance",m.guidance.toString(),255);treeStat(c,"Radar mode",m.radarMode.toString().replace('_',' '),289);treeStat(c,"Max guidance",fmt("%.0f s",m.guidanceSeconds),323);treeStat(c,"Max speed",fmt("%,.0f km/h",m.maxSpeedKmh),357);treeStat(c,"Max G load",fmt("%.0f G",m.maxG),391);treeStat(c,"Mass",fmt("%.0f kg",m.massKg),425);treeStat(c,"Max AOA",fmt("%.0f deg",m.maxAoADeg),459);treeStat(c,"Max thrust",fmt("%.0f kN",m.thrustN/1000),493);treeStat(c,"Research / price",node.starter()?"FREE":node.bp+" BP / $"+compact(node.dollars),539);}
   boolean owned=w.owned.contains(node.id),done=TechTree.researched(w,node);String action,label;boolean enabled;
   if(owned){action="equip";label=node.battery||node.id.equals(w.equippedMissile)?"EQUIPPED":"EQUIP MISSILE";enabled=!node.battery&&!node.id.equals(w.equippedMissile);}
   else if(!done){action="research";int spend=(int)Math.min(w.bp,node.bp-TechTree.progress(w,node));label=spend>0?"RESEARCH / USE "+spend+" BP":"EARN BP TO RESEARCH";enabled=spend>0&&TechTree.prerequisite(w,node);}
   else{action="purchase";label="BUY / $"+compact(node.dollars);enabled=w.dollars>=node.dollars;}
   button(c,action,label,754,602,492,enabled&&!economy.saveFailed);
   text(c,"Spend banked BP on research, then buy with Dollars.",68,620,13,muted);text(c,"Research progress and owned equipment are saved.",68,645,13,muted);
   if(economy.saveFailed){text(c,"SAVE FAILED / RETRY BEFORE SPENDING",68,682,13,amber);button(c,"retrysave","RETRY SAVE",1030,663,216,true);}else{text(c,treeNotice,68,687,14,amber);if(done&&!owned&&w.dollars<node.dollars)text(c,"NEED $"+compact(node.dollars-w.dollars)+" MORE",780,682,14,muted);}
  }
  int contactColor(Game.Contact t){return new int[]{0xff999999,0xffffa040,0xffff4545,green}[game.identity(t)];}
  Game.Missile selectedSupport(){for(Game.Missile m:game.missiles)if(m.alive&&m.target==selected&&Game.activeHoming(game.profile(m))&&m.linkReserved&&!m.autonomous)return m;return null;}
  String missileTag(Game.Missile m){return "M"+fmt("%02d",game.missiles.indexOf(m)+1);}
  String supportingSemiActive(Game.Contact t){String ids="";for(Game.Missile m:game.missiles)if(m.alive&&m.target==t&&!m.infrared&&!Game.activeHoming(game.profile(m)))ids+=(ids.isEmpty()?"":" ")+missileTag(m);return ids;}
  void controlBar(Canvas c){boolean active=!paused&&!game.finished,ir=game.irMode;
   button(c,"next","NEXT TARGET",874,89,367,active);button(c,"prioritize",selected!=null&&selected.priority?"CLEAR PRIORITY":"PRIORITIZE",874,149,367,active&&game.visible(selected));
   String label=ir?(game.ir.locked?"IR SEEKER LOCKED":game.ir.active?"SEEKER ACQUIRING":"IR LOCK / ACTIVATE"):game.hardLocked(selected)?"RADAR LOCKED":"LOCK";
   boolean canLock=ir?game.ir.beginBlock(selected)==null&&!game.ir.active:game.lockBlock(selected)==null;
   button(c,"illuminate",label,874,209,367,active&&canLock);button(c,"launch","FIRE "+game.weapon.name,874,269,367,active&&game.weaponBlock(selected)==null);
   button(c,"range","RANGE LIMIT: "+game.range+" KM",874,329,367,active);
   String affected=supportingSemiActive(selected);button(c,"release",ir?"CANCEL IR SEEKER":!affected.isEmpty()?"UNLOCK "+affected:"UNLOCK",874,389,367,active&&(ir?game.ir.active:selected!=null&&selected.illuminated));
   button(c,"guidancelines",guidanceLines?"LINES ON":"LINES OFF",874,449,162,active);Game.Missile m=selectedSupport();button(c,"releasesupport",m==null?"RELEASE SUPPORT":"RELEASE "+missileTag(m),1044,449,197,active&&m!=null);
  }
  void seekerPanel(Canvas c){rect(c,20,654,1240,46,border,false);if(game.irMode){text(c,game.weapon.name+"  "+game.ready()+" READY / "+game.reserve+" RES",36,675,15,green);text(c,game.launchers[game.chosenLauncher].reload>0?fmt("RELOAD %.0fs",game.reloadSeconds(game.chosenLauncher)):fmt("ALL-ASPECT / UP TO %.1fkm / PASSIVE",game.ir.lockRange()),36,693,11,muted);
    text(c,game.ir.state(),440,675,15,game.ir.locked?amber:green);double strength=game.ir.baseHeat();text(c,fmt("HEAT %.0f%% / LOCK %.0f%%",Math.min(150,strength*100),game.ir.acquire/Infrared.ACQUIRE_TIME*100),440,693,11,muted);rect(c,823,671,185,11,border,true);rect(c,823,671,185*(float)Math.min(1,strength),11,green,true);text(c,"OWN SEEKER / NO LINK",1030,681,12,amber);
    if(sound&&tone!=null&&!paused&&!game.finished&&game.ir.active&&game.ir.cool>=Infrared.COOL_TIME){long now=System.nanoTime();if(now-lastSeekerTone>(game.ir.locked?400000000L:900000000L)){tone.startTone(ToneGenerator.TONE_PROP_BEEP,game.ir.locked?180:45);lastSeekerTone=now;}}
   }else{text(c,game.weapon.name,36,682,17,green);text(c,fmt("GUIDANCE %.0fs / %.0f km/h",game.weapon.guidanceSeconds,game.weapon.maxSpeedKmh),400,682,13,muted);text(c,Game.activeHoming(game.weapon)?"ACTIVE / TRACK > LINK > SEEKER":"SEMI-ACTIVE / KEEP TARGET LOCKED",850,682,14,amber);}}
  void telemetry(Canvas c){rect(c,20,74,315,484,border,false);text(c,"TARGET ANALYSIS",36,101,18,green);
   if(selected==null){text(c,"SELECT AIRCRAFT OR MISSILE",36,140,14,amber);text(c,"DETECT > ACQUIRE > STABLE",36,171,14,muted);}else{Game.Contact t=selected;int col=contactColor(t);text(c,game.tag(t)+" / "+game.trackState(t),36,129,13,col);text(c,game.identityLabel(t),36,155,13,col);text(c,game.stale(t)?fmt("LAST UPDATE %.1fs AGO",game.elapsed-t.seen):game.identification(t),36,181,12,col);text(c,fmt("RNG %.1f km / ALT %s m",game.displayRange(t),game.altitudeText(t)),36,210,12,muted);text(c,"SPD "+game.speedText(t)+" km/h / HDG "+game.directionText(t),36,235,12,muted);text(c,"BEARING "+game.bearingText(t)+" deg",36,254,11,muted);
    if(!game.tracked(t)){text(c,game.trackStage(t)==Game.TrackStage.LOST?"LAST KNOWN POSITION / LOST":"DETECTION ONLY",36,272,13,muted);text(c,"DETAILS REQUIRE A VALID TRACK",36,299,12,muted);text(c,"POSITION UPDATES ON SWEEPS",36,326,12,muted);}else{text(c,fmt("TRACK QUALITY %03.0f%%",game.trackQuality(t)*100),36,279,13,game.trackQuality(t)<.55?amber:green);text(c,game.trackStage(t)==Game.TrackStage.ACQUIRING?"AWAITING MORE OBSERVATIONS":game.stale(t)?"COASTING / ESTIMATE AGING":game.trackQuality(t)<.55?"MARGINAL / WEAK SOLUTION":"STABLE / ESTIMATED MOTION",36,304,11,muted);text(c,game.hardLocked(t)?fmt("ILLUMINATING / QUALITY %.0f%%",game.lockQuality(t)*100):"NO RADAR ILLUMINATION",36,330,11,muted);}
    if(t instanceof Game.EnemyMissile)text(c,"INCOMING MISSILE CONTACT",36,353,12,col);
   }
   line(c,30,383,325,383,border);text(c,"AUTO TRACKS",36,408,17,green);ArrayList<Game.Contact> list=new ArrayList<>();for(Game.Contact t:game.targets())if(game.tracked(t))list.add(t);list.sort((x,y)->Double.compare(x.measuredRange(),y.measuredRange()));for(int i=0;i<Math.min(5,list.size());i++){Game.Contact t=list.get(i);text(c,fmt("%s %-9s %.1fkm",game.tag(t),game.trackStage(t).toString(),game.displayRange(t)),36,434+i*24,11,contactColor(t));}if(list.isEmpty())text(c,"WAITING FOR RADAR CONTACTS",36,440,12,muted);if(list.size()>5)text(c,"+"+(list.size()-5)+" more",36,552,10,muted);
  }
  int scopeContactColor(Game.Contact t){int color=contactColor(t);return game.stale(t)?(color&0x00ffffff)|0x88000000:color;}
  void scope(Canvas c){float cx=595,cy=302,r=183;rect(c,347,74,496,484,border,false);text(c,"S2 / SEARCH RADAR",365,100,17,green);text(c,game.range+" km",750,100,14,amber);circle(c,cx,cy,r,0xff000600,true);
   // Terrain is a fixed fictional map, rendered only within the circular scope.
   c.save();Path clip=new Path();clip.addCircle(cx,cy,r,Path.Direction.CW);c.clipPath(clip);for(int x=-80;x<80;x+=3)for(int y=-80;y<80;y+=3){double height=Game.terrain(x,y);if(height>180){int a=(int)Math.min(100,height/12);rect(c,cx+x/(float)game.range*r,cy+y/(float)game.range*r,3*r/game.range+1,3*r/game.range+1,Color.argb(a,35,100,35),true);}}c.restore();
   for(int i=1;i<=4;i++){circle(c,cx,cy,r*i/4,border,false);text(c,fmt("%.0f",game.range*i/4.0),cx+5,cy-r*i/4+13,10,muted);}if(game.range>=game.lockRange())circle(c,cx,cy,(float)(r*game.lockRange()/game.range),0xffa0d0a0,false);line(c,cx-r,cy,cx+r,cy,border);line(c,cx,cy-r,cx,cy+r,border);text(c,"N",cx-5,cy-r-6,12,green);
   for(int i=0;i<32;i++){p.setColor(Color.argb(55-i,0,255,0));p.setStyle(Paint.Style.FILL);c.drawArc(new RectF(cx-r,cy-r,cx+r,cy+r),(float)Math.toDegrees(game.sweep)-90-i,1.3f,true,p);}line(c,cx,cy,cx+(float)Math.sin(game.sweep)*r,cy-(float)Math.cos(game.sweep)*r,green);
   rect(c,cx-5,cy-5,10,10,green,true);
   c.save();c.clipPath(clip);for(Game.Contact t:game.contacts)if(game.visible(t)){float x=cx+(float)game.displayX(t)/game.range*r,y=cy+(float)game.displayY(t)/game.range*r;int col=scopeContactColor(t);if(guidanceLines&&t==selected&&game.hardLocked(t))line(c,cx,cy,x,y,0xff567856);aircraftGlyph(c,x,y,t,col);targetMarker(c,x,y,t,col);text(c,fmt("%03d",t.id),x+25,y+5,12,col);text(c,fmt("%.1fkm",game.displayRange(t)),x+25,y+17,9,col);}
   for(Game.Missile m:game.missiles)if(m.alive&&Math.hypot(m.x,m.y)<=game.range){float x=cx+(float)m.x/game.range*r,y=cy+(float)m.y/game.range*r;if(guidanceLines&&m.target==selected)guidanceLine(c,m,cx,cy,x,y,r);missileGlyph(c,x,y,Math.atan2(m.vx,-m.vy),green);text(c,missileTag(m)+" "+game.missileState(m),x+7,y-10-(game.missiles.indexOf(m)%3)*11,8,green);}
   for(Game.EnemyMissile m:game.enemyMissiles)if(game.visible(m)){float x=cx+(float)game.displayX(m)/game.range*r,y=cy+(float)game.displayY(m)/game.range*r;int col=scopeContactColor(m);if(game.tracked(m)&&m.samples>=2)missileGlyph(c,x,y,m.pheading,col);else circle(c,x,y,5,col,false);targetMarker(c,x,y,m,col);text(c,"M-"+m.id,x+25,y+5,10,col);text(c,fmt("%.1fkm",game.displayRange(m)),x+25,y+17,9,col);}
   c.save();c.clipPath(clip);for(Game.Bomb b:game.bombs)if(b.alive&&b.observed&&Math.hypot(b.x,b.y)<=game.range){float x=cx+(float)b.x/game.range*r,y=cy+(float)b.y/game.range*r;circle(c,x,y,3,amber,false);text(c,"BOMB",x+5,y-5,9,amber);}for(Game.Blast b:game.blasts){float x=cx+(float)b.x/game.range*r,y=cy+(float)b.y/game.range*r;circle(c,x,y,Math.max(5,(float)b.radius/game.range*r),amber,false);}c.restore();
   if(game.irMode){if(game.ir.lockRange()<=game.range)circle(c,cx,cy,(float)(r*game.ir.lockRange()/game.range),0xff70b870,false);Game.Contact t=game.ir.target;if(t!=null&&game.visible(t)){float x=cx+(float)game.displayX(t)/game.range*r,y=cy+(float)game.displayY(t)/game.range*r;if(game.ir.locked||((int)(game.elapsed*4)%2==0)){for(int sign:new int[]{-1,1}){line(c,x+sign*21,y-10,x+sign*21,y+10,amber);line(c,x+sign*21,y-10,x+sign*14,y-10,amber);line(c,x+sign*21,y+10,x+sign*14,y+10,amber);}}text(c,game.ir.locked?"IR LOCK":"IR SEEK",x+25,y+31,11,amber);}}
   for(Infrared.Flare f:game.ir.flares)if(Math.hypot(f.x,f.y)<=game.range&&game.visible(f.source)){float x=cx+(float)f.x/game.range*r,y=cy+(float)f.y/game.range*r;circle(c,x,y,3,amber,true);text(c,"FLARE",x+5,y-4,9,amber);}
   c.restore();text(c,game.irMode?fmt("IR RING: %.1fkm / HEAT & TERRAIN LIMIT LOCK",game.ir.lockRange()):fmt("LOCK %.1fkm / TRACK %.1fkm / SCAN %.0fs",game.lockRange(),game.trackingRange(),equipment.radar.sweepSeconds()),367,510,11,muted);text(c,"GRAY UNKNOWN / ORANGE UNSURE / RED ENEMY",367,530,11,muted);text(c,"DOTTED: TRACK / SOLID: LOCK / BRACKETS: IR",367,548,11,muted);
  }
  void guidanceLine(Canvas c,Game.Missile m,float cx,float cy,float x,float y,float r){Game.GuidanceLine kind=game.guidanceLine(m);
   if(kind==Game.GuidanceLine.BATTERY_SOLID)line(c,cx,cy,x,y,muted);
   else if(kind==Game.GuidanceLine.BATTERY_DOTTED){for(float f=0;f<1;f+=.08f){float end=Math.min(1,f+.035f);line(c,cx+(x-cx)*f,cy+(y-cy)*f,cx+(x-cx)*end,cy+(y-cy)*end,green);}}
   else if(kind==Game.GuidanceLine.TARGET_SOLID){float tx=cx+(float)(m.estimateValid?game.estimateX(m):game.displayX(m.target))/game.range*r,ty=cy+(float)(m.estimateValid?game.estimateY(m):game.displayY(m.target))/game.range*r;line(c,x,y,tx,ty,green);}
  }
  void targetMarker(Canvas c,float x,float y,Game.Contact t,int color){Game.TrackStage stage=game.trackStage(t);
   if(game.tracked(t)){if(game.hardLocked(t)){circle(c,x,y,17,color,false);Path star=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?7:3,xx=x+21+(float)Math.cos(a)*r,yy=y-22+(float)Math.sin(a)*r;if(i==0)star.moveTo(xx,yy);else star.lineTo(xx,yy);}star.close();p.setColor(amber);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);c.drawPath(star,p);p.setStyle(Paint.Style.FILL);}
    else{p.setColor(stage==Game.TrackStage.COASTING?(color&0x00ffffff)|0x88000000:color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);RectF ring=new RectF(x-17,y-17,x+17,y+17);for(int a=0;a<360;a+=30)c.drawArc(ring,a,17,false,p);p.setStyle(Paint.Style.FILL);}
    if(t.samples>=2){float dx=(float)Math.sin(t.pheading)*25,dy=-(float)Math.cos(t.pheading)*25;line(c,x,y,x+dx,y+dy,stage==Game.TrackStage.COASTING?(color&0x00ffffff)|0x88000000:color);}}
   if(stage==Game.TrackStage.COASTING)text(c,fmt("COAST %.0fs",game.elapsed-t.seen),x-24,y-24,9,amber);else if(stage==Game.TrackStage.LOST)text(c,"LOST",x-17,y-20,10,amber);else if(stage==Game.TrackStage.ACQUIRING)text(c,"ACQ",x-17,y-20,9,amber);
   if(t==selected){line(c,x-5,y+29,x,y+24,amber);line(c,x,y+24,x+5,y+29,amber);}
  }
  void aircraftGlyph(Canvas c,float x,float y,Game.Contact t,int col){c.save();c.translate(x,y);if(game.tracked(t)&&t.samples>=2)c.rotate((float)Math.toDegrees(t.pheading));if(!game.tracked(t)||t.samples<2||t.confidence<.75||game.stale(t)){line(c,0,-8,6,0,col);line(c,6,0,0,8,col);line(c,0,8,-6,0,col);line(c,-6,0,0,-8,col);}else{int type=t.guess;float wing=type==0?11:type==1?6:type==2?9:10;float sweep=type==0?1:type==1?6:type==2?4:type==3?5:6;line(c,0,-12,0,11,col);line(c,0,-5,-wing,sweep,col);line(c,-wing,sweep,0,3,col);line(c,0,-5,wing,sweep,col);line(c,wing,sweep,0,3,col);line(c,-4,9,4,9,col);}c.restore();}
  void missileGlyph(Canvas c,float x,float y,double heading,int col){c.save();c.translate(x,y);c.rotate((float)Math.toDegrees(heading));line(c,0,-6,0,6,col);line(c,-3,-1,0,-6,col);line(c,3,-1,0,-6,col);c.restore();}
  void engagement(Canvas c){rect(c,855,74,405,484,border,false);String why=game.weaponBlock(selected);text(c,why==null?"READY TO FIRE":why,874,516,10,amber);text(c,fmt("TRACKS %d/%d    ILLUMINATION %d/%d",game.trackedCount(),equipment.radar.tracks,game.illuminationUsed(),equipment.radar.illuminationChannels),874,535,11,green);text(c,fmt("MIDCOURSE %d/%d    S-A SUPPORT %d/%d",game.midcourseUsed(),equipment.radar.midcourseChannels,game.supportedMissilesUsed(),equipment.radar.supportedMissiles),874,551,11,green);}
  void launchers(Canvas c){for(int i=0;i<3;i++){float x=20+i*282;Game.Launcher l=game.launchers[i];rect(c,x,570,270,73,bg,true);rect(c,x,570,270,73,i==game.chosenLauncher?amber:border,false);
   if(launcherIcon!=null){float iw=122,ih=iw*launcherIcon.getHeight()/launcherIcon.getWidth();p.setFilterBitmap(true);p.setAlpha(l.ammo==0?100:255);c.drawBitmap(launcherIcon,null,new RectF(x+7,606.5f-ih/2,x+7+iw,606.5f+ih/2),p);p.setAlpha(255);}
   Battery.Component part=game.battery.parts[Battery.L1+i];String status=!part.alive()?"DISABLED":!game.battery.powered(game.elapsed)?"NO POWER":l.reload>0?fmt("RELOAD %03ds",(int)Math.ceil(game.reloadSeconds(i))):l.ammo==0?"EMPTY":"READY";
   text(c,"L"+(i+1)+" "+part.percent()+"%",x+141,591,12,i==game.chosenLauncher?amber:green);text(c,l.ammo+" / 3",x+210,591,13,muted);text(c,status,x+141,611,12,l.reload>0?amber:green);
   for(int j=0;j<3;j++)rect(c,x+141+j*37,620,27,12,j<l.ammo?green:border,true);
   if(l.reload>0)rect(c,x+141,597,101*(float)(1-l.reload/90),2,green,true);
   buttons.add(new Btn(x,570,270,73,"l"+i,true));}
   rect(c,866,570,394,73,border,false);text(c,fmt("READY %02d   RESERVE %02d",game.ready(),game.reserve),883,597,18,green);text(c,fmt("%d allocated / damage slows reload",game.reserved),883,623,13,muted);
  }
  void batteryScreen(Canvas c){rect(c,0,0,1280,720,0xee000000,true);rect(c,160,62,960,612,panel,true);rect(c,160,62,960,612,green,false);buttons.clear();text(c,"BATTERY STATUS",194,110,28,green);text(c,game.battery.warning(game.elapsed),194,141,15,amber);text(c,"SIMULATION PAUSED",879,106,12,muted);
   String[] effects={fmt("DETECT %dkm / TRACK %.1f / LOCK %.1f",game.maxRange,game.trackingRange(),game.lockRange()),"", "", "",game.battery.powered(game.elapsed)?"EQUIPMENT POWERED":game.battery.parts[Battery.POWER].alive()?fmt("OUTAGE / %.1fs REMAIN",Math.max(0,game.battery.outageUntil-game.elapsed)):"CONNECTED EQUIPMENT OFFLINE",fmt("OBSERVATION DELAY %.2fs",game.battery.processingDelay())};
   for(int i=0;i<6;i++){Battery.Component part=game.battery.parts[i];float x=194+(i%2)*448,y=169+(i/2)*123;rect(c,x,y,426,107,border,false);text(c,part.name,x+15,y+27,16,green);text(c,part.percent()+"%",x+350,y+27,16,part.alive()?green:amber);rect(c,x+15,y+43,395,7,border,true);if(part.hp>0)rect(c,x+15,y+43,395*(float)part.hp/100,7,green,true);
    String effect=effects[i];if(i>=Battery.L1&&i<=Battery.L3){int li=i-Battery.L1;effect=!part.alive()?"CANNOT FIRE":!game.battery.powered(game.elapsed)?"POWER OFF / RELOAD PAUSED":game.launchers[li].reload>0?fmt("RELOAD %.0fs REMAIN",game.reloadSeconds(li)):fmt("FULL RELOAD %.0fs",90/game.battery.reloadRate(li,game.elapsed));}if(i==Battery.RADAR&&!game.battery.radarOnline(game.elapsed))effect="TRACKING AND LOCKING OFFLINE";
    text(c,effect,x+15,y+72,12,part.alive()?muted:amber);text(c,fmt("PROTECTION x%.2f / %s",part.protection,part.alive()?part.hp<99.5?"DAMAGED":"OPERATIONAL":"DESTROYED"),x+15,y+94,11,muted);
   }text(c,fmt("AVERAGE CONDITION %.1f%% / EACH COMPONENT HAS ITS OWN HP",game.battery.condition()),194,566,13,muted);button(c,"start",game.finished?"NEW MISSION":"RESUME MISSION",194,599,300,true);button(c,"help","GUIDE",514,599,250,true);button(c,"menu","MAIN MENU",784,599,290,true);
  }
  void overlay(Canvas c){rect(c,0,0,1280,720,0xee000000,true);rect(c,190,69,900,610,panel,true);rect(c,190,69,900,610,green,false);buttons.clear();String title=game.finished?(game.won?"MISSION COMPLETE":"BATTERY OVERRUN"):help?"OPERATOR GUIDE":started?"MISSION PAUSED":"AIR DEFENSE";text(c,title,225,118,30,green);
   if(game.finished){text(c,fmt("SCORE %d / BEST %d / %d INTERCEPTS",game.score,best,game.kills),225,150,16,muted);rewardReceipt(c);rect(c,705,175,350,320,border,false);text(c,"AIRCRAFT DEBRIEF",725,201,16,green);int i=0;for(Game.Contact t:game.contacts){text(c,fmt("%03d S%d %-9s %s",t.id,t.pilot==null?1:t.pilot.smart,Game.NAMES[t.type],t.alive?"UNRESOLVED":t.outcome),725,229+i*21,10,muted);i++;}Economy.State w=economy.snapshot();text(c,fmt("AI / BOMB RUNS %d / DAMAGING %d / ABORTS %d / RETREATS %d",game.ai.bombRuns,game.ai.damagingRuns,game.ai.abortedPasses,game.ai.retreats),225,513,12,muted);text(c,"BALANCE $"+compact(w.dollars)+"    BP "+compact(w.bp),225,531,19,green);}

   else{String[] rows={"Protect the COMMAND UNIT. Its destruction ends the mission.","DETECTED: last measured position/range; details stay hidden.","ACQUIRING > STABLE: motion estimates need repeated detections.","COASTING: faded aging estimate. LOST: no valid firing solution.","Dotted ring: track. Solid + star: illumination. Brackets: IR lock.","S-A: radar LOCK maintained; same-target missiles share illumination.","A-H: one midcourse channel per missile until its seeker ACQUIRES.","SEARCHING does not mean acquired. Lost support permits recovery.","IR: use IR LOCK, then FIRE. An IRST sensor is not required.","UNLOCK names affected S-A support. RELEASE Mxx frees one link.","LINES ON/OFF shows selected engagements; confirmed guidance only.","PRIORITIZE keeps protected engagements; incoming missiles first.","USA TECH TREE: research, buy, equip. Loadout applies next mission."};for(int i=0;i<rows.length;i++)text(c,rows[i],225,163+i*27,15,i==0?amber:muted);}
   button(c,"start",started&&!game.finished?"RESUME MISSION":"START MISSION",225,558,270,true);button(c,"sound",sound?"SOUND ON":"SOUND OFF",510,558,240,true);if(started&&!game.finished)button(c,"restart","RESTART",765,558,285,true);button(c,"menu","MAIN MENU",765,615,285,true);
  }
  @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;for(Btn b:buttons)if(b.enabled&&b.r.contains(x,y)){act(b.id);performClick();invalidate();return true;}if(started&&!paused&&!game.finished){Game.Contact pick=null;double near=30;for(Game.Contact t:game.targets())if(game.visible(t)){double d=Math.hypot(x-(595+game.displayX(t)/game.range*183),y-(302+game.displayY(t)/game.range*183));if(d<near){near=d;pick=t;}}if(pick!=null){choose(pick);notice="CONTACT "+pick.id+" SELECTED / "+game.trackState(pick);beep();}}invalidate();return true;}
  public boolean performClick(){super.performClick();return true;}
  void choose(Game.Contact t){if(t!=selected)game.ir.cancel();selected=t;}
  void act(String id){if(intro.active){
    if(id.equals("introskip"))finishIntro();
    else if(id.equals("intropause")){captureIntro();intro.paused=!intro.paused;last=System.nanoTime();syncMusic();}
    else if(id.equals("intromusic")){intro.music=!intro.music;syncMusic();}
    invalidate();return;
   }
   if(id.equals("playmodes")&&!started){selectionScreen=1;economyOpen=equipmentOpen=treeOpen=false;}
   else if(id.equals("storymode")&&selectionScreen==1&&!started)selectionScreen=2;
   else if(id.equals("selectionback")&&!started){if(selectionScreen>0)selectionScreen--;}
   else if(id.equals("missionstore")&&selectionScreen==2&&!started){treeOpen=true;treeNotice="RESEARCH / BUY / EQUIP FOR MISSION 01";}
   else if((id.equals("missionloadout")||id.equals("mission:0"))&&selectionScreen==2&&!started){selectionScreen=3;loadoutNotice="SELECT YOUR MISSILE / LOADOUT APPLIES TO ALL LAUNCHERS";}
   else if(id.startsWith("loadout:")&&selectionScreen==3&&!started){loadoutNotice=economy.equip(id.substring(8));}
   else if(id.equals("missiondeploy")&&selectionScreen==3&&!started&&!economy.saveFailed)beginIntro();
   else if(id.equals("start")){if(!started||game.finished)beginIntro();else{paused=help=statusOpen=false;last=System.nanoTime();notice="SEARCH ACTIVE / AUTOMATIC TRACKING ENABLED";}}
   else if(id.equals("restart"))beginIntro();
   else if(id.equals("techtree")){treeOpen=true;treeNotice="SELECT EQUIPMENT / RESEARCH > BUY > EQUIP";}
   else if(id.equals("treeback"))treeOpen=false;
   else if(id.startsWith("node:")){String node=id.substring(5);if(TechTree.get(node)!=null){treeSelected=node;treeNotice="SELECT EQUIPMENT / RESEARCH > BUY > EQUIP";}}
   else if(id.equals("research"))treeNotice=economy.research(treeSelected);
   else if(id.equals("purchase"))treeNotice=economy.purchase(treeSelected);
   else if(id.equals("equip"))treeNotice=economy.equip(treeSelected);
   else if(id.equals("equipment"))equipmentOpen=true;
   else if(id.equals("equipmentback"))equipmentOpen=false;
   else if(id.equals("equipnext"))equipmentIndex=(equipmentIndex+1)%playerWeapons().length;
   else if(id.equals("equipprev"))equipmentIndex=(equipmentIndex+playerWeapons().length-1)%playerWeapons().length;
   else if(id.equals("economy"))economyOpen=true;
   else if(id.equals("economyback"))economyOpen=false;
   else if(id.equals("retrysave"))economy.flush();
   else if(id.equals("status")){paused=true;statusOpen=true;help=false;}
   else if(id.equals("pause")){paused=true;statusOpen=false;help=false;}
   else if(id.equals("help")){paused=true;statusOpen=false;help=true;}
   else if(id.equals("sound"))sound=!sound;
   else if(id.equals("speed")){speed=speed==1?2:speed==2?4:1;notice="SIMULATION SPEED "+speed+"x / ALL TIMERS ACCELERATE";}
   else if(id.equals("range")){game.cycleRange();notice="RADAR DISPLAY RANGE "+game.range+" km";}
   else if(id.equals("next")){choose(game.nextTarget(selected));notice=selected==null?"NO DETECTED CONTACTS / WAIT FOR SWEEP":"TARGET "+selected.id+" SELECTED";}
   else if(id.equals("guidancelines")){guidanceLines=!guidanceLines;notice=guidanceLines?"GUIDANCE LINES / SELECTED TARGET ENGAGEMENTS":"GUIDANCE LINES HIDDEN";}
   else if(id.equals("releasesupport")){Game.Missile m=selectedSupport();notice=m==null?"NO ACTIVE MIDCOURSE SUPPORT TO RELEASE":missileTag(m)+" / "+game.releaseSupport(m);}
   else if(id.equals("prioritize"))notice=game.prioritize(selected);
   else if(id.equals("menu")){selectionScreen=0;economy.abandon();economyOpen=equipmentOpen=treeOpen=false;started=false;paused=statusOpen=false;game.running=false;game.ir.cancel();selected=null;}
   else if(id.equals("illuminate"))notice=game.weaponLock(selected);
   else if(id.equals("release"))notice=game.weaponRelease(selected);
   else if(id.equals("launch"))notice=game.fireWeapon(selected);
   else if(id.matches("l[012]")){game.chosenLauncher=Integer.parseInt(id.substring(1));notice="LAUNCHER "+(game.chosenLauncher+1)+" SELECTED / "+(game.irMode?"IR SEEKER":"RADAR GUIDANCE");}
   syncMusic();if(!intro.active)beep();
  }
 }
}

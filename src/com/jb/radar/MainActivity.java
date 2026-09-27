package com.jb.radar;
import android.app.*;import android.os.*;import android.view.*;import android.graphics.*;import android.media.*;import java.util.*;
public class MainActivity extends Activity {
 Radar view;
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(1024,1024);getWindow().addFlags(128);view=new Radar();setContentView(view);view.setOnApplyWindowInsetsListener((v,i)->{int l=i.getSystemWindowInsetLeft(),r=i.getSystemWindowInsetRight(),t=i.getSystemWindowInsetTop(),bt=i.getSystemWindowInsetBottom();if(Build.VERSION.SDK_INT>=28&&i.getDisplayCutout()!=null){DisplayCutout d=i.getDisplayCutout();l=Math.max(l,d.getSafeInsetLeft());r=Math.max(r,d.getSafeInsetRight());t=Math.max(t,d.getSafeInsetTop());bt=Math.max(bt,d.getSafeInsetBottom());}v.setPadding(l,t,r,bt);return i;});}
 public void onResume(){super.onResume();if(view!=null){view.last=System.nanoTime();view.invalidate();}}
 public void onPause(){super.onPause();if(view!=null){view.paused=true;view.last=0;}}
 public void onDestroy(){if(view!=null&&view.tone!=null)view.tone.release();super.onDestroy();}
 @Override public void onBackPressed(){view.paused=true;view.help=false;view.invalidate();}
 class Radar extends View {
  final int green=0xff00ff00,muted=0xff70b870,amber=0xffeeeeee,red=0xffffffff,bg=0xff000000,border=0xff164516,panel=0xff020a02;
  Paint p=new Paint(3);Game game=new Game(System.nanoTime());Game.Contact selected;ArrayList<Btn> buttons=new ArrayList<>();long last;float scale,ox,oy;boolean paused=false,started=false,sound=false,ended=false,help=false;int speed=1,best=getPreferences(0).getInt("hawk_best",0);String notice="MISSION 1 / HAWK BATTERY";ToneGenerator tone;Bitmap launcherIcon;
  class Btn{RectF r;String id;boolean enabled;Btn(float x,float y,float w,float h,String i,boolean e){r=new RectF(x,y,x+w,y+h);id=i;enabled=e;}}
  Radar(){super(MainActivity.this);setFocusable(true);try(java.io.InputStream stream=getAssets().open("hawk_launcher.png")){BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=4;launcherIcon=BitmapFactory.decodeStream(stream,null,opts);}catch(java.io.IOException e){launcherIcon=null;}try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,30);}catch(Exception e){}}
  void text(Canvas c,String s,float x,float y,float size,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.MONOSPACE);p.setTextSize(size);c.drawText(s,x,y,p);}
  void rect(Canvas c,float x,float y,float w,float h,int col,boolean fill){p.setColor(col);p.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRoundRect(new RectF(x,y,x+w,y+h),7,7,p);p.setStyle(Paint.Style.FILL);}
  void line(Canvas c,float x,float y,float xx,float yy,int col){p.setColor(col);p.setStrokeWidth(1);c.drawLine(x,y,xx,yy,p);}
  void circle(Canvas c,float x,float y,float r,int col,boolean fill){p.setColor(col);p.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);p.setStrokeWidth(1);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);}
  void button(Canvas c,String id,String label,float x,float y,float w,boolean enabled){rect(c,x,y,w,48,enabled?border:0xff061006,true);rect(c,x,y,w,48,enabled?muted:border,false);text(c,label,x+12,y+30,15,enabled?green:muted);buttons.add(new Btn(x,y,w,48,id,enabled));}
  String fmt(String s,Object...o){return String.format(Locale.US,s,o);}void beep(){if(sound&&tone!=null)tone.startTone(ToneGenerator.TONE_PROP_BEEP,60);}
  protected void onDraw(Canvas c){super.onDraw(c);long now=System.nanoTime();if(last!=0&&!paused&&started){double remaining=Math.min(.15,(now-last)/1e9)*speed;while(remaining>0){double d=Math.min(.05,remaining);game.tick(d);remaining-=d;}}last=now;
   if(game.finished&&!ended){ended=true;if(game.score>best){best=game.score;getPreferences(0).edit().putInt("hawk_best",best).apply();}}
   float aw=getWidth()-getPaddingLeft()-getPaddingRight(),ah=getHeight()-getPaddingTop()-getPaddingBottom();scale=Math.min(aw/1280f,ah/720f);ox=getPaddingLeft()+(aw-1280*scale)/2;oy=getPaddingTop()+(ah-720*scale)/2;c.drawColor(bg);c.save();c.translate(ox,oy);c.scale(scale,scale);buttons.clear();
   text(c,"HAWK / AIR DEFENSE",20,32,25,green);text(c,"MISSION 01   /   MIM-23A BATTERY",20,54,12,muted);text(c,fmt("SITE %d%%",game.health),445,32,20,game.health>25?green:red);text(c,fmt("%02d:%02d",(int)game.elapsed/60,(int)game.elapsed%60),622,32,22,amber);
   button(c,"speed",speed+"x TIME",770,11,140,true);button(c,"help","GUIDE",922,11,145,true);button(c,"pause","PAUSE",1080,11,180,started&&!game.finished);
   telemetry(c);scope(c);engagement(c);launchers(c);
   text(c,notice,20,708,13,amber);
   if(!started||paused||game.finished)overlay(c);c.restore();if(isShown())postInvalidateDelayed(33);
  }
  void telemetry(Canvas c){rect(c,20,74,315,524,border,false);text(c,"S1 / TARGET ANALYSIS",36,100,17,green);text(c,"ALT km",36,127,12,muted);
   float gx=69,gy=143,gw=244,gh=133;for(int i=0;i<=4;i++){float x=gx+gw*i/4,y=gy+gh*i/4;line(c,x,gy,x,gy+gh,border);line(c,gx,y,gx+gw,y,border);text(c,""+(20-i*5),38,y+4,11,muted);text(c,""+(i*600),x-10,gy+gh+19,10,muted);}text(c,"SPEED km/h",189,317,12,muted);
   for(Game.Contact t:game.contacts)if(game.visible(t)){float x=gx+gw*(float)Math.min(1,t.pspeed/2400),y=gy+gh*(float)(1-Math.min(1,t.palt/20000));circle(c,x,y,t==selected?5:3,t==selected?amber:green,true);}
   text(c,selected==null?"SELECT A TRACK":fmt("TRACK %03d / %s",selected.id,game.trackState(selected)),36,350,14,amber);
   if(selected!=null){Game.Contact t=selected;text(c,game.identification(t),36,378,13,green);text(c,fmt("RANGE %5.1f km",t.measuredRange()),36,405,14,muted);text(c,fmt("ALT %5.0f m  SPD %4.0f",t.palt,t.pspeed),36,428,13,muted);text(c,fmt("HDG %03d  V/S %+.0f m/s",((int)Math.toDegrees(t.pheading)%360+360)%360,t.pclimb),36,451,13,muted);text(c,fmt("SIZE ~%.1fm / SIGNAL %d%%",t.psize,(int)(t.quality*100)),36,474,12,muted);
    Integer[] order={0,1,2,3,4};Arrays.sort(order,(a,b)->Double.compare(t.probabilities[b],t.probabilities[a]));for(int i=0;i<3;i++){int k=order[i];text(c,fmt("%-10s %2.0f%%",Game.NAMES[k],t.probabilities[k]*100),36,503+i*22,13,i==0?green:muted);}text(c,fmt("UNRESOLVED %.0f%%",t.probabilities[5]*100),36,575,12,muted);
   }else{String[] lines={"New contacts start unknown.","Wait for two radar sweeps.","Select a blip or NEXT TRACK.","", "Speed + altitude + size", "inform the computer's guess.","Close contacts are clearer.","Su-17 / Su-22 can look alike."};for(int i=0;i<lines.length;i++)text(c,lines[i],36,388+i*24,13,muted);}
  }
  void scope(Canvas c){float cx=595,cy=322,r=211;rect(c,347,74,496,524,border,false);text(c,"S2 / SEARCH RADAR",365,100,17,green);text(c,game.range+" km",750,100,14,amber);circle(c,cx,cy,r,0xff000600,true);
   // Terrain is a fixed fictional map, rendered only within the circular scope.
   c.save();Path clip=new Path();clip.addCircle(cx,cy,r,Path.Direction.CW);c.clipPath(clip);for(int x=-80;x<80;x+=3)for(int y=-80;y<80;y+=3){double height=Game.terrain(x,y);if(height>180){int a=(int)Math.min(100,height/12);rect(c,cx+x/(float)game.range*r,cy+y/(float)game.range*r,3*r/game.range+1,3*r/game.range+1,Color.argb(a,35,100,35),true);}}c.restore();
   for(int i=1;i<=4;i++){circle(c,cx,cy,r*i/4,border,false);text(c,fmt("%.0f",game.range*i/4.0),cx+5,cy-r*i/4+13,10,muted);}circle(c,cx,cy,r*25/game.range,0xffa0d0a0,false);line(c,cx-r,cy,cx+r,cy,border);line(c,cx,cy-r,cx,cy+r,border);text(c,"N",cx-5,cy-r-6,12,green);
   for(int i=0;i<32;i++){p.setColor(Color.argb(55-i,0,255,0));p.setStyle(Paint.Style.FILL);c.drawArc(new RectF(cx-r,cy-r,cx+r,cy+r),(float)Math.toDegrees(game.sweep)-90-i,1.3f,true,p);}line(c,cx,cy,cx+(float)Math.sin(game.sweep)*r,cy-(float)Math.cos(game.sweep)*r,green);
   rect(c,cx-5,cy-5,10,10,green,true);
   for(Game.Contact t:game.contacts)if(game.visible(t)){float x=cx+(float)t.px/game.range*r,y=cy+(float)t.py/game.range*r;int col=game.elapsed-t.seen>7?muted:t.illuminated?amber:green;if(t.illuminated)line(c,cx,cy,x,y,0xff567856);circle(c,x,y,4,col,true);line(c,x,y,x+(float)Math.sin(t.pheading)*12,y-(float)Math.cos(t.pheading)*12,col);if(t==selected)rect(c,x-12,y-12,24,24,amber,false);text(c,fmt("%03d",t.id),x+9,y-8,12,col);}
   for(Game.Missile m:game.missiles)if(m.alive&&Math.hypot(m.x,m.y)<=game.range){float x=cx+(float)m.x/game.range*r,y=cy+(float)m.y/game.range*r;text(c,"+",x-5,y+5,18,m.lost>0?red:amber);}
   text(c,"PALE RING: 25 km / SHADE: TERRAIN",367,555,12,muted);text(c,"BLIP = TRACK    + = MISSILE",367,579,12,muted);
  }
  void engagement(Canvas c){rect(c,855,74,405,524,border,false);text(c,"FIRE CONTROL",874,101,18,green);text(c,fmt("ILLUMINATION %d / 2",game.channels()),874,131,16,amber);
   int i=0;for(Game.Contact t:game.contacts)if(t.alive&&t.illuminated){text(c,fmt("CH%d  TRACK %03d   %d IN FLIGHT",++i,t.id,game.inbound(t)),874,153+i*21,13,green);}while(i<2){text(c,"CH"+(++i)+"  AVAILABLE",874,153+i*21,13,muted);}
   button(c,"next","NEXT TRACK",874,215,178,true);button(c,"range","RANGE "+game.range,1064,215,178,true);
   boolean active=started&&!paused&&!game.finished;button(c,"illuminate","ILLUMINATE",874,275,178,active&&game.liveTrack(selected)&&selected!=null&&!selected.illuminated&&game.channels()<2);button(c,"release","RELEASE CH",1064,275,178,active&&selected!=null&&selected.illuminated);
   button(c,"launch","LAUNCH MIM-23A  /  L"+(game.chosenLauncher+1),874,335,368,active&&game.launchBlock(selected)==null);
   String reason=game.launchBlock(selected);text(c,reason==null?"READY / MAINTAIN ILLUMINATION":reason,874,408,12,reason==null?green:amber);
   text(c,fmt("KILLS %d   MISSES %d   LEAKS %d",game.kills,game.misses,game.leaks),874,437,13,green);text(c,fmt("SCORE %d   WAVES %d/12",game.score,game.spawned),874,459,13,muted);
   text(c,"EVENT LOG",874,488,12,muted);int n=game.log.size();for(int j=Math.max(0,n-4);j<n;j++){String s=game.log.get(j);if(s.length()>43)s=s.substring(0,43);text(c,s,874,510+(j-Math.max(0,n-4))*21,12,muted);}
  }
  void launchers(Canvas c){for(int i=0;i<3;i++){float x=20+i*282;Game.Launcher l=game.launchers[i];rect(c,x,610,270,73,bg,true);rect(c,x,610,270,73,i==game.chosenLauncher?amber:border,false);
   if(launcherIcon!=null){float iw=122,ih=iw*launcherIcon.getHeight()/launcherIcon.getWidth();p.setFilterBitmap(true);p.setAlpha(l.ammo==0?100:255);c.drawBitmap(launcherIcon,null,new RectF(x+7,646.5f-ih/2,x+7+iw,646.5f+ih/2),p);p.setAlpha(255);}
   String status=l.reload>0?fmt("RELOAD %02ds",(int)Math.ceil(l.reload)):l.ammo==0?"EMPTY":"READY";
   text(c,"L"+(i+1),x+141,631,16,i==game.chosenLauncher?amber:green);text(c,l.ammo+" / 3",x+210,631,13,muted);text(c,status,x+141,651,12,l.reload>0?amber:green);
   for(int j=0;j<3;j++)rect(c,x+141+j*37,660,27,12,j<l.ammo?green:border,true);
   if(l.reload>0)rect(c,x+141,677,101*(float)(1-l.reload/90),2,green,true);
   buttons.add(new Btn(x,610,270,73,"l"+i,true));}
   rect(c,866,610,394,73,border,false);text(c,fmt("READY %02d   RESERVE %02d",game.ready(),game.reserve),883,637,18,green);text(c,fmt("%d allocated / 90s per full reload",game.reserved),883,663,13,muted);
  }
  void overlay(Canvas c){rect(c,0,0,1280,720,0xee000000,true);rect(c,190,69,900,580,panel,true);rect(c,190,69,900,580,green,false);buttons.clear();String title=game.finished?(game.won?"MISSION COMPLETE":"BATTERY OVERRUN"):help?"OPERATOR GUIDE":started?"MISSION PAUSED":"HAWK / MISSION 01";text(c,title,225,118,30,green);
   if(game.finished){text(c,fmt("COMMAND SITE %d%% / SCORE %d",game.health,game.score),225,170,23,amber);text(c,fmt("%d intercepts   %d missiles fired   %d leaks",game.kills,game.shots,game.leaks),225,208,19,green);text(c,"DEBRIEF / CONFIRMED AIRCRAFT",225,249,16,muted);int i=0;for(Game.Contact t:game.contacts){int col=i/6,row=i%6;text(c,fmt("%03d %-9s %s",t.id,Game.NAMES[t.type],t.alive?"UNRESOLVED":t.outcome),225+col*421,278+row*32,13,muted);i++;}text(c,"BEST SCORE "+best,225,505,17,amber);}
   else{String[] rows={"Defend the command site through 12 incoming aircraft.","1. Select a radar contact. Two sweeps establish a track.","2. Check estimated identity, altitude, speed and range.","3. ILLUMINATE the target, then LAUNCH inside the envelope.","4. Keep illumination active until intercept or miss.","", "Only TWO targets can be illuminated. RELEASE CH frees one.","Release risks missiles in flight. Low terrain can break guidance.","Select L1 / L2 / L3 below the radar to choose a launcher.","Empty launchers reload automatically: 90 simulation seconds.","9 ready + 9 reserve. A partially used launcher cannot reload.","25 km missile limit / 13,700 m ceiling / 80 km radar coverage.","1x, 2x and 4x accelerate the entire mission, including reloads."};for(int i=0;i<rows.length;i++)text(c,rows[i],225,163+i*27,15,i==0?amber:muted);}
   button(c,"start",started&&!game.finished?"RESUME MISSION":"START MISSION",225,558,270,true);button(c,"sound",sound?"SOUND ON":"SOUND OFF",510,558,240,true);if(started&&!game.finished)button(c,"restart","RESTART",765,558,285,true);text(c,"Simplified fictional simulation / performance references from your guide",225,633,12,muted);
  }
  @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;for(Btn b:buttons)if(b.enabled&&b.r.contains(x,y)){act(b.id);performClick();invalidate();return true;}if(started&&!paused&&!game.finished){Game.Contact pick=null;double near=30;for(Game.Contact t:game.contacts)if(game.visible(t)){double d=Math.hypot(x-(595+t.px/game.range*211),y-(322+t.py/game.range*211));if(d<near){near=d;pick=t;}}if(pick!=null){selected=pick;notice="TRACK "+pick.id+" SELECTED / "+game.trackState(pick);beep();}}invalidate();return true;}
  public boolean performClick(){super.performClick();return true;}
  void act(String id){if(id.equals("start")){if(!started||game.finished){game.start();selected=null;started=true;ended=false;speed=1;}paused=help=false;last=System.nanoTime();notice="SEARCH ACTIVE / TWO SWEEPS TO ESTABLISH TRACK";}
   else if(id.equals("restart")){game.start();selected=null;ended=false;paused=help=false;speed=1;notice="NEW MISSION STARTED";}
   else if(id.equals("pause")){paused=true;help=false;}
   else if(id.equals("help")){paused=true;help=true;}
   else if(id.equals("sound"))sound=!sound;
   else if(id.equals("speed")){speed=speed==1?2:speed==2?4:1;notice="SIMULATION SPEED "+speed+"x / ALL TIMERS ACCELERATE";}
   else if(id.equals("range")){game.range=game.range==50?80:game.range==80?25:50;notice="RADAR DISPLAY RANGE "+game.range+" km";}
   else if(id.equals("next")){ArrayList<Game.Contact> v=new ArrayList<>();for(Game.Contact t:game.contacts)if(game.visible(t))v.add(t);if(!v.isEmpty()){selected=v.get((v.indexOf(selected)+1)%v.size());notice="TRACK "+selected.id+" SELECTED";}else notice="NO VISIBLE TRACKS / WAIT FOR THE SWEEP";}
   else if(id.equals("illuminate"))notice=game.illuminate(selected);
   else if(id.equals("release"))notice=game.release(selected);
   else if(id.equals("launch"))notice=game.launch(selected);
   else if(id.matches("l[012]")){game.chosenLauncher=Integer.parseInt(id.substring(1));notice="LAUNCHER "+(game.chosenLauncher+1)+" SELECTED";}
   beep();
  }
 }
}

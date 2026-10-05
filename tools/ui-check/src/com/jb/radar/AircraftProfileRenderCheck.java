package com.jb.radar;
import android.graphics.Canvas;import android.graphics.Paint;import android.view.MotionEvent;
import java.awt.Graphics2D;import java.awt.RenderingHints;import java.awt.image.BufferedImage;import java.io.File;import java.lang.reflect.Field;import java.util.ArrayList;import javax.imageio.ImageIO;
/** Desktop landscape proof plus recognition/mission-time gates for the aircraft profile overlay. */
public class AircraftProfileRenderCheck {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 static final class Label {final String text;final float x,y,right;Label(String s,float x,float y,Paint p){text=s;this.x=x;this.y=y;right=x+p.measureText(s);}}
 static class Recording extends Canvas {
  final ArrayList<Label> labels=new ArrayList<>();
  Recording(int width,int height)throws Exception{Field field=Canvas.class.getDeclaredField("g");field.setAccessible(true);((Graphics2D)field.get(this)).dispose();image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);field.set(this,graphics);}
  public void drawText(String s,float x,float y,Paint p){labels.add(new Label(s,x,y,p));super.drawText(s,x,y,p);}
  boolean has(String fragment){for(Label label:labels)if(label.text.contains(fragment))return true;return false;}
 }
 static Recording draw(MainActivity.Radar view,int width,int height,String filename)throws Exception{view.last=0;Recording canvas=new Recording(width,height);view.onDraw(canvas);if(filename!=null)ImageIO.write(canvas.image,"png",new File("out/v020-preview/"+filename+".png"));return canvas;}
 static void tap(MainActivity.Radar view,final float x,final float y){view.onTouchEvent(new MotionEvent(){public float getX(){return view.ox+x*view.scale;}public float getY(){return view.oy+y*view.scale;}});}
 static void noRoster(Recording canvas){for(String name:Game.NAMES)ok(!canvas.has(name),"no pre-mission aircraft roster: "+name);}
 public static void main(String[] args)throws Exception{
  new File("out/v020-preview").mkdirs();
  for(final int width:new int[]{800,1600}){final int height=width*9/16;MainActivity app=new MainActivity();app.onCreate(null);app.view=app.new Radar(){public int getWidth(){return width;}public int getHeight(){return height;}};app.onResume();MainActivity.Radar view=app.view;
   noRoster(draw(view,width,height,"menu-"+width));view.act("playmodes");noRoster(draw(view,width,height,"modes-"+width));view.act("storymode");noRoster(draw(view,width,height,"missions-"+width));view.act("missionloadout");noRoster(draw(view,width,height,null));
   ok(view.game.elapsed==0&&view.game.contacts.isEmpty(),"menus do not start mission activity");view.act("missiondeploy");noRoster(draw(view,width,height,null));ok(view.intro.active&&view.game.elapsed==0&&view.game.contacts.isEmpty(),"intro keeps surprise attack gated");view.act("introskip");Game g=view.game;g.contacts.clear();g.nextSpawn=9999;
   Game.Contact target=new Game.Contact();target.id=8;target.type=AircraftProfiles.TU160;target.x=target.px=5;target.alt=target.palt=3000;target.samples=4;target.radarTracked=true;target.seen=g.elapsed;target.quality=1;g.contacts.add(target);view.choose(target);
   draw(view,width,height,null);view.act("aircraftinfo");ok(!view.aircraftOpen,"unrecognized contact cannot expose a profile");
   for(int type=0;type<AircraftProfiles.COUNT;type++){target.type=type;target.recognizedType=g.aircraft(target).name;draw(view,width,height,null);tap(view,1220,169);ok(view.aircraftOpen,"visible INFO label is tappable");double at=g.elapsed;view.last=System.nanoTime()-1000000000L;Recording frame=new Recording(width,height);view.onDraw(frame);ok(g.elapsed==at,"profile overlay freezes simulation");
    ok(frame.has("MISSION TASK  UNCONFIRMED"),"unobserved task stays unknown");ok(frame.has("PROFILE LIMITS / NOT LIVE ENEMY TELEMETRY"),"static capacity is not represented as live data");boolean overlay=false;for(Label label:frame.labels){if(label.text.contains(" / NCTR PROFILE"))overlay=true;if(overlay)ok(label.x>=184&&label.right<=1098&&label.y<=640,"overlay text fits: "+label.text);}
    ImageIO.write(frame.image,"png",new File("out/v020-preview/profile-"+type+"-"+width+".png"));if(type==AircraftProfiles.TU160)ok(frame.has("PROVISIONAL GAME FIGURES"),"Tu-160 proposals are identified");
    tap(view,1020,105);ok(!view.aircraftOpen,"profile BACK touch returns to radar");
   }
   target.recognizedType="";g.finished=true;view.act("debrief:8");ok(view.aircraftOpen&&view.displayedAircraft()!=null,"debrief may reveal encountered aircraft");draw(view,width,height,"debrief-profile-"+width);app.onBackPressed();ok(!view.aircraftOpen,"Android back closes profile");app.onDestroy();
  }
  System.out.println("PASS: 800x450 and 1600x900 aircraft profiles, text bounds, INFO/BACK touch, no premature roster, paused overlay and debrief recognition gate");
 }
}

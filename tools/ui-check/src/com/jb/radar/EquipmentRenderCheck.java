package com.jb.radar;
import android.graphics.*;import javax.imageio.ImageIO;import java.io.File;
public class EquipmentRenderCheck {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void draw(MainActivity.Radar v,String name)throws Exception{v.last=0;Canvas c=new Canvas();v.onDraw(c);ImageIO.write(c.image,"png",new File("out/"+name+".png"));}
 public static void main(String[] args)throws Exception{
  MainActivity app=new MainActivity();app.onCreate(null);app.onResume();MainActivity.Radar v=app.view;check(!v.profileFallback,"APK asset profiles load");draw(v,"menu-v08");v.act("equipment");draw(v,"equipment-radar-v08");check(v.equipmentOpen&&app.menuMusic.isPlaying(),"equipment menu and music");v.act("equipprev");check(v.equipmentIndex==6,"previous wraps");draw(v,"equipment-passive-v08");v.act("equipnext");v.act("equipnext");draw(v,"equipment-ir-v08");check(v.equipmentIndex==1,"next cycles");app.onBackPressed();check(!v.equipmentOpen&&!v.started,"back returns to menu");v.act("start");for(int i=0;i<2000;i++)v.game.tick(.05);for(Game.Contact t:v.game.contacts)if(v.game.liveTrack(t)){v.choose(t);v.game.track(t);v.game.illuminate(t);break;}draw(v,"gameplay-v08");v.act("help");draw(v,"guide-v08");check(v.paused,"guide pauses mission");app.onPause();app.onDestroy();System.out.println("PASS: loaded equipment screen, profile browsing/wrapping, back, music, gameplay stats and guide");
 }
}

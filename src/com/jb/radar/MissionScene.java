package com.jb.radar;
import android.graphics.*;
import java.util.Random;

/** Decorative selection screens: never advance the combat simulation. */
final class MissionScene {
 final MenuScene art=new MenuScene();final float[][] flecks=new float[450][3];
 static final int GREEN=0xff58ffa0,WHITE=0xffeef6f0,DIM=0xff709383;
 MissionScene(){Random r=new Random(1973);for(float[] f:flecks){f[0]=r.nextFloat()*1280;f[1]=r.nextFloat()*720;f[2]=r.nextFloat()*2+1;}}
 void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){art.txt(c,s,x,y,size,color,bold);}
 void center(Canvas c,String s,float x,float y,float w,float size,int color,boolean bold){art.p.setTypeface(bold?art.condensed:Typeface.MONOSPACE);art.p.setTextSize(size);text(c,s,x+(w-art.p.measureText(s))/2,y,size,color,bold);}
 void background(Canvas c,double time,String title){c.drawColor(0xff000502);
  for(int x=0;x<1280;x+=64)art.line(c,x,0,x,720,0xff06110b,1);
  for(int y=0;y<720;y+=64)art.line(c,0,y,1280,y,0xff06110b,1);
  for(float[] f:flecks)art.circle(c,f[0],f[1],f[2],0xff082116,true,1);
  for(int r=90;r<=650;r+=85)art.circle(c,640,350,r,0xff103223,false,1);
  art.line(c,0,350,1280,350,0xff15412a,1);art.line(c,640,0,640,720,0xff15412a,1);
  for(int i=0;i<72;i++){double a=i*Math.PI/36;float r=i%3==0?515:526;art.line(c,640+(float)Math.cos(a)*r,350+(float)Math.sin(a)*r,640+(float)Math.cos(a)*533,350+(float)Math.sin(a)*533,0xff1b3d2b,1);}
  double angle=time*.12;art.line(c,640,350,640+(float)Math.cos(angle)*780,350+(float)Math.sin(angle)*780,0xff17462c,1);
  float scan=(float)(time*14%720);art.line(c,0,scan,1280,scan,0x1258ffa0,2);
  text(c,title,48,51,24,WHITE,false);art.line(c,title.equals("SELECT MODE")?315:355,49,1232,49,GREEN,1);
  text(c,"AIR DEFENSE",1110,39,13,GREEN,false);
 }
 void frame(Canvas c,float x,float y,float w,float h,boolean ready){int color=ready?GREEN:0xff387e60;
  Path shape=MenuScene.chamfer(x,y,w,h,13);art.paint(0xff000301,true,1);c.drawPath(shape,art.p);art.paint(color,false,1.5f);c.drawPath(shape,art.p);
  for(int side=0;side<2;side++)for(int row=0;row<2;row++){float xx=side==0?x:x+w,yy=row==0?y:y+h,sx=side==0?1:-1,sy=row==0?1:-1;art.line(c,xx,yy+sy*27,xx,yy+sy*13,color,4);art.line(c,xx,yy+sy*13,xx+sx*13,yy,color,4);art.line(c,xx+sx*13,yy,xx+sx*27,yy,color,4);}
 }
 void lock(Canvas c,float x,float y){art.paint(DIM,false,2);c.drawArc(new RectF(x+4,y,x+14,y+16),180,180,false,art.p);art.rect(c,x,y+8,18,16,DIM,true,2);art.circle(c,x+9,y+14,2,0xff000301,true,1);}
 void control(Canvas c,String label,float x,float y,float w,boolean ready){frame(c,x,y,w,76,ready);center(c,label,x,y+48,w,27,ready?WHITE:DIM,true);}
 void modes(Canvas c,double time){background(c,time,"SELECT MODE");frame(c,150,202,445,263,true);frame(c,685,202,445,263,false);center(c,"STORY",150,354,445,61,WHITE,true);center(c,"SURVIVAL",685,354,445,61,WHITE,true);center(c,"UNAVAILABLE",685,405,445,17,DIM,false);text(c,"< BACK",42,683,18,GREEN,false);}
 void missions(Canvas c,double time){background(c,time,"MISSION SELECT");text(c,"< MODES",42,99,16,GREEN,false);
  for(int i=0;i<8;i++){float x=115+(i%4)*284,y=178+(i/4)*209;frame(c,x,y,195,140,i==0);text(c,String.format(java.util.Locale.US,"%02d",i+1),x+18,y+52,47,WHITE,true);if(i==0)text(c,"READY",x+18,y+123,17,GREEN,false);else{lock(c,x+18,y+104);text(c,"LOCKED",x+47,y+122,15,DIM,false);}}
  control(c,"STORE",36,594,280,true);control(c,"LOADOUT >",964,594,280,true);center(c,"BEFORE THE DAWN",335,643,610,29,WHITE,true);center(c,"06 OCT 1973 / 0445",335,671,610,16,GREEN,false);center(c,"MISSION 01 SELECTED",335,611,610,13,GREEN,false);
 }
 void loadout(Canvas c,double time,Economy.State wallet,Equipment equipment,String notice){background(c,time,"MISSION LOADOUT");text(c,"BEFORE THE DAWN",64,112,30,WHITE,true);text(c,"06 OCT 1973 / 0445",64,141,16,GREEN,false);
  frame(c,64,184,1152,111,true);text(c,"BATTERY",87,218,14,GREEN,false);text(c,equipment.radar.name,87,260,31,WHITE,true);text(c,"L1 / L2 / L3",968,222,16,GREEN,false);text(c,"9 READY + 9 RESERVE",916,260,19,WHITE,false);
  for(int i=0;i<2;i++){Equipment.Weapon w=equipment.all[i];float x=64+i*592;boolean owned=wallet.owned.contains(w.id),selected=wallet.equippedMissile.equals(w.id);frame(c,x,329,560,189,owned);text(c,w.name,x+22,378,31,WHITE,true);text(c,"RADAR / SEMI-ACTIVE",x+22,417,17,GREEN,false);text(c,selected?"EQUIPPED":owned?"TAP TO EQUIP":"LOCKED / RESEARCH IN STORE",x+22,488,16,owned?GREEN:DIM,false);}
  text(c,notice,65,556,16,GREEN,false);control(c,"< MISSIONS",36,594,280,true);control(c,"START MISSION >",876,594,368,!notice.startsWith("SAVE FAILED"));
 }
}

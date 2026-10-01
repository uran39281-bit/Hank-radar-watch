package com.jb.radar;
import android.graphics.*;
import java.util.Random;

/** Animated, decorative menu radar. It never reads or advances mission state. */
final class MenuScene {
 static final int GREEN=0xff00ff00,WHITE=0xffecf6ec,DIM=0xff7bb87b;
 final Paint p=new Paint(3);
 final Typeface condensed=Typeface.create("sans-serif-condensed",Typeface.BOLD);
 final Path radarClip=new Path(),playShape=chamfer(52,309,554,91,12);
 final RectF sweepBounds=new RectF(711,129,1145,563);
 final Path[] coast=new Path[6];
 final float[][] blips={{-.39f,-.56f},{.68f,-.22f},{.65f,.43f},{-.68f,.32f},{.17f,.70f}};
 MenuScene(){radarClip.addCircle(928,346,217,Path.Direction.CW);Random rng=new Random(201);
  float[][] islands={{-93,-80,68,105},{-53,79,52,95},{65,-85,114,60},{105,8,54,71},{133,132,48,26},{-10,-175,28,20}};
  for(int k=0;k<coast.length;k++){coast[k]=new Path();float[] a=islands[k];for(int i=0;i<=64;i++){double angle=i*Math.PI/32,rough=.82+rng.nextDouble()*.28;float x=928+a[0]+(float)(Math.cos(angle)*a[2]*rough),y=346+a[1]+(float)(Math.sin(angle)*a[3]*rough);if(i==0)coast[k].moveTo(x,y);else coast[k].lineTo(x,y);}coast[k].close();}
 }
 static Path chamfer(float x,float y,float w,float h,float cut){Path s=new Path();s.moveTo(x+cut,y);s.lineTo(x+w-cut,y);s.lineTo(x+w,y+cut);s.lineTo(x+w,y+h-cut);s.lineTo(x+w-cut,y+h);s.lineTo(x+cut,y+h);s.lineTo(x,y+h-cut);s.lineTo(x,y+cut);s.close();return s;}
 void paint(int color,boolean fill,float width){p.setColor(color);p.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);p.setStrokeWidth(width);}
 void line(Canvas c,float x,float y,float xx,float yy,int color,float width){paint(color,false,width);c.drawLine(x,y,xx,yy,p);}
 void circle(Canvas c,float x,float y,float radius,int color,boolean fill,float width){paint(color,fill,width);c.drawCircle(x,y,radius,p);}
 void rect(Canvas c,float x,float y,float w,float h,int color,boolean fill,float radius){paint(color,fill,1);c.drawRoundRect(new RectF(x,y,x+w,y+h),radius,radius,p);}
 void txt(Canvas c,String s,float x,float y,float size,int color,boolean bold){paint(color,true,1);p.setTypeface(bold?condensed:Typeface.MONOSPACE);p.setTextSize(size);c.drawText(s,x,y,p);}
 void corners(Canvas c,float x,float y,float w,float h,int color){float d=11;line(c,x,y,x+d,y,color,2);line(c,x,y,x,y+d,color,2);line(c,x+w-d,y,x+w,y,color,2);line(c,x+w,y,x+w,y+d,color,2);line(c,x,y+h-d,x,y+h,color,2);line(c,x,y+h,x+d,y+h,color,2);line(c,x+w-d,y+h,x+w,y+h,color,2);line(c,x+w,y+h-d,x+w,y+h,color,2);}
 void reticle(Canvas c,float x,float y,float r,int color){circle(c,x,y,r,color,false,1);circle(c,x,y,r*.52f,color,false,1);line(c,x-r-5,y,x+r+5,y,color,1);line(c,x,y-r-5,x,y+r+5,color,1);circle(c,x,y,2,WHITE,true,1);}
 void draw(Canvas c,double seconds,String dollars,String bp,boolean saveFailed,boolean fallback){
  c.drawColor(0xff000400);
  // A quiet grid, small registration crosses and moving phosphor scan lines.
  for(int x=0;x<1280;x+=40)line(c,x,0,x,720,0xff020b02,1);
  for(int y=0;y<720;y+=40)line(c,0,y,1280,y,0xff020b02,1);
  for(int x=640;x<1280;x+=148)for(int y=135;y<710;y+=165){line(c,x-6,y,x+6,y,0xff123412,1);line(c,x,y-6,x,y+6,0xff123412,1);}
  float scan=(float)((seconds*19)%724)-4;for(int i=0;i<5;i++)line(c,0,scan+i,1280,scan+i,0x0800ff00,1);
  for(int y=0;y<720;y+=4)line(c,0,y,1280,y,0x18000000,1);
  radar(c,seconds);
  // Solid header and left control area keep decorative movement behind the content.
  rect(c,0,0,1280,69,0xe8000500,true,0);line(c,9,68,1271,68,0xff397239,1);corners(c,9,8,1262,60,0xff56a256);
  reticle(c,59,37,22,0xff328532);
  wallet(c,785,9,217,"DOLLARS",dollars,false);wallet(c,1038,9,224,"BP / BATTLE POINTS",bp,true);
  rect(c,27,105,600,582,0x88000400,true,0);
  rect(c,52,139,5,20,GREEN,true,0);txt(c,"M A I N  M E N U",77,157,15,DIM,false);line(c,242,150,605,150,0xff437b43,1);
  paint(WHITE,true,1);p.setTypeface(condensed);p.setTextSize(104);float fit=Math.min(1,554/p.measureText("AIR DEFENSE"));c.save();c.translate(49,270);c.scale(fit,1);c.drawText("AIR DEFENSE",0,0,p);c.restore();
  play(c,seconds);
  menuButton(c,418,"USA TECH TREE",0);menuButton(c,504,"ECONOMY / REWARDS",1);menuButton(c,590,"EQUIPMENT / STATS",2);
  if(saveFailed)txt(c,"SAVE PENDING / OPEN ECONOMY TO RETRY",53,688,12,WHITE,false);
  else if(fallback)txt(c,"CONFIG ERROR / DEFAULT VALUES LOADED",53,688,12,WHITE,false);
  else txt(c,"AIR DEFENSE  /  v0.19",53,694,10,0xff436343,false);
 }
 void wallet(Canvas c,float x,float y,float w,String label,String value,boolean bp){rect(c,x,y,w,55,0xff010901,true,7);rect(c,x,y,w,55,0xff214721,false,7);corners(c,x,y,w,55,0xff346c34);icon(c,x+33,y+29,bp?3:1,.70f,0xff60d260);txt(c,label,x+71,y+18,bp?10:12,DIM,false);txt(c,value,x+71,y+46,28,0xffc2f2c2,true);}
 void play(Canvas c,double time){float pulse=(float)(.5+.5*Math.sin(time*1.5));
  for(int n=8;n>0;n--){paint(Color.argb(3+(int)(pulse*2),0,255,0),false,n*3);c.drawPath(playShape,p);}
  paint(0xff00ed00,true,1);c.drawPath(playShape,p);
  c.save();c.clipPath(playShape);for(int i=0;i<45;i++)line(c,52,309+i,606,309+i,Color.argb((45-i)/3,255,255,255),1);
  float shine=52+(float)((time*90)%1400)-500;Path strip=new Path();strip.moveTo(shine,309);strip.lineTo(shine+35,309);strip.lineTo(shine+125,400);strip.lineTo(shine+90,400);strip.close();paint(0x16ffffff,true,1);c.drawPath(strip,p);c.restore();
  paint(0xff8dff8d,false,1);c.drawPath(playShape,p);corners(c,49,306,560,97,0xffbaffba);
  Path arrow=new Path();arrow.moveTo(94,336);arrow.lineTo(94,374);arrow.lineTo(126,355);arrow.close();paint(0xff001300,true,1);c.drawPath(arrow,p);txt(c,"PLAY",165,374,53,0xff001100,true);
 }
 void menuButton(Canvas c,float y,String name,int symbol){rect(c,52,y,554,71,0xff010a01,true,7);rect(c,52,y,554,71,0xff43aa43,false,7);line(c,66,y+1,593,y+1,0xff67b267,1);icon(c,99,y+36,symbol,1,0xff83e883);txt(c,name,155,y+44,23,WHITE,true);line(c,560,y+26,570,y+36,0xff83e883,2);line(c,570,y+36,560,y+46,0xff83e883,2);}
 void icon(Canvas c,float x,float y,int kind,float size,int color){c.save();c.translate(x,y);c.scale(size,size);
  if(kind==0){rect(c,-5,-16,10,10,color,false,0);line(c,0,-6,0,2,color,2);line(c,-13,2,13,2,color,2);line(c,-13,2,-13,10,color,2);line(c,13,2,13,10,color,2);rect(c,-18,10,10,10,color,false,0);rect(c,8,10,10,10,color,false,0);}
  else if(kind==1){for(int col=1;col>=0;col--){float xx=-15+col*12,yy=-1-col*10;paint(color,false,1.7f);for(int row=0;row<3;row++)c.drawOval(new RectF(xx,yy+row*6,xx+20,yy+9+row*6),p);line(c,xx,yy+4,xx,yy+16,color,1.7f);line(c,xx+20,yy+4,xx+20,yy+16,color,1.7f);}}
  else if(kind==2){Path gear=new Path();for(int i=0;i<48;i++){double a=i*Math.PI/24;float r=(i%6<3)?18:14;float xx=(float)Math.cos(a)*r,yy=(float)Math.sin(a)*r;if(i==0)gear.moveTo(xx,yy);else gear.lineTo(xx,yy);}gear.close();paint(color,false,2);c.drawPath(gear,p);circle(c,0,0,7,color,false,2);}
  else{Path shield=new Path();shield.moveTo(-12,-17);shield.lineTo(0,-11);shield.lineTo(12,-17);shield.lineTo(12,0);shield.lineTo(0,9);shield.lineTo(-12,0);shield.close();paint(color,false,2);c.drawPath(shield,p);for(int j=0;j<2;j++){line(c,-11,9+j*8,0,16+j*8,color,2);line(c,0,16+j*8,11,9+j*8,color,2);}}
  c.restore();
 }
 void radar(Canvas c,double seconds){float cx=928,cy=346,r=217;double sweep=(seconds*27-44)%360;
  circle(c,cx,cy,273,0xff0b260b,false,1);circle(c,cx,cy,261,0xff163f16,false,1);circle(c,cx,cy,253,0xff0a230a,false,1);
  c.save();c.clipPath(radarClip);circle(c,cx,cy,r,0xff000800,true,1);
  for(Path path:coast){paint(0xff021202,true,1);c.drawPath(path,p);paint(0xff082408,false,1);c.drawPath(path,p);}
  // Latitude curves and a faint cartographic grid remain under the beam.
  for(int i=-3;i<=3;i++){paint(0xff062306,false,1);c.drawOval(new RectF(cx-r,cy+i*49-20,cx+r,cy+i*49+20),p);c.drawOval(new RectF(cx+i*49-22,cy-r,cx+i*49+22,cy+r),p);}
  for(int i=66;i>=0;i--){paint(Color.argb((int)(75*Math.pow(1-i/67.0,2)),0,255,0),true,1);c.drawArc(sweepBounds,(float)sweep-i,1.2f,true,p);}
  c.restore();
  for(int i=1;i<=4;i++)circle(c,cx,cy,r*i/4, i==4?0xff2eb82e:0xff164c16,false,i==4?1.3f:1);
  line(c,cx-r-12,cy,cx+r+12,cy,0xff488648,1);line(c,cx,cy-r-12,cx,cy+r+12,0xff488648,1);
  for(int deg=0;deg<360;deg+=2){double a=Math.toRadians(deg);float inside=deg%30==0?239:deg%10==0?245:248;int color=deg%30==0?0xff629e62:0xff264926;line(c,cx+(float)Math.cos(a)*inside,cy+(float)Math.sin(a)*inside,cx+(float)Math.cos(a)*252,cy+(float)Math.sin(a)*252,color,deg%30==0?1.4f:1);}
  txt(c,"360",cx-14,cy-r-25,13,DIM,false);txt(c,"90",cx+r+28,cy+5,13,DIM,false);txt(c,"180",cx-14,cy+r+36,13,DIM,false);txt(c,"270",cx-r-44,cy+5,13,DIM,false);
  double radians=Math.toRadians(sweep);for(int i=4;i>0;i--)line(c,cx,cy,cx+(float)Math.cos(radians)*r,cy+(float)Math.sin(radians)*r,Color.argb(7,0,255,0),i*3);line(c,cx,cy,cx+(float)Math.cos(radians)*r,cy+(float)Math.sin(radians)*r,0xff62ff62,1.3f);
  for(int i=0;i<blips.length;i++){float dx=blips[i][0]+(float)Math.sin(seconds*.13+i)*.025f,dy=blips[i][1]+(float)Math.cos(seconds*.10+i)*.02f;double bearing=Math.toDegrees(Math.atan2(dy,dx)),since=(sweep-bearing+720)%360;float fresh=(float)Math.exp(-since/90),x=cx+dx*r,y=cy+dy*r;int alpha=(int)(105+150*fresh);
   for(int glow=5;glow>=1;glow--)circle(c,x,y,4+glow*2,Color.argb((int)(4+fresh*8),0,255,0),true,1);circle(c,x,y,3.3f+fresh,Color.argb(alpha,132,255,132),true,1);
   if(since<32)circle(c,x,y,5+(float)since*.36f,Color.argb((int)((1-since/32)*95),70,255,70),false,1);
  }
  for(int i=6;i>0;i--)circle(c,cx,cy,i*2,0x0900ff00,true,1);circle(c,cx,cy,2.5f,0xff9aff9a,true,1);
 }
}

package com.jb.radar;
public class TrackingPreview{
 public static void main(String[] args)throws Exception{MainActivity a=new MainActivity();a.onCreate(null);a.onResume();MainActivity.Radar v=a.view;v.act("start");Game g=v.game;g.contacts.clear();g.elapsed=10;
  for(int i=0;i<3;i++){Game.Contact t=new Game.Contact();t.id=11+i;t.type=i;t.x=t.px=new double[]{-10,9,12}[i];t.y=t.py=new double[]{-6,-6,7}[i];t.alt=t.palt=3000;t.speed=t.pspeed=600;t.pheading=.8*i;t.samples=i==0?1:6;t.seen=10;t.priority=i!=0;t.radarTracked=i!=0;t.confidence=i==2?.92:.50;t.guess=i;t.launchObserved=i!=0;t.illuminated=i==2;g.contacts.add(t);if(i==1)v.selected=t;}
  v.notice="TRACK: DASHED / LOCK: SOLID + STAR / ICON COLOR = IDENTITY";TechTreeRenderCheck.draw(v,"tracking-v012");v.act("help");TechTreeRenderCheck.draw(v,"guide-v012");a.onDestroy();
 }
}

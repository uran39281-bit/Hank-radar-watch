package com.jb.radar;
public class GameTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Game.Contact target(Game g,int n){Game.Contact t=new Game.Contact();t.id=100+n;t.type=0;t.x=9+n;t.y=0;t.alt=3000;t.speed=500;t.heading=-Math.PI/2;t.px=t.x;t.palt=t.alt;t.pspeed=t.speed;t.samples=4;t.seen=g.elapsed;t.quality=1;t.priority=true;t.radarTracked=true;t.alive=true;t.desiredAlt=2900;g.contacts.add(t);return t;}
 public static void main(String[] a){
  Game g=new Game(8);g.start();check(g.ready()==9&&g.reserve==9,"inventory");g.contacts.clear();
  Game.Contact t=target(g,0),t2=target(g,1),t3=target(g,2);
  check(g.launchBlock(t)!=null,"illumination required");g.illuminate(t);g.illuminate(t2);g.illuminate(t3);check(g.channels()==2&&!t3.illuminated,"two channel cap");
  g.launch(t);check(g.launchers[0].ammo==2&&g.launchers[0].reload==0,"no partial reload");g.launch(t);g.launch(t);check(g.launchers[0].ammo==0&&g.launchers[0].reload==90&&g.reserve==9&&g.reserved==3,"empty starts reload but reserves not yet deducted");
  g.running=false;g.tick(.1);check(g.launchers[0].reload==90,"paused reload");g.running=true;
  for(int i=0;i<899;i++)g.tick(.1);check(g.launchers[0].ammo==0,"not ready before 90 seconds");for(int i=0;i<3;i++)g.tick(.1);check(g.launchers[0].ammo==3&&g.reserve==6&&g.reserved==0,"90 second refill");
  g.start();g.contacts.clear();t=target(g,0);g.illuminate(t);t.px=17;check(g.launchBlock(t).contains("RANGE"),"range gate");t.px=2;t.palt=14000;check(g.launchBlock(t).contains("CEILING"),"ceiling gate");
  t.palt=3000;t.samples=1;check(g.launchBlock(t).contains("TRACK"),"two scans required");t.samples=3;t.seen=-17;check(!g.liveTrack(t)&&g.visible(t)&&g.trackState(t).equals("COASTING"),"coast persistence");t.seen=-24;check(!g.visible(t),"lost track expiry");
  g.start();g.contacts.clear();t=target(g,0);t.x=13;t.y=-15;t.alt=100;check(g.signal(t)==0,"terrain masks low target");t.alt=9000;check(g.signal(t)>.8,"high target clears terrain");
  g.start();g.contacts.clear();t=target(g,0);t.x=10;t.y=0;t.alt=3000;g.illuminate(t);g.launch(t);Game.Missile m=g.missiles.get(0);check(m.speed<.2,"missile does not spawn at max speed");g.release(t);for(int i=0;i<45;i++)g.fly(m,.1);check(!m.alive&&g.kills==0,"lost illumination causes miss");
  g.start();g.contacts.clear();t=target(g,0);g.illuminate(t);g.launch(t);m=g.missiles.get(0);for(int i=0;i<600&&m.alive;i++)g.fly(m,.1);check(g.kills==1&&!t.alive,"geometric intercept stationary target");
  g.start();for(int i=0;i<1200;i++)g.tick(.05);boolean observed=false;for(Game.Contact c:g.contacts){if(c.samples>0){observed=true;double sum=0;for(double p:c.probabilities)sum+=p;check(Math.abs(sum-1)<1e-9,"normalized probabilities");check(c.confidence<1,"no perfect identification");}}check(observed,"radar detection");
  for(int i=0;i<15000&&!g.finished;i++)g.tick(.05);check(g.finished,"mission resolves");check(g.reserve>=0&&g.reserved>=0,"no negative inventory");
  g.start();g.tick(0);check(Double.isFinite(g.contacts.get(0).heading),"zero dt safe");check(g.score==0&&g.shots==0&&g.health==100,"restart reset");
  System.out.println("PASS: inventory, two channels, reload restrictions and timing, range/ceiling, coasting, terrain, acceleration, guidance loss, geometric interception, uncertainty, mission ending, restart");
 }
}

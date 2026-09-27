package com.jb.radar;
import java.util.*;
/** Fictional arcade heat seeker. Values are game balance, not weapon specifications. */
public class Infrared {
 public static final double COOL_TIME=2,ACQUIRE_TIME=1.2,MAX_RANGE=9;
 public final Game game;
 public Game.Contact target;
 public int ammo=6,reserve=6,shots;
 public double cool,acquire,reload,activeTime;
 public boolean active,locked;
 public static class Flare {public double x,y,z,vx,vy,age;public Game.Contact source;}
 public ArrayList<Flare> flares=new ArrayList<>();
 public Infrared(Game g){game=g;}
 public void reset(){ammo=reserve=6;shots=0;reload=0;flares.clear();cancel();}
 public void cancel(){target=null;active=locked=false;cool=acquire=activeTime=0;}
 public String begin(Game.Contact t){if(!game.running)return "MISSION NOT ACTIVE";if(t==null||!t.alive||!t.priority||t.samples==0)return "TRACK A CONTACT FIRST";if(ammo==0)return reload>0?"IR-6 RELOADING":"IR-6 EMPTY";cancel();target=t;active=true;game.event("IR SEEKER COOLING / "+game.tag(t));return game.message;}
 public boolean lineOfSight(double x,double y,double z,Game.Contact t){return clearLine(x,y,z,t.x,t.y,t.alt/1000);}
 boolean clearLine(double x,double y,double z,double tx,double ty,double tz){for(int i=1;i<24;i++){double f=i/24.0;if(Game.terrain(x+(tx-x)*f,y+(ty-y)*f)/1000>z+(tz-z)*f)return false;}return true;}
 public double heat(Game.Contact t,double x,double y,double z){if(t==null||!t.alive)return 0;double dx=t.x-x,dy=t.y-y,d=Math.sqrt(dx*dx+dy*dy+Math.pow(t.alt/1000-z,2));if(!lineOfSight(x,y,z,t))return 0;
  // Rear views expose more exhaust; front views still work at shorter range.
  double rear=(1+Math.cos(Game.delta(t.heading,Game.angle(dx,dy))))/2;
  double strength=t instanceof Game.EnemyMissile?(((Game.EnemyMissile)t).age<12?5.5:2.5):6+3*rear;
  return strength/Math.max(.5,d);
 }
 public double baseHeat(){return heat(target,0,0,(Game.terrain(0,0)+35)/1000);}
 public boolean canSee(Game.Contact t){return t!=null&&t.alive&&Math.hypot(t.range(),t.alt/1000)<=MAX_RANGE&&t.alt<=6000&&heat(t,0,0,(Game.terrain(0,0)+35)/1000)>=1;}
 boolean nearbyFlare(Game.Contact t){for(Flare f:flares)if(f.age<3&&Math.hypot(f.x-t.x,f.y-t.y)<.8)return true;return false;}
 public void tick(double dt){if(reload>0){reload=Math.max(0,reload-dt);if(reload==0){ammo=6;reserve-=6;game.event("IR-6 RELOAD COMPLETE");}}
  for(Iterator<Flare> i=flares.iterator();i.hasNext();){Flare f=i.next();f.age+=dt;f.x+=f.vx*dt;f.y+=f.vy*dt;f.vx*=Math.max(0,1-dt*.7);f.vy*=Math.max(0,1-dt*.7);f.z-=.015*dt;if(f.age>4||f.z*1000<Game.terrain(f.x,f.y))i.remove();}
  if(!active)return;if(target==null||!target.alive){cancel();return;}activeTime+=dt;if(activeTime>30){cancel();game.event("IR SEEKER TIMED OUT / REACTIVATE");return;}if(cool<COOL_TIME){cool=Math.min(COOL_TIME,cool+dt);return;}
  boolean was=locked;if(canSee(target)&&!nearbyFlare(target))acquire=Math.min(ACQUIRE_TIME,acquire+dt);else acquire=0;
  locked=acquire>=ACQUIRE_TIME;if(locked&&!was)game.event("IR HEAT LOCK / "+game.tag(target));if(!locked&&was)game.event("IR HEAT LOCK LOST");
 }
 public String state(){if(!active)return "SEEKER OFF";if(cool<COOL_TIME)return String.format(Locale.US,"COOLING %.1fs",COOL_TIME-cool);if(locked)return "HEAT LOCK / READY";if(nearbyFlare(target))return "FLARE INTERFERENCE";if(!canSee(target))return "SEARCH / WEAK OR MASKED HEAT";return String.format(Locale.US,"ACQUIRING %d%%",(int)(acquire/ACQUIRE_TIME*100));}
 public String launchBlock(Game.Contact t){if(!game.running)return "MISSION NOT ACTIVE";if(ammo==0)return reload>0?"IR-6 RELOADING":"IR-6 EMPTY";if(!active||target!=t)return "ACTIVATE IR LOCK";if(!canSee(t))return "HEAT TOO WEAK / MASKED / OUT OF RANGE";if(nearbyFlare(t))return "FLARE INTERFERENCE";if(!locked)return state();if(Math.hypot(t.range(),t.alt/1000)<.6)return "INSIDE IR MINIMUM RANGE";return null;}
 public String launch(Game.Contact t){String why=launchBlock(t);if(why!=null)return why;Game.Missile m=new Game.Missile();m.target=t;m.infrared=true;m.z=(Game.terrain(0,0)+5)/1000;double dx=t.x,dy=t.y,dz=t.alt/1000-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);m.vx=dx/d;m.vy=dy/d;m.vz=dz/d;game.missiles.add(m);ammo--;shots++;game.shots++;if(ammo==0&&reserve>=6)reload=60;cancel();game.event("IR-6 AWAY / SELF GUIDING");return game.message;}
 public void deployFlares(Game.Contact t){if(t instanceof Game.EnemyMissile||t.flareBursts<=0)return;t.flareBursts--;t.nextFlare=game.elapsed+5;for(int side:new int[]{-1,1}){Flare f=new Flare();f.source=t;f.x=t.x;f.y=t.y;f.z=t.alt/1000;f.vx=Math.sin(t.heading)*t.speed/7200+Math.cos(t.heading)*.08*side;f.vy=-Math.cos(t.heading)*t.speed/7200+Math.sin(t.heading)*.08*side;flares.add(f);}game.event("FLARES / "+game.tag(t));}
 void evade(Game.Missile m){Game.Contact t=m.target;if(t instanceof Game.EnemyMissile||!t.alive||game.elapsed<t.nextFlare)return;if(Math.hypot(t.x-m.x,t.y-m.y)<3.5){t.evasion=Math.max(t.evasion,4);t.nextFlare=game.elapsed+5;if(game.random.nextDouble()<.65)deployFlares(t);}}
 public void fly(Game.Missile m,double dt){m.age+=dt;Game.Contact t=m.target;evade(m);double tx=t.x,ty=t.y,tz=t.alt/1000;
  if(m.decoy==null){for(Flare f:flares){double dx=f.x-m.x,dy=f.y-m.y,dz=f.z-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);double dot=(dx*m.vx+dy*m.vy+dz*m.vz)/Math.max(.001,d);if(f.age<2.5&&dot>.93&&clearLine(m.x,m.y,m.z,f.x,f.y,f.z)&&12/Math.max(.5,d)>heat(t,m.x,m.y,m.z)*1.2){m.decoy=f;game.event("IR-6 DIVERTED BY FLARE");break;}}}
  if(m.decoy!=null){tx=m.decoy.x;ty=m.decoy.y;tz=m.decoy.z;}
  double dx=tx-m.x,dy=ty-m.y,dz=tz-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz),dot=(dx*m.vx+dy*m.vy+dz*m.vz)/Math.max(.001,d);
  boolean guided=m.decoy!=null?m.decoy.age<4&&clearLine(m.x,m.y,m.z,tx,ty,tz):t.alive&&heat(t,m.x,m.y,m.z)>.5;
  guided=guided&&dot>.50;if(guided)m.lost=0;else m.lost+=dt;
  if(guided&&d>.001){double blend=Math.min(1,dt*2);m.vx+=(dx/d-m.vx)*blend;m.vy+=(dy/d-m.vy)*blend;m.vz+=(dz/d-m.vz)*blend;}
  double n=Math.sqrt(m.vx*m.vx+m.vy*m.vy+m.vz*m.vz);if(n<.001)n=1;m.vx/=n;m.vy/=n;m.vz/=n;
  if(m.age<4)m.speed=Math.min(.72,m.speed+.16*dt);else if(m.age>12)m.speed=Math.max(.2,m.speed-.018*dt);
  double ox=m.x,oy=m.y,oz=m.z;m.x+=m.vx*m.speed*dt;m.y+=m.vy*m.speed*dt;m.z+=m.vz*m.speed*dt;m.path+=m.speed*dt;
  double sx=m.x-ox,sy=m.y-oy,sz=m.z-oz,len=sx*sx+sy*sy+sz*sz,f=len==0?0:Game.clamp(((tx-ox)*sx+(ty-oy)*sy+(tz-oz)*sz)/len,0,1);double near=Math.sqrt(Math.pow(tx-ox-f*sx,2)+Math.pow(ty-oy-f*sy,2)+Math.pow(tz-oz-f*sz,2));
  if(guided&&near<.10&&m.age>.5){m.alive=false;if(m.decoy==null&&t.alive){t.alive=false;t.illuminated=false;t.outcome="IR INTERCEPT";game.kills++;game.score+=250;game.event("IR INTERCEPT / "+game.tag(t));}else{game.misses++;game.event("IR-6 LOST TO FLARE");}}
  else if(m.lost>2||m.age>28||m.path>12||(m.age>.5&&m.z*1000<Game.terrain(m.x,m.y))||(m.decoy==null&&!t.alive)){m.alive=false;game.misses++;game.event("IR-6 LOST / "+game.tag(t));}
 }
}

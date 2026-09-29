package com.jb.radar;
import java.util.*;
/** Fictional passive seeker and optional IRST. A missile seeker does not require an IRST. */
public class Infrared {
 public static final double COOL_TIME=2,ACQUIRE_TIME=1.2;
 public final Game game;
 public Game.Contact target;
 /** Compatibility snapshots only. All missile families use the actual L1-L3 launchers. */
 @Deprecated public int ammo,reserve,shots;
 @Deprecated public double reload;
 public double cool,acquire,activeTime,cueX,cueY,cueZ,cueSeen=-999,searchLost;
 public boolean active,locked,hadLock;
 public double thermalContrast=1,visibility=1;
 public static class Flare {public double x,y,z,vx,vy,age;public Game.Contact source;}
 public static final class IrstObservation {
  public final int contactId;public final double bearing,elevation,seen;public final boolean rangeKnown=false;
  IrstObservation(Game.Contact t,double time,double height){contactId=t.id;seen=time;bearing=Game.angle(t.x,t.y);elevation=Math.atan2(t.alt/1000-height,t.range());}
 }
 public ArrayList<Flare> flares=new ArrayList<>();
 private final IdentityHashMap<Game.Contact,IrstObservation> passive=new IdentityHashMap<>();
 public Infrared(Game g){game=g;}
 public void setConditions(double contrast,double visibility){if(Double.isNaN(contrast)||Double.isNaN(visibility)||Double.isInfinite(contrast)||Double.isInfinite(visibility))throw new IllegalArgumentException("IR conditions must be finite");thermalContrast=Game.clamp(contrast,0,1);this.visibility=Game.clamp(visibility,0,1);}
 Equipment.Weapon weapon(){return game.weapon!=null&&game.weapon.guidance==Equipment.Guidance.INFRARED?game.weapon:game.equipment.infrared;}
 boolean equipped(){return game.weapon!=null&&game.weapon.guidance==Equipment.Guidance.INFRARED;}
 public void reset(){shots=0;ammo=equipped()?9:0;reserve=equipped()?9:0;reload=0;flares.clear();passive.clear();cancel();}
 public void syncInventory(){ammo=equipped()?game.ready():0;reserve=equipped()?game.reserve:0;reload=equipped()?game.launchers[game.chosenLauncher].reload:0;}
 public void cancel(){target=null;active=locked=hadLock=false;cool=acquire=activeTime=searchLost=0;cueSeen=-999;}
 public double lockRange(){Equipment.Weapon w=weapon();return w==null?0:Math.min(w.seekerKm,w.rangeKm);}
 double height(){return (Game.terrain(0,0)+35)/1000;}
 boolean radarCue(Game.Contact t){return t!=null&&game.detected(t)&&!game.stale(t);}
 boolean passiveCue(Game.Contact t){IrstObservation o=passive.get(t);return o!=null&&game.elapsed-o.seen<=3;}
 public String beginBlock(Game.Contact t){if(!equipped())return "EQUIP AN IR MISSILE";if(!game.running)return "MISSION NOT ACTIVE";if(!game.battery.powered(game.elapsed))return "POWER FAILURE";if(t==null||!t.alive||!radarCue(t)&&!passiveCue(t))return "FRESH SENSOR CUE REQUIRED";return launcherBlock();}
 String launcherBlock(){if(!game.battery.parts[Battery.L1+game.chosenLauncher].alive())return "LAUNCHER DISABLED";Game.Launcher l=game.launchers[game.chosenLauncher];if(l.reload>0)return "LAUNCHER RELOADING";if(l.ammo<=0)return "LAUNCHER EMPTY";return null;}
 void cue(Game.Contact t){if(radarCue(t)){double x=game.displayX(t),y=game.displayY(t),z=t.palt/1000-height(),n=Math.sqrt(x*x+y*y+z*z);cueX=x/Math.max(.001,n);cueY=y/Math.max(.001,n);cueZ=z/Math.max(.001,n);cueSeen=t.seen;}else{IrstObservation o=passive.get(t);cueX=Math.sin(o.bearing)*Math.cos(o.elevation);cueY=-Math.cos(o.bearing)*Math.cos(o.elevation);cueZ=Math.sin(o.elevation);cueSeen=o.seen;}}
 public String begin(Game.Contact t){String why=beginBlock(t);if(why!=null)return why;cancel();target=t;cue(t);active=true;game.event("IR SEEKER COOLING / "+game.tag(t));return game.message;}
 public boolean lineOfSight(double x,double y,double z,Game.Contact t){return t!=null&&clearLine(x,y,z,t.x,t.y,t.alt/1000);}
 boolean clearLine(double x,double y,double z,double tx,double ty,double tz){for(int i=1;i<24;i++){double f=i/24.0;if(Game.terrain(x+(tx-x)*f,y+(ty-y)*f)/1000>z+(tz-z)*f)return false;}return true;}
 public double heat(Game.Contact t,double x,double y,double z){return heat(weapon(),t,x,y,z);}
 double heat(Equipment.Weapon w,Game.Contact t,double x,double y,double z){if(w==null||t==null||!t.alive)return 0;double dx=t.x-x,dy=t.y-y,d=Math.sqrt(dx*dx+dy*dy+Math.pow(t.alt/1000-z,2));if(!lineOfSight(x,y,z,t))return 0;
  double rear=(1+Math.cos(Game.delta(t.heading,Game.angle(dx,dy))))/2;
  double strength=t instanceof Game.EnemyMissile?(((Game.EnemyMissile)t).age<game.equipment.enemy[t.weapon].burnSeconds?5.5:2.5):w.seekerKm*(2+rear)/3;
  return strength/Math.max(.5,d)*thermalContrast*visibility;
 }
 public double baseHeat(){return heat(target,0,0,height());}
 double cone(Equipment.Weapon w){return Math.cos(Math.toRadians(w.seekerFovDeg)*.5);}
 boolean inCue(Game.Contact t){double d=Math.sqrt(t.x*t.x+t.y*t.y+Math.pow(t.alt/1000-height(),2));return (t.x*cueX+t.y*cueY+(t.alt/1000-height())*cueZ)/Math.max(.001,d)>=cone(weapon());}
 public boolean canSee(Game.Contact t){Equipment.Weapon w=weapon();return w!=null&&t!=null&&t.alive&&Math.hypot(t.range(),t.alt/1000-height())<=lockRange()&&t.alt<=w.ceilingM&&heat(t,0,0,height())>=1&&(!active||target!=t||inCue(t));}
 /** Optional passive cue keeps the same contact ID. It deliberately exposes no measured range. */
 public IrstObservation observeIrst(Game.Contact t){if(!game.equipment.radar.infrared||!game.battery.powered(game.elapsed)||t==null||!t.alive||Math.hypot(t.range(),t.alt/1000-height())>game.equipment.radar.irLockKm||heat(t,0,0,height())<1)return null;IrstObservation o=new IrstObservation(t,game.elapsed,height());passive.put(t,o);return o;}
 public String observationRange(Game.Contact t){return radarCue(t)?String.format(Locale.US,"RADAR %.1fkm",game.displayRange(t)):"RANGE UNKNOWN";}
 boolean nearbyFlare(Game.Contact t){if(t==null||visibility<=0||thermalContrast<=0)return false;for(Flare f:flares){double z=f.z-height(),d=Math.sqrt(f.x*f.x+f.y*f.y+z*z),dot=(f.x*cueX+f.y*cueY+z*cueZ)/Math.max(.001,d);if(f.age<3&&d<=lockRange()&&dot>=cone(weapon())&&Math.hypot(f.x-t.x,f.y-t.y)<.8&&clearLine(0,0,height(),f.x,f.y,f.z))return true;}return false;}
 void followSeeker(Game.Contact t){double z=t.alt/1000-height(),n=Math.sqrt(t.x*t.x+t.y*t.y+z*z);cueX=t.x/Math.max(.001,n);cueY=t.y/Math.max(.001,n);cueZ=z/Math.max(.001,n);}
 public void tick(double dt){syncInventory();for(Iterator<Flare> i=flares.iterator();i.hasNext();){Flare f=i.next();f.age+=dt;f.x+=f.vx*dt;f.y+=f.vy*dt;f.vx*=Math.max(0,1-dt*.7);f.vy*=Math.max(0,1-dt*.7);f.z-=.015*dt;if(f.age>4||f.z*1000<Game.terrain(f.x,f.y))i.remove();}
  if(!active)return;if(target==null||!target.alive){cancel();return;}if(!game.battery.powered(game.elapsed)){cancel();game.event("IR SEEKER OFF / POWER FAILURE");return;}activeTime+=dt;if(activeTime>30){cancel();game.event("IR SEEKER TIMED OUT / REACTIVATE");return;}if(cool<COOL_TIME){cool=Math.min(COOL_TIME,cool+dt);return;}
  if(!locked&&radarCue(target)&&target.seen>cueSeen)cue(target);
  boolean was=locked;if(canSee(target)&&!nearbyFlare(target)){acquire=Math.min(ACQUIRE_TIME,acquire+dt);followSeeker(target);}else acquire=0;
  locked=acquire>=ACQUIRE_TIME;if(locked){hadLock=true;searchLost=0;}else if(hadLock){searchLost+=dt;if(searchLost>weapon().seekerSearchSeconds){cancel();game.event("IR SEEKER LOST / REACTIVATE");return;}}
  if(locked&&!was)game.event("IR SEEKER LOCK / "+game.tag(target));if(!locked&&was)game.event("IR SEEKER LOST / SEARCHING");
 }
 public String state(){if(!active)return "SEEKER OFF";if(cool<COOL_TIME)return String.format(Locale.US,"COOLING %.1fs",COOL_TIME-cool);if(locked)return "IR SEEKER LOCK / READY";if(nearbyFlare(target))return "SEARCHING / FLARE INTERFERENCE";if(!canSee(target))return hadLock?"IR SEEKER LOST / SEARCHING":"SEARCHING / WEAK OR MASKED HEAT";return String.format(Locale.US,"ACQUIRING %d%%",(int)(acquire/ACQUIRE_TIME*100));}
 public String launchBlock(Game.Contact t){if(!equipped())return "EQUIP AN IR MISSILE";if(!game.running)return "MISSION NOT ACTIVE";if(!game.battery.powered(game.elapsed))return "POWER FAILURE";String launcher=launcherBlock();if(launcher!=null)return launcher;if(!active||target!=t||!locked)return "IR SEEKER NOT LOCKED";if(!canSee(t)||nearbyFlare(t))return "IR SEEKER LOST";Equipment.Weapon w=weapon();double d=Math.hypot(t.range(),t.alt/1000-height());if(d<w.minRangeKm||d>w.rangeKm||t.alt>w.ceilingM)return "OUTSIDE ENGAGEMENT ENVELOPE";return null;}
 public String launch(Game.Contact t){String why=launchBlock(t);if(why!=null)return why;Game.Missile m=new Game.Missile();m.target=t;m.profile=weapon();m.infrared=true;m.autonomous=true;m.seekerState=Game.SeekerState.ACQUIRED;m.z=(Game.terrain(0,0)+5)/1000;remember(m,t.x,t.y,t.alt/1000);MissileMotion.aim(m,m.estimateX,m.estimateY,m.estimateZ-m.z);game.missiles.add(m);Game.Launcher l=game.launchers[game.chosenLauncher];l.ammo--;if(l.ammo==0&&game.reserve-game.reserved>=3){l.reload=90;game.reserved+=3;}shots++;game.shots++;syncInventory();game.ai.launchWarning(t,true);cancel();game.event(m.profile.name+" AWAY / SELF GUIDING");return game.message;}
 public void deployFlares(Game.Contact t){if(t instanceof Game.EnemyMissile||t.flareBursts<=0)return;t.flareBursts--;t.nextFlare=game.elapsed+5;for(int side:new int[]{-1,1}){Flare f=new Flare();f.source=t;f.x=t.x;f.y=t.y;f.z=t.alt/1000;f.vx=Math.sin(t.heading)*t.speed/7200+Math.cos(t.heading)*.08*side;f.vy=-Math.cos(t.heading)*t.speed/7200+Math.sin(t.heading)*.08*side;flares.add(f);}game.event("FLARES / "+game.tag(t));}
 void evade(Game.Missile m){Game.Contact t=m.target;if(t==null||t instanceof Game.EnemyMissile||!t.alive||game.elapsed<t.nextFlare)return;if(t.pilot!=null){game.ai.visualWarning(t,m);if(t.pilot.smart<3||!game.ai.defensive(t))return;}else return;if(Math.hypot(t.x-m.x,t.y-m.y)<3.5){t.evasion=Math.max(t.evasion,4);t.nextFlare=game.elapsed+5;if(game.random.nextDouble()<.65)deployFlares(t);}}
 void remember(Game.Missile m,double x,double y,double z){m.estimateX=x;m.estimateY=y;m.estimateZ=z;m.estimateAge=0;m.estimateValid=true;}
 public String missileState(Game.Missile m){return confirmedGuidance(m)?"IR / TARGET ACQUIRED":"IR SEEKER LOST / SEARCHING";}
 public boolean confirmedGuidance(Game.Missile m){return m!=null&&m.alive&&m.infrared&&m.seekerState==Game.SeekerState.ACQUIRED&&m.lost==0&&m.decoy==null;}
 public void fly(Game.Missile m,double dt){Equipment.Weapon w=m.profile==null?weapon():m.profile;m.age+=dt;Game.Contact t=m.target;evade(m);boolean oldAcquired=m.seekerState==Game.SeekerState.ACQUIRED;
  if(m.decoy!=null&&m.decoy.age>=4)m.decoy=null;
  if(m.decoy==null){for(Flare f:flares){double dx=f.x-m.x,dy=f.y-m.y,dz=f.z-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);double dot=(dx*m.vx+dy*m.vy+dz*m.vz)/Math.max(.001,d),strength=12/Math.max(.5,d)*thermalContrast*visibility;if(f.age<2.5&&strength>.5&&d<=w.seekerKm&&dot>=cone(w)&&clearLine(m.x,m.y,m.z,f.x,f.y,f.z)&&strength>heat(w,t,m.x,m.y,m.z)*1.2){m.decoy=f;game.event(w.name+" / SEEKER DISTRACTED");break;}}}
  double tx=m.decoy!=null?m.decoy.x:t==null?m.estimateX:t.x,ty=m.decoy!=null?m.decoy.y:t==null?m.estimateY:t.y,tz=m.decoy!=null?m.decoy.z:t==null?m.estimateZ:t.alt/1000;
  double dx=tx-m.x,dy=ty-m.y,dz=tz-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz),dot=(dx*m.vx+dy*m.vy+dz*m.vz)/Math.max(.001,d);
  boolean guided=m.decoy!=null?m.decoy.age<4&&12/Math.max(.5,d)*thermalContrast*visibility>.5&&clearLine(m.x,m.y,m.z,tx,ty,tz):t!=null&&t.alive&&heat(w,t,m.x,m.y,m.z)>.5;
  guided=guided&&d<=w.seekerKm&&dot>=cone(w)&&m.age<=w.guidanceSeconds;
  if(guided){m.lost=0;m.searchAge=0;m.seekerState=Game.SeekerState.ACQUIRED;m.autonomous=true;remember(m,tx,ty,tz);if(!oldAcquired&&m.decoy==null)game.event(w.name+" / IR TARGET REACQUIRED");}
  else{m.lost+=dt;m.searchAge+=dt;m.estimateAge+=dt;m.seekerState=Game.SeekerState.SEARCHING;m.autonomous=false;if(oldAcquired)game.event(w.name+" / IR SEEKER LOST");}
  double ox=m.x,oy=m.y,oz=m.z;MissileMotion.step(m,w,m.estimateX,m.estimateY,m.estimateZ,guided,dt);double near=MissileMotion.nearest(ox,oy,oz,m,tx,ty,tz);
  if(guided&&near<.10&&m.age>.5){m.alive=false;if(m.decoy==null&&t!=null&&t.alive)game.confirmIntercept(t,true);else{game.misses++;game.event(w.name+" LOST TO FLARE");}}
  else if(m.searchAge>w.seekerSearchSeconds||m.age>w.guidanceSeconds+w.seekerSearchSeconds||m.path>w.rangeKm||(m.age>.5&&m.z*1000<Game.terrain(m.x,m.y))){m.alive=false;game.misses++;game.event(w.name+" / IR SEEKER LOST");}
 }
}

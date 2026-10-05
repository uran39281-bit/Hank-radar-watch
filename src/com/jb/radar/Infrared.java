package com.jb.radar;
import java.util.*;
/** Fictional passive sensors. IRST measurements and launched missile seekers remain independent. */
public class Infrared {
 public static final double COOL_TIME=2,ACQUIRE_TIME=1.2;
 public final Game game;
 public Game.Contact target;
 /** Compatibility snapshots; actual ammunition belongs to the selected generic launcher. */
 @Deprecated public int ammo,reserve,shots;
 @Deprecated public double reload;
 public double cool,acquire,activeTime,cueX,cueY,cueZ,cueSeen=-999,searchLost;
 public boolean active,locked,hadLock;
 public double thermalContrast=1,visibility=1;
 public boolean irstRequested,irstEnabled;
 private double switchRemaining,updateRemaining;
 private Game.Contact pointedIrst;
 /** One short-lived combined chaff/flare cloud; both seeker types evaluate this same deployment. */
 public static class Flare {public double x,y,z,vx,vy,age,lifetime=3;public Game.Contact source;}
 public static final class IrstObservation {
  public final int contactId,samples;public final double bearing,elevation,seen,quality,firstSeen;public final boolean rangeKnown=false,acquired,stable;
  IrstObservation(Game.Contact t,double bearing,double elevation,double seen,double quality,double firstSeen,int samples,boolean acquired,boolean stable){contactId=t.id;this.bearing=bearing;this.elevation=elevation;this.seen=seen;this.quality=quality;this.firstSeen=firstSeen;this.samples=samples;this.acquired=acquired;this.stable=stable;}
  IrstObservation assignment(Game.Contact t,boolean stable){return new IrstObservation(t,bearing,elevation,seen,quality,firstSeen,samples,acquired,stable);}
 }
 static final class SeekerMemory {Game.Contact candidate;Flare flare,diversion;double acquire,diversionTime;boolean acquiredBefore;}
 public ArrayList<Flare> flares=new ArrayList<>();
 private final IdentityHashMap<Game.Contact,IrstObservation> passive=new IdentityHashMap<>();
 private final IdentityHashMap<Game.Contact,Double> attempted=new IdentityHashMap<>();
 private final WeakHashMap<Game.Missile,SeekerMemory> seekers=new WeakHashMap<>();
 public Infrared(Game g){game=g;}
 public void setConditions(double contrast,double visibility){if(!Double.isFinite(contrast)||!Double.isFinite(visibility))throw new IllegalArgumentException("IR conditions must be finite");thermalContrast=Game.clamp(contrast,0,1);this.visibility=Game.clamp(visibility,0,1);}
 Equipment.Weapon weapon(){return game.weapon!=null&&game.weapon.guidance==Equipment.Guidance.INFRARED?game.weapon:game.equipment.infrared;}
 boolean equipped(){return game.weapon!=null&&game.weapon.guidance==Equipment.Guidance.INFRARED;}
 public void reset(){shots=0;ammo=equipped()?game.launchers.length*game.equipment.radar.launcherCapacity:0;reserve=equipped()?game.equipment.radar.reserveRounds:0;reload=0;flares.clear();passive.clear();attempted.clear();seekers.clear();irstRequested=irstEnabled=false;switchRemaining=updateRemaining=0;pointedIrst=null;cancel();}
 public void syncInventory(){ammo=equipped()?game.ready():0;reserve=equipped()?game.reserve:0;reload=equipped()?game.launchers[game.chosenLauncher].reload:0;}
 public void cancel(){target=null;active=locked=hadLock=false;cool=acquire=activeTime=searchLost=0;cueSeen=-999;}
 public double lockRange(){Equipment.Weapon w=weapon();return w==null?0:Math.min(w.seekerKm,w.rangeKm);}
 double height(){return (Game.terrain(0,0)+35)/1000;}
 boolean radarCue(Game.Contact t){return t!=null&&game.detected(t)&&game.detectionAge(t)<=game.freshSeconds();}
 double radarX(Game.Contact t){return game.liveTrack(t)?game.displayX(t):game.plotX(t);}
 double radarY(Game.Contact t){return game.liveTrack(t)?game.displayY(t):game.plotY(t);}
 boolean radarAltitudeKnown(Game.Contact t){return t.samples>0&&game.elapsed-t.seen<=game.freshSeconds();}
 double radarRange(Game.Contact t){return Math.hypot(Math.hypot(radarX(t),radarY(t)),radarAltitudeKnown(t)?t.palt/1000-height():0);}
 public boolean passiveCue(Game.Contact t){return irstOnline()&&irstTracked(t)&&irstAge(t)<=game.equipment.radar.irstUpdateSeconds*1.5;}
 public boolean irstOnline(){return game.equipment.radar.infrared&&irstEnabled&&switchRemaining<=0&&game.battery.powered(game.elapsed)&&game.battery.parts[Battery.COMMAND].alive();}
 public double irstSwitchRemaining(){return switchRemaining;}
 public int irstCapacity(){return game.equipment.radar.infrared?game.equipment.radar.irstCapacity:0;}
 public int irstCount(){int count=0;for(Game.Contact t:passive.keySet())if(irstTracked(t))count++;return count;}
 public double irstAge(Game.Contact t){IrstObservation o=passive.get(t);return o==null?Double.POSITIVE_INFINITY:Math.max(0,game.elapsed-o.seen);}
 public boolean irstVisible(Game.Contact t){return t!=null&&t.alive&&passive.containsKey(t)&&irstAge(t)<game.equipment.radar.irstTrackTimeoutSeconds*1.5;}
 public boolean irstTracked(Game.Contact t){IrstObservation o=passive.get(t);return irstVisible(t)&&o.stable&&irstAge(t)<game.equipment.radar.irstTrackTimeoutSeconds;}
 public IrstObservation observation(Game.Contact t){return irstVisible(t)?passive.get(t):null;}
 public double irstQuality(Game.Contact t){IrstObservation o=observation(t);return o==null?0:o.quality*Math.max(0,1-irstAge(t)/game.equipment.radar.irstTrackTimeoutSeconds);}
 public String irstState(){if(!game.equipment.radar.infrared)return "IRST NOT FITTED";if(!game.battery.powered(game.elapsed))return "IRST NO POWER";if(switchRemaining>0)return String.format(Locale.US,"IRST %s %.1fs",irstRequested?"STARTING":"STOPPING",switchRemaining);return irstEnabled?"IRST ON "+irstCount()+"/"+irstCapacity():"IRST OFF";}
 public String toggleIrst(){if(!game.equipment.radar.infrared)return "IRST NOT FITTED";if(!game.running)return "MISSION NOT ACTIVE";irstRequested=!irstRequested;switchRemaining=game.equipment.radar.irstSwitchSeconds;if(!irstRequested)irstEnabled=false;if(switchRemaining==0){irstEnabled=irstRequested;updateRemaining=0;}game.event(irstState());return game.message;}
 public String cueIrst(Game.Contact t){if(!irstOnline())return irstState();if(!radarCue(t))return "FRESH RADAR CUE REQUIRED";pointedIrst=t;game.event("IRST CUED / "+game.tag(t)+" / DETECTION NOT GUARANTEED");return game.message;}
 public String cueRadar(Game.Contact t){if(!passiveCue(t))return "FRESH IRST CUE REQUIRED";return game.cueRadar(t);}
 public String beginBlock(Game.Contact t){if(!equipped())return "EQUIP AN IR MISSILE";if(!game.running)return "MISSION NOT ACTIVE";if(!game.battery.powered(game.elapsed))return "POWER FAILURE";if(t==null||!t.alive||!radarCue(t)&&!passiveCue(t))return "FRESH SENSOR CUE REQUIRED";return launcherBlock();}
 String launcherBlock(){if(!game.battery.parts[Battery.L1+game.chosenLauncher].alive())return "LAUNCHER DISABLED";Game.Launcher l=game.launchers[game.chosenLauncher];if(l.reload>0)return "LAUNCHER RELOADING";if(l.ammo<=0)return "LAUNCHER EMPTY";return null;}
 void cue(Game.Contact t){if(passiveCue(t)){IrstObservation o=passive.get(t);cueX=Math.sin(o.bearing)*Math.cos(o.elevation);cueY=-Math.cos(o.bearing)*Math.cos(o.elevation);cueZ=Math.sin(o.elevation);cueSeen=o.seen;}else if(radarCue(t)){double x=radarX(t),y=radarY(t),z=radarAltitudeKnown(t)?t.palt/1000-height():0,n=Math.sqrt(x*x+y*y+z*z);cueX=x/Math.max(.001,n);cueY=y/Math.max(.001,n);cueZ=z/Math.max(.001,n);cueSeen=t.seen;}}
 public String begin(Game.Contact t){String why=beginBlock(t);if(why!=null)return why;cancel();target=t;cue(t);active=true;game.event("IR SENSOR COOLING / "+game.tag(t));return game.message;}
 public boolean lineOfSight(double x,double y,double z,Game.Contact t){return t!=null&&clearLine(x,y,z,t.x,t.y,t.alt/1000);}
 boolean clearLine(double x,double y,double z,double tx,double ty,double tz){for(int i=1;i<24;i++){double f=i/24.0;if(Game.terrain(x+(tx-x)*f,y+(ty-y)*f)/1000>z+(tz-z)*f)return false;}return true;}
 public double heat(Game.Contact t,double x,double y,double z){return heat(weapon(),t,x,y,z);}
 double sourceHeat(Game.Contact t,double x,double y,double z,double sensorRange){if(t==null||!t.alive)return 0;double dx=t.x-x,dy=t.y-y,d=Math.sqrt(dx*dx+dy*dy+Math.pow(t.alt/1000-z,2));if(!lineOfSight(x,y,z,t))return 0;double rear=(1+Math.cos(Game.delta(t.heading,Game.angle(dx,dy))))/2;
  double strength=t instanceof Game.EnemyMissile&&t.weapon>=0&&t.weapon<game.equipment.enemy.length?(((Game.EnemyMissile)t).age<game.equipment.enemy[t.weapon].burnSeconds?5.5:2.5):sensorRange*(2+rear)/3;
  if(!(t instanceof Game.EnemyMissile)&&game.ai.reducingHeat(t))strength*=.8;
  return strength/Math.max(.5,d)*thermalContrast*visibility;
 }
 double heat(Equipment.Weapon w,Game.Contact t,double x,double y,double z){return w==null?0:sourceHeat(t,x,y,z,w.seekerKm);}
 public double baseHeat(){return heat(target,0,0,height());}
 double cone(Equipment.Weapon w){return Math.cos(Math.toRadians(w.seekerFovDeg)*.5);}
 boolean inCue(Game.Contact t){double d=Math.sqrt(t.x*t.x+t.y*t.y+Math.pow(t.alt/1000-height(),2));return (t.x*cueX+t.y*cueY+(t.alt/1000-height())*cueZ)/Math.max(.001,d)>=cone(weapon());}
 public boolean canSee(Game.Contact t){Equipment.Weapon w=weapon();return w!=null&&t!=null&&t.alive&&Math.hypot(t.range(),t.alt/1000-height())<=lockRange()&&t.alt<=w.ceilingM&&heat(t,0,0,height())>=1&&(!active||target!=t||inCue(t));}
 /** Physical IRST sampling; interval, LOS and heat sensitivity still apply when externally cued. */
 public IrstObservation observeIrst(Game.Contact t){if(!irstOnline()||t==null||!t.alive)return null;Double last=attempted.get(t);if(last!=null&&game.elapsed-last<game.equipment.radar.irstUpdateSeconds-.00001)return observation(t);attempted.put(t,game.elapsed);double range=game.equipment.radar.irstRangeKm,q=sourceHeat(t,0,0,height(),range);
  if(Math.hypot(t.range(),t.alt/1000-height())>range||q<game.equipment.radar.irstSensitivity)return null;
  IrstObservation old=passive.get(t);boolean continuing=old!=null&&game.elapsed-old.seen<=game.equipment.radar.irstUpdateSeconds*1.5;double first=continuing?old.firstSeen:game.elapsed;int samples=continuing?old.samples+1:1;boolean acquired=game.equipment.radar.irstAcquireSeconds==0||samples>=2&&game.elapsed-first>=game.equipment.radar.irstAcquireSeconds;
  // Only angular measurements are stored. Small contrast-dependent angular noise never creates range.
  double noise=Math.toRadians(.08)/Math.max(.25,Math.min(1,q));IrstObservation o=new IrstObservation(t,Game.angle(t.x,t.y)+game.random.nextGaussian()*noise,Math.atan2(t.alt/1000-height(),t.range())+game.random.nextGaussian()*noise,game.elapsed,Math.min(1,q),first,samples,acquired,old!=null&&old.stable&&acquired);passive.put(t,o);if(old==null)game.event("IRST CONTACT / "+game.tag(t)+" / RANGE UNKNOWN");allocateIrst();return passive.get(t);
 }
 void allocateIrst(){ArrayList<Game.Contact> eligible=new ArrayList<>();for(Game.Contact t:passive.keySet()){IrstObservation o=passive.get(t);if(t.alive&&o.acquired&&irstAge(t)<game.equipment.radar.irstTrackTimeoutSeconds)eligible.add(t);}eligible.sort((a,b)->{IrstObservation x=passive.get(a),y=passive.get(b);int n=Boolean.compare(y.stable,x.stable);if(n==0)n=Boolean.compare(b==pointedIrst,a==pointedIrst);if(n==0)n=Double.compare(y.quality,x.quality);return n!=0?n:Integer.compare(a.id,b.id);});HashSet<Game.Contact> chosen=new HashSet<>();for(int i=0;i<Math.min(irstCapacity(),eligible.size());i++)chosen.add(eligible.get(i));for(Game.Contact t:passive.keySet()){IrstObservation o=passive.get(t);boolean stable=chosen.contains(t);if(stable&&!o.stable)game.event("IRST TRACK / "+game.tag(t));if(o.stable&&!stable)game.event("IRST TRACK LOST / "+game.tag(t));if(stable!=o.stable)passive.put(t,o.assignment(t,stable));}}
 void tickIrst(double dt){if(switchRemaining>0){switchRemaining=Math.max(0,switchRemaining-dt);if(switchRemaining==0){irstEnabled=irstRequested;updateRemaining=0;game.event(irstState());}}if(irstOnline()){updateRemaining-=dt;if(updateRemaining<=0){updateRemaining=game.equipment.radar.irstUpdateSeconds;for(Game.Contact t:game.targets())observeIrst(t);}}allocateIrst();for(Iterator<Map.Entry<Game.Contact,IrstObservation>> i=passive.entrySet().iterator();i.hasNext();){Map.Entry<Game.Contact,IrstObservation> e=i.next();if(!e.getKey().alive||game.elapsed-e.getValue().seen>=game.equipment.radar.irstTrackTimeoutSeconds*1.5){attempted.remove(e.getKey());i.remove();}}}
 public String observationRange(Game.Contact t){return radarCue(t)?String.format(Locale.US,"RADAR %.1fkm",Math.hypot(radarX(t),radarY(t))):"RANGE UNKNOWN";}
 double decoyFade(Flare f){return f!=null&&f.lifetime>0?Game.clamp(1-f.age/f.lifetime,0,1):0;}
 double flareHeat(Equipment.Weapon w,Flare f,double x,double y,double z){if(w==null||f==null||!clearLine(x,y,z,f.x,f.y,f.z))return 0;double d=Math.sqrt(Math.pow(f.x-x,2)+Math.pow(f.y-y,2)+Math.pow(f.z-z,2));return 18*decoyFade(f)*(1-w.irDecoyResistance)/Math.max(.5,d)*thermalContrast*visibility;}
 boolean nearbyFlare(Game.Contact t){Equipment.Weapon w=weapon();if(t==null||w==null||visibility<=0||thermalContrast<=0)return false;for(Flare f:flares){double z=f.z-height(),d=Math.sqrt(f.x*f.x+f.y*f.y+z*z),dot=(f.x*cueX+f.y*cueY+z*cueZ)/Math.max(.001,d);if(decoyFade(f)>0&&d<=lockRange()&&dot>=cone(w)&&Math.hypot(f.x-t.x,f.y-t.y)<.8&&flareHeat(w,f,0,0,height())>heat(w,t,0,0,height())*1.1)return true;}return false;}
 void followSeeker(Game.Contact t){double z=t.alt/1000-height(),n=Math.sqrt(t.x*t.x+t.y*t.y+z*z);cueX=t.x/Math.max(.001,n);cueY=t.y/Math.max(.001,n);cueZ=z/Math.max(.001,n);}
 public void tick(double dt){if(dt<=0||!game.running)return;syncInventory();tickIrst(dt);for(Iterator<Flare> i=flares.iterator();i.hasNext();){Flare f=i.next();f.age+=dt;f.x+=f.vx*dt;f.y+=f.vy*dt;f.vx*=Math.max(0,1-dt*.7);f.vy*=Math.max(0,1-dt*.7);f.z-=.015*dt;if(f.age>=f.lifetime||f.z*1000<Game.terrain(f.x,f.y))i.remove();}
  // The pilot decides from delivered evidence. This runs once per aircraft, never once per missile.
  for(Game.Contact t:game.contacts)if(t.alive&&game.ai.shouldCountermeasure(t))emitCountermeasure(t,t.pilot.countermeasureEffectSeconds);
  if(!active)return;if(target==null||!target.alive){cancel();return;}if(!game.battery.powered(game.elapsed)){cancel();game.event("IR SENSOR OFF / POWER FAILURE");return;}activeTime+=dt;if(activeTime>30){cancel();game.event("IR SENSOR TIMED OUT / REACTIVATE");return;}if(cool<COOL_TIME){cool=Math.min(COOL_TIME,cool+dt);return;}
  if(!locked&&radarCue(target)&&target.seen>cueSeen)cue(target);boolean was=locked;if(canSee(target)&&!nearbyFlare(target)){acquire=Math.min(ACQUIRE_TIME,acquire+dt);followSeeker(target);}else acquire=0;locked=acquire>=ACQUIRE_TIME;if(locked){hadLock=true;searchLost=0;}else if(hadLock){searchLost+=dt;if(searchLost>weapon().seekerSearchSeconds){cancel();game.event("IR SENSOR LOST / REACTIVATE");return;}}
  if(locked&&!was)game.event((weapon().irLockAfterLaunch?"THERMAL CUE READY / ":"IR SEEKER LOCK / ")+game.tag(target));if(!locked&&was)game.event("IR SENSOR LOST / SEARCHING");
 }
 public String state(){if(!active)return "IR SENSOR OFF";if(cool<COOL_TIME)return String.format(Locale.US,"COOLING %.1fs",COOL_TIME-cool);if(locked)return weapon().irLockAfterLaunch?"THERMAL CUE / READY":"IR SEEKER LOCK / READY";if(nearbyFlare(target))return "SEARCHING / FLARE INTERFERENCE";if(!canSee(target))return hadLock?"IR SENSOR LOST / SEARCHING":"SEARCHING / WEAK OR MASKED HEAT";return String.format(Locale.US,"ACQUIRING %d%%",(int)(acquire/ACQUIRE_TIME*100));}
 public String launchBlock(Game.Contact t){if(!equipped())return "EQUIP AN IR MISSILE";if(!game.running)return "MISSION NOT ACTIVE";if(!game.battery.powered(game.elapsed))return "POWER FAILURE";String launcher=launcherBlock();if(launcher!=null)return launcher;if(t==null||!t.alive)return "VALID THERMAL CUE REQUIRED";Equipment.Weapon w=weapon();boolean thermal=active&&target==t&&locked&&canSee(t)&&!nearbyFlare(t);if(w.irLockAfterLaunch){if(!passiveCue(t)&&!thermal)return "VALID THERMAL CUE REQUIRED";}else if(!thermal)return "IR SEEKER NOT LOCKED";
  // A fresh radar range can constrain a cue. An IRST-only bearing never supplies hidden range or altitude.
  if(radarCue(t)){double d=radarRange(t);if(d<w.minRangeKm||d>w.rangeKm||radarAltitudeKnown(t)&&t.palt>w.ceilingM)return "OUTSIDE ENGAGEMENT ENVELOPE";}return null;}
 public String launch(Game.Contact t){String why=launchBlock(t);if(why!=null)return why;Equipment.Weapon w=weapon();boolean thermal=active&&target==t&&locked;if(passiveCue(t)||!thermal)cue(t);Game.Missile m=new Game.Missile();m.serial=++game.missileSerial;m.target=t;m.profile=w;m.infrared=true;m.z=(Game.terrain(0,0)+5)/1000;double aimDistance=radarCue(t)?radarRange(t):w.rangeKm;remember(m,cueX*aimDistance,cueY*aimDistance,m.z+cueZ*aimDistance);MissileMotion.aim(m,cueX,cueY,cueZ);m.seekerState=w.irLockAfterLaunch?Game.SeekerState.SEARCHING:Game.SeekerState.ACQUIRED;m.autonomous=!w.irLockAfterLaunch;SeekerMemory memory=new SeekerMemory();memory.acquiredBefore=!w.irLockAfterLaunch;seekers.put(m,memory);game.missiles.add(m);game.consumeRound();shots++;syncInventory();cancel();game.event(w.name+" AWAY / "+(w.irLockAfterLaunch?"IR SEARCHING":"IR SEEKER LOCK"));return game.message;}
 void emitCountermeasure(Game.Contact t,double lifetime){if(t==null||!t.alive||t instanceof Game.EnemyMissile)return;Flare f=new Flare();f.source=t;f.x=t.x;f.y=t.y;f.z=t.alt/1000;f.lifetime=Math.max(.01,lifetime);f.vx=Math.sin(t.heading)*t.speed/7200+Math.cos(t.heading)*.06*t.side;f.vy=-Math.cos(t.heading)*t.speed/7200+Math.sin(t.heading)*.06*t.side;flares.add(f);
  // A passive sensor may see the event. An old radar plot alone cannot reveal it.
  if(passiveCue(t)&&sourceHeat(t,0,0,height(),game.equipment.radar.irstRangeKm)>=game.equipment.radar.irstSensitivity)game.event("COUNTERMEASURES OBSERVED / "+game.tag(t));
 }
 /** Compatibility for scripted test contacts; fitted aircraft always use their sole pilot inventory. */
 public void deployFlares(Game.Contact t){if(t==null||!t.alive||t instanceof Game.EnemyMissile)return;if(t.pilot!=null){if(game.ai.shouldCountermeasure(t))emitCountermeasure(t,t.pilot.countermeasureEffectSeconds);return;}if(t.flareBursts<=0)return;t.flareBursts--;t.nextFlare=game.elapsed+1.5;emitCountermeasure(t,3);}
 /** Radar quality multiplier, not a forced unlock or destruction. Physical geometry is evaluated only by the seeker. */
 public double radarDecoyFactor(Game.Missile m){if(m==null||m.infrared||m.target==null||!m.target.alive)return 1;Equipment.Weapon w=m.profile==null?game.weapon:m.profile;if(w==null||w.radarDecoyResistance>=1)return 1;double tx=m.target.x-m.x,ty=m.target.y-m.y,tz=m.target.alt/1000-m.z,td=Math.sqrt(tx*tx+ty*ty+tz*tz);if(td<.001)return 1;double strongest=0;
  for(Flare f:flares){double fade=decoyFade(f);if(fade<=0||!seekerGeometry(m,w,f.x,f.y,f.z))continue;double fx=f.x-m.x,fy=f.y-m.y,fz=f.z-m.z,fd=Math.sqrt(fx*fx+fy*fy+fz*fz);if(fd<.001)continue;double separation=Math.acos(Game.clamp((tx*fx+ty*fy+tz*fz)/(td*fd),-1,1)),angular=Math.max(0,1-separation/Math.toRadians(4)),range=Math.max(0,1-Math.abs(td-fd)/.75);strongest=Math.max(strongest,fade*angular*range*(1-w.radarDecoyResistance));}
  return Math.max(.25,1-.85*strongest);
 }
 void remember(Game.Missile m,double x,double y,double z){m.estimateX=x;m.estimateY=y;m.estimateZ=z;m.estimateVx=m.estimateVy=m.estimateVz=0;m.estimateAge=0;m.estimateValid=true;}
 public String missileState(Game.Missile m){if(m.seekerState==Game.SeekerState.ACQUIRED)return m.decoy==null?"IR SEEKER LOCK":"IR SEEKER / HEAT SOURCE";return "IR SEARCHING";}
 public boolean confirmedGuidance(Game.Missile m){return m!=null&&m.alive&&m.infrared&&m.seekerState==Game.SeekerState.ACQUIRED&&m.lost==0&&m.decoy==null;}
 double sourceDistance(Game.Missile m,double x,double y,double z){return Math.sqrt(Math.pow(x-m.x,2)+Math.pow(y-m.y,2)+Math.pow(z-m.z,2));}
 boolean seekerGeometry(Game.Missile m,Equipment.Weapon w,double x,double y,double z){double d=sourceDistance(m,x,y,z);return d<=w.seekerKm&&((x-m.x)*m.vx+(y-m.y)*m.vy+(z-m.z)*m.vz)/Math.max(.001,d)>=cone(w)&&clearLine(m.x,m.y,m.z,x,y,z);}
 boolean validSource(Game.Missile m,Equipment.Weapon w,Game.Contact t){return t!=null&&t.alive&&m.age<=w.guidanceSeconds&&seekerGeometry(m,w,t.x,t.y,t.alt/1000)&&heat(w,t,m.x,m.y,m.z)>=w.seekerSensitivity;}
 boolean validFlare(Game.Missile m,Equipment.Weapon w,Flare f){return f!=null&&decoyFade(f)>0&&m.age<=w.guidanceSeconds&&seekerGeometry(m,w,f.x,f.y,f.z)&&flareHeat(w,f,m.x,m.y,m.z)>=w.seekerSensitivity;}
 void search(Game.Missile m,Equipment.Weapon w,SeekerMemory memory,double dt){Game.Contact first=null;Flare firstFlare=null;double distance=Double.POSITIVE_INFINITY;
  // Provisional simple rule: nearest currently detectable heat source is encountered first; ties use ID.
  for(Game.Contact t:game.targets())if(validSource(m,w,t)){double d=sourceDistance(m,t.x,t.y,t.alt/1000);if(d<distance||(d==distance&&first!=null&&t.id<first.id)){distance=d;first=t;}}
  for(Flare f:flares)if(validFlare(m,w,f)){double d=sourceDistance(m,f.x,f.y,f.z);if(d<distance){distance=d;first=null;firstFlare=f;}}
  if(first==null&&firstFlare==null){memory.candidate=null;memory.flare=null;memory.acquire=0;return;}if(memory.candidate!=first||memory.flare!=firstFlare){memory.candidate=first;memory.flare=firstFlare;memory.acquire=0;}memory.acquire+=dt;if(memory.acquire<w.seekerAcquireSeconds)return;
  if(first!=null)m.target=first;m.decoy=firstFlare;m.seekerState=Game.SeekerState.ACQUIRED;m.autonomous=true;m.searchAge=m.lost=0;memory.acquiredBefore=true;memory.acquire=0;game.event(w.name+" / IR SEEKER LOCK");
 }
 public void fly(Game.Missile m,double dt){Equipment.Weapon w=m.profile==null?weapon():m.profile;m.age+=dt;SeekerMemory memory=seekers.get(m);if(memory==null){memory=new SeekerMemory();memory.acquiredBefore=m.seekerState==Game.SeekerState.ACQUIRED;seekers.put(m,memory);}boolean was=m.seekerState==Game.SeekerState.ACQUIRED;
  if(was&&m.decoy==null&&validSource(m,w,m.target)){Flare strongest=null;double strength=heat(w,m.target,m.x,m.y,m.z)*1.1;for(Flare f:flares)if(validFlare(m,w,f)){double q=flareHeat(w,f,m.x,m.y,m.z);if(q>strength){strength=q;strongest=f;}}
   if(strongest==null){memory.diversion=null;memory.diversionTime=0;}else{if(memory.diversion!=strongest){memory.diversion=strongest;memory.diversionTime=0;}memory.diversionTime+=dt;if(memory.diversionTime>=.12+w.irDecoyResistance*.4){m.decoy=strongest;memory.diversion=null;memory.diversionTime=0;game.event(w.name+" / SEEKER DISTRACTED");}}}
  else{memory.diversion=null;memory.diversionTime=0;}
  boolean guided=was&&(m.decoy!=null?validFlare(m,w,m.decoy):validSource(m,w,m.target));if(!guided){if(was){m.seekerState=Game.SeekerState.SEARCHING;m.autonomous=false;memory.acquire=0;memory.candidate=null;memory.flare=null;game.event(w.name+" / IR SEEKER LOST");}m.decoy=null;m.lost+=dt;m.searchAge+=dt;m.estimateAge+=dt;search(m,w,memory,dt);guided=m.seekerState==Game.SeekerState.ACQUIRED;}
  double tx=m.estimateX,ty=m.estimateY,tz=m.estimateZ;if(guided){if(m.decoy!=null){tx=m.decoy.x;ty=m.decoy.y;tz=m.decoy.z;}else{tx=m.target.x;ty=m.target.y;tz=m.target.alt/1000;}m.lost=m.searchAge=0;remember(m,tx,ty,tz);}
  double ox=m.x,oy=m.y,oz=m.z;MissileMotion.step(m,w,m.estimateX,m.estimateY,m.estimateZ,guided,dt);double near=MissileMotion.nearest(ox,oy,oz,m,tx,ty,tz);boolean passed=(tx-m.x)*(m.x-ox)+(ty-m.y)*(m.y-oy)+(tz-m.z)*(m.z-oz)<=0;
  if(guided&&game.canDetonate(m,near,passed)&&m.age>.5){if(m.decoy==null&&m.target!=null&&m.target.alive)game.resolvePlayerHit(m,near,true);else{m.alive=false;game.misses++;game.event(w.name+" LOST TO FLARE");}}
  else if(m.searchAge>w.seekerSearchSeconds||m.age>w.guidanceSeconds+w.seekerSearchSeconds||m.path>w.rangeKm||(m.age>.5&&m.z*1000<Game.terrain(m.x,m.y))){m.alive=false;game.misses++;game.event(w.name+" / IR SELF-DESTRUCT / NO SEEKER SOLUTION");}if(!m.alive)seekers.remove(m);
 }
}

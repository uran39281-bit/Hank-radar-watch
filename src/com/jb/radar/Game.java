package com.jb.radar;
import java.util.*;
/** A fictional, deliberately simplified game simulation. Not an operational model. */
public class Game {
 public static final double TAU=Math.PI*2;
 public static final String[] NAMES={"Su-25","MiG-21bis","MiG-23M","Su-22M3","Su-17M4"};
 public static final double[] MAX_SPEED={979,2240,2358,2232,2052}, LENGTH={14.2,14.1,16.7,18.9,18.9}, SPAN={14.4,7.2,14,13.7,13.7}, CEILING={11000,16000,16000,19500,19500};
 public static final double[] CRUISE={680,1080,1280,960,940}, TURN={.12,.28,.19,.17,.17}, ACCEL={13,28,35,24,24}, BASE_ALT={850,4800,6500,2300,2200};
 public enum Allegiance { UNKNOWN, HOSTILE, FRIENDLY }
 public enum TrackStage { DETECTED, ACQUIRING, STABLE, COASTING, LOST }
 public enum SeekerState { MIDCOURSE, SEARCHING, ACQUIRED }
 public enum GuidanceLine { NONE, BATTERY_SOLID, BATTERY_DOTTED, TARGET_SOLID }
 public static class Contact {
  public Allegiance affiliation=Allegiance.UNKNOWN;public String recognizedType="";public boolean recognitionLogged;public double trackStarted=-999,detectionQuality;public boolean radarCuePending;public double health=-1,maxHealth,vulnerability=-1,detectedAt=-999,detectionX,detectionY,lockStarted=-1,lockBadSince=-1;public int detectionSamples;
  public boolean hadTrack;public long priorityOrder;public int missed;public double missedAt=-1;public PilotAI.Pilot pilot;public int id,type,samples,weapon=-1,flareBursts=3; public double nextFlare; public boolean priority,radarTracked,radarEmitting,launchObserved,weaponFired; public double born,nextPriority; public double x,y,alt,speed,heading,climb,desiredAlt,evasion,side=1,seen=-999,px,py,palt,pspeed,psize,plen,pheading,pclimb,pturn,quality,confidence,previousHeading;
  public double[] probabilities=new double[6]; public int guess;public boolean alive=true,illuminated,attacked,escaped; public String outcome="";
  public double range(){return Math.hypot(x,y);}public double measuredRange(){return Math.hypot(px,py);}
 }
 public static class Missile {public long serial;public Contact releasedIllumination;public double retargetUntil=-1,seekerAcquireTime;public SeekerState seekerState=SeekerState.MIDCOURSE;public boolean estimateValid,linkReserved,supportReleased;public double estimateX,estimateY,estimateZ,estimateVx,estimateVy,estimateVz,estimateAge,estimateSeen=-999,searchAge,supportLostFor;public boolean infrared,autonomous,datalink;public Equipment.Weapon profile;public double vx,vy,vz;public Infrared.Flare decoy;public Contact target;public double x,y,z,speed=.08,age,lost,path;public boolean alive=true;}
 public static class EnemyMissile extends Contact {public Contact source;public final Missile flight=new Missile();public double z,age,lost,pz;public EnemyMissile(){speed=.18;}}
 public static class Bomb {public Contact source;public int category;public double startX,startY,x,y,z,impactX,impactY,startZ,age,fall;public boolean alive=true,observed;}
 public static class Blast {public double x,y,radius,time,damage;public String label;}
 public ArrayList<Bomb> bombs=new ArrayList<>();public ArrayList<Blast> blasts=new ArrayList<>();public int lostAmmo;
 public ArrayList<EnemyMissile> enemyMissiles=new ArrayList<>();public int maxRange=40,enemySerial,radarHits;public double impactUntil,lastDamage;
 public static class Launcher {public int ammo=3;public double reload;}
 public Random random;public ArrayList<Contact> contacts=new ArrayList<>();public ArrayList<Missile> missiles=new ArrayList<>();public ArrayList<String> log=new ArrayList<>();public Launcher[] launchers=new Launcher[4];
 public boolean autoTrack=true,radarOn=true,radarDesired=true;public double radarSwitchAt=-1;public long missileSerial;
 public long prioritySerial;
 public double elapsed,sweep,nextSpawn;public int reserve=9,reserved=0,spawned,kills,misses,leaks,health=100,score,shots,range=40,chosenLauncher=0;public boolean running,finished,won;public String message="";
 public final Infrared ir=new Infrared(this);public boolean irMode;
 public void toggleWeapon(){event("EQUIPPED: "+weapon.name+" / CHANGE MISSILE IN LOADOUT");}
 public String weaponLock(Contact t){return irMode?ir.begin(t):illuminate(t);}
 public String weaponRelease(Contact t){if(irMode){ir.cancel();return "IR SEEKER OFF / FIRED MISSILES SELF GUIDE";}return release(t);}
 public String weaponBlock(Contact t){return irMode?ir.launchBlock(t):launchBlock(t);}
 public String fireWeapon(Contact t){return fireWeapon(t,false);}
 public String fireWeapon(Contact t,boolean confirmed){return irMode?ir.launch(t):launch(t,confirmed);}
 public String confirmFire(Contact t,Missile expectedVictim){if(!irMode&&channels()>=equipment.radar.channels&&oldestSupported()!=expectedVictim)return "GUIDANCE POOL CHANGED / REVIEW TRANSFER";return fireWeapon(t,true);}
 public String cycleWeapon(int direction){ArrayList<Equipment.Weapon> available=new ArrayList<>();for(String id:new String[]{"rampart","stonebolt","active","ir6"})if(economy.snapshot().owned.contains(id))available.add(equipment.playerWeapon(id));if(available.isEmpty())return "NO OWNED MISSILE";int n=available.indexOf(weapon);weapon=available.get((n+(direction<0?-1:1)+available.size())%available.size());irMode=weapon.guidance==Equipment.Guidance.INFRARED;ir.cancel();event("SELECTED MISSILE / "+weapon.name);return message;}
 public final Economy economy;public final Equipment equipment;public final CombatRules rules;public final Battery battery;public final PilotAI ai;public Equipment.Weapon weapon;
 public Game(long seed){this(seed,new Economy(new Economy.MemoryStore()));}
 public Game(long seed,Economy economy){this(seed,economy,Equipment.defaults());}
 public Game(long seed,Economy economy,Equipment equipment){this(seed,economy,equipment,CombatRules.defaults());}
 public Game(long seed,Economy economy,Equipment equipment,CombatRules rules){this.rules=rules;battery=new Battery(rules);ai=new PilotAI(this);this.economy=economy;this.equipment=equipment;weapon=equipment.playerWeapon(economy.snapshot().equippedMissile);irMode=weapon.guidance==Equipment.Guidance.INFRARED;maxRange=range=(int)Math.ceil(equipment.radar.detectionAbsoluteKm);random=new Random(seed);for(int i=0;i<launchers.length;i++){launchers[i]=new Launcher();launchers[i].ammo=equipment.radar.launcherCapacity;}}
 public boolean start(){if(!economy.beginMission())return false;prioritySerial=missileSerial=0;autoTrack=radarOn=radarDesired=true;radarSwitchAt=-1;weapon=equipment.playerWeapon(economy.snapshot().equippedMissile);ir.reset();ai.reset();battery.reset();lostAmmo=0;bombs.clear();blasts.clear();irMode=weapon.guidance==Equipment.Guidance.INFRARED;contacts.clear();missiles.clear();enemyMissiles.clear();maxRange=(int)Math.ceil(equipment.radar.detectionAbsoluteKm);radarHits=enemySerial=0;impactUntil=lastDamage=0;log.clear();elapsed=sweep=0;nextSpawn=48;reserve=equipment.radar.reserveRounds;reserved=spawned=kills=misses=leaks=score=shots=0;health=100;range=maxRange;chosenLauncher=0;running=true;finished=won=false;for(int i=0;i<launchers.length;i++){launchers[i]=new Launcher();launchers[i].ammo=equipment.radar.launcherCapacity;}spawn(0,34);spawn(1,46);event("MISSION 1 / DEFEND THE COMMAND SITE");return true;}
 public void event(String s){message=s;log.add(String.format(Locale.US,"%02d:%02d %s",(int)elapsed/60,(int)elapsed%60,s));if(log.size()>40)log.remove(0);}
 public boolean radarReady(){return radarOn&&battery.radarOnline(elapsed);}
 public String radarState(){if(radarSwitchAt>=0)return radarDesired?"RADAR STARTING":"RADAR STOPPING";return !radarOn?"RADAR OFF":battery.radarOnline(elapsed)?"RADAR ON":battery.warning(elapsed);}
 public String toggleRadar(){radarDesired=!radarDesired;radarSwitchAt=elapsed+equipment.radar.switchSeconds;event((radarDesired?"RADAR STARTING":"RADAR STOPPING")+" / "+String.format(Locale.US,"%.1fs",equipment.radar.switchSeconds));return message;}
 public String toggleAutoTrack(){autoTrack=!autoTrack;event(autoTrack?"AUTO TRACK ON / WAITING FOR RADAR REVISIT":"AUTO TRACK OFF / TRACKS COAST / GUIDANCE MAY BE LOST");maintainTracks();return message;}
 public String resetRadar(){for(Contact t:targets()){t.samples=t.detectionSamples=0;t.seen=t.detectedAt=t.trackStarted=-999;t.radarCuePending=false;t.missed=0;t.missedAt=-1;t.radarTracked=t.hadTrack=t.illuminated=t.priority=false;t.lockStarted=t.lockBadSince=-1;t.priorityOrder=0;t.quality=t.confidence=0;t.recognizedType="";t.recognitionLogged=false;t.affiliation=Allegiance.UNKNOWN;t.launchObserved=false;}refreshDatalinks();event("RADAR RESET / EXTERNAL GUIDANCE LOST / IRST UNCHANGED");return message;}
 public String cueRadar(Contact t){if(!radarReady())return "RADAR NOT READY";if(t==null)return "NO CUE";t.radarCuePending=true;event("RADAR CUE QUEUED / NEXT SWEEP / "+tag(t));return message;}
 void sensorTick(){if(radarSwitchAt>=0&&elapsed>=radarSwitchAt){radarOn=radarDesired;radarSwitchAt=-1;event(radarOn?"RADAR ON":"RADAR OFF / ILLUMINATION LOST");}maintainTracks();}
 public boolean lockAcquiring(Contact t){return t!=null&&t.lockStarted>=0&&!t.illuminated;}
 public double lockRemaining(Contact t){return lockAcquiring(t)?Math.max(0,equipment.radar.lockAcquireSeconds-(elapsed-t.lockStarted)):0;}
 public void consumeRound(){Launcher l=launchers[chosenLauncher];l.ammo--;shots++;int count=equipment.radar.launcherCapacity;if(l.ammo==0&&reserve-reserved>=count){l.reload=equipment.radar.reloadSeconds;reserved+=count;}}
 public double detectionAge(Contact t){return t==null?Double.POSITIVE_INFINITY:elapsed-Math.max(t.detectedAt,t.seen);}
 public double dataAge(Contact t){return t==null?Double.POSITIVE_INFINITY:tracked(t)?elapsed-trackTime(t):detected(t)?detectionAge(t):ir.irstAge(t);}
 public double trackTime(Contact t){return t.samples>0?t.seen:t.trackStarted;}
 public double plotX(Contact t){return t.detectedAt>=t.seen?t.detectionX:t.px;}
 public double plotY(Contact t){return t.detectedAt>=t.seen?t.detectionY:t.py;}
 void spawn(int type,double distance){Contact t=new Contact();t.id=++spawned;t.type=type;t.radarEmitting=type==1||type==2;t.born=elapsed;t.weapon=type==1?-1:type==0?random.nextInt(2):type==2?2:type==3?3:4;double a=random.nextDouble()*TAU;t.x=Math.sin(a)*distance;t.y=-Math.cos(a)*distance;t.heading=angle(-t.x,-t.y);t.speed=CRUISE[type]*(.85+random.nextDouble()*.2);t.desiredAlt=BASE_ALT[type]*(.5+random.nextDouble());if(type>=3&&random.nextBoolean())t.desiredAlt=130+random.nextDouble()*300;t.alt=terrain(t.x,t.y)+t.desiredAlt;t.side=random.nextBoolean()?1:-1;ai.assign(t,rules.mission1SmartLevel,type==0?2:1,new int[]{2,1,3,2,0,4}[t.id%6]);if(t.pilot.smart==1){t.desiredAlt=Math.max(1800,t.desiredAlt);t.alt=terrain(t.x,t.y)+t.desiredAlt;}t.maxHealth=t.health=rules.aircraftHealthByType[type];t.vulnerability=rules.aircraftVulnerabilityByType[type];contacts.add(t);}
 static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}static double angle(double x,double y){return Math.atan2(x,-y);}static double delta(double a,double b){return Math.atan2(Math.sin(a-b),Math.cos(a-b));}
 public static double terrain(double x,double y){double ridge1=680*Math.exp(-Math.pow((x-13)/3.5,2)-Math.pow((y+8)/15,2));double ridge2=1050*Math.exp(-Math.pow((x+19)/5,2)-Math.pow((y-11)/12,2));return 80+ridge1+ridge2;}
 /** Smooth, fictional quality falloff between nominal and absolute sensor limits. */
 public double rangeFactor(double distance,double effective,double absolute){if(distance>absolute)return 0;if(distance<=effective*.8)return 1;if(distance<=effective)return 1-.12*(distance-effective*.8)/Math.max(.1,effective*.2);return .88-.63*(distance-effective)/Math.max(.1,absolute-effective);}
 public double signal(Contact t){if(t==null||!radarReady())return 0;double r=t.range(),radarHeight=35,targetASL=t.alt;double horizon=4.12*(Math.sqrt(radarHeight)+Math.sqrt(Math.max(0,targetASL)));double absolute=equipment.radar.detectionAbsoluteKm*battery.radarFactor();if(r>absolute||r>horizon||absolute<=0)return 0;double origin=terrain(0,0)+radarHeight;for(int i=1;i<24;i++){double f=i/24.0;if(terrain(t.x*f,t.y*f)>origin+(targetASL-origin)*f)return 0;}double agl=t.alt-terrain(t.x,t.y);return (agl<150?.32:agl<500?.58:agl<1500?.82:1)*(.35+.65*battery.parts[Battery.RADAR].hp/100)*rangeFactor(r,equipment.radar.detectionKm*battery.radarFactor(),absolute);}
 public double trackingRange(){return Math.min(equipment.radar.detectionAbsoluteKm,equipment.radar.trackingAbsoluteKm)*battery.radarFactor();}
 public double lockRange(){return Math.min(equipment.radar.detectionAbsoluteKm,equipment.radar.lockAbsoluteKm)*battery.radarFactor();}
 public double freshSeconds(){return equipment.radar.freshSeconds;}
 public double trackExpiry(){return equipment.radar.trackTimeoutSeconds;}
 public double contactExpiry(){return equipment.radar.contactTimeoutSeconds;}
 public boolean detected(Contact t){return t!=null&&t.alive&&(t.samples>0||t.detectionSamples>0)&&detectionAge(t)<contactExpiry();}
 public boolean stale(Contact t){return t!=null&&(!autoTrack||!radarReady()||t.missed>0||elapsed-t.seen>freshSeconds());}
 public boolean tracked(Contact t){return detected(t)&&t.radarTracked&&elapsed-trackTime(t)<trackExpiry()&&t.measuredRange()<=trackingRange();}
 public boolean liveTrack(Contact t){return tracked(t)&&!stale(t);}
 public double trackQuality(Contact t){if(!tracked(t))return 0;double freshness=stale(t)?Math.max(0,1-(elapsed-t.seen)/trackExpiry()):1;return clamp(t.quality*rangeFactor(t.measuredRange(),equipment.radar.trackingKm*battery.radarFactor(),trackingRange())*freshness,0,1);}
 public double lockQuality(Contact t){return trackQuality(t)*rangeFactor(slant(t),equipment.radar.lockKm*battery.radarFactor(),lockRange());}
 public boolean stableTrack(Contact t){return liveTrack(t)&&t.samples>=Math.max(2,equipment.radar.acquireObservations)&&trackQuality(t)>=equipment.radar.trackQualityMinimum;}
 public boolean visible(Contact t){return t!=null&&t.alive&&((detected(t)&&Math.hypot(plotX(t),plotY(t))<=range)||ir.irstVisible(t));}
 public int trackedCount(){int n=0;for(Contact t:targets())if(tracked(t))n++;return n;}
 public boolean protectedTrack(Contact t){if(t.illuminated)return true;for(Missile m:missiles)if(m.alive&&m.target==t&&!m.infrared&&!m.supportReleased&&!m.autonomous&&(activeHoming(profile(m))?m.linkReserved:true))return true;return false;}
 boolean candidate(Contact t){return detected(t)&&Math.hypot(plotX(t),plotY(t))<=trackingRange()&&(tracked(t)||(autoTrack&&radarReady()&&detectionAge(t)<freshSeconds()));}
 int priorityRank(Contact t){return t.radarTracked&&protectedTrack(t)?0:t instanceof EnemyMissile?1:t.priority?2:equipment.radar.retainTracks&&tracked(t)?3:4;}
 int comparePriority(Contact a,Contact b){int n=Integer.compare(priorityRank(a),priorityRank(b));if(n!=0)return n;if(a.priority&&b.priority){n=Long.compare(b.priorityOrder,a.priorityOrder);if(n!=0)return n;}n=Double.compare(Math.hypot(plotX(a),plotY(a)),Math.hypot(plotX(b),plotY(b)));return n!=0?n:Integer.compare(a.id,b.id);}
 void acquireTrack(Contact t){maintainTracks();}
 void maintainTracks(){ArrayList<Contact> all=targets(),eligible=new ArrayList<>();
  for(Contact t:all){if(!detected(t)||elapsed-trackTime(t)>=trackExpiry()||t.measuredRange()>trackingRange())t.radarTracked=false;if(candidate(t))eligible.add(t);}
  eligible.sort((a,b)->comparePriority(a,b));HashSet<Contact> chosen=new HashSet<>();for(int i=0;i<Math.min(equipment.radar.tracks,eligible.size());i++)chosen.add(eligible.get(i));
  for(Contact t:all){boolean before=t.radarTracked;t.radarTracked=chosen.contains(t);if(t.radarTracked){t.hadTrack=true;if(!before){if(t.detectedAt<t.seen){t.detectedAt=t.seen;t.detectionX=t.px;t.detectionY=t.py;t.detectionSamples=t.samples;t.detectionQuality=t.quality;}t.trackStarted=elapsed;t.samples=0;t.seen=-999;t.px=plotX(t);t.py=plotY(t);t.pspeed=t.palt=t.pheading=t.pclimb=t.pturn=0;t.quality=t.detectionQuality;event("TWS TRACK ACQUIRED / "+tag(t));}}else if(before)event("TWS TRACK LOST / "+tag(t));}
  maintainLocks();refreshDatalinks();
 }
 public void maintainLocks(){for(Contact t:targets()){boolean usable=stableTrack(t)&&slant(t)<=lockRange()&&lockQuality(t)>=equipment.radar.lockQualityMinimum;
   if(lockAcquiring(t)){if(!usable||!radarReady()){t.lockStarted=-1;event("LOCK ACQUISITION LOST / "+tag(t));}else if(elapsed-t.lockStarted>=equipment.radar.lockAcquireSeconds){t.illuminated=true;t.lockStarted=-1;t.lockBadSince=-1;ai.warn(t,PilotAI.Warning.LOCK,signal(t)>0);event("LOCK ESTABLISHED / "+tag(t));retargetReleased(t);}}
   if(t.illuminated){if(usable)t.lockBadSince=-1;else if(t.lockBadSince<0)t.lockBadSince=elapsed;if(!radarReady()||!t.alive||slant(t)>lockRange()||!tracked(t)||(t.lockBadSince>=0&&elapsed-t.lockBadSince>=equipment.radar.lockLossSeconds)){t.illuminated=false;t.lockBadSince=-1;event("ILLUMINATION LOST / "+tag(t)+" / "+sarhInbound(t)+" MISSILES AFFECTED");}}
  }}
 public TrackStage trackStage(Contact t){if(t==null||!detected(t))return TrackStage.LOST;if(!tracked(t))return t.hadTrack&&(stale(t)||!radarReady())?TrackStage.LOST:TrackStage.DETECTED;if(t.samples==0)return TrackStage.ACQUIRING;if(stale(t))return TrackStage.COASTING;if(t.samples<Math.max(2,equipment.radar.acquireObservations))return TrackStage.ACQUIRING;return TrackStage.STABLE;}
 public String trackState(Contact t){if(t!=null&&ir.target==t&&ir.locked)return "IR LOCK";if(t==null)return "NO CONTACT";if(!t.alive)return t.outcome;TrackStage stage=trackStage(t);if(lockAcquiring(t))return "LOCK ACQUIRING";if(hardLocked(t))return "LOCKED";if(stage==TrackStage.STABLE&&trackQuality(t)<equipment.radar.trackQualityMinimum)return "MARGINAL";return stage.toString();}
 public boolean hardLocked(Contact t){return t!=null&&t.alive&&radarReady()&&tracked(t)&&t.illuminated&&slant(t)<=lockRange()&&(stableTrack(t)&&lockQuality(t)>=equipment.radar.lockQualityMinimum||t.lockBadSince>=0&&elapsed-t.lockBadSince<equipment.radar.lockLossSeconds);}
 /** UI predictions and midcourse guidance use recorded measurements, never hidden target motion. */
 public double predictionAge(Contact t){if(!tracked(t)||t.samples<2)return 0;return Math.min(Math.max(0,elapsed-t.seen),trackExpiry());}
 public double displayX(Contact t){if(!tracked(t)||t.samples==0)return detected(t)?plotX(t):ir.irstVisible(t)?Math.sin(ir.observation(t).bearing)*range*.82:0;return t.px+Math.sin(t.pheading)*t.pspeed/3600*predictionAge(t);}
 public double displayY(Contact t){if(!tracked(t)||t.samples==0)return detected(t)?plotY(t):ir.irstVisible(t)?-Math.cos(ir.observation(t).bearing)*range*.82:0;return t.py-Math.cos(t.pheading)*t.pspeed/3600*predictionAge(t);}
 public double displayRange(Contact t){return Math.hypot(displayX(t),displayY(t));}
 public String altitudeText(Contact t){return tracked(t)&&t.samples>0?String.format(Locale.US,"%.0f",t.palt):"---";}
 public String speedText(Contact t){return tracked(t)&&t.samples>=2?String.format(Locale.US,"%.0f",t.pspeed):"---";}
 public String directionText(Contact t){return tracked(t)&&t.samples>=2?String.format(Locale.US,"%03d",((int)Math.toDegrees(t.pheading)%360+360)%360):"---";}
 public String bearingText(Contact t){return detected(t)?String.format(Locale.US,"%03d",((int)Math.toDegrees(angle(displayX(t),displayY(t)))%360+360)%360):"---";}
 public static boolean activeHoming(Equipment.Weapon w){return w.guidance==Equipment.Guidance.RADAR&&w.radarMode==Equipment.RadarMode.ACTIVE;}
 Equipment.Weapon profile(Missile m){return m.profile==null?weapon:m.profile;}
 boolean canUpdate(Contact t){return autoTrack&&radarReady()&&stableTrack(t)&&slant(t)<=trackingRange()&&signal(t)>.15;}
 /** Shared game resource: one channel per externally supported radar missile. */
 LinkedHashSet<Contact> refreshDatalinks(){LinkedHashSet<Contact> occupied=new LinkedHashSet<>();for(Missile m:missiles){Equipment.Weapon w=profile(m);if(!m.alive||m.infrared||m.autonomous||m.age>w.guidanceSeconds||activeHoming(w)&&m.seekerState!=SeekerState.MIDCOURSE){m.linkReserved=m.datalink=false;continue;}m.datalink=m.linkReserved&&!m.supportReleased&&(activeHoming(w)?canUpdate(m.target):hardLocked(m.target));if(m.linkReserved&&!m.supportReleased)occupied.add(m.target);}return occupied;}
 public int illuminationUsed(){int n=0;for(Contact t:targets())if(hardLocked(t)||lockAcquiring(t))n++;return n;}
 public int midcourseUsed(){refreshDatalinks();int n=0;for(Missile m:missiles)if(m.alive&&m.linkReserved&&!m.supportReleased&&activeHoming(profile(m)))n++;return n;}
 public int supportedMissilesUsed(){refreshDatalinks();int n=0;for(Missile m:missiles)if(m.alive&&m.linkReserved&&!m.supportReleased&&!activeHoming(profile(m)))n++;return n;}
 public int channels(){return midcourseUsed()+supportedMissilesUsed();}
 public boolean channelAvailable(Contact t){return tracked(t);}
 public GuidanceLine guidanceLine(Missile m){if(!m.alive||m.infrared)return GuidanceLine.NONE;if(activeHoming(profile(m)))return m.autonomous&&m.seekerState==SeekerState.ACQUIRED&&m.lost==0?GuidanceLine.TARGET_SOLID:m.datalink?GuidanceLine.BATTERY_DOTTED:GuidanceLine.NONE;return m.linkReserved&&!m.supportReleased&&hardLocked(m.target)&&m.lost==0?GuidanceLine.BATTERY_SOLID:GuidanceLine.NONE;}
 public String missileState(Missile m){if(!m.alive)return "ENGAGEMENT ENDED";if(m.infrared)return ir.missileState(m);if(activeHoming(profile(m))){refreshDatalinks();return m.autonomous?"SEEKER LOCK":m.seekerState==SeekerState.SEARCHING?"SEARCHING":m.datalink?"MIDCOURSE":"MIDCOURSE / SUPPORT LOST";}return m.linkReserved&&!m.supportReleased&&hardLocked(m.target)&&m.lost==0?"S-A / ILLUMINATED":"SUPPORT LOST";}
 public Missile earliestReleasableSupport(Contact t){refreshDatalinks();for(Missile m:missiles)if(m.alive&&m.target==t&&m.linkReserved&&!m.supportReleased)return m;return null;}
 public Missile oldestSupported(){refreshDatalinks();for(Missile m:missiles)if(m.alive&&m.linkReserved&&!m.supportReleased)return m;return null;}
 public String releaseSupport(Missile m){if(m==null||!m.alive||!m.linkReserved)return "NO BATTERY SUPPORT TO RELEASE";m.supportReleased=true;m.linkReserved=m.datalink=false;if(activeHoming(profile(m)))startSeekerSearch(m);event("GUIDANCE CHANNEL RELEASED / M-"+m.serial+" / "+tag(m.target));return message;}
 public String launchWarning(Contact t){if(irMode||launchBlock(t)!=null||channels()<equipment.radar.channels)return null;Missile old=oldestSupported();return old==null?null:"TRANSFER M-"+old.serial+" CHANNEL? "+(activeHoming(profile(old))?"OLD MISSILE WILL SEARCH EARLY":"OLD MISSILE WILL LOSE GUIDANCE");}
 public static boolean needsGroundLink(Equipment.Weapon w){return w.guidance==Equipment.Guidance.COMMAND||w.guidance==Equipment.Guidance.LASER||w.guidance==Equipment.Guidance.RADAR&&w.radarMode!=Equipment.RadarMode.PASSIVE;}
 public double slant(Contact t){return t==null?Double.POSITIVE_INFINITY:Math.hypot(t.measuredRange(),(t.palt-terrain(0,0))/1000);}
 public String lockBlock(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!radarReady())return radarState();if(!tracked(t)||!stableTrack(t))return "TRACK TOO WEAK";if(slant(t)>lockRange())return "OUTSIDE RADAR LOCK RANGE";if(lockQuality(t)<equipment.radar.lockQualityMinimum)return "TRACK TOO WEAK / MARGINAL";if(t.illuminated)return "TARGET ALREADY LOCKED";if(lockAcquiring(t))return "LOCK ACQUIRING";return null;}
 public int inbound(Contact t){int n=0;for(Missile m:missiles)if(m.alive&&m.target==t)n++;return n;}
 public int sarhInbound(Contact t){int n=0;for(Missile m:missiles)if(m.alive&&m.target==t&&!m.infrared&&!activeHoming(profile(m)))n++;return n;}
 public String illuminate(Contact t){String block=lockBlock(t);if(block!=null)return block;t.lockStarted=elapsed;event("LOCK ACQUIRING / "+tag(t));maintainLocks();return message;}
 public String release(Contact t){if(t==null||(!t.illuminated&&!lockAcquiring(t)))return "NO ILLUMINATION TO RELEASE";t.illuminated=false;t.lockStarted=t.lockBadSince=-1;for(Missile m:missiles)if(m.alive&&m.target==t&&!m.infrared&&!activeHoming(profile(m))&&m.linkReserved&&!m.supportReleased){m.releasedIllumination=t;m.retargetUntil=elapsed+profile(m).supportRecoverySeconds;}event("LOCK RELEASED / "+tag(t)+" / "+sarhInbound(t)+" MISSILES AFFECTED");return message;}
 void retargetReleased(Contact t){for(Missile m:missiles)if(m.alive&&!m.infrared&&!activeHoming(profile(m))&&m.releasedIllumination!=null&&!m.supportReleased&&m.linkReserved&&elapsed<=m.retargetUntil&&m.supportLostFor<=profile(m).supportRecoverySeconds){Equipment.Weapon w=profile(m);Contact old=m.target;m.target=t;boolean allowed=t==old||w.retargeting&&seekerCanSee(m)&&Math.hypot(t.x-m.x,t.y-m.y)<=Math.max(0,w.rangeKm-m.path)&&m.age<w.guidanceSeconds;if(allowed){m.releasedIllumination=null;m.retargetUntil=-1;m.supportLostFor=m.lost=0;cacheSeekerEstimate(m);event("S-A RETARGET / M-"+m.serial+" / "+tag(old)+" TO "+tag(t));}else m.target=old;}}
 public String launchBlock(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!battery.powered(elapsed))return "POWER FAILURE";if(!battery.parts[Battery.L1+chosenLauncher].alive())return "LAUNCHER DISABLED";if(!radarReady())return radarState();if(!stableTrack(t))return "TRACK TOO WEAK";
  Equipment.Weapon w=weapon;boolean active=activeHoming(w);if(!active&&!hardLocked(t))return "RADAR LOCK REQUIRED";double slant=slant(t);
  if(slant>Math.min(active?trackingRange():lockRange(),w.rangeKm)||slant<w.minRangeKm)return "OUTSIDE ENGAGEMENT ENVELOPE";if(t.palt>w.ceilingM)return "ABOVE MISSILE CEILING";
  if(equipment.radar.channels<=0)return "NO GUIDANCE CHANNELS";
  Launcher l=launchers[chosenLauncher];if(l.reload>0)return "LAUNCHER RELOADING";if(l.ammo==0)return "LAUNCHER EMPTY";return null;
 }
 void cacheEstimate(Missile m){Contact t=m.target;m.estimateX=displayX(t);m.estimateY=displayY(t);m.estimateZ=Math.max(0,t.palt/1000+t.pclimb/1000*predictionAge(t));m.estimateVx=Math.sin(t.pheading)*t.pspeed/3600;m.estimateVy=-Math.cos(t.pheading)*t.pspeed/3600;m.estimateVz=t.pclimb/1000;m.estimateAge=0;m.estimateSeen=t.seen;m.estimateValid=true;}
 public String launch(Contact t){return launch(t,false);}
 public String launch(Contact t,boolean confirmed){String block=launchBlock(t);if(block!=null)return block;String warning=launchWarning(t);if(warning!=null&&!confirmed)return warning;if(warning!=null){Missile old=oldestSupported();releaseSupport(old);event("CHANNEL TRANSFER / M-"+old.serial+" TO NEXT MISSILE");}consumeRound();Missile m=new Missile();m.serial=++missileSerial;m.target=t;m.profile=weapon;m.z=(terrain(0,0)+5)/1000;cacheEstimate(m);MissileMotion.aim(m,m.estimateX,m.estimateY,m.estimateZ-m.z);m.linkReserved=true;missiles.add(m);refreshDatalinks();ai.launchWarning(t,false);event("MISSILE AWAY / M-"+m.serial+" / "+tag(t));return message;}
 public void observe(Contact t){if(!radarReady())return;ai.radarPresence(t);if(elapsed-Math.max(t.detectedAt,t.seen)<battery.processingDelay())return;double q=signal(t);
  if(q==0||random.nextDouble()>q){if(autoTrack){t.missed++;if(t.missedAt<0)t.missedAt=elapsed;}return;}
  double r=t.range(),error=(.02+r/160)*(2-q)*1.2,dx=t.x+random.nextGaussian()*error*.45,dy=t.y+random.nextGaussian()*error*.45;boolean first=!detected(t);t.detectedAt=elapsed;t.detectionX=dx;t.detectionY=dy;t.detectionQuality=q;t.detectionSamples++;t.radarCuePending=false;if(first)event("RADAR DETECTION / "+tag(t));
  if(!autoTrack)return;maintainTracks();if(!t.radarTracked)return;double previousSeen=t.seen,previousBearing=t.pheading,previousX=t.px,previousY=t.py,previousAlt=t.palt;if(elapsed-t.seen>=trackExpiry())t.samples=0;t.seen=elapsed;t.missed=0;t.missedAt=-1;t.samples++;t.px=dx;t.py=dy;
  t.palt=Math.max(20,t.alt+random.nextGaussian()*error*300);t.quality=q;
  if(t.samples>=2){double interval=Math.max(.1,elapsed-previousSeen);t.pspeed=clamp(Math.hypot(t.px-previousX,t.py-previousY)/interval*3600,0,4200);t.pheading=angle(t.px-previousX,t.py-previousY);t.pclimb=clamp((t.palt-previousAlt)/interval,-100,100);t.pturn=Math.abs(delta(t.pheading,previousBearing))/Math.max(1,interval);}else{t.pspeed=t.pheading=t.pclimb=t.pturn=0;}
  if(!(t instanceof EnemyMissile)){double span=SPAN[t.type];if(t.type==2)span=14-6.2*clamp((t.speed-650)/650,0,1);else if(t.type>=3)span=13.7-3.7*clamp((t.speed-600)/700,0,1);double aspect=Math.abs(Math.sin(t.heading-angle(-t.x,-t.y)));t.psize=Math.max(3,span*(.82+.18*aspect)+random.nextGaussian()*error*8);t.plen=Math.max(8,LENGTH[t.type]+random.nextGaussian()*error*5);identify(t);}else identifyMissile((EnemyMissile)t);
  acquireTrack(t);
 }
 public void identify(Contact t){double r=t.measuredRange(),noise=1+r/28,history=Math.min(1,t.samples/12.0);double[] weights=new double[5];double sum=0;for(int i=0;i<5;i++){double speedFit=Math.exp(-Math.pow((t.pspeed-CRUISE[i])/(250*noise),2));if(t.pspeed>MAX_SPEED[i]*1.12)speedFit*=.01;double span=SPAN[i];if(i==2)span=14-6.2*clamp((t.pspeed-650)/650,0,1);else if(i>=3)span=13.7-3.7*clamp((t.pspeed-600)/700,0,1);double sizeFit=Math.exp(-Math.pow((t.psize-span*.9)/(2.2*noise),2)-Math.pow((t.plen-LENGTH[i])/(2*noise),2));double altFit=Math.exp(-Math.abs(t.palt-BASE_ALT[i])/(3500*noise));if(t.palt>CEILING[i])altFit*=.1;double behavior=Math.exp(-Math.abs(t.pclimb)/(i==0?22:60))*Math.exp(-Math.max(0,t.pturn-TURN[i])/(.04*noise));double match=.30*speedFit+.25*sizeFit+.20*altFit+.15*behavior+.10*history;weights[i]=Math.exp(match*12);sum+=weights[i];}
  double quality=(.4+.6*(1-clamp(r/90,0,1)))*(.55+.45*t.quality)*(.65+.35*history);quality*=1-Math.min(.25,t.pturn*1.5);int top=0;for(int i=0;i<5;i++){t.probabilities[i]=weights[i]/sum*quality;if(t.probabilities[i]>t.probabilities[top])top=i;}t.probabilities[5]=1-quality;t.guess=top;t.confidence=t.probabilities[top];if(t.samples>=3&&t.confidence>=.75&&nctrCovers(NAMES[top],false)){t.recognizedType=NAMES[top];if(!t.recognitionLogged){t.recognitionLogged=true;event("NCTR RECOGNIZED / "+tag(t)+" / "+t.recognizedType);}}
 }
 void identifyMissile(EnemyMissile t){if(t.samples<3)return;int best=0;double difference=Double.POSITIVE_INFINITY;for(int i=0;i<equipment.enemy.length;i++){double d=Math.abs(t.pspeed-equipment.enemy[i].maxSpeedKmh);if(d<difference){difference=d;best=i;}}t.confidence=clamp((1-difference/1800)*t.quality*Math.min(1,t.samples/6.0),0,.95);if(t.confidence>=.75&&nctrCovers(equipment.enemy[best].name,true)){t.recognizedType=equipment.enemy[best].name;if(!t.recognitionLogged){t.recognitionLogged=true;event("NCTR RECOGNIZED / "+tag(t)+" / "+t.recognizedType);}}}
 public boolean nctrCovers(String name,boolean missile){String memory=equipment.radar.nctrMemory.trim().toUpperCase(Locale.US);if(memory.equals("ALL"))return true;if(memory.equals("NONE"))return false;if(memory.equals("CAMPAIGN AIRCRAFT"))return !missile;for(String entry:memory.split(","))if(entry.trim().equals(name.toUpperCase(Locale.US)))return true;return false;}
 public boolean recognized(Contact t){return t!=null&&!t.recognizedType.isEmpty();}
 public Allegiance allegiance(Contact t){return t==null?Allegiance.UNKNOWN:t.affiliation!=Allegiance.UNKNOWN?t.affiliation:t.launchObserved?Allegiance.HOSTILE:Allegiance.UNKNOWN;}
 public void identifyAllegiance(Contact t,Allegiance value,String evidence){if(t!=null&&t.affiliation!=value){t.affiliation=value;event("IDENTIFICATION / "+tag(t)+" / "+value+" / "+evidence);}}
 public String identification(Contact t){if(t==null)return "UNKNOWN";return allegiance(t)+" / "+(recognized(t)?t.recognizedType:"NCTR UNRECOGNIZED");}
 public void tick(double dt){if(!running||finished)return;dt=clamp(dt,0,.1);if(dt==0)return;elapsed+=dt;sensorTick();double adv=radarReady()?dt*TAU/equipment.radar.sweepSeconds():0;sweep=(sweep+adv)%TAU;
  for(int li=0;li<launchers.length;li++){Launcher l=launchers[li];if(l.reload>0){l.reload=Math.max(0,l.reload-dt*battery.reloadRate(li,elapsed));if(l.reload==0){reserve-=equipment.radar.launcherCapacity;reserved-=equipment.radar.launcherCapacity;l.ammo=equipment.radar.launcherCapacity;event("LAUNCHER RELOAD COMPLETE");}}}
  if(spawned<12&&elapsed>=nextSpawn){int count=spawned<5?1:2;for(int i=0;i<count&&spawned<12;i++)spawn((spawned+1)%5,40+random.nextDouble()*18);nextSpawn=elapsed+48;}
  for(Contact t:contacts)if(t.alive){ai.tick(t,dt);double oldHeading=t.heading;boolean guiding=guiding(t)&&!ai.defensive(t);double desired=ai.heading(t);boolean defensive=t.evasion>0||ai.defensive(t);if(t.pilot==null&&t.evasion>0){t.evasion=Math.max(0,t.evasion-dt);desired+=t.side*(t.type==1?1.7:t.type==2?1.0:1.35);}
   double turnLimit=Math.min(guiding?.035:TURN[t.type],(t.type==0?5:8)*9.81/(t.speed/3.6));double turn=clamp(delta(desired,t.heading),-turnLimit*dt,turnLimit*dt);t.heading+=turn;double hard=Math.abs(turn)/dt;double desiredSpeed=CRUISE[t.type]*(defensive&&t.type==2?1.25:1);double acceleration=clamp(desiredSpeed-t.speed,-25,ACCEL[t.type]);t.speed=clamp(t.speed+(acceleration-hard*(t.type==1?175:100))*dt,260,Math.min(MAX_SPEED[t.type],t.alt<1500?1250:MAX_SPEED[t.type]));
   t.x+=Math.sin(t.heading)*t.speed/3600*dt;t.y-=Math.cos(t.heading)*t.speed/3600*dt;double ground=terrain(t.x,t.y),agl=defensive&&(t.pilot==null||t.pilot.smart>=3)&&(t.type==0||t.type>=3)?90:t.desiredAlt;t.climb=clamp((ground+agl-t.alt)*.12,-(t.type==0?22:60),t.type==0?16:45);t.alt=clamp(t.alt+t.climb*dt,ground+40,CEILING[t.type]);t.previousHeading=oldHeading;
   double bearing=(angle(t.x,t.y)+TAU)%TAU;if((sweep-bearing+TAU)%TAU<adv)observe(t);tryEnemyLaunch(t);releaseBombs(t);
   if((t.pilot!=null&&(t.pilot.state==PilotAI.State.EXIT||t.pilot.state==PilotAI.State.RETREAT)||t.pilot==null&&t.weaponFired)&&t.range()>60){ai.exited(t);t.alive=false;t.illuminated=false;t.escaped=true;t.outcome="WITHDREW";}
   if(t.pilot==null&&t.range()<2&&!t.attacked){t.attacked=true;leaks++;score-=100;t.alive=false;t.illuminated=false;t.outcome="REACHED SITE";event("AIRCRAFT PASSED THE SITE / TRACK "+t.id);}
  }
  maintainTracks();ir.tick(dt);for(Missile m:missiles)if(m.alive)fly(m,dt);for(EnemyMissile m:enemyMissiles)if(m.alive&&!finished)flyEnemy(m,dt);for(Bomb b:bombs)if(b.alive&&!finished)flyBomb(b,dt);for(Iterator<Blast> it=blasts.iterator();it.hasNext();)if(elapsed-it.next().time>3)it.remove();
  maintainTracks();if(!battery.parts[Battery.COMMAND].alive())finish(false);else if(spawned==12&&aliveCount()==0&&incomingCount()==0&&bombCount()==0)finish(true);else if(elapsed>=600&&incomingCount()==0&&bombCount()==0)finish(true);
 }
 void finish(boolean victory){if(finished)return;economy.finish(victory,integrityUnits());finished=true;running=false;won=victory;event(victory?"MISSION COMPLETE / SITE SURVIVED":"MISSION FAILED / COMMAND UNIT DESTROYED");}
 public int aliveCount(){int n=0;for(Contact t:contacts)if(t.alive)n++;return n;}
 public int ready(){int n=0;for(Launcher l:launchers)n+=l.ammo;return n;}
 /** Assigned-target acquisition only: range, field of view, obstruction and target motion all matter. */
 public boolean seekerCanSee(Missile m){Contact t=m.target;Equipment.Weapon w=profile(m);if(t==null||!t.alive||ai.notchBreak(t))return false;double dx=t.x-m.x,dy=t.y-m.y,dz=t.alt/1000-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);if(d>w.seekerKm||d<1e-8||!ir.lineOfSight(m.x,m.y,m.z,t))return false;double n=Math.sqrt(m.vx*m.vx+m.vy*m.vy+m.vz*m.vz);if(n<1e-9)return false;double dot=clamp((dx*m.vx+dy*m.vy+dz*m.vz)/(d*n),-1,1);return Math.acos(dot)<=Math.toRadians(w.seekerFovDeg*.5);}
 double estimateX(Missile m){return m.estimateX+m.estimateVx*m.estimateAge;}
 double estimateY(Missile m){return m.estimateY+m.estimateVy*m.estimateAge;}
 double estimateZ(Missile m){return Math.max(.02,m.estimateZ+m.estimateVz*m.estimateAge);}
 void cacheSeekerEstimate(Missile m){Contact t=m.target;m.estimateX=t.x;m.estimateY=t.y;m.estimateZ=t.alt/1000;if(t instanceof EnemyMissile){EnemyMissile e=(EnemyMissile)t;m.estimateVx=e.flight.vx*e.speed;m.estimateVy=e.flight.vy*e.speed;m.estimateVz=e.flight.vz*e.speed;}else{m.estimateVx=Math.sin(t.heading)*t.speed/3600;m.estimateVy=-Math.cos(t.heading)*t.speed/3600;m.estimateVz=t.climb/1000;}m.estimateAge=0;m.estimateValid=true;}
 void startSeekerSearch(Missile m){if(m.seekerState==SeekerState.SEARCHING)return;m.seekerState=SeekerState.SEARCHING;m.searchAge=0;m.seekerAcquireTime=0;m.autonomous=false;m.linkReserved=m.datalink=false;ai.activeSeekerWarning(m.target,m);event(profile(m).name+" / SEARCHING / "+tag(m.target));}
 public boolean guided(Missile m){Contact t=m.target;Equipment.Weapon w=profile(m);if(!m.alive||t==null||!t.alive||m.age>w.guidanceSeconds)return false;
  if(activeHoming(w)){
   refreshDatalinks();if(!m.estimateValid&&detected(t))cacheEstimate(m);
   if(m.datalink&&m.estimateSeen!=t.seen)cacheEstimate(m);
   double dx=estimateX(m)-m.x,dy=estimateY(m)-m.y,dz=estimateZ(m)-m.z,predictedDistance=Math.sqrt(dx*dx+dy*dy+dz*dz);
   if(m.seekerState==SeekerState.MIDCOURSE&&(predictedDistance<=w.seekerKm||(!m.datalink)))startSeekerSearch(m);
   if(m.seekerState==SeekerState.ACQUIRED&&!seekerCanSee(m)){m.autonomous=false;startSeekerSearch(m);event(w.name+" / SEEKER LOST / SEARCHING");}
   if(m.seekerState==SeekerState.SEARCHING&&m.seekerAcquireTime>=w.seekerAcquireSeconds&&seekerCanSee(m)){m.seekerState=SeekerState.ACQUIRED;m.autonomous=true;m.linkReserved=m.datalink=false;m.searchAge=0;event(w.name+" / ACTIVE - TARGET ACQUIRED");}
   if(m.autonomous){cacheSeekerEstimate(m);return true;}
   return m.seekerState==SeekerState.MIDCOURSE&&m.datalink;
  }
  if(w.radarMode==Equipment.RadarMode.PASSIVE)return t.radarEmitting&&ir.lineOfSight(m.x,m.y,m.z,t);
  return m.linkReserved&&!m.supportReleased&&m.supportLostFor<=w.supportRecoverySeconds&&hardLocked(t)&&signal(t)>.15&&!ai.notchBreak(t)&&ir.lineOfSight(m.x,m.y,m.z,t);
 }
 public void fly(Missile m,double dt){if(!m.alive||dt<=0)return;if(m.infrared){ir.fly(m,dt);return;}Contact t=m.target;Equipment.Weapon w=profile(m);m.age+=dt;m.estimateAge+=dt;boolean guided=guided(m);boolean active=activeHoming(w);if(active&&m.seekerState==SeekerState.SEARCHING){if(seekerCanSee(m))m.seekerAcquireTime+=dt;else m.seekerAcquireTime=0;if(m.seekerAcquireTime>=w.seekerAcquireSeconds)guided=guided(m);}
  if(guided){m.lost=0;if(!active)cacheSeekerEstimate(m);}else m.lost+=dt;
  boolean supportAvailable=active?(m.datalink||m.autonomous):guided;if(supportAvailable)m.supportLostFor=0;else m.supportLostFor+=dt;
  if(active&&m.seekerState==SeekerState.SEARCHING)m.searchAge+=dt;
  if(active&&m.seekerState==SeekerState.MIDCOURSE&&!m.datalink)startSeekerSearch(m);
  // All unsupported flight retains a finite cached intercept estimate. No hidden target steering.
  double tx=estimateX(m),ty=estimateY(m),tz=estimateZ(m),ox=m.x,oy=m.y,oz=m.z;
  if(guided&&(!active||m.autonomous)){tx=t.x;ty=t.y;tz=t.alt/1000;}else if(m.estimateValid){double distance=Math.sqrt(Math.pow(tx-m.x,2)+Math.pow(ty-m.y,2)+Math.pow(tz-m.z,2)),lead=clamp(distance/Math.max(.25,m.speed),0,12);tx+=m.estimateVx*lead;ty+=m.estimateVy*lead;tz=Math.max(.02,tz+m.estimateVz*lead);}
  MissileMotion.step(m,w,tx,ty,tz,m.estimateValid,dt);ai.visualWarning(t,m);
  double nearest=t==null?Double.POSITIVE_INFINITY:MissileMotion.nearest(ox,oy,oz,m,t.x,t.y,t.alt/1000);
  // Collision uses world truth; sensor displays and midcourse estimates never receive it.
  if(t!=null&&t.alive&&guided&&(!active||m.autonomous)&&m.age>1&&canDetonate(m,nearest,passedClosest(ox,oy,oz,m,t)))resolvePlayerHit(m,nearest,false);
  else if((!active&&m.supportLostFor>w.supportRecoverySeconds)||(active&&m.seekerState==SeekerState.SEARCHING&&m.searchAge>w.seekerSearchSeconds)||m.age>w.guidanceSeconds||m.path>w.rangeKm||(m.age>1&&m.z*1000<terrain(m.x,m.y))||t==null||!t.alive){m.alive=false;m.linkReserved=m.datalink=false;misses++;event(w.name+" FAILED / "+(t==null?"NO TARGET":tag(t)));}
 }
 /** A near pass is a graze only after closest approach; never detonate early on the way to a direct hit. */
 boolean passedClosest(double ox,double oy,double oz,Missile m,Contact t){double before=Math.pow(t.x-ox,2)+Math.pow(t.y-oy,2)+Math.pow(t.alt/1000-oz,2);double after=Math.pow(t.x-m.x,2)+Math.pow(t.y-m.y,2)+Math.pow(t.alt/1000-m.z,2);return after>before+1e-12;}
 public void confirmIntercept(Contact t,boolean infrared){if(!t.alive||finished)return;economy.intercept(tag(t),t instanceof EnemyMissile);ai.destroyed(t);t.alive=false;t.illuminated=false;t.outcome=infrared?"IR INTERCEPT":"INTERCEPTED";kills++;score+=250;maintainTracks();event((infrared?"IR INTERCEPT / ":"INTERCEPT / ")+tag(t));}
 public String prioritize(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!radarReady())return radarState();if(!visible(t))return "SELECT A DETECTED CONTACT";if(t.priority){t.priority=false;t.priorityOrder=0;maintainTracks();event("AUTOMATIC PRIORITY / "+tag(t));return message;}if(!detected(t))return "RADAR DETECTION REQUIRED";if(Math.hypot(plotX(t),plotY(t))>trackingRange())return "OUTSIDE TRACKING RANGE";if(!tracked(t)&&detectionAge(t)>freshSeconds())return "CONTACT STALE / WAIT FOR RADAR";
  maintainTracks();if(!tracked(t)&&trackedCount()>=equipment.radar.tracks){boolean replaceable=false;for(Contact other:targets())if(tracked(other)&&!protectedTrack(other))replaceable=true;if(!replaceable)return "NO FREE TRACK SLOT";}
  t.priority=true;t.priorityOrder=++prioritySerial;maintainTracks();event("PLAYER PRIORITY / "+tag(t));return message;
 }
 /** Legacy harness alias. UI uses PRIORITIZE; tracking itself is automatic. */
 public String track(Contact t){return prioritize(t);}
 public double rangeQuality(){return 1;}
 public int[] presets(){ArrayList<Integer> a=new ArrayList<>();for(int n:new int[]{10,20,30,40})if(n<=maxRange)a.add(n);if(maxRange>0&&!a.contains(maxRange))a.add(maxRange);int[] r=new int[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}
 public void cycleRange(){int[] a=presets();if(a.length==0){range=1;return;}int next=0;for(int i=0;i<a.length;i++)if(a[i]==range)next=(i+1)%a.length;range=a[next];}
 public ArrayList<Contact> targets(){ArrayList<Contact> a=new ArrayList<>(contacts);a.addAll(enemyMissiles);return a;}
 public int integrityUnits(){return (int)Math.ceil(battery.condition()/25);}
 @Deprecated public int hitsRemaining(){return integrityUnits();}
 public int identity(Contact t){Allegiance a=allegiance(t);return a==Allegiance.FRIENDLY?3:a==Allegiance.HOSTILE?(recognized(t)?2:1):0;}
 public String identityLabel(Contact t){return allegiance(t)+" / "+(recognized(t)?"TYPE RECOGNIZED":"TYPE UNRECOGNIZED");}
 public String contactName(Contact t){return recognized(t)?t.recognizedType:"UNKNOWN";}
 public String tag(Contact t){return "T-"+t.id;}
 public Contact nextTarget(Contact current){ArrayList<Contact> a=new ArrayList<>();for(Contact t:targets())if(visible(t))a.add(t);a.sort((x,y)->Integer.compare(x.id,y.id));return a.isEmpty()?null:a.get((a.indexOf(current)+1)%a.size());}
 public String payload(Contact t){return t.launchObserved?"WEAPON LAUNCH OBSERVED":t.confidence>.65&&t.guess!=1?"POSSIBLE A/G WEAPONS":"UNKNOWN PAYLOAD";}
 public String threat(Contact t){if(!tracked(t))return "UNCERTAIN";double approach=Math.cos(delta(t.pheading,angle(-t.px,-t.py)));return t.launchObserved||t.confidence>.6&&t.guess>=3?"HIGH":approach>.5&&t.measuredRange()<20?"ELEVATED":"UNCERTAIN";}
 boolean guiding(Contact t){for(EnemyMissile m:enemyMissiles)if(m.alive&&m.source==t&&needsGroundLink(equipment.enemy[m.weapon]))return true;return false;}
 boolean sourceSiteVisible(Contact t){return t!=null&&ir.clearLine(t.x,t.y,t.alt/1000,0,0,(terrain(0,0)+20)/1000);}
 void tryEnemyLaunch(Contact t){if(t.weapon<0||t.weaponFired||elapsed-t.born<30||maxRange<=0||t.pilot!=null&&t.pilot.state!=PilotAI.State.APPROACH)return;Equipment.Weapon w=equipment.enemy[t.weapon];double r=t.range();if(r>w.rangeKm||r<w.minRangeKm||t.alt>w.ceilingM||!sourceSiteVisible(t)||(w.radarMode==Equipment.RadarMode.PASSIVE&&!radarReady())||Math.abs(delta(t.heading,angle(-t.x,-t.y)))>.65)return;EnemyMissile m=new EnemyMissile();m.id=100+(++enemySerial);m.source=t;m.weapon=t.weapon;m.x=t.x;m.y=t.y;m.z=t.alt/1000;m.alt=t.alt;m.flight.profile=w;MissileMotion.aim(m.flight,-m.x,-m.y,terrain(0,0)/1000-m.z);enemyMissiles.add(m);t.weaponFired=true;if(liveTrack(t)){t.launchObserved=true;identifyAllegiance(t,Allegiance.HOSTILE,"OBSERVED ATTACK");identifyAllegiance(m,Allegiance.HOSTILE,"OBSERVED HOSTILE LAUNCH");event("WEAPON LAUNCH DETECTED / TRACK "+t.id);}}
 public int incomingCount(){int n=0;for(EnemyMissile m:enemyMissiles)if(m.alive)n++;return n;}
 public boolean visible(EnemyMissile m){return visible((Contact)m);}
 public String missileName(EnemyMissile m){return recognized(m)?m.recognizedType:"UNKNOWN MSL";}
 void initializeHealth(Contact t){if(t.health<0){t.maxHealth=t instanceof EnemyMissile?rules.missileHealth:rules.aircraftHealthByType[t.type];t.health=t.maxHealth*(t.pilot==null?1:clamp(t.pilot.health/100,0,1));}if(t.maxHealth<=0)t.maxHealth=Math.max(1,t.health);if(t.vulnerability<0)t.vulnerability=t instanceof EnemyMissile?rules.missileVulnerability:rules.aircraftVulnerabilityByType[t.type];if(t.pilot!=null)t.health=Math.min(t.health,t.maxHealth*clamp(t.pilot.health/100,0,1));}
 public boolean canDetonate(Missile m,double distance,boolean passed){Equipment.Weapon w=profile(m);return distance<=w.directHitKm||w.warheadType!=Equipment.WarheadType.KINETIC&&passed&&distance<=w.fuzeKm;}
 public double hitDamage(Missile m,Contact t,double distance){Equipment.Weapon w=profile(m);initializeHealth(t);if(w.warheadType==Equipment.WarheadType.KINETIC){if(distance>w.directHitKm)return 0;double tx=t instanceof EnemyMissile?((EnemyMissile)t).flight.vx*t.speed:Math.sin(t.heading)*t.speed/3600,ty=t instanceof EnemyMissile?((EnemyMissile)t).flight.vy*t.speed:-Math.cos(t.heading)*t.speed/3600,tz=t instanceof EnemyMissile?((EnemyMissile)t).flight.vz*t.speed:t.climb/1000;double relative2=Math.pow((m.vx*m.speed-tx)*1000,2)+Math.pow((m.vy*m.speed-ty)*1000,2)+Math.pow((m.vz*m.speed-tz)*1000,2);return .5*w.massKg*relative2*w.kineticScale*t.vulnerability;}
  double falloff=Math.pow(clamp(1-distance/w.blastRadiusKm,0,1),w.damageExponent),type=w.warheadType==Equipment.WarheadType.FRAGMENTATION?1.1:1;return w.warheadKg*w.damageScale*falloff*type*t.vulnerability;}
 public void resolvePlayerHit(Missile m,double distance,boolean infrared){if(m==null||!m.alive||m.target==null||!m.target.alive)return;Equipment.Weapon w=profile(m);if(w.warheadType==Equipment.WarheadType.KINETIC&&distance>w.directHitKm)return;m.alive=false;m.linkReserved=m.datalink=false;Contact t=m.target;double damage=hitDamage(m,t,distance);applyTargetDamage(t,damage,infrared);event(String.format(Locale.US,"MISSILE DETONATION / %s / %.0f DAMAGE / %.0fm",tag(t),damage,distance*1000));if(t.alive)misses++;}
 void applyTargetDamage(Contact t,double amount,boolean infrared){if(t==null||!t.alive||amount<=0)return;initializeHealth(t);t.health=Math.max(0,t.health-amount);if(t.pilot!=null)t.pilot.health=100*t.health/t.maxHealth;if(t.health==0)confirmIntercept(t,infrared);else{event(String.format(Locale.US,"TARGET DAMAGED / %s / %.0f%% HEALTH",tag(t),100*t.health/t.maxHealth));if(t.pilot!=null&&t.pilot.health<=30)ai.retreat(t);}}
 public void damageAircraft(Contact t,double amount){if(t==null||!t.alive||t instanceof EnemyMissile||amount<=0)return;if(t.pilot==null)ai.assign(t,1,1,2);applyTargetDamage(t,amount,false);}
 public void syncBattery(){health=(int)Math.ceil(battery.condition());maxRange=(int)Math.ceil(equipment.radar.detectionAbsoluteKm*battery.radarFactor());range=Math.max(1,Math.min(range,maxRange));
  for(int i=0;i<launchers.length;i++)if(!battery.parts[Battery.L1+i].alive()){Launcher l=launchers[i];lostAmmo+=l.ammo;l.ammo=0;if(l.reload>0){reserved=Math.max(0,reserved-equipment.radar.launcherCapacity);l.reload=0;}}
  maintainTracks();
 }
 public double explode(double x,double y,double kg,double radius,String label){if(finished)return 0;double damage=battery.explode(x,y,kg,radius,elapsed);syncBattery();Blast blast=new Blast();blast.x=x;blast.y=y;blast.radius=radius;blast.time=elapsed;blast.damage=damage;blast.label=label;blasts.add(blast);
  if(damage>0){radarHits++;lastDamage=damage;impactUntil=elapsed+3;score-=100;event(label+" / "+battery.warning(elapsed));}else event("BLAST MISSED THE BATTERY");if(!battery.parts[Battery.COMMAND].alive())finish(false);return damage;
 }
 /** Compatibility hook for older harnesses; now a charge-based explosion. */
 public void damageRadar(){explode(0,0,87.1,.32,"MISSILE IMPACT");}
 boolean releaseObservable(Contact t){return radarReady()&&liveTrack(t)&&signal(t)>0||ir.irstOnline()&&ir.irstTracked(t)&&ir.irstAge(t)<=equipment.radar.irstUpdateSeconds*1.5;}
 public boolean releaseBombs(Contact t){if(!running||finished||!ai.canBomb(t))return false;PilotAI.Pilot p=t.pilot;int count=p.bombs;
  for(int i=0;i<count;i++){Bomb b=new Bomb();b.source=t;b.category=p.category;b.startX=b.x=t.x;b.startY=b.y=t.y;b.startZ=b.z=t.alt/1000;b.fall=clamp(3+(t.alt-terrain(t.x,t.y))/2000,2,10);double drift=Math.min(.35,t.speed/3600*b.fall*.2),scatter=.04+Math.min(.18,t.alt/50000);b.impactX=t.x+Math.sin(t.heading)*drift+random.nextGaussian()*scatter;b.impactY=t.y-Math.cos(t.heading)*drift+random.nextGaussian()*scatter;b.observed=releaseObservable(t);bombs.add(b);}
  ai.released(t,count);t.attacked=true;leaks++;if(releaseObservable(t)){t.launchObserved=true;identifyAllegiance(t,Allegiance.HOSTILE,"OBSERVED BOMB RELEASE");event("BOMB RELEASE / "+tag(t));}return true;
 }
 public void flyBomb(Bomb b,double dt){if(!b.alive||dt<=0)return;b.age+=dt;double f=clamp(b.age/b.fall,0,1);b.x=b.startX+(b.impactX-b.startX)*f;b.y=b.startY+(b.impactY-b.startY)*f;b.z=b.startZ+(terrain(b.impactX,b.impactY)/1000-b.startZ)*f;if(f>=1){b.alive=false;double damage=explode(b.impactX,b.impactY,rules.explosiveKg[b.category],rules.radiusKm[b.category],"BOMB IMPACT");ai.bombOutcome(b.source,damage);}}
 public int bombCount(){int n=0;for(Bomb b:bombs)if(b.alive)n++;return n;}
 public double reloadSeconds(int i){double rate=battery.reloadRate(i,elapsed);return rate>0?launchers[i].reload/rate:Double.POSITIVE_INFINITY;}
 public void flyEnemy(EnemyMissile m,double dt){Equipment.Weapon w=equipment.enemy[m.weapon];m.age+=dt;Missile f=m.flight;f.x=m.x;f.y=m.y;f.z=m.z;f.speed=m.speed;f.age=m.age;
  double tz=terrain(0,0)/1000,d=Math.sqrt(m.x*m.x+m.y*m.y+Math.pow(tz-m.z,2));boolean link=m.source!=null&&m.source.alive&&sourceSiteVisible(m.source)&&Math.abs(delta(m.source.heading,angle(-m.source.x,-m.source.y)))<.8;
  boolean guided;
  if(w.guidance==Equipment.Guidance.RADAR&&w.radarMode==Equipment.RadarMode.ACTIVE){if(d<=w.seekerKm)f.autonomous=true;m.radarEmitting=f.autonomous;guided=f.autonomous||link;}
  else if(w.guidance==Equipment.Guidance.RADAR&&w.radarMode==Equipment.RadarMode.PASSIVE)guided=radarReady();
  else if(w.guidance==Equipment.Guidance.INFRARED)guided=d<=w.seekerKm;
  else guided=link;
  guided=guided&&m.age<=w.guidanceSeconds&&ir.clearLine(m.x,m.y,m.z,0,0,tz);if(!guided)m.lost+=dt;else m.lost=0;
  double ox=m.x,oy=m.y,oz=m.z;MissileMotion.step(f,w,0,0,tz,guided,dt);m.x=f.x;m.y=f.y;m.z=f.z;m.speed=f.speed;m.alt=m.z*1000;
  if(MissileMotion.nearest(ox,oy,oz,f,0,0,tz)<.08){m.alive=false;explode(m.x,m.y,w.explosiveKg,w.blastRadiusKm,"MISSILE IMPACT");return;}
  if(m.z*1000<terrain(m.x,m.y)){m.alive=false;explode(m.x,m.y,w.explosiveKg,w.blastRadiusKm,"MISSILE GROUND IMPACT");return;}
  if(m.lost>3||m.age>w.guidanceSeconds+3||f.path>w.rangeKm){m.alive=false;return;}
  double adv=radarReady()?dt*TAU/equipment.radar.sweepSeconds():0,bearing=(angle(m.x,m.y)+TAU)%TAU;if((sweep-bearing+TAU)%TAU<adv)observe(m);
 }
}

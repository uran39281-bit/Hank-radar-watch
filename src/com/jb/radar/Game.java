package com.jb.radar;
import java.util.*;
/** A fictional, deliberately simplified game simulation. Not an operational model. */
public class Game {
 public static final double TAU=Math.PI*2;
 public static final String[] NAMES={"Su-25","MiG-21bis","MiG-23M","Su-22M3","Su-17M4"};
 public static final double[] MAX_SPEED={979,2240,2358,2232,2052}, LENGTH={14.2,14.1,16.7,18.9,18.9}, SPAN={14.4,7.2,14,13.7,13.7}, CEILING={11000,16000,16000,19500,19500};
 public static final double[] CRUISE={680,1080,1280,960,940}, TURN={.12,.28,.19,.17,.17}, ACCEL={13,28,35,24,24}, BASE_ALT={850,4800,6500,2300,2200};
 public enum TrackStage { DETECTED, ACQUIRING, STABLE, COASTING, LOST }
 public enum SeekerState { MIDCOURSE, SEARCHING, ACQUIRED }
 public enum GuidanceLine { NONE, BATTERY_SOLID, BATTERY_DOTTED, TARGET_SOLID }
 public static class Contact {
  public boolean hadTrack;public long priorityOrder;public int missed;public double missedAt=-1;public PilotAI.Pilot pilot;public int id,type,samples,weapon=-1,flareBursts=3; public double nextFlare; public boolean priority,radarTracked,radarEmitting,launchObserved,weaponFired; public double born,nextPriority; public double x,y,alt,speed,heading,climb,desiredAlt,evasion,side=1,seen=-999,px,py,palt,pspeed,psize,plen,pheading,pclimb,pturn,quality,confidence,previousHeading;
  public double[] probabilities=new double[6]; public int guess;public boolean alive=true,illuminated,attacked,escaped; public String outcome="";
  public double range(){return Math.hypot(x,y);}public double measuredRange(){return Math.hypot(px,py);}
 }
 public static class Missile {public SeekerState seekerState=SeekerState.MIDCOURSE;public boolean estimateValid,linkReserved,supportReleased;public double estimateX,estimateY,estimateZ,estimateVx,estimateVy,estimateVz,estimateAge,estimateSeen=-999,searchAge,supportLostFor;public boolean infrared,autonomous,datalink;public Equipment.Weapon profile;public double vx,vy,vz;public Infrared.Flare decoy;public Contact target;public double x,y,z,speed=.08,age,lost,path;public boolean alive=true;}
 public static class EnemyMissile extends Contact {public Contact source;public final Missile flight=new Missile();public double z,age,lost,pz;public EnemyMissile(){speed=.18;}}
 public static class Bomb {public Contact source;public int category;public double startX,startY,x,y,z,impactX,impactY,startZ,age,fall;public boolean alive=true,observed;}
 public static class Blast {public double x,y,radius,time,damage;public String label;}
 public ArrayList<Bomb> bombs=new ArrayList<>();public ArrayList<Blast> blasts=new ArrayList<>();public int lostAmmo;
 public ArrayList<EnemyMissile> enemyMissiles=new ArrayList<>();public int maxRange=40,enemySerial,radarHits;public double impactUntil,lastDamage;
 public static class Launcher {public int ammo=3;public double reload;}
 public Random random;public ArrayList<Contact> contacts=new ArrayList<>();public ArrayList<Missile> missiles=new ArrayList<>();public ArrayList<String> log=new ArrayList<>();public Launcher[] launchers=new Launcher[3];
 public long prioritySerial;
 public double elapsed,sweep,nextSpawn;public int reserve=9,reserved=0,spawned,kills,misses,leaks,health=100,score,shots,range=40,chosenLauncher=0;public boolean running,finished,won;public String message="";
 public final Infrared ir=new Infrared(this);public boolean irMode;
 public void toggleWeapon(){event("EQUIPPED: "+weapon.name+" / CHANGE MISSILE IN LOADOUT");}
 public String weaponLock(Contact t){return irMode?ir.begin(t):illuminate(t);}
 public String weaponRelease(Contact t){if(irMode){ir.cancel();return "IR SEEKER OFF / FIRED MISSILES SELF GUIDE";}return release(t);}
 public String weaponBlock(Contact t){return irMode?ir.launchBlock(t):launchBlock(t);}
 public String fireWeapon(Contact t){return irMode?ir.launch(t):launch(t);}
 public final Economy economy;public final Equipment equipment;public final CombatRules rules;public final Battery battery;public final PilotAI ai;public Equipment.Weapon weapon;
 public Game(long seed){this(seed,new Economy(new Economy.MemoryStore()));}
 public Game(long seed,Economy economy){this(seed,economy,Equipment.defaults());}
 public Game(long seed,Economy economy,Equipment equipment){this(seed,economy,equipment,CombatRules.defaults());}
 public Game(long seed,Economy economy,Equipment equipment,CombatRules rules){this.rules=rules;battery=new Battery(rules);ai=new PilotAI(this);this.economy=economy;this.equipment=equipment;weapon=equipment.playerWeapon(economy.snapshot().equippedMissile);irMode=weapon.guidance==Equipment.Guidance.INFRARED;maxRange=range=(int)Math.ceil(equipment.radar.detectionAbsoluteKm);random=new Random(seed);for(int i=0;i<3;i++)launchers[i]=new Launcher();}
 public boolean start(){if(!economy.beginMission())return false;prioritySerial=0;weapon=equipment.playerWeapon(economy.snapshot().equippedMissile);ir.reset();ai.reset();battery.reset();lostAmmo=0;bombs.clear();blasts.clear();irMode=weapon.guidance==Equipment.Guidance.INFRARED;contacts.clear();missiles.clear();enemyMissiles.clear();maxRange=(int)Math.ceil(equipment.radar.detectionAbsoluteKm);radarHits=enemySerial=0;impactUntil=lastDamage=0;log.clear();elapsed=sweep=0;nextSpawn=48;reserve=9;reserved=spawned=kills=misses=leaks=score=shots=0;health=100;range=maxRange;chosenLauncher=0;running=true;finished=won=false;for(int i=0;i<3;i++)launchers[i]=new Launcher();spawn(0,34);spawn(1,46);event("MISSION 1 / DEFEND THE COMMAND SITE");return true;}
 public void event(String s){message=s;log.add(String.format(Locale.US,"%02d:%02d %s",(int)elapsed/60,(int)elapsed%60,s));if(log.size()>40)log.remove(0);}
 void spawn(int type,double distance){Contact t=new Contact();t.id=++spawned;t.type=type;t.radarEmitting=type==1||type==2;t.born=elapsed;t.weapon=type==1?-1:type==0?random.nextInt(2):type==2?2:type==3?3:4;double a=random.nextDouble()*TAU;t.x=Math.sin(a)*distance;t.y=-Math.cos(a)*distance;t.heading=angle(-t.x,-t.y);t.speed=CRUISE[type]*(.85+random.nextDouble()*.2);t.desiredAlt=BASE_ALT[type]*(.5+random.nextDouble());if(type>=3&&random.nextBoolean())t.desiredAlt=130+random.nextDouble()*300;t.alt=terrain(t.x,t.y)+t.desiredAlt;t.side=random.nextBoolean()?1:-1;ai.assign(t,rules.mission1SmartLevel,type==0?2:1,new int[]{2,1,3,2,0,4}[t.id%6]);if(t.pilot.smart==1){t.desiredAlt=Math.max(1800,t.desiredAlt);t.alt=terrain(t.x,t.y)+t.desiredAlt;}contacts.add(t);}
 static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}static double angle(double x,double y){return Math.atan2(x,-y);}static double delta(double a,double b){return Math.atan2(Math.sin(a-b),Math.cos(a-b));}
 public static double terrain(double x,double y){double ridge1=680*Math.exp(-Math.pow((x-13)/3.5,2)-Math.pow((y+8)/15,2));double ridge2=1050*Math.exp(-Math.pow((x+19)/5,2)-Math.pow((y-11)/12,2));return 80+ridge1+ridge2;}
 /** Smooth, fictional quality falloff between nominal and absolute sensor limits. */
 public double rangeFactor(double distance,double effective,double absolute){if(distance>absolute)return 0;if(distance<=effective*.8)return 1;if(distance<=effective)return 1-.12*(distance-effective*.8)/Math.max(.1,effective*.2);return .88-.63*(distance-effective)/Math.max(.1,absolute-effective);}
 public double signal(Contact t){if(t==null||!battery.radarOnline(elapsed))return 0;double r=t.range(),radarHeight=35,targetASL=t.alt;double horizon=4.12*(Math.sqrt(radarHeight)+Math.sqrt(Math.max(0,targetASL)));double absolute=equipment.radar.detectionAbsoluteKm*battery.radarFactor();if(r>absolute||r>horizon||absolute<=0)return 0;double origin=terrain(0,0)+radarHeight;for(int i=1;i<24;i++){double f=i/24.0;if(terrain(t.x*f,t.y*f)>origin+(targetASL-origin)*f)return 0;}double agl=t.alt-terrain(t.x,t.y);return (agl<150?.32:agl<500?.58:agl<1500?.82:1)*(.35+.65*battery.parts[Battery.RADAR].hp/100)*rangeFactor(r,equipment.radar.detectionKm*battery.radarFactor(),absolute);}
 public double trackingRange(){return Math.min(equipment.radar.detectionAbsoluteKm,equipment.radar.trackingAbsoluteKm)*battery.radarFactor();}
 public double lockRange(){return Math.min(equipment.radar.detectionAbsoluteKm,equipment.radar.lockAbsoluteKm)*battery.radarFactor();}
 public double freshSeconds(){return equipment.radar.sweepSeconds()*1.15;}
 public double trackExpiry(){return Math.max(22,equipment.radar.sweepSeconds()*2.2);}
 public double contactExpiry(){return Math.max(32,equipment.radar.sweepSeconds()*3.2);}
 public boolean detected(Contact t){return t!=null&&t.alive&&t.samples>0&&elapsed-t.seen<contactExpiry();}
 public boolean stale(Contact t){return t!=null&&(t.missed>0||elapsed-t.seen>freshSeconds());}
 public boolean tracked(Contact t){return detected(t)&&t.radarTracked&&elapsed-t.seen<trackExpiry()&&t.measuredRange()<=trackingRange()&&battery.radarOnline(elapsed);}
 public boolean liveTrack(Contact t){return tracked(t)&&!stale(t);}
 public double trackQuality(Contact t){if(!tracked(t))return 0;double freshness=stale(t)?Math.max(0,1-(elapsed-t.seen)/trackExpiry()):1;return clamp(t.quality*rangeFactor(t.measuredRange(),equipment.radar.trackingKm*battery.radarFactor(),trackingRange())*freshness,0,1);}
 public double lockQuality(Contact t){return trackQuality(t)*rangeFactor(slant(t),equipment.radar.lockKm*battery.radarFactor(),lockRange());}
 public boolean stableTrack(Contact t){return liveTrack(t)&&t.samples>=Math.max(2,equipment.radar.acquireObservations)&&trackQuality(t)>=equipment.radar.trackQualityMinimum;}
 public boolean visible(Contact t){return detected(t)&&t.measuredRange()<=range;}
 public int trackedCount(){int n=0;for(Contact t:targets())if(tracked(t))n++;return n;}
 public boolean protectedTrack(Contact t){if(t.illuminated)return true;for(Missile m:missiles)if(m.alive&&m.target==t&&!m.infrared&&!m.supportReleased&&!m.autonomous&&(activeHoming(profile(m))?m.linkReserved:true))return true;return false;}
 boolean candidate(Contact t){return detected(t)&&t.measuredRange()<=trackingRange()&&(tracked(t)||!stale(t));}
 int priorityRank(Contact t){return t.radarTracked&&protectedTrack(t)?0:t instanceof EnemyMissile?1:t.priority?2:3;}
 int comparePriority(Contact a,Contact b){int n=Integer.compare(priorityRank(a),priorityRank(b));if(n!=0)return n;if(a.priority&&b.priority){n=Long.compare(b.priorityOrder,a.priorityOrder);if(n!=0)return n;}n=Double.compare(a.measuredRange(),b.measuredRange());return n!=0?n:Integer.compare(a.id,b.id);}
 void acquireTrack(Contact t){maintainTracks();}
 void maintainTracks(){ArrayList<Contact> all=targets(),eligible=new ArrayList<>();boolean online=battery.radarOnline(elapsed);
  for(Contact t:all){if(!online||!detected(t)||elapsed-t.seen>=trackExpiry()||t.measuredRange()>trackingRange())t.radarTracked=false;
   if(t.illuminated&&(!stableTrack(t)||slant(t)>lockRange()||lockQuality(t)<equipment.radar.lockQualityMinimum)){t.illuminated=false;event("ILLUMINATION LOST / "+tag(t)+" / "+sarhInbound(t)+" MISSILES AFFECTED");}
   if(online&&candidate(t))eligible.add(t);
  }
  eligible.sort((a,b)->comparePriority(a,b));HashSet<Contact> chosen=new HashSet<>();for(int i=0;i<Math.min(equipment.radar.tracks,eligible.size());i++)chosen.add(eligible.get(i));
  for(Contact t:all){t.radarTracked=chosen.contains(t);if(t.radarTracked)t.hadTrack=true;}
  refreshDatalinks();
 }
 public TrackStage trackStage(Contact t){if(t==null||!detected(t))return TrackStage.LOST;if(!tracked(t))return t.hadTrack&&(stale(t)||!battery.radarOnline(elapsed))?TrackStage.LOST:TrackStage.DETECTED;if(stale(t))return TrackStage.COASTING;if(t.samples<Math.max(2,equipment.radar.acquireObservations))return TrackStage.ACQUIRING;return TrackStage.STABLE;}
 public String trackState(Contact t){if(t!=null&&ir.target==t&&ir.locked)return "IR LOCK";if(t==null)return "NO CONTACT";if(!t.alive)return t.outcome;TrackStage stage=trackStage(t);if(hardLocked(t))return "LOCKED";if(stage==TrackStage.STABLE&&trackQuality(t)<equipment.radar.trackQualityMinimum)return "MARGINAL";return stage.toString();}
 public boolean hardLocked(Contact t){return t!=null&&stableTrack(t)&&t.illuminated&&slant(t)<=lockRange()&&lockQuality(t)>=equipment.radar.lockQualityMinimum;}
 /** UI predictions and midcourse guidance use recorded measurements, never hidden target motion. */
 public double predictionAge(Contact t){if(!tracked(t)||t.samples<2)return 0;double age=Math.min(Math.max(0,elapsed-t.seen),freshSeconds());if(t.missedAt>=t.seen)age=Math.min(age,t.missedAt-t.seen);return age;}
 public double displayX(Contact t){return t.px+Math.sin(t.pheading)*t.pspeed/3600*predictionAge(t);}
 public double displayY(Contact t){return t.py-Math.cos(t.pheading)*t.pspeed/3600*predictionAge(t);}
 public double displayRange(Contact t){return Math.hypot(displayX(t),displayY(t));}
 public String altitudeText(Contact t){return tracked(t)?String.format(Locale.US,"%.0f",t.palt):"---";}
 public String speedText(Contact t){return tracked(t)&&t.samples>=2?String.format(Locale.US,"%.0f",t.pspeed):"---";}
 public String directionText(Contact t){return tracked(t)&&t.samples>=2?String.format(Locale.US,"%03d",((int)Math.toDegrees(t.pheading)%360+360)%360):"---";}
 public String bearingText(Contact t){return detected(t)?String.format(Locale.US,"%03d",((int)Math.toDegrees(angle(displayX(t),displayY(t)))%360+360)%360):"---";}
 public static boolean activeHoming(Equipment.Weapon w){return w.guidance==Equipment.Guidance.RADAR&&w.radarMode==Equipment.RadarMode.ACTIVE;}
 Equipment.Weapon profile(Missile m){return m.profile==null?weapon:m.profile;}
 boolean canUpdate(Contact t){return stableTrack(t)&&slant(t)<=trackingRange()&&signal(t)>.15;}
 /** Each active missile reserves its own slot until acquisition, expiry or explicit release. */
 LinkedHashSet<Contact> refreshDatalinks(){LinkedHashSet<Contact> occupied=new LinkedHashSet<>();
  for(Missile m:missiles){Equipment.Weapon w=profile(m);if(!m.alive||m.autonomous||m.age>w.guidanceSeconds){m.linkReserved=false;m.datalink=false;continue;}
   m.datalink=!m.infrared&&activeHoming(w)&&m.linkReserved&&!m.supportReleased&&canUpdate(m.target)&&m.supportLostFor<=w.supportRecoverySeconds;
   if(m.linkReserved)occupied.add(m.target);
  }return occupied;
 }
 public int illuminationUsed(){int n=0;for(Contact t:targets())if(hardLocked(t))n++;return n;}
 public int midcourseUsed(){refreshDatalinks();int n=0;for(Missile m:missiles)if(m.alive&&m.linkReserved&&!m.supportReleased)n++;return n;}
 public int supportedMissilesUsed(){int n=0;for(Missile m:missiles)if(m.alive&&!m.infrared&&!activeHoming(profile(m))&&m.age<=profile(m).guidanceSeconds)n++;return n;}
 /** Compatibility total; the interface labels the two distinct resource pools separately. */
 public int channels(){return illuminationUsed()+midcourseUsed();}
 public boolean channelAvailable(Contact t){return hardLocked(t)||illuminationUsed()<equipment.radar.illuminationChannels;}
 public GuidanceLine guidanceLine(Missile m){if(!m.alive)return GuidanceLine.NONE;if(m.infrared)return ir.confirmedGuidance(m)?GuidanceLine.TARGET_SOLID:GuidanceLine.NONE;if(activeHoming(profile(m)))return m.autonomous&&m.seekerState==SeekerState.ACQUIRED&&m.lost==0?GuidanceLine.TARGET_SOLID:m.datalink?GuidanceLine.BATTERY_DOTTED:GuidanceLine.NONE;return hardLocked(m.target)&&m.lost==0?GuidanceLine.BATTERY_SOLID:GuidanceLine.NONE;}
 public String missileState(Missile m){if(!m.alive)return "ENGAGEMENT ENDED";if(m.infrared)return ir.missileState(m);if(activeHoming(profile(m))){refreshDatalinks();return m.autonomous?"ACTIVE - TARGET ACQUIRED":m.seekerState==SeekerState.SEARCHING?"SEARCHING":m.datalink?"MIDCOURSE":"MIDCOURSE / SUPPORT LOST";}return hardLocked(m.target)&&m.lost==0?"S-A / ILLUMINATED":"SUPPORT LOST";}
 public Missile earliestReleasableSupport(Contact t){for(Missile m:missiles)if(m.alive&&m.target==t&&!m.infrared&&!m.autonomous&&!m.supportReleased&&(activeHoming(profile(m))?m.linkReserved:hardLocked(t)))return m;return null;}
 public String releaseSupport(Missile m){if(m==null||!m.alive||m.infrared||m.autonomous)return "NO BATTERY SUPPORT TO RELEASE";if(!activeHoming(profile(m)))return release(m.target);if(!m.linkReserved)return "NO BATTERY SUPPORT TO RELEASE";m.supportReleased=true;m.linkReserved=m.datalink=false;event("SUPPORT RELEASED / "+profile(m).name+" / "+tag(m.target));return message;}
 public static boolean needsGroundLink(Equipment.Weapon w){return w.guidance==Equipment.Guidance.COMMAND||w.guidance==Equipment.Guidance.LASER||w.guidance==Equipment.Guidance.RADAR&&w.radarMode!=Equipment.RadarMode.PASSIVE;}
 public double slant(Contact t){return t==null?Double.POSITIVE_INFINITY:Math.hypot(t.measuredRange(),(t.palt-terrain(0,0))/1000);}
 public String lockBlock(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!battery.radarOnline(elapsed))return battery.warning(elapsed);if(!tracked(t)||!stableTrack(t))return "TRACK TOO WEAK";if(slant(t)>lockRange())return "OUTSIDE RADAR LOCK RANGE";if(lockQuality(t)<equipment.radar.lockQualityMinimum)return "TRACK TOO WEAK / MARGINAL";if(t.illuminated)return "TARGET ALREADY LOCKED";if(!channelAvailable(t))return "NO FREE ILLUMINATION CHANNEL";return null;}
 public int inbound(Contact t){int n=0;for(Missile m:missiles)if(m.alive&&m.target==t)n++;return n;}
 public int sarhInbound(Contact t){int n=0;for(Missile m:missiles)if(m.alive&&m.target==t&&!m.infrared&&!activeHoming(profile(m)))n++;return n;}
 public String illuminate(Contact t){String block=lockBlock(t);if(block!=null)return block;t.illuminated=true;ai.warn(t,PilotAI.Warning.LOCK,signal(t)>0);event("LOCK ESTABLISHED / TRACK "+t.id);return message;}
 public String release(Contact t){if(t==null||!t.illuminated)return "NO ILLUMINATION TO RELEASE";t.illuminated=false;event("LOCK RELEASED / "+tag(t)+" / "+sarhInbound(t)+" MISSILES AFFECTED");return message;}
 public String launchBlock(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!battery.powered(elapsed))return "POWER FAILURE";if(!battery.parts[Battery.L1+chosenLauncher].alive())return "LAUNCHER DISABLED";if(!battery.radarOnline(elapsed))return battery.warning(elapsed);if(!stableTrack(t))return "TRACK TOO WEAK";
  Equipment.Weapon w=weapon;boolean active=activeHoming(w);if(!active&&!hardLocked(t))return "RADAR LOCK REQUIRED";double slant=slant(t);
  if(slant>Math.min(active?trackingRange():lockRange(),w.rangeKm)||slant<w.minRangeKm)return "OUTSIDE ENGAGEMENT ENVELOPE";if(t.palt>w.ceilingM)return "ABOVE MISSILE CEILING";
  if(active&&midcourseUsed()>=equipment.radar.midcourseChannels)return "NO FREE SUPPORT CHANNEL";
  if(!active&&supportedMissilesUsed()>=equipment.radar.supportedMissiles)return "SUPPORTED MISSILE LIMIT";
  Launcher l=launchers[chosenLauncher];if(l.reload>0)return "LAUNCHER RELOADING";if(l.ammo==0)return "LAUNCHER EMPTY";return null;
 }
 void cacheEstimate(Missile m){Contact t=m.target;m.estimateX=displayX(t);m.estimateY=displayY(t);m.estimateZ=Math.max(0,t.palt/1000+t.pclimb/1000*predictionAge(t));m.estimateVx=Math.sin(t.pheading)*t.pspeed/3600;m.estimateVy=-Math.cos(t.pheading)*t.pspeed/3600;m.estimateVz=t.pclimb/1000;m.estimateAge=0;m.estimateSeen=t.seen;m.estimateValid=true;}
 public String launch(Contact t){String block=launchBlock(t);if(block!=null)return block;Launcher l=launchers[chosenLauncher];l.ammo--;shots++;Missile m=new Missile();m.target=t;m.profile=weapon;m.z=(terrain(0,0)+5)/1000;cacheEstimate(m);MissileMotion.aim(m,m.estimateX,m.estimateY,m.estimateZ-m.z);m.linkReserved=activeHoming(weapon);missiles.add(m);refreshDatalinks();ai.launchWarning(t,false);if(l.ammo==0&&reserve-reserved>=3){l.reload=90;reserved+=3;}event("MISSILE AWAY / TRACK "+t.id);return message;}
 public void observe(Contact t){ai.radarPresence(t);if(elapsed-t.seen<battery.processingDelay())return;double q=signal(t);
  if(q==0||random.nextDouble()>q){t.missed++;if(t.missedAt<0)t.missedAt=elapsed;return;}
  double r=t.range(),error=(.02+r/160)*(2-q)*(2.2-rangeQuality());double previousSeen=t.seen,previousBearing=t.pheading,previousX=t.px,previousY=t.py,previousAlt=t.palt;if(elapsed-t.seen>=trackExpiry())t.samples=0;t.seen=elapsed;t.missed=0;t.missedAt=-1;t.samples++;
  t.px=t.x+random.nextGaussian()*error*.45;t.py=t.y+random.nextGaussian()*error*.45;
  t.palt=Math.max(20,t.alt+random.nextGaussian()*error*300);t.quality=q;
  if(t.samples>=2){double interval=Math.max(.1,elapsed-previousSeen),dx=t.px-previousX,dy=t.py-previousY;t.pspeed=clamp(Math.hypot(dx,dy)/interval*3600,0,4200);t.pheading=angle(dx,dy);t.pclimb=clamp((t.palt-previousAlt)/interval,-100,100);t.pturn=Math.abs(delta(t.pheading,previousBearing))/Math.max(1,interval);}else{t.pspeed=t.pheading=t.pclimb=t.pturn=0;}
  if(!(t instanceof EnemyMissile)){double span=SPAN[t.type];if(t.type==2)span=14-6.2*clamp((t.speed-650)/650,0,1);else if(t.type>=3)span=13.7-3.7*clamp((t.speed-600)/700,0,1);double aspect=Math.abs(Math.sin(t.heading-angle(-t.x,-t.y)));t.psize=Math.max(3,span*(.82+.18*aspect)+random.nextGaussian()*error*8);t.plen=Math.max(8,LENGTH[t.type]+random.nextGaussian()*error*5);identify(t);}
  acquireTrack(t);
 }
 public void identify(Contact t){double r=t.measuredRange(),noise=1+r/28,history=Math.min(1,t.samples/12.0);double[] weights=new double[5];double sum=0;for(int i=0;i<5;i++){double speedFit=Math.exp(-Math.pow((t.pspeed-CRUISE[i])/(250*noise),2));if(t.pspeed>MAX_SPEED[i]*1.12)speedFit*=.01;double span=SPAN[i];if(i==2)span=14-6.2*clamp((t.pspeed-650)/650,0,1);else if(i>=3)span=13.7-3.7*clamp((t.pspeed-600)/700,0,1);double sizeFit=Math.exp(-Math.pow((t.psize-span*.9)/(2.2*noise),2)-Math.pow((t.plen-LENGTH[i])/(2*noise),2));double altFit=Math.exp(-Math.abs(t.palt-BASE_ALT[i])/(3500*noise));if(t.palt>CEILING[i])altFit*=.1;double behavior=Math.exp(-Math.abs(t.pclimb)/(i==0?22:60))*Math.exp(-Math.max(0,t.pturn-TURN[i])/(.04*noise));double match=.30*speedFit+.25*sizeFit+.20*altFit+.15*behavior+.10*history;weights[i]=Math.exp(match*12);sum+=weights[i];}
  double quality=(.4+.6*(1-clamp(r/90,0,1)))*(.55+.45*t.quality)*(.65+.35*history);quality*=rangeQuality()*(1-Math.min(.25,t.pturn*1.5));int top=0;for(int i=0;i<5;i++){t.probabilities[i]=weights[i]/sum*quality;if(t.probabilities[i]>t.probabilities[top])top=i;}t.probabilities[5]=1-quality;t.guess=top;t.confidence=t.probabilities[top];
 }
 public String identification(Contact t){if(t instanceof EnemyMissile)return missileName((EnemyMissile)t);if(t==null||t.samples<2)return "UNKNOWN AIR CONTACT";double conf=t.confidence*Math.max(.3,1-Math.max(0,elapsed-t.seen-5)/30);int pc=(int)(conf*100);if(pc<30)return "UNKNOWN / "+pc+"%";String label=pc<50?"POSSIBLE":pc<75?"LIKELY":pc<90?"PROBABLE":"HIGH CONF.";return label+" "+NAMES[t.guess]+" "+pc+"%";}
 public void tick(double dt){if(!running||finished)return;dt=clamp(dt,0,.1);if(dt==0)return;elapsed+=dt;maintainTracks();double adv=dt*TAU/equipment.radar.sweepSeconds();sweep=(sweep+adv)%TAU;
  for(int li=0;li<launchers.length;li++){Launcher l=launchers[li];if(l.reload>0){l.reload=Math.max(0,l.reload-dt*battery.reloadRate(li,elapsed));if(l.reload==0){reserve-=3;reserved-=3;l.ammo=3;event("LAUNCHER RELOAD COMPLETE");}}}
  if(spawned<12&&elapsed>=nextSpawn){int count=spawned<5?1:2;for(int i=0;i<count&&spawned<12;i++)spawn((spawned+1)%5,40+random.nextDouble()*18);nextSpawn=elapsed+48;event("NEW CONTACTS APPROACHING THE SECTOR");}
  for(Contact t:contacts)if(t.alive){ai.tick(t,dt);double oldHeading=t.heading;boolean guiding=guiding(t)&&!ai.defensive(t);double desired=ai.heading(t);boolean defensive=t.evasion>0||ai.defensive(t);if(t.pilot==null&&t.evasion>0){t.evasion=Math.max(0,t.evasion-dt);desired+=t.side*(t.type==1?1.7:t.type==2?1.0:1.35);}
   double turnLimit=Math.min(guiding?.035:TURN[t.type],(t.type==0?5:8)*9.81/(t.speed/3.6));double turn=clamp(delta(desired,t.heading),-turnLimit*dt,turnLimit*dt);t.heading+=turn;double hard=Math.abs(turn)/dt;double desiredSpeed=CRUISE[t.type]*(defensive&&t.type==2?1.25:1);double acceleration=clamp(desiredSpeed-t.speed,-25,ACCEL[t.type]);t.speed=clamp(t.speed+(acceleration-hard*(t.type==1?175:100))*dt,260,Math.min(MAX_SPEED[t.type],t.alt<1500?1250:MAX_SPEED[t.type]));
   t.x+=Math.sin(t.heading)*t.speed/3600*dt;t.y-=Math.cos(t.heading)*t.speed/3600*dt;double ground=terrain(t.x,t.y),agl=defensive&&(t.pilot==null||t.pilot.smart>=3)&&(t.type==0||t.type>=3)?90:t.desiredAlt;t.climb=clamp((ground+agl-t.alt)*.12,-(t.type==0?22:60),t.type==0?16:45);t.alt=clamp(t.alt+t.climb*dt,ground+40,CEILING[t.type]);t.previousHeading=oldHeading;
   double bearing=(angle(t.x,t.y)+TAU)%TAU;if((sweep-bearing+TAU)%TAU<adv)observe(t);tryEnemyLaunch(t);releaseBombs(t);
   if(t.illuminated&&(!stableTrack(t)||slant(t)>lockRange()||lockQuality(t)<equipment.radar.lockQualityMinimum)){t.illuminated=false;event("CHANNEL LOST / TRACK "+t.id);}
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
 void startSeekerSearch(Missile m){if(m.seekerState==SeekerState.SEARCHING)return;m.seekerState=SeekerState.SEARCHING;m.searchAge=0;m.autonomous=false;ai.activeSeekerWarning(m.target,m);event(profile(m).name+" / SEARCHING / "+tag(m.target));}
 public boolean guided(Missile m){Contact t=m.target;Equipment.Weapon w=profile(m);if(!m.alive||t==null||!t.alive||m.age>w.guidanceSeconds)return false;
  if(activeHoming(w)){
   refreshDatalinks();if(!m.estimateValid&&detected(t))cacheEstimate(m);
   if(m.datalink&&m.estimateSeen!=t.seen)cacheEstimate(m);
   double dx=estimateX(m)-m.x,dy=estimateY(m)-m.y,dz=estimateZ(m)-m.z,predictedDistance=Math.sqrt(dx*dx+dy*dy+dz*dz);
   if(m.seekerState==SeekerState.MIDCOURSE&&(predictedDistance<=w.seekerKm||(w.earlyActivation&&!m.datalink&&m.supportLostFor>0)))startSeekerSearch(m);
   if(m.seekerState==SeekerState.ACQUIRED&&!seekerCanSee(m)){m.autonomous=false;startSeekerSearch(m);event(w.name+" / SEEKER LOST / SEARCHING");}
   if(m.seekerState==SeekerState.SEARCHING&&seekerCanSee(m)){m.seekerState=SeekerState.ACQUIRED;m.autonomous=true;m.linkReserved=m.datalink=false;m.searchAge=0;event(w.name+" / ACTIVE - TARGET ACQUIRED");}
   if(m.autonomous){cacheSeekerEstimate(m);return true;}
   return m.seekerState==SeekerState.MIDCOURSE&&m.datalink;
  }
  if(w.radarMode==Equipment.RadarMode.PASSIVE)return t.radarEmitting&&ir.lineOfSight(m.x,m.y,m.z,t);
  return m.supportLostFor<=w.supportRecoverySeconds&&hardLocked(t)&&signal(t)>.15&&!ai.notchBreak(t)&&ir.lineOfSight(m.x,m.y,m.z,t);
 }
 public void fly(Missile m,double dt){if(!m.alive||dt<=0)return;if(m.infrared){ir.fly(m,dt);return;}Contact t=m.target;Equipment.Weapon w=profile(m);m.age+=dt;m.estimateAge+=dt;boolean guided=guided(m);boolean active=activeHoming(w);
  if(guided){m.lost=0;if(!active)cacheSeekerEstimate(m);}else m.lost+=dt;
  boolean supportAvailable=active?(m.datalink||m.autonomous):guided;if(supportAvailable)m.supportLostFor=0;else m.supportLostFor+=dt;
  if(active&&m.seekerState==SeekerState.SEARCHING)m.searchAge+=dt;
  if(active&&m.linkReserved&&!m.datalink&&m.supportLostFor>w.supportRecoverySeconds){m.linkReserved=false;m.supportReleased=true;event(w.name+" / UPDATE RECOVERY EXPIRED / INERTIAL");}
  // All unsupported flight retains a finite cached intercept estimate. No hidden target steering.
  double tx=estimateX(m),ty=estimateY(m),tz=estimateZ(m),ox=m.x,oy=m.y,oz=m.z;
  if(guided&&(!active||m.autonomous)){tx=t.x;ty=t.y;tz=t.alt/1000;}else if(m.estimateValid){double distance=Math.sqrt(Math.pow(tx-m.x,2)+Math.pow(ty-m.y,2)+Math.pow(tz-m.z,2)),lead=clamp(distance/Math.max(.25,m.speed),0,12);tx+=m.estimateVx*lead;ty+=m.estimateVy*lead;tz=Math.max(.02,tz+m.estimateVz*lead);}
  MissileMotion.step(m,w,tx,ty,tz,m.estimateValid,dt);ai.visualWarning(t,m);
  double nearest=t==null?Double.POSITIVE_INFINITY:MissileMotion.nearest(ox,oy,oz,m,t.x,t.y,t.alt/1000);
  // Contact truth is used for collision only; guidance cannot read it before seeker acquisition.
  if(t!=null&&t.alive&&guided&&(!active||m.autonomous)&&nearest<.12&&m.age>1){m.alive=false;m.linkReserved=m.datalink=false;confirmIntercept(t,false);}
  else if(t!=null&&t.pilot!=null&&guided&&(!active||m.autonomous)&&nearest<.25&&m.age>1&&passedClosest(ox,oy,oz,m,t)){m.alive=false;m.linkReserved=m.datalink=false;damageAircraft(t,25+70*(1-clamp((nearest-.12)/.13,0,1)));if(t.alive)misses++;}
  else if((!active&&m.supportLostFor>w.supportRecoverySeconds)||(active&&m.seekerState==SeekerState.SEARCHING&&m.searchAge>w.seekerSearchSeconds)||m.age>w.guidanceSeconds||m.path>w.rangeKm||(m.age>1&&m.z*1000<terrain(m.x,m.y))||t==null||!t.alive){m.alive=false;m.linkReserved=m.datalink=false;misses++;event(w.name+" FAILED / "+(t==null?"NO TARGET":tag(t)));}
 }
 /** A near pass is a graze only after closest approach; never detonate early on the way to a direct hit. */
 boolean passedClosest(double ox,double oy,double oz,Missile m,Contact t){double before=Math.pow(t.x-ox,2)+Math.pow(t.y-oy,2)+Math.pow(t.alt/1000-oz,2);double after=Math.pow(t.x-m.x,2)+Math.pow(t.y-m.y,2)+Math.pow(t.alt/1000-m.z,2);return after>before+1e-12;}
 public void confirmIntercept(Contact t,boolean infrared){if(!t.alive||finished)return;economy.intercept(tag(t),t instanceof EnemyMissile);ai.destroyed(t);t.alive=false;t.illuminated=false;t.outcome=infrared?"IR INTERCEPT":"INTERCEPTED";kills++;score+=250;maintainTracks();event((infrared?"IR INTERCEPT / ":"INTERCEPT / ")+tag(t));}
 public String prioritize(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!battery.radarOnline(elapsed))return battery.warning(elapsed);if(!visible(t))return "SELECT A DETECTED CONTACT";if(t.priority){t.priority=false;t.priorityOrder=0;maintainTracks();event("AUTOMATIC PRIORITY / "+tag(t));return message;}if(t.measuredRange()>trackingRange())return "OUTSIDE TRACKING RANGE";if(!tracked(t)&&stale(t))return "CONTACT STALE / WAIT FOR RADAR";
  maintainTracks();if(!tracked(t)&&trackedCount()>=equipment.radar.tracks){boolean replaceable=false;for(Contact other:targets())if(tracked(other)&&!protectedTrack(other))replaceable=true;if(!replaceable)return "NO FREE TRACK SLOT";}
  t.priority=true;t.priorityOrder=++prioritySerial;maintainTracks();event("PLAYER PRIORITY / "+tag(t));return message;
 }
 /** Legacy harness alias. UI uses PRIORITIZE; tracking itself is automatic. */
 public String track(Contact t){return prioritize(t);}
 public double rangeQuality(){return range<=10?1:range<=20?.92:range<=30?.82:.70;}
 public int[] presets(){ArrayList<Integer> a=new ArrayList<>();for(int n:new int[]{10,20,30,40})if(n<=maxRange)a.add(n);if(maxRange>0&&!a.contains(maxRange))a.add(maxRange);int[] r=new int[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}
 public void cycleRange(){int[] a=presets();if(a.length==0){range=1;return;}int next=0;for(int i=0;i<a.length;i++)if(a[i]==range)next=(i+1)%a.length;range=a[next];}
 public ArrayList<Contact> targets(){ArrayList<Contact> a=new ArrayList<>(contacts);a.addAll(enemyMissiles);return a;}
 public int integrityUnits(){return (int)Math.ceil(battery.condition()/25);}
 @Deprecated public int hitsRemaining(){return integrityUnits();}
 public int identity(Contact t){if(t==null||elapsed-t.seen>7)return 0;if(t instanceof EnemyMissile)return t.samples<2?0:t.samples>=3&&t.measuredRange()<12?2:1;boolean enemy=t.launchObserved||(t.samples>=4&&t.confidence>=.3);return !enemy?0:t.confidence>=.75?2:1;}
 public String identityLabel(Contact t){return new String[]{"UNKNOWN TARGET","UNSURE ID / ENEMY","POSITIVE ID / ENEMY","POSITIVE ID / FRIENDLY"}[identity(t)];}
 public String contactName(Contact t){return t instanceof EnemyMissile?missileName((EnemyMissile)t):t.confidence>=.3?NAMES[t.guess]:"UNKNOWN";}
 public String tag(Contact t){return (t instanceof EnemyMissile?"M-":"T-")+t.id;}
 public Contact nextTarget(Contact current){ArrayList<Contact> a=new ArrayList<>();for(Contact t:targets())if(visible(t))a.add(t);a.sort((x,y)->Double.compare(x.measuredRange(),y.measuredRange()));return a.isEmpty()?null:a.get((a.indexOf(current)+1)%a.size());}
 public String payload(Contact t){return t.launchObserved?"WEAPON LAUNCH OBSERVED":t.confidence>.65&&t.guess!=1?"POSSIBLE A/G WEAPONS":"UNKNOWN PAYLOAD";}
 public String threat(Contact t){if(!tracked(t))return "UNCERTAIN";double approach=Math.cos(delta(t.pheading,angle(-t.px,-t.py)));return t.launchObserved||t.confidence>.6&&t.guess>=3?"HIGH":approach>.5&&t.measuredRange()<20?"ELEVATED":"UNCERTAIN";}
 boolean guiding(Contact t){for(EnemyMissile m:enemyMissiles)if(m.alive&&m.source==t&&needsGroundLink(equipment.enemy[m.weapon]))return true;return false;}
 void tryEnemyLaunch(Contact t){if(t.weapon<0||t.weaponFired||elapsed-t.born<30||maxRange<=0||t.pilot!=null&&t.pilot.state!=PilotAI.State.APPROACH)return;Equipment.Weapon w=equipment.enemy[t.weapon];double r=t.range();if(r>w.rangeKm||r<w.minRangeKm||t.alt>w.ceilingM||signal(t)==0||Math.abs(delta(t.heading,angle(-t.x,-t.y)))>.65)return;EnemyMissile m=new EnemyMissile();m.id=100+(++enemySerial);m.source=t;m.weapon=t.weapon;m.x=t.x;m.y=t.y;m.z=t.alt/1000;m.alt=t.alt;m.flight.profile=w;MissileMotion.aim(m.flight,-m.x,-m.y,terrain(0,0)/1000-m.z);enemyMissiles.add(m);t.weaponFired=true;if(liveTrack(t)){t.launchObserved=true;event("WEAPON LAUNCH DETECTED / TRACK "+t.id);}}
 public int incomingCount(){int n=0;for(EnemyMissile m:enemyMissiles)if(m.alive)n++;return n;}
 public boolean visible(EnemyMissile m){return visible((Contact)m);}
 public String missileName(EnemyMissile m){return m.samples>=3&&Math.hypot(m.px,m.py)<12?equipment.enemy[m.weapon].name:"UNKNOWN MSL";}
 public void damageAircraft(Contact t,double amount){if(t==null||!t.alive||t instanceof EnemyMissile||amount<=0)return;if(t.pilot==null)ai.assign(t,1,1,2);t.pilot.health=Math.max(0,t.pilot.health-amount);if(t.pilot.health==0)confirmIntercept(t,false);else{event("AIRCRAFT DAMAGED / "+tag(t));if(t.pilot.health<=30)ai.retreat(t);}}
 public void syncBattery(){health=(int)Math.ceil(battery.condition());maxRange=(int)Math.ceil(equipment.radar.detectionAbsoluteKm*battery.radarFactor());range=Math.max(1,Math.min(range,maxRange));
  for(int i=0;i<3;i++)if(!battery.parts[Battery.L1+i].alive()){Launcher l=launchers[i];lostAmmo+=l.ammo;l.ammo=0;if(l.reload>0){reserved=Math.max(0,reserved-3);l.reload=0;}}
  maintainTracks();
 }
 public double explode(double x,double y,double kg,double radius,String label){if(finished)return 0;double damage=battery.explode(x,y,kg,radius,elapsed);syncBattery();Blast blast=new Blast();blast.x=x;blast.y=y;blast.radius=radius;blast.time=elapsed;blast.damage=damage;blast.label=label;blasts.add(blast);
  if(damage>0){radarHits++;lastDamage=damage;impactUntil=elapsed+3;score-=100;event(label+" / "+battery.warning(elapsed));}else event("BLAST MISSED THE BATTERY");if(!battery.parts[Battery.COMMAND].alive())finish(false);return damage;
 }
 /** Compatibility hook for older harnesses; now a charge-based explosion. */
 public void damageRadar(){explode(0,0,87.1,.32,"MISSILE IMPACT");}
 public boolean releaseBombs(Contact t){if(!running||finished||!ai.canBomb(t))return false;PilotAI.Pilot p=t.pilot;int count=p.bombs;
  for(int i=0;i<count;i++){Bomb b=new Bomb();b.source=t;b.category=p.category;b.startX=b.x=t.x;b.startY=b.y=t.y;b.startZ=b.z=t.alt/1000;b.fall=clamp(3+(t.alt-terrain(t.x,t.y))/2000,2,10);double drift=Math.min(.35,t.speed/3600*b.fall*.2),scatter=.04+Math.min(.18,t.alt/50000);b.impactX=t.x+Math.sin(t.heading)*drift+random.nextGaussian()*scatter;b.impactY=t.y-Math.cos(t.heading)*drift+random.nextGaussian()*scatter;b.observed=visible(t);bombs.add(b);}
  ai.released(t,count);t.attacked=true;leaks++;if(visible(t)){t.launchObserved=true;event("BOMB RELEASE / "+tag(t));}return true;
 }
 public void flyBomb(Bomb b,double dt){if(!b.alive||dt<=0)return;b.age+=dt;double f=clamp(b.age/b.fall,0,1);b.x=b.startX+(b.impactX-b.startX)*f;b.y=b.startY+(b.impactY-b.startY)*f;b.z=b.startZ+(terrain(b.impactX,b.impactY)/1000-b.startZ)*f;if(f>=1){b.alive=false;double damage=explode(b.impactX,b.impactY,rules.explosiveKg[b.category],rules.radiusKm[b.category],"BOMB IMPACT");ai.bombOutcome(b.source,damage);}}
 public int bombCount(){int n=0;for(Bomb b:bombs)if(b.alive)n++;return n;}
 public double reloadSeconds(int i){double rate=battery.reloadRate(i,elapsed);return rate>0?launchers[i].reload/rate:Double.POSITIVE_INFINITY;}
 public void flyEnemy(EnemyMissile m,double dt){Equipment.Weapon w=equipment.enemy[m.weapon];m.age+=dt;Missile f=m.flight;f.x=m.x;f.y=m.y;f.z=m.z;f.speed=m.speed;f.age=m.age;
  double tz=terrain(0,0)/1000,d=Math.sqrt(m.x*m.x+m.y*m.y+Math.pow(tz-m.z,2));boolean link=m.source!=null&&m.source.alive&&signal(m.source)>0&&Math.abs(delta(m.source.heading,angle(-m.source.x,-m.source.y)))<.8;
  boolean guided;
  if(w.guidance==Equipment.Guidance.RADAR&&w.radarMode==Equipment.RadarMode.ACTIVE){if(d<=w.seekerKm)f.autonomous=true;m.radarEmitting=f.autonomous;guided=f.autonomous||link;}
  else if(w.guidance==Equipment.Guidance.RADAR&&w.radarMode==Equipment.RadarMode.PASSIVE)guided=battery.radarOnline(elapsed);
  else if(w.guidance==Equipment.Guidance.INFRARED)guided=d<=w.seekerKm;
  else guided=link;
  guided=guided&&m.age<=w.guidanceSeconds&&ir.clearLine(m.x,m.y,m.z,0,0,tz);if(!guided)m.lost+=dt;else m.lost=0;
  double ox=m.x,oy=m.y,oz=m.z;MissileMotion.step(f,w,0,0,tz,guided,dt);m.x=f.x;m.y=f.y;m.z=f.z;m.speed=f.speed;m.alt=m.z*1000;
  if(MissileMotion.nearest(ox,oy,oz,f,0,0,tz)<.08){m.alive=false;explode(m.x,m.y,w.explosiveKg,w.blastRadiusKm,"MISSILE IMPACT");return;}
  if(m.z*1000<terrain(m.x,m.y)){m.alive=false;explode(m.x,m.y,w.explosiveKg,w.blastRadiusKm,"MISSILE GROUND IMPACT");return;}
  if(m.lost>3||m.age>w.guidanceSeconds+3||f.path>w.rangeKm){m.alive=false;return;}
  double adv=dt*TAU/equipment.radar.sweepSeconds(),bearing=(angle(m.x,m.y)+TAU)%TAU;if((sweep-bearing+TAU)%TAU<adv)observe(m);
 }
}

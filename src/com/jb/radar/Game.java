package com.jb.radar;
import java.util.*;
/** A fictional, deliberately simplified game simulation. Not an operational model. */
public class Game {
 public static final double TAU=Math.PI*2;
 public static final String[] NAMES={"Su-25","MiG-21bis","MiG-23M","Su-22M3","Su-17M4"};
 public static final double[] MAX_SPEED={979,2240,2358,2232,2052}, LENGTH={14.2,14.1,16.7,18.9,18.9}, SPAN={14.4,7.2,14,13.7,13.7}, CEILING={11000,16000,16000,19500,19500};
 public static final double[] CRUISE={680,1080,1280,960,940}, TURN={.12,.28,.19,.17,.17}, ACCEL={13,28,35,24,24}, BASE_ALT={850,4800,6500,2300,2200};
 public static class Contact {
  public long priorityOrder;public int missed;public double missedAt=-1;public PilotAI.Pilot pilot;public int id,type,samples,weapon=-1,flareBursts=3; public double nextFlare; public boolean priority,radarTracked,radarEmitting,launchObserved,weaponFired; public double born,nextPriority; public double x,y,alt,speed,heading,climb,desiredAlt,evasion,side=1,seen=-999,px,py,palt,pspeed,psize,plen,pheading,pclimb,pturn,quality,confidence,previousHeading;
  public double[] probabilities=new double[6]; public int guess;public boolean alive=true,illuminated,attacked,escaped; public String outcome="";
  public double range(){return Math.hypot(x,y);}public double measuredRange(){return Math.hypot(px,py);}
 }
 public static class Missile {public boolean infrared,autonomous,datalink;public Equipment.Weapon profile;public double vx,vy,vz;public Infrared.Flare decoy;public Contact target;public double x,y,z,speed=.08,age,lost,path;public boolean alive=true;}
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
 public void toggleWeapon(){if(!equipment.radar.infrared){irMode=false;event("THIS BATTERY DOES NOT SUPPORT IR");return;}irMode=!irMode;ir.cancel();}
 public String weaponLock(Contact t){return irMode?ir.begin(t):illuminate(t);}
 public String weaponRelease(Contact t){if(irMode){ir.cancel();return "IR SEEKER OFF / FIRED MISSILES SELF GUIDE";}return release(t);}
 public String weaponBlock(Contact t){return irMode?ir.launchBlock(t):launchBlock(t);}
 public String fireWeapon(Contact t){return irMode?ir.launch(t):launch(t);}
 public final Economy economy;public final Equipment equipment;public final CombatRules rules;public final Battery battery;public final PilotAI ai;public Equipment.Weapon weapon;
 public Game(long seed){this(seed,new Economy(new Economy.MemoryStore()));}
 public Game(long seed,Economy economy){this(seed,economy,Equipment.defaults());}
 public Game(long seed,Economy economy,Equipment equipment){this(seed,economy,equipment,CombatRules.defaults());}
 public Game(long seed,Economy economy,Equipment equipment,CombatRules rules){this.rules=rules;battery=new Battery(rules);ai=new PilotAI(this);this.economy=economy;this.equipment=equipment;weapon=equipment.playerWeapon(economy.snapshot().equippedMissile);maxRange=range=equipment.radar.detectionKm;random=new Random(seed);for(int i=0;i<3;i++)launchers[i]=new Launcher();}
 public boolean start(){if(!economy.beginMission())return false;prioritySerial=0;weapon=equipment.playerWeapon(economy.snapshot().equippedMissile);ir.reset();ai.reset();battery.reset();lostAmmo=0;bombs.clear();blasts.clear();irMode=false;contacts.clear();missiles.clear();enemyMissiles.clear();maxRange=equipment.radar.detectionKm;radarHits=enemySerial=0;impactUntil=lastDamage=0;log.clear();elapsed=sweep=0;nextSpawn=48;reserve=9;reserved=spawned=kills=misses=leaks=score=shots=0;health=100;range=maxRange;chosenLauncher=0;running=true;finished=won=false;for(int i=0;i<3;i++)launchers[i]=new Launcher();spawn(0,34);spawn(1,46);event("MISSION 1 / DEFEND THE COMMAND SITE");return true;}
 public void event(String s){message=s;log.add(String.format(Locale.US,"%02d:%02d %s",(int)elapsed/60,(int)elapsed%60,s));if(log.size()>40)log.remove(0);}
 void spawn(int type,double distance){Contact t=new Contact();t.id=++spawned;t.type=type;t.radarEmitting=type==1||type==2;t.born=elapsed;t.weapon=type==1?-1:type==0?random.nextInt(2):type==2?2:type==3?3:4;double a=random.nextDouble()*TAU;t.x=Math.sin(a)*distance;t.y=-Math.cos(a)*distance;t.heading=angle(-t.x,-t.y);t.speed=CRUISE[type]*(.85+random.nextDouble()*.2);t.desiredAlt=BASE_ALT[type]*(.5+random.nextDouble());if(type>=3&&random.nextBoolean())t.desiredAlt=130+random.nextDouble()*300;t.alt=terrain(t.x,t.y)+t.desiredAlt;t.side=random.nextBoolean()?1:-1;ai.assign(t,1+random.nextInt(5),type==0?2:1,new int[]{2,1,3,2,0,4}[t.id%6]);contacts.add(t);}
 static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}static double angle(double x,double y){return Math.atan2(x,-y);}static double delta(double a,double b){return Math.atan2(Math.sin(a-b),Math.cos(a-b));}
 public static double terrain(double x,double y){double ridge1=680*Math.exp(-Math.pow((x-13)/3.5,2)-Math.pow((y+8)/15,2));double ridge2=1050*Math.exp(-Math.pow((x+19)/5,2)-Math.pow((y-11)/12,2));return 80+ridge1+ridge2;}
 public double signal(Contact t){if(!battery.radarOnline(elapsed))return 0;double r=t.range(),radarHeight=35,targetASL=t.alt;double horizon=4.12*(Math.sqrt(radarHeight)+Math.sqrt(Math.max(0,targetASL)));if(r>maxRange||r>horizon||maxRange<=0)return 0;double origin=terrain(0,0)+radarHeight;for(int i=1;i<24;i++){double f=i/24.0;if(terrain(t.x*f,t.y*f)>origin+(targetASL-origin)*f)return 0;}double agl=t.alt-terrain(t.x,t.y);return (agl<150?.32:agl<500?.58:agl<1500?.82:1)*(.35+.65*battery.parts[Battery.RADAR].hp/100);}
 public double trackingRange(){return Math.min(maxRange,equipment.radar.trackingKm*battery.radarFactor());}
 public double lockRange(){return Math.min(maxRange,equipment.radar.lockKm*battery.radarFactor());}
 public double freshSeconds(){return equipment.radar.sweepSeconds()*1.15;}
 public double trackExpiry(){return Math.max(22,equipment.radar.sweepSeconds()*2.2);}
 public double contactExpiry(){return Math.max(32,equipment.radar.sweepSeconds()*3.2);}
 public boolean detected(Contact t){return t!=null&&t.alive&&t.samples>0&&elapsed-t.seen<contactExpiry();}
 public boolean stale(Contact t){return t!=null&&(t.missed>0||elapsed-t.seen>freshSeconds());}
 public boolean tracked(Contact t){return detected(t)&&t.radarTracked&&elapsed-t.seen<trackExpiry()&&t.measuredRange()<=trackingRange()&&battery.radarOnline(elapsed);}
 public boolean liveTrack(Contact t){return tracked(t)&&!stale(t);}
 public boolean visible(Contact t){return detected(t)&&t.measuredRange()<=range;}
 public int trackedCount(){int n=0;for(Contact t:targets())if(tracked(t))n++;return n;}
 public boolean protectedTrack(Contact t){if(t.illuminated)return true;for(Missile m:missiles)if(m.alive&&m.target==t)return true;return false;}
 boolean candidate(Contact t){return detected(t)&&t.measuredRange()<=trackingRange()&&(tracked(t)||!stale(t));}
 int priorityRank(Contact t){return t.radarTracked&&protectedTrack(t)?0:t.priority?1:t instanceof EnemyMissile?2:3;}
 int comparePriority(Contact a,Contact b){int n=Integer.compare(priorityRank(a),priorityRank(b));if(n!=0)return n;if(a.priority&&b.priority){n=Long.compare(b.priorityOrder,a.priorityOrder);if(n!=0)return n;}n=Double.compare(a.measuredRange(),b.measuredRange());return n!=0?n:Integer.compare(a.id,b.id);}
 void acquireTrack(Contact t){maintainTracks();}
 void maintainTracks(){ArrayList<Contact> all=targets(),eligible=new ArrayList<>();boolean online=battery.radarOnline(elapsed);
  for(Contact t:all){if(!online||!detected(t)||elapsed-t.seen>=trackExpiry()||t.measuredRange()>trackingRange())t.radarTracked=false;
   if(t.illuminated&&(!liveTrack(t)||slant(t)>lockRange())){t.illuminated=false;event("CHANNEL LOST / "+tag(t));}
   if(online&&candidate(t))eligible.add(t);
  }
  eligible.sort((a,b)->comparePriority(a,b));HashSet<Contact> chosen=new HashSet<>();for(int i=0;i<Math.min(equipment.radar.tracks,eligible.size());i++)chosen.add(eligible.get(i));
  for(Contact t:all){boolean assigned=chosen.contains(t);if(assigned&&!t.radarTracked)ai.warn(t,PilotAI.Warning.TRACK,rules.trackingCue&&signal(t)>0);t.radarTracked=assigned;}
 }
 public String trackState(Contact t){if(t!=null&&ir.target==t&&ir.locked)return "IR LOCK";if(t==null)return "NO CONTACT";if(!t.alive)return t.outcome;if(!detected(t))return "CONTACT LOST";if(stale(t))return tracked(t)?"STALE TRACK":"STALE CONTACT";return hardLocked(t)?"LOCKED":tracked(t)?"AUTO TRACK":"DETECTED";}
 public boolean hardLocked(Contact t){return liveTrack(t)&&t.illuminated&&slant(t)<=lockRange();}
 /** UI positions depend only on the last radar measurement, never target truth. */
 public double predictionAge(Contact t){if(!tracked(t))return 0;double age=Math.min(Math.max(0,elapsed-t.seen),freshSeconds());if(t.missedAt>=t.seen)age=Math.min(age,t.missedAt-t.seen);return age;}
 public double displayX(Contact t){return t.px+Math.sin(t.pheading)*t.pspeed/3600*predictionAge(t);}
 public double displayY(Contact t){return t.py-Math.cos(t.pheading)*t.pspeed/3600*predictionAge(t);}
 public double displayRange(Contact t){return Math.hypot(displayX(t),displayY(t));}
 public String altitudeText(Contact t){return tracked(t)?String.format(Locale.US,"%.0f",t.palt):"---";}
 public String speedText(Contact t){return tracked(t)?String.format(Locale.US,"%.0f",t.pspeed):"---";}
 public String directionText(Contact t){return tracked(t)?String.format(Locale.US,"%03d",((int)Math.toDegrees(t.pheading)%360+360)%360):"---";}
 public static boolean activeHoming(Equipment.Weapon w){return w.guidance==Equipment.Guidance.RADAR&&w.radarMode==Equipment.RadarMode.ACTIVE;}
 boolean canUpdate(Contact t){return liveTrack(t)&&slant(t)<=trackingRange()&&signal(t)>.15;}
 /** One shared channel per supported target, including hard locks and active midcourse updates. */
 LinkedHashSet<Contact> refreshDatalinks(){LinkedHashSet<Contact> occupied=new LinkedHashSet<>();
  if(needsGroundLink(weapon))for(Contact t:targets())if(hardLocked(t))occupied.add(t);
  for(Missile m:missiles){m.datalink=false;Equipment.Weapon w=m.profile==null?weapon:m.profile;
   if(m.alive&&!m.infrared&&!m.autonomous&&activeHoming(w)&&m.age<=w.guidanceSeconds&&canUpdate(m.target)){
    if(occupied.contains(m.target)||occupied.size()<equipment.radar.channels){occupied.add(m.target);m.datalink=true;}
   }
  }return occupied;
 }
 public int channels(){return refreshDatalinks().size();}
 public boolean channelAvailable(Contact t){LinkedHashSet<Contact> used=refreshDatalinks();return used.contains(t)||used.size()<equipment.radar.channels;}
 public String missileState(Missile m){Equipment.Weapon w=m.profile==null?weapon:m.profile;if(m.infrared)return "IR";if(activeHoming(w)){refreshDatalinks();return m.autonomous?"ACTIVE":m.datalink?"DATALINK":"LINK LOST";}return m.lost>0?"LOCK LOST":"SARH";}
 public static boolean needsGroundLink(Equipment.Weapon w){return w.guidance==Equipment.Guidance.COMMAND||w.guidance==Equipment.Guidance.LASER||w.guidance==Equipment.Guidance.RADAR&&w.radarMode!=Equipment.RadarMode.PASSIVE;}
 public double slant(Contact t){return Math.hypot(t.measuredRange(),(t.palt-terrain(0,0))/1000);}
 public String lockBlock(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!battery.radarOnline(elapsed))return battery.warning(elapsed);if(!tracked(t))return "AUTO TRACK REQUIRED / PRIORITIZE CONTACT";if(!liveTrack(t))return "TRACK STALE / WAIT FOR RADAR";if(slant(t)>lockRange())return "OUTSIDE RADAR LOCK RANGE";if(t.illuminated)return "TARGET ALREADY LOCKED";if(weapon.radarMode==Equipment.RadarMode.PASSIVE&&!t.radarEmitting)return "NO RADAR EMISSION TO LOCK";if(needsGroundLink(weapon)&&!channelAvailable(t))return "NO DATALINK CHANNEL AVAILABLE";return null;}
 public int inbound(Contact t){int n=0;for(Missile m:missiles)if(m.alive&&m.target==t)n++;return n;}
 public String illuminate(Contact t){String block=lockBlock(t);if(block!=null)return block;t.illuminated=true;if(needsGroundLink(weapon))ai.warn(t,PilotAI.Warning.LOCK,signal(t)>0);event("LOCK ESTABLISHED / TRACK "+t.id);return message;}
 public String release(Contact t){if(t==null||!t.illuminated)return "NO CHANNEL TO RELEASE";t.illuminated=false;event(tracked(t)?"LOCK RELEASED / TRACK MAINTAINED / "+tag(t):"LOCK RELEASED / TRACK LOST / "+tag(t));return message;}
 public String launchBlock(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!battery.powered(elapsed))return "POWER FAILURE";if(!battery.parts[Battery.L1+chosenLauncher].alive())return "LAUNCHER DISABLED";if(!battery.radarOnline(elapsed))return battery.warning(elapsed);if(!tracked(t))return "TRACK REQUIRED";if(!liveTrack(t))return "TRACK STALE / WAIT FOR RADAR";
  Equipment.Weapon w=weapon;boolean active=activeHoming(w);if(!active&&!t.illuminated)return "LOCK REQUIRED";double slant=slant(t);
  if(slant>Math.min(active?trackingRange():lockRange(),w.rangeKm))return "OUT OF RANGE";if(slant<w.minRangeKm)return "INSIDE MINIMUM RANGE";if(t.palt>w.ceilingM)return "ABOVE MISSILE CEILING";
  if(needsGroundLink(w)&&!channelAvailable(t))return "NO DATALINK CHANNEL AVAILABLE";
  Launcher l=launchers[chosenLauncher];if(l.reload>0)return "SELECTED LAUNCHER IS RELOADING";if(l.ammo==0)return "SELECTED LAUNCHER IS EMPTY";return null;
 }
 public String launch(Contact t){String block=launchBlock(t);if(block!=null)return block;Launcher l=launchers[chosenLauncher];l.ammo--;shots++;Missile m=new Missile();m.target=t;m.profile=weapon;m.z=(terrain(0,0)+5)/1000;MissileMotion.aim(m,t.x,t.y,t.alt/1000-m.z);missiles.add(m);refreshDatalinks();ai.launchWarning(t,false);if(l.ammo==0&&reserve-reserved>=3){l.reload=90;reserved+=3;}event("MISSILE AWAY / TRACK "+t.id);return message;}
 public void observe(Contact t){if(elapsed-t.seen<battery.processingDelay())return;double q=signal(t);
  if(q==0||random.nextDouble()>q){t.missed++;if(t.missedAt<0)t.missedAt=elapsed;return;}
  double r=t.range(),error=(.02+r/160)*(2-q)*(2.2-rangeQuality());double previousSeen=t.seen,previousBearing=t.pheading;t.seen=elapsed;t.missed=0;t.missedAt=-1;t.samples++;
  t.px=t.x+random.nextGaussian()*error*.45;t.py=t.y+random.nextGaussian()*error*.45;
  double speed=t instanceof EnemyMissile?t.speed*3600:t.speed,heading=t instanceof EnemyMissile?angle(((EnemyMissile)t).flight.vx,((EnemyMissile)t).flight.vy):t.heading;
  t.pspeed=Math.max(100,speed*(1+random.nextGaussian()*error*.12));t.palt=Math.max(20,t.alt+random.nextGaussian()*error*300);t.pheading=heading+random.nextGaussian()*error*.12;t.pclimb=t.climb+random.nextGaussian()*error*4;t.pturn=t.samples>1?Math.abs(delta(t.pheading,previousBearing))/Math.max(1,elapsed-previousSeen):0;t.quality=q;
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
   t.x+=Math.sin(t.heading)*t.speed/3600*dt;t.y-=Math.cos(t.heading)*t.speed/3600*dt;double ground=terrain(t.x,t.y),agl=defensive&&(t.type==0||t.type>=3)?90:t.desiredAlt;t.climb=clamp((ground+agl-t.alt)*.12,-(t.type==0?22:60),t.type==0?16:45);t.alt=clamp(t.alt+t.climb*dt,ground+40,CEILING[t.type]);t.previousHeading=oldHeading;
   double bearing=(angle(t.x,t.y)+TAU)%TAU;if((sweep-bearing+TAU)%TAU<adv)observe(t);tryEnemyLaunch(t);releaseBombs(t);
   if(t.illuminated&&(!liveTrack(t)||slant(t)>lockRange())){t.illuminated=false;event("CHANNEL LOST / TRACK "+t.id);}
   if((t.pilot!=null&&(t.pilot.state==PilotAI.State.EXIT||t.pilot.state==PilotAI.State.RETREAT)||t.pilot==null&&t.weaponFired)&&t.range()>60){ai.exited(t);t.alive=false;t.illuminated=false;t.escaped=true;t.outcome="WITHDREW";}
   if(t.pilot==null&&t.range()<2&&!t.attacked){t.attacked=true;leaks++;score-=100;t.alive=false;t.illuminated=false;t.outcome="REACHED SITE";event("AIRCRAFT PASSED THE SITE / TRACK "+t.id);}
  }
  maintainTracks();ir.tick(dt);for(Missile m:missiles)if(m.alive)fly(m,dt);for(EnemyMissile m:enemyMissiles)if(m.alive&&!finished)flyEnemy(m,dt);for(Bomb b:bombs)if(b.alive&&!finished)flyBomb(b,dt);for(Iterator<Blast> it=blasts.iterator();it.hasNext();)if(elapsed-it.next().time>3)it.remove();
  maintainTracks();if(!battery.parts[Battery.COMMAND].alive())finish(false);else if(spawned==12&&aliveCount()==0&&incomingCount()==0&&bombCount()==0)finish(true);else if(elapsed>=600&&incomingCount()==0&&bombCount()==0)finish(true);
 }
 void finish(boolean victory){if(finished)return;economy.finish(victory,integrityUnits());finished=true;running=false;won=victory;event(victory?"MISSION COMPLETE / SITE SURVIVED":"MISSION FAILED / COMMAND UNIT DESTROYED");}
 public int aliveCount(){int n=0;for(Contact t:contacts)if(t.alive)n++;return n;}
 public int ready(){int n=0;for(Launcher l:launchers)n+=l.ammo;return n;}
 public boolean guided(Missile m){Contact t=m.target;Equipment.Weapon w=m.profile==null?weapon:m.profile;if(t==null||!t.alive||m.age>w.guidanceSeconds)return false;if(w.guidance==Equipment.Guidance.RADAR&&ai.notchBreak(t))return false;
  boolean line=ir.lineOfSight(m.x,m.y,m.z,t);double d=Math.sqrt(Math.pow(t.x-m.x,2)+Math.pow(t.y-m.y,2)+Math.pow(t.alt/1000-m.z,2));
  if(w.radarMode==Equipment.RadarMode.PASSIVE)return t.radarEmitting&&line;
  if(activeHoming(w)){
   if(!m.autonomous&&d<=w.seekerKm&&line){m.autonomous=true;m.datalink=false;event(w.name+" / ACTIVE SEEKER");}
   if(m.autonomous)return line;
   refreshDatalinks();return m.datalink&&canUpdate(t)&&line;
  }
  boolean link=hardLocked(t)&&signal(t)>.15;
  return w.guidance==Equipment.Guidance.COMMAND?link:link&&line;
 }
 public void fly(Missile m,double dt){if(m.infrared){ir.fly(m,dt);return;}Contact t=m.target;Equipment.Weapon w=m.profile==null?weapon:m.profile;m.age+=dt;boolean guided=guided(m);if(guided)m.lost=Math.max(0,m.lost-dt*2);else m.lost+=dt;
  double tx=t.x,ty=t.y,tz=t.alt/1000,ox=m.x,oy=m.y,oz=m.z;MissileMotion.step(m,w,tx,ty,tz,guided,dt);double nearest=MissileMotion.nearest(ox,oy,oz,m,tx,ty,tz);
  if(t.alive&&guided&&nearest<.12&&m.age>1){m.alive=false;confirmIntercept(t,false);}
  else if(t.pilot!=null&&guided&&nearest<.25&&m.age>1){m.alive=false;damageAircraft(t,25+70*(1-clamp((nearest-.12)/.13,0,1)));if(t.alive)misses++;}
  else if(m.lost>4||m.age>w.guidanceSeconds+4||m.path>w.rangeKm||(m.age>1&&m.z*1000<terrain(m.x,m.y))||!t.alive){m.alive=false;misses++;event(w.name+" LOST / TRACK "+t.id);}
 }
 public void confirmIntercept(Contact t,boolean infrared){if(!t.alive||finished)return;economy.intercept(tag(t),t instanceof EnemyMissile);ai.destroyed(t);t.alive=false;t.illuminated=false;t.outcome=infrared?"IR INTERCEPT":"INTERCEPTED";kills++;score+=250;maintainTracks();event((infrared?"IR INTERCEPT / ":"INTERCEPT / ")+tag(t));}
 public String prioritize(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!battery.radarOnline(elapsed))return battery.warning(elapsed);if(!visible(t))return "SELECT A DETECTED CONTACT";if(t.priority){t.priority=false;t.priorityOrder=0;maintainTracks();event("AUTOMATIC PRIORITY / "+tag(t));return message;}if(t.measuredRange()>trackingRange())return "OUTSIDE TRACKING RANGE";if(!tracked(t)&&stale(t))return "CONTACT STALE / WAIT FOR RADAR";
  maintainTracks();if(!tracked(t)&&trackedCount()>=equipment.radar.tracks){boolean replaceable=false;for(Contact other:targets())if(tracked(other)&&!protectedTrack(other))replaceable=true;if(!replaceable)return "ALL TRACKS PROTECTED / NO SLOT";}
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
 public void syncBattery(){health=(int)Math.ceil(battery.condition());maxRange=(int)Math.ceil(equipment.radar.detectionKm*battery.radarFactor());range=Math.max(1,Math.min(range,maxRange));
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

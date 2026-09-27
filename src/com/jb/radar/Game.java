package com.jb.radar;
import java.util.*;
/** A fictional, deliberately simplified game simulation. Not an operational model. */
public class Game {
 public static final double TAU=Math.PI*2;
 public static final String[] NAMES={"Su-25","MiG-21bis","MiG-23M","Su-22M3","Su-17M4"};
 public static final double[] MAX_SPEED={979,2240,2358,2232,2052}, LENGTH={14.2,14.1,16.7,18.9,18.9}, SPAN={14.4,7.2,14,13.7,13.7}, CEILING={11000,16000,16000,19500,19500};
 public static final double[] CRUISE={680,1080,1280,960,940}, TURN={.12,.28,.19,.17,.17}, ACCEL={13,28,35,24,24}, BASE_ALT={850,4800,6500,2300,2200};
 public static class Contact {
  public int id,type,samples,weapon=-1; public boolean priority,launchObserved,weaponFired; public double born,nextPriority; public double x,y,alt,speed,heading,climb,desiredAlt,evasion,side=1,seen=-999,px,py,palt,pspeed,psize,plen,pheading,pclimb,pturn,quality,confidence,previousHeading;
  public double[] probabilities=new double[6]; public int guess;public boolean alive=true,illuminated,attacked,escaped; public String outcome="";
  public double range(){return Math.hypot(x,y);}public double measuredRange(){return Math.hypot(px,py);}
 }
 public static class Missile {public Contact target;public double x,y,z,speed=.08,age,lost,path;public boolean alive=true;}
 public static class EnemyMissile {public int id,weapon,samples;public Contact source;public double x,y,z,speed=.18,age,lost,seen=-999,px,py,pz;public boolean alive=true;}
 public static final String[] WEAPONS={"Kh-25ML","Kh-29L","Kh-23M","Kh-27PS","Kh-58U"};
 // ARM envelopes below are fictional mission-balancing values, not operational data.
 public static final double[] WEAPON_RANGE={10,13,10,26,32}, WEAPON_TIME={45,40,25,70,80}, WEAPON_SPEED={.68,.61,.68,.72,.85};
 public ArrayList<EnemyMissile> enemyMissiles=new ArrayList<>();public int maxRange=40,enemySerial,radarHits;public double impactUntil,lastDamage;
 public static class Launcher {public int ammo=3;public double reload;}
 public Random random;public ArrayList<Contact> contacts=new ArrayList<>();public ArrayList<Missile> missiles=new ArrayList<>();public ArrayList<String> log=new ArrayList<>();public Launcher[] launchers=new Launcher[3];
 public double elapsed,sweep,nextSpawn;public int reserve=9,reserved=0,spawned,kills,misses,leaks,health=100,score,shots,range=40,chosenLauncher=0;public boolean running,finished,won;public String message="";
 public Game(long seed){random=new Random(seed);for(int i=0;i<3;i++)launchers[i]=new Launcher();}
 public void start(){contacts.clear();missiles.clear();enemyMissiles.clear();maxRange=40;radarHits=enemySerial=0;impactUntil=lastDamage=0;vectors.clear();log.clear();elapsed=sweep=0;nextSpawn=48;reserve=9;reserved=spawned=kills=misses=leaks=score=shots=0;health=100;range=40;chosenLauncher=0;running=true;finished=won=false;for(int i=0;i<3;i++)launchers[i]=new Launcher();spawn(0,34);spawn(1,46);event("MISSION 1 / DEFEND THE COMMAND SITE");}
 public void event(String s){message=s;log.add(String.format(Locale.US,"%02d:%02d %s",(int)elapsed/60,(int)elapsed%60,s));if(log.size()>40)log.remove(0);}
 void spawn(int type,double distance){Contact t=new Contact();t.id=++spawned;t.type=type;t.born=elapsed;t.weapon=type==1?-1:type==0?random.nextInt(2):type==2?2:type==3?3:4;double a=random.nextDouble()*TAU;t.x=Math.sin(a)*distance;t.y=-Math.cos(a)*distance;t.heading=angle(-t.x,-t.y);t.speed=CRUISE[type]*(.85+random.nextDouble()*.2);t.desiredAlt=BASE_ALT[type]*(.5+random.nextDouble());if(type>=3&&random.nextBoolean())t.desiredAlt=130+random.nextDouble()*300;t.alt=terrain(t.x,t.y)+t.desiredAlt;t.side=random.nextBoolean()?1:-1;contacts.add(t);}
 static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}static double angle(double x,double y){return Math.atan2(x,-y);}static double delta(double a,double b){return Math.atan2(Math.sin(a-b),Math.cos(a-b));}
 public static double terrain(double x,double y){double ridge1=680*Math.exp(-Math.pow((x-13)/3.5,2)-Math.pow((y+8)/15,2));double ridge2=1050*Math.exp(-Math.pow((x+19)/5,2)-Math.pow((y-11)/12,2));return 80+ridge1+ridge2;}
 public double signal(Contact t){double r=t.range(),radarHeight=35,targetASL=t.alt;double horizon=4.12*(Math.sqrt(radarHeight)+Math.sqrt(Math.max(0,targetASL)));if(r>maxRange||r>horizon||maxRange<=0)return 0;double origin=terrain(0,0)+radarHeight;for(int i=1;i<24;i++){double f=i/24.0;if(terrain(t.x*f,t.y*f)>origin+(targetASL-origin)*f)return 0;}double agl=t.alt-terrain(t.x,t.y);return agl<150?.32:agl<500?.58:agl<1500?.82:1;}
 public boolean liveTrack(Contact t){return t!=null&&t.alive&&t.samples>=2&&elapsed-t.seen<=7&&t.measuredRange()<=maxRange;}
 public boolean visible(Contact t){return t!=null&&t.alive&&t.samples>0&&elapsed-t.seen<22&&t.measuredRange()<=Math.min(range,maxRange);}
 public String trackState(Contact t){if(t==null)return "NO TRACK";if(!t.alive)return t.outcome;if(elapsed-t.seen>22)return "TRACK LOST";if(elapsed-t.seen>7)return "COASTING";return t.illuminated?"LOCKED":t.priority?"TRACKING":t.samples<2?"TENTATIVE":"DETECTED";}
 public int channels(){int n=0;for(Contact t:contacts)if(t.alive&&t.illuminated)n++;return n;}
 public int inbound(Contact t){int n=0;for(Missile m:missiles)if(m.alive&&m.target==t)n++;return n;}
 public String illuminate(Contact t){if(t==null||!t.priority)return "TRACK THE TARGET BEFORE LOCKING";if(!liveTrack(t))return "ESTABLISH TRACK / WAIT FOR TWO SWEEPS";if(t.illuminated)return "TARGET ALREADY ILLUMINATED";if(channels()>=2)return "BOTH ENGAGEMENT CHANNELS ARE OCCUPIED";t.illuminated=true;if(random.nextDouble()<.75)t.evasion=5+random.nextDouble()*7;event("LOCK ESTABLISHED / TRACK "+t.id);return message;}
 public String release(Contact t){if(t==null||!t.illuminated)return "NO CHANNEL TO RELEASE";t.illuminated=false;event("CHANNEL RELEASED / TRACK "+t.id);return message;}
 public String launchBlock(Contact t){if(!running)return "MISSION NOT ACTIVE";if(!liveTrack(t))return "NO VALID TARGET TRACK";if(!t.illuminated)return "LOCK THE TARGET FIRST";double slant=Math.hypot(t.measuredRange(),t.palt/1000);if(slant>25)return "OUTSIDE 25 KM ENGAGEMENT RANGE";if(t.palt>13700)return "ABOVE 13,700 M CEILING";Launcher l=launchers[chosenLauncher];if(l.reload>0)return "SELECTED LAUNCHER IS RELOADING";if(l.ammo==0)return "SELECTED LAUNCHER IS EMPTY";return null;}
 public String launch(Contact t){String block=launchBlock(t);if(block!=null)return block;Launcher l=launchers[chosenLauncher];l.ammo--;shots++;Missile m=new Missile();m.target=t;m.z=(terrain(0,0)+5)/1000;missiles.add(m);t.evasion=12+random.nextDouble()*7;if(l.ammo==0&&reserve-reserved>=3){l.reload=90;reserved+=3;}event("MISSILE AWAY / TRACK "+t.id);return message;}
 public void observe(Contact t){double q=signal(t);if(t.range()>range)return;if(q==0||random.nextDouble()>q)return;double r=t.range(),error=(.02+r/160)*(2-q)*(2.2-rangeQuality())*(t.priority?.60:1);double previousSeen=t.seen,previousBearing=t.pheading;t.seen=elapsed;t.samples++;t.px=t.x+random.nextGaussian()*error*.45;t.py=t.y+random.nextGaussian()*error*.45;t.pspeed=Math.max(100,t.speed*(1+random.nextGaussian()*error*.12));t.palt=Math.max(20,t.alt+random.nextGaussian()*error*300);t.pheading=t.heading+random.nextGaussian()*error*.12;t.pclimb=t.climb+random.nextGaussian()*error*4;t.pturn=t.samples>1?Math.abs(delta(t.pheading,previousBearing))/Math.max(1,elapsed-previousSeen):0;
  double span=SPAN[t.type];if(t.type==2)span=14-6.2*clamp((t.speed-650)/650,0,1);else if(t.type>=3)span=13.7-3.7*clamp((t.speed-600)/700,0,1);double aspect=Math.abs(Math.sin(t.heading-angle(-t.x,-t.y)));t.psize=Math.max(3,span*(.82+.18*aspect)+random.nextGaussian()*error*8);t.plen=Math.max(8,LENGTH[t.type]+random.nextGaussian()*error*5);t.quality=q;identify(t);
 }
 public void identify(Contact t){double r=t.measuredRange(),noise=1+r/28,history=Math.min(1,t.samples/12.0);double[] weights=new double[5];double sum=0;for(int i=0;i<5;i++){double speedFit=Math.exp(-Math.pow((t.pspeed-CRUISE[i])/(250*noise),2));if(t.pspeed>MAX_SPEED[i]*1.12)speedFit*=.01;double span=SPAN[i];if(i==2)span=14-6.2*clamp((t.pspeed-650)/650,0,1);else if(i>=3)span=13.7-3.7*clamp((t.pspeed-600)/700,0,1);double sizeFit=Math.exp(-Math.pow((t.psize-span*.9)/(2.2*noise),2)-Math.pow((t.plen-LENGTH[i])/(2*noise),2));double altFit=Math.exp(-Math.abs(t.palt-BASE_ALT[i])/(3500*noise));if(t.palt>CEILING[i])altFit*=.1;double behavior=Math.exp(-Math.abs(t.pclimb)/(i==0?22:60))*Math.exp(-Math.max(0,t.pturn-TURN[i])/(.04*noise));double match=.30*speedFit+.25*sizeFit+.20*altFit+.15*behavior+.10*history;weights[i]=Math.exp(match*12);sum+=weights[i];}
  double quality=(.4+.6*(1-clamp(r/90,0,1)))*(.55+.45*t.quality)*(.65+.35*history);quality*=rangeQuality()*(1-Math.min(.25,t.pturn*1.5));int top=0;for(int i=0;i<5;i++){t.probabilities[i]=weights[i]/sum*quality;if(t.probabilities[i]>t.probabilities[top])top=i;}t.probabilities[5]=1-quality;t.guess=top;t.confidence=t.probabilities[top];
 }
 public String identification(Contact t){if(t==null||t.samples<2)return "UNKNOWN AIR CONTACT";double conf=t.confidence*Math.max(.3,1-Math.max(0,elapsed-t.seen-5)/30);int pc=(int)(conf*100);if(pc<30)return "UNKNOWN / "+pc+"%";String label=pc<50?"POSSIBLE":pc<75?"LIKELY":pc<90?"PROBABLE":"HIGH CONF.";return label+" "+NAMES[t.guess]+" "+pc+"%";}
 public void tick(double dt){if(!running||finished)return;dt=clamp(dt,0,.1);if(dt==0)return;elapsed+=dt;double adv=dt*TAU/5;sweep=(sweep+adv)%TAU;
  for(Launcher l:launchers)if(l.reload>0){l.reload=Math.max(0,l.reload-dt);if(l.reload==0){reserve-=3;reserved-=3;l.ammo=3;event("LAUNCHER RELOAD COMPLETE");}}
  if(spawned<12&&elapsed>=nextSpawn){int count=spawned<5?1:2;for(int i=0;i<count&&spawned<12;i++)spawn((spawned+1)%5,40+random.nextDouble()*18);nextSpawn=elapsed+48;event("NEW CONTACTS APPROACHING THE SECTOR");}
  for(Contact t:contacts)if(t.alive){double oldHeading=t.heading;boolean guiding=guiding(t);double desired=t.weaponFired&&!guiding?angle(t.x,t.y):angle(-t.x,-t.y);boolean defensive=t.evasion>0;if(defensive){t.evasion=Math.max(0,t.evasion-dt);desired+=t.side*(t.type==1?1.7:t.type==2?1.0:1.35);}
   double turnLimit=Math.min(guiding?.035:TURN[t.type],(t.type==0?5:8)*9.81/(t.speed/3.6));double turn=clamp(delta(desired,t.heading),-turnLimit*dt,turnLimit*dt);t.heading+=turn;double hard=Math.abs(turn)/dt;double desiredSpeed=CRUISE[t.type]*(defensive&&t.type==2?1.25:1);double acceleration=clamp(desiredSpeed-t.speed,-25,ACCEL[t.type]);t.speed=clamp(t.speed+(acceleration-hard*(t.type==1?175:100))*dt,260,Math.min(MAX_SPEED[t.type],t.alt<1500?1250:MAX_SPEED[t.type]));
   t.x+=Math.sin(t.heading)*t.speed/3600*dt;t.y-=Math.cos(t.heading)*t.speed/3600*dt;double ground=terrain(t.x,t.y),agl=defensive&&(t.type==0||t.type>=3)?90:t.desiredAlt;t.climb=clamp((ground+agl-t.alt)*.12,-(t.type==0?22:60),t.type==0?16:45);t.alt=clamp(t.alt+t.climb*dt,ground+40,CEILING[t.type]);t.previousHeading=oldHeading;
   double bearing=(angle(t.x,t.y)+TAU)%TAU;if((sweep-bearing+TAU)%TAU<adv)observe(t);if(t.priority&&elapsed>=t.nextPriority){observe(t);t.nextPriority=elapsed+1.5;}tryEnemyLaunch(t);
   if(t.illuminated&&elapsed-t.seen>22){t.illuminated=false;event("CHANNEL LOST / TRACK "+t.id);}
   if(t.weaponFired&&t.range()>60){t.alive=false;t.illuminated=false;t.escaped=true;t.outcome="WITHDREW";}
   if(t.range()<2&&!t.attacked){t.attacked=true;leaks++;score-=100;t.alive=false;t.illuminated=false;t.outcome="REACHED SITE";event("AIRCRAFT PASSED THE SITE / TRACK "+t.id);}
  }
  for(Missile m:missiles)if(m.alive)fly(m,dt);for(EnemyMissile m:enemyMissiles)if(m.alive)flyEnemy(m,dt);
  if(maxRange<=0)finish(false);else if(spawned==12&&aliveCount()==0&&incomingCount()==0)finish(true);else if(elapsed>=600&&incomingCount()==0)finish(maxRange>0);
 }
 void finish(boolean victory){finished=true;running=false;won=victory;event(victory?"MISSION COMPLETE / SITE SURVIVED":"MISSION FAILED / RADAR SYSTEM DESTROYED");}
 public int aliveCount(){int n=0;for(Contact t:contacts)if(t.alive)n++;return n;}
 public int ready(){int n=0;for(Launcher l:launchers)n+=l.ammo;return n;}
 public void fly(Missile m,double dt){Contact t=m.target;m.age+=dt;boolean guided=t.alive&&t.illuminated&&signal(t)>.15;if(guided)m.lost=Math.max(0,m.lost-dt*2);else m.lost+=dt;
  double speedCap=2.5*(340-.004*Math.min(11000,m.z*1000))/1000;
  if(m.age<5)m.speed=Math.min(speedCap,m.speed+.145*dt);else if(m.age>18)m.speed=Math.max(.16,m.speed-.009*dt);
  double tx=t.x,ty=t.y,tz=t.alt/1000,dx=tx-m.x,dy=ty-m.y,dz=tz-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
  // Arcade pursuit with finite steering/energy; intentionally not a real guidance law.
  if(!vectors.containsKey(m))vectors.put(m,new double[]{0,0,1});double[] v=vectors.get(m);if(guided&&d>0){double blend=Math.min(1,dt*(m.age<18?2.8:1.0));v[0]+=(dx/d-v[0])*blend;v[1]+=(dy/d-v[1])*blend;v[2]+=(dz/d-v[2])*blend;}
  double norm=Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);double ox=m.x,oy=m.y,oz=m.z;m.x+=v[0]/norm*m.speed*dt;m.y+=v[1]/norm*m.speed*dt;m.z+=v[2]/norm*m.speed*dt;m.path+=m.speed*dt;
  double sx=m.x-ox,sy=m.y-oy,sz=m.z-oz,len=sx*sx+sy*sy+sz*sz;double f=len==0?0:clamp(((tx-ox)*sx+(ty-oy)*sy+(tz-oz)*sz)/len,0,1);double nearest=Math.sqrt(Math.pow(tx-ox-f*sx,2)+Math.pow(ty-oy-f*sy,2)+Math.pow(tz-oz-f*sz,2));
  if(t.alive&&guided&&nearest<.12&&m.age>1){m.alive=false;t.alive=false;t.illuminated=false;t.outcome="INTERCEPTED";kills++;score+=250;event("INTERCEPT CONFIRMED / TRACK "+t.id);}
  else if(m.lost>4||m.age>65||m.path>32||(m.age>1&&m.z*1000<terrain(m.x,m.y))||!t.alive){m.alive=false;misses++;event("MISSILE LOST / TRACK "+t.id);}
  if(!m.alive)vectors.remove(m);
 }
 public String track(Contact t){if(!visible(t))return "SELECT A DETECTED AIRCRAFT";t.priority=true;t.nextPriority=elapsed;event("PRIORITY TRACK / "+t.id);return message;}
 public double rangeQuality(){return range<=10?1:range<=20?.92:range<=30?.82:.70;}
 public int[] presets(){ArrayList<Integer> a=new ArrayList<>();for(int n:new int[]{10,20,30,40})if(n<=maxRange)a.add(n);if(maxRange>0&&!a.contains(maxRange))a.add(maxRange);int[] r=new int[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}
 public void cycleRange(){int[] a=presets();if(a.length==0){range=1;return;}int next=0;for(int i=0;i<a.length;i++)if(a[i]==range)next=(i+1)%a.length;range=a[next];}
 public Contact nextTarget(Contact current){ArrayList<Contact> a=new ArrayList<>();for(Contact t:contacts)if(visible(t))a.add(t);a.sort((x,y)->Double.compare(x.measuredRange(),y.measuredRange()));return a.isEmpty()?null:a.get((a.indexOf(current)+1)%a.size());}
 public String payload(Contact t){return t.launchObserved?"WEAPON LAUNCH OBSERVED":t.confidence>.65&&t.guess!=1?"POSSIBLE A/G WEAPONS":"UNKNOWN PAYLOAD";}
 public String threat(Contact t){double approach=Math.cos(delta(t.pheading,angle(-t.px,-t.py)));return t.launchObserved||t.confidence>.6&&t.guess>=3?"HIGH":approach>.5&&t.measuredRange()<20?"ELEVATED":"UNCERTAIN";}
 boolean guiding(Contact t){for(EnemyMissile m:enemyMissiles)if(m.alive&&m.source==t&&m.weapon<3)return true;return false;}
 void tryEnemyLaunch(Contact t){if(t.weapon<0||t.weaponFired||elapsed-t.born<30||maxRange<=0)return;double r=t.range();if(r>WEAPON_RANGE[t.weapon]||r<3||signal(t)==0||Math.abs(delta(t.heading,angle(-t.x,-t.y)))>.65)return;EnemyMissile m=new EnemyMissile();m.id=++enemySerial;m.source=t;m.weapon=t.weapon;m.x=t.x;m.y=t.y;m.z=t.alt/1000;enemyMissiles.add(m);t.weaponFired=true;if(liveTrack(t)){t.launchObserved=true;event("WEAPON LAUNCH DETECTED / TRACK "+t.id);}}
 public int incomingCount(){int n=0;for(EnemyMissile m:enemyMissiles)if(m.alive)n++;return n;}
 public boolean visible(EnemyMissile m){return m.alive&&elapsed-m.seen<8&&Math.hypot(m.px,m.py)<=Math.min(range,maxRange);}
 public String missileName(EnemyMissile m){return m.samples>=3&&Math.hypot(m.px,m.py)<12?WEAPONS[m.weapon]:"UNKNOWN MSL";}
 public void damageRadar(){int damage=2+random.nextInt(7);lastDamage=Math.min(maxRange,damage);maxRange=Math.max(0,maxRange-damage);range=Math.max(1,Math.min(range,maxRange));health=(int)Math.round(maxRange*2.5);radarHits++;impactUntil=elapsed+3;score-=100;event("RADAR IMPACT / RANGE -"+(int)lastDamage+" km");if(maxRange==0)finish(false);}
 public void flyEnemy(EnemyMissile m,double dt){m.age+=dt;double dx=-m.x,dy=-m.y,dz=terrain(0,0)/1000-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);boolean guided=m.weapon>=3?maxRange>0:m.source.alive&&signal(m.source)>0&&Math.abs(delta(m.source.heading,angle(-m.source.x,-m.source.y)))<.8;if(!guided)m.lost+=dt;else m.lost=0;if(m.lost>3||m.age>WEAPON_TIME[m.weapon]){m.alive=false;return;}m.speed=Math.min(WEAPON_SPEED[m.weapon],m.speed+.12*dt);double step=m.speed*dt;if(d<=step+.08){m.alive=false;damageRadar();return;}m.x+=dx/d*step;m.y+=dy/d*step;m.z+=dz/d*step;if(m.z*1000<terrain(m.x,m.y)){m.alive=false;return;}
  Contact proxy=new Contact();proxy.x=m.x;proxy.y=m.y;proxy.alt=m.z*1000;double q=signal(proxy);if(elapsed-m.seen>=1&&Math.hypot(m.x,m.y)<=range&&q>0){m.seen=elapsed;m.px=m.x;m.py=m.y;m.pz=m.z;m.samples++;}
 }
 private HashMap<Missile,double[]> vectors=new HashMap<>();
}

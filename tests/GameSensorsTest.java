package com.jb.radar;

import java.util.*;

/** World-to-receiver integration: no UI/track truth is a pilot warning. */
public final class GameSensorsTest {
 static final class Certain extends Random {
  private static final long serialVersionUID=1;
  public double nextDouble(){return .5;}
  public double nextGaussian(){return 0;}
 }
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static Game game(String... settings){return game(Equipment.defaults(),AircraftProfiles.defaults(),settings);}
 static Game game(Equipment equipment,AircraftProfiles aircraft,String... settings){
  Properties p=new Properties();
  for(int i=0;i<4;i++){
   String k="profile.GEN-"+i+".";
   p.setProperty(k+"missChance","0");p.setProperty(k+"ambiguityChance","0");
   p.setProperty(k+"visual","false");p.setProperty(k+"bearingErrorDegrees","0");
  }
  for(int i=0;i<settings.length;i+=2)p.setProperty(settings[i],settings[i+1]);
  Game g=new Game(18,new Economy(new Economy.MemoryStore()),equipment,CombatRules.defaults(),aircraft,new RwrReceiver.Config(p));
  g.start();g.contacts.clear();g.enemyMissiles.clear();g.missiles.clear();g.random=new Certain();return g;
 }
 static Game.Contact contact(Game g,int id,double x,double y){
  Game.Contact t=new Game.Contact();t.id=id;t.type=0;t.x=t.px=t.detectionX=x;t.y=t.py=t.detectionY=y;t.alt=t.palt=3000;t.heading=Game.angle(-x,-y);t.speed=0;
  g.ai.assign(t,5,2,2);g.contacts.add(t);return t;
 }
 static void ready(Game g,Game.Contact t){t.seen=t.detectedAt=g.elapsed;t.samples=t.detectionSamples=4;t.quality=t.detectionQuality=1;t.radarTracked=t.hadTrack=true;}
 static void step(Game g,double seconds){for(int i=0;i<(int)Math.ceil(seconds/.1);i++){g.elapsed+=.1;if(g.radarReady())g.sweep=(g.sweep+.1*Game.TAU/g.equipment.radar.sweepSeconds())%Game.TAU;g.sensors.tick(.1);for(Game.Contact t:g.contacts)if(t.alive)g.ai.tick(t,.1);}}
 static void sweepNow(Game g,Game.Contact t){g.sweep=(Game.angle(t.x,t.y)+Game.TAU-.01)%Game.TAU;step(g,.1);}
 static boolean kind(Game g,Game.Contact t,RwrReceiver.Kind kind){for(RwrReceiver.Observation o:g.sensors.receiver(t).observations(g.elapsed))if(o.kind==kind)return true;return false;}
 static boolean pilotKind(Game.Contact t,PilotAI.Warning kind){for(PilotAI.Evidence e:t.pilot.evidence.values())if(e.kind==kind)return true;return false;}
 static Game.Missile missile(Game g,Game.Contact target,boolean active,boolean ir,double x){
  Game.Missile m=new Game.Missile();m.target=target;m.profile=ir?g.equipment.infrared:active?g.equipment.active:g.equipment.primary;m.infrared=ir;m.x=x;m.y=target.y;m.z=target.alt/1000;m.speed=.6;
  MissileMotion.aim(m,target.x-x,0,0);g.missiles.add(m);return m;
 }
 static void independentReception(){
  Game g=game();Game.Contact t=contact(g,1,50,0);sweepNow(g,t);
  check(g.sensors.receiver(t).observations(g.elapsed).isEmpty(),"receiver classification has a real delay");
  step(g,1);check(!g.detected(t)&&kind(g,t,RwrReceiver.Kind.SEARCH)&&pilotKind(t,PilotAI.Warning.SEARCH),"aircraft hears emissions outside the player's detection range");
  check(t.pilot.evidence.size()==1&&t.pilot.evidence.values().iterator().next().emitterId.startsWith("E"),"only anonymous perceived emitter evidence delivered");
  g=game("profile.GEN-2.bands","K");t=contact(g,1,8,0);ready(g,t);t.illuminated=true;step(g,2);
  check(t.pilot.evidence.isEmpty(),"detected and hard-locked contact with incompatible bands receives no RF warning");
 }
 static void fastSweepAndInstalledMaws(){
  Properties p=new Properties();p.setProperty("radar.scanSpeed","6");
  Game g=game(new Equipment(p),AircraftProfiles.defaults());Game.Contact t=contact(g,1,50,0);
  sweepNow(g,t);step(g,1);
  check(kind(g,t,RwrReceiver.Kind.SEARCH),"fast sweep crossing receives pulse even when end-of-step beam already moved past contact");
  g=game("profile.RWR-S27.maws","true");t=contact(g,1,10,0);
  check(!g.sensors.receiver(t).profile.maws,"receiver tier configuration cannot grant an uninstalled MAWS");
  p=new Properties();p.setProperty("aircraft.su27.maws","true");
  g=game(Equipment.defaults(),new AircraftProfiles(p));t=contact(g,1,10,0);g.radarOn=false;
  Game.Missile m=missile(g,t,false,true,9);MissileMotion.aim(m,1,0,0);step(g,1);
  check(g.sensors.receiver(t).profile.maws&&kind(g,t,RwrReceiver.Kind.MAWS_MISSILE),"separately installed MAWS reports an approaching missile with optical sensor disabled");
 }
 static void lockBeamAndGuidance(){
  Game g=game();Game.Contact locked=contact(g,1,10,0),other=contact(g,2,12,.1),offBeam=contact(g,3,-10,0);
  ready(g,locked);locked.illuminated=true;step(g,1);
  check(kind(g,locked,RwrReceiver.Kind.FIRE_CONTROL)&&kind(g,other,RwrReceiver.Kind.FIRE_CONTROL),"actual fire-control beam can reach a nearby untargeted aircraft");
  check(!kind(g,offBeam,RwrReceiver.Kind.FIRE_CONTROL),"off-beam aircraft does not learn another target is locked");
  Game.Missile m=missile(g,locked,false,false,0);m.linkReserved=true;step(g,1);
  check(kind(g,locked,RwrReceiver.Kind.ILLUMINATION),"supported semi-active engagement emits a recognized guidance mode");
  m.supportReleased=true;m.linkReserved=false;locked.illuminated=false;
  for(int i=0;i<160;i++){if(i%10==0)sweepNow(g,locked);step(g,.1);}
  check(!kind(g,locked,RwrReceiver.Kind.ILLUMINATION)&&!pilotKind(locked,PilotAI.Warning.ILLUMINATION),"expired guidance warning cannot be kept alive by ordinary search refreshes");
 }
 static void seekersIndependent(){
  Game g=game();Game.Contact target=contact(g,1,10,0),bystander=contact(g,2,9,.05);g.radarOn=false;
  Game.Missile m=missile(g,target,true,false,7);step(g,1);
  check(target.pilot.evidence.isEmpty(),"active missile midcourse is not itself a launch warning");
  m.seekerState=Game.SeekerState.SEARCHING;step(g,1);
  check(kind(g,target,RwrReceiver.Kind.ACTIVE_SEEKER)&&kind(g,bystander,RwrReceiver.Kind.ACTIVE_SEEKER),"active seeker radiation reaches compatible receivers with battery off");
  g=game();target=contact(g,1,10,0);g.radarOn=false;m=missile(g,target,false,true,9);m.seekerState=Game.SeekerState.ACQUIRED;step(g,2);
  check(target.pilot.evidence.isEmpty(),"IR seeker cannot emit an RF warning");
 }
 static void opticalAndPause(){
  Game g=game("profile.GEN-2.visual","true","profile.GEN-2.visualMissChance","0","profile.GEN-2.visualAcquireSeconds","1","visual.nightVisibility","1","visual.weatherVisibility","1");
  Game.Contact t=contact(g,1,10,0);t.heading=-Math.PI/2;g.radarOn=false;
  Game.Missile m=missile(g,t,false,true,11);step(g,2);
  check(t.pilot.evidence.isEmpty(),"missile behind limited visual sector stays unseen");
  m.x=9;MissileMotion.aim(m,1,0,0);step(g,.5);
  check(t.pilot.evidence.isEmpty(),"visible missile needs finite visual acquisition time");
  double before=g.elapsed;for(int i=0;i<100;i++)g.sensors.tick(0);
  check(g.elapsed==before&&t.pilot.evidence.isEmpty(),"paused simulation cannot complete sensor acquisition");
  step(g,1);check(kind(g,t,RwrReceiver.Kind.VISUAL_MISSILE)&&pilotKind(t,PilotAI.Warning.VISUAL_MISSILE),"later actual optical acquisition supplies uncertain missile evidence");
 }
 public static void main(String[] args){independentReception();fastSweepAndInstalledMaws();lockBeamAndGuidance();seekersIndependent();opticalAndPause();System.out.println("PASS: independent/fast sweep reception, separately installed MAWS, incompatible receiver silence, untargeted beam reception, guidance expiry, radar-off active seekers, silent IR and finite optical acquisition/pause");}
}

package com.jb.radar;
import java.io.*;
import java.util.*;
/** Mission 1 difficulty is explicit and does not clamp pilots used by later missions. */
public class MissionDifficultyTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static class Fixed extends Random {private static final long serialVersionUID=1;final double value;Fixed(double value){this.value=value;}public double nextDouble(){return value;}public double nextGaussian(){return 0;}}
 static CombatRules rules(String level){Properties p=new Properties();p.setProperty("mission1.smartLevel",level);return new CombatRules(p);}
 static Game mission(CombatRules rules){Game g=new Game(17,new Economy(new Economy.MemoryStore()),Equipment.defaults(),rules);g.start();return g;}
 static void bad(String value){try{rules(value);throw new AssertionError("Invalid Mission 1 Smart Level accepted: "+value);}catch(IllegalArgumentException expected){}}
 static void fullRoster(Game g,int expected){while(g.spawned<12)g.spawn(g.spawned%Game.NAMES.length,40);check(g.contacts.size()==12,"complete twelve-aircraft roster exists");for(Game.Contact t:g.contacts){check(t.pilot!=null&&t.pilot.smart==expected,"every Mission 1 spawn uses configured Smart Level");if(expected==1)check(t.desiredAlt>=1800&&t.alt-Game.terrain(t.x,t.y)>=1799.99,"rookie approaches stay above terrain-hugging altitude");}}
 public static void main(String[] args)throws Exception {
  check(CombatRules.defaults().mission1SmartLevel==1,"Mission 1 defaults to Rookie Smart Level 1");
  File f=new File("assets/combat.properties");if(!f.exists())f=new File("radar/assets/combat.properties");try(InputStream in=new FileInputStream(f)){check(CombatRules.load(in).mission1SmartLevel==1,"packaged combat profile also selects Rookie");}
  bad("0");bad("6");bad("1.5");bad("NaN");bad("rookie");
  Game g=mission(CombatRules.defaults());fullRoster(g,1);g.start();fullRoster(g,1);
  Game veteran=mission(rules("4"));fullRoster(veteran,4);Game.Contact t=veteran.contacts.get(0);veteran.ai.assign(t,5,1,2);check(t.pilot.smart==5,"explicit future high-skill pilots are not globally clamped");
  for(PilotAI.Warning warning:new PilotAI.Warning[]{PilotAI.Warning.LOCK,PilotAI.Warning.LAUNCH,PilotAI.Warning.SEARCH,PilotAI.Warning.ACTIVE_SEEKER}){
   check(PilotAI.recognitionChance(1,warning)<PilotAI.recognitionChance(4,warning),"rookie less likely to recognize "+warning);
   check(PilotAI.reactionDelay(1,warning)>PilotAI.reactionDelay(4,warning),"rookie reacts later to "+warning);
  }
  g=mission(CombatRules.defaults());g.contacts.clear();t=GameTest.target(g,0);g.ai.assign(t,1,1,2);g.random=new Fixed(.30);check(!g.ai.warn(t,PilotAI.Warning.LOCK,true)&&t.pilot.cues.isEmpty(),"rookie can miss a lock warning");g.ai.assign(t,4,1,2);check(g.ai.warn(t,PilotAI.Warning.LOCK,true),"same observable warning can be recognized by trained pilot");
  g.ai.assign(t,1,1,2);g.random=new Fixed(.20);check(g.ai.warn(t,PilotAI.Warning.LOCK,true),"recognized rookie warning queues");double rookieDue=t.pilot.cues.get(0).due;g.elapsed=rookieDue-.001;g.ai.tick(t,.001);check(t.pilot.cues.size()==1&&t.pilot.awareness==PilotAI.Awareness.UNAWARE,"rookie does not react before warning delay");g.elapsed=rookieDue+.001;g.ai.tick(t,.001);check(t.pilot.cues.isEmpty()&&t.pilot.awareness!=PilotAI.Awareness.UNAWARE,"rookie can still react after delayed warning");
  for(int level=1;level<=2;level++){g.ai.assign(t,level,1,2);for(PilotAI.Warning w:new PilotAI.Warning[]{PilotAI.Warning.LOCK,PilotAI.Warning.LAUNCH,PilotAI.Warning.ACTIVE_SEEKER})check(g.ai.weights(t.pilot,w)[PilotAI.Action.NOTCH.ordinal()]==0,"inexperienced pilot cannot select advanced notch");t.pilot.state=PilotAI.State.NOTCH;t.heading=Game.angle(t.x,t.y)+Math.PI/2;g.elapsed=0;check(!g.ai.notchBreak(t),"inexperienced pilot cannot trigger notch guidance break");}
  g.ai.assign(t,4,1,2);check(g.ai.weights(t.pilot,PilotAI.Warning.LAUNCH)[PilotAI.Action.NOTCH.ordinal()]>0,"advanced pilots retain notch behavior");g.ai.assign(t,1,1,2);for(int i=0;i<20;i++)g.ai.recordFire(t.pilot,18);check(g.ai.caution(t.pilot)==0,"rookie does not gain high-skill adaptive counterplay");
  // Crossing the outer graze radius on an inbound shot must not consume the missile.
  g=mission(CombatRules.defaults());g.contacts.clear();t=GameTest.target(g,0);g.ai.assign(t,1,1,2);GameTest.lock(g,t);Game.Missile shot=new Game.Missile();shot.profile=g.weapon;shot.target=t;shot.linkReserved=true;shot.x=t.x-g.weapon.fuzeKm*.95;shot.y=t.y;shot.z=t.alt/1000;shot.vx=1;shot.speed=.5;shot.age=2;g.missiles.add(shot);g.fly(shot,.025);check(shot.alive&&t.pilot.health==100&&g.kills==0,"inbound missile crosses outer graze radius without premature detonation");for(int i=0;i<30&&shot.alive;i++)g.fly(shot,.025);check(g.kills==1&&!t.alive,"inbound missile can continue into the direct-hit radius");
  // A real near miss still detonates and damages the aircraft after its closest approach.
  g=mission(CombatRules.defaults());g.contacts.clear();t=GameTest.target(g,0);g.ai.assign(t,1,1,2);GameTest.lock(g,t);shot=new Game.Missile();shot.profile=g.weapon;shot.target=t;shot.linkReserved=true;shot.x=t.x+.02;shot.y=t.y+g.weapon.fuzeKm*.75;shot.z=t.alt/1000;shot.vx=1;shot.speed=.5;shot.age=2;g.missiles.add(shot);g.fly(shot,.025);check(!shot.alive&&t.alive&&t.pilot.health>0&&t.pilot.health<100&&g.kills==0&&g.misses==1,"near pass damages after closest approach without becoming a false direct kill");
  // A radar-supported starter missile intercepts an actual moving rookie at normal frame rates.
  for(double dt:new double[]{.025,.05,.1}){g=mission(CombatRules.defaults());g.contacts.clear();g.nextSpawn=9999;g.random=new Fixed(.30);t=GameTest.target(g,0);t.x=t.px=7;t.alt=t.palt=3000;t.speed=t.pspeed=600;t.heading=t.pheading=-Math.PI/2;t.desiredAlt=2920;t.weapon=-1;t.flareBursts=0;g.ai.assign(t,1,1,2);check(GameTest.lock(g,t).contains("LOCK ESTABLISHED"),"controlled moving rookie can be locked");check(g.launchBlock(t)==null,"starter missile has valid firing solution");g.launch(t);for(int i=0;i<Math.ceil(20/dt)&&t.alive;i++)g.tick(dt);check(!t.alive&&g.kills==1&&g.shots==1,"supported starter shot intercepts moving rookie at dt="+dt+", hp="+t.pilot.health);}
  System.out.println("PASS: all twelve Mission 1 pilots are configurable rookies, lower/delayed warning recognition, no advanced novice notching, future skill preserved and moving starter-missile interception");
 }
}

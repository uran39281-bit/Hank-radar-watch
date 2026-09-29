package com.jb.radar;
import java.util.*;
/** Channel ownership and guidance regressions across the rebuilt sensor model. */
public class TrackingTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Game active(int channels){return EquipmentTest.game(EquipmentTest.config("missile.rampart.radarMode","ACTIVE","missile.rampart.seekerKm","3","radar.illuminationChannels","2","radar.midcourseChannels",""+channels));}
 static Game.Contact target(Game g,int n){Game.Contact t=GameTest.target(g,n);t.x=t.px=18+n;return t;}
 static void inSeeker(Game.Missile m){m.x=m.target.px-1;m.y=m.target.py;m.z=m.target.palt/1000;m.vx=1;m.vy=0;m.vz=0;}
 public static void main(String[] args){
  Game g=active(2);Game.Contact t=target(g,0);t.radarTracked=false;
  check(g.launchBlock(t).equals("TRACK TOO WEAK"),"unallocated detection cannot fire");g.track(t);
  check(g.tracked(t)&&!g.hardLocked(t)&&g.illuminationUsed()==0&&g.midcourseUsed()==0,"automatic track alone uses no support channel");
  check(g.lockBlock(t)!=null&&g.launchBlock(t)==null,"active fires within track envelope outside hard-lock range");
  g.launch(t);Game.Missile first=g.missiles.get(0);check(first.datalink&&g.midcourseUsed()==1&&g.illuminationUsed()==0&&g.guided(first)&&!first.autonomous,"active launch reserves only midcourse support");
  g.launch(t);Game.Missile second=g.missiles.get(1);check(g.midcourseUsed()==2,"two missiles at one target consume two midcourse channels");
  Game.Contact other=target(g,1);int shots=g.shots,ammo=g.ready();check(g.launchBlock(other).equals("NO FREE SUPPORT CHANNEL"),"third supported launch blocked");g.launch(other);check(g.shots==shots&&g.ready()==ammo,"denied launch spends no ammo");
  inSeeker(first);check(g.guided(first)&&first.autonomous&&!first.datalink&&g.midcourseUsed()==1,"actual seeker acquisition frees only that missile channel");
  g.chosenLauncher=1;check(g.launchBlock(other)==null,"handoff admits another supported shot");g.launch(other);check(g.midcourseUsed()==2,"new launch reserves the released channel");
  second.alive=false;check(g.midcourseUsed()==1,"destroyed missile releases its reservation");t.radarTracked=false;g.battery.parts[Battery.POWER].hp=0;check(g.guided(first),"acquired seeker survives radar and power loss");

  g=active(1);t=target(g,0);g.launch(t);Game.Missile m=g.missiles.get(0);t.radarTracked=false;
  check(!g.guided(m)&&g.midcourseUsed()==1,"transient track loss interrupts updates but retains reservation");other=GameTest.target(g,1);g.illuminate(other);
  check(g.hardLocked(other)&&g.illuminationUsed()==1&&g.midcourseUsed()==1,"illumination has a separate pool from reserved midcourse");t.radarTracked=true;
  check(g.guided(m)&&g.midcourseUsed()==1,"fresh track recovers existing reservation without competing with hard lock");g.release(other);check(g.illuminationUsed()==0&&g.midcourseUsed()==1,"unlock releases only illumination");m.age=g.weapon.guidanceSeconds+.1;check(!g.guided(m)&&g.midcourseUsed()==0,"guidance expiry releases missile support");

  g=active(1);t=GameTest.target(g,0);g.launch(t);m=g.missiles.get(0);g.illuminate(t);
  check(g.hardLocked(t)&&g.illuminationUsed()==1&&g.midcourseUsed()==1,"manual hard lock and active missile reserve independent channels");g.release(t);
  check(g.tracked(t)&&!g.hardLocked(t)&&g.guided(m)&&g.illuminationUsed()==0,"unlock retains automatic track and active updates");g.illuminate(t);inSeeker(m);
  check(g.guided(m)&&m.autonomous&&g.hardLocked(t)&&g.illuminationUsed()==1&&g.midcourseUsed()==0,"seeker handoff leaves manually established illumination intact");g.release(t);check(g.channels()==0&&g.guided(m),"manual unlock leaves acquired seeker independent");

  g=new Game(3);g.start();g.contacts.clear();t=GameTest.target(g,0);
  check(g.launchBlock(t).equals("RADAR LOCK REQUIRED"),"semi-active cannot fire with only a track");g.illuminate(t);g.launch(t);m=g.missiles.get(0);g.launch(t);
  check(g.guided(m)&&g.illuminationUsed()==1&&g.midcourseUsed()==0&&g.supportedMissilesUsed()==2,"SARH missiles share target illumination and count toward missile support cap");g.release(t);
  check(g.tracked(t)&&!g.hardLocked(t)&&!g.guided(m),"automatic track alone cannot guide semi-active missile");g.illuminate(t);check(g.guided(m),"lock recovery inside the recovery window resumes SARH");
  t.seen=-100;g.maintainTracks();check(!g.tracked(t)&&!g.hardLocked(t)&&g.illuminationUsed()==0,"expired track clears fire-control state");
  g=active(0);t=target(g,0);check(g.launchBlock(t).equals("NO FREE SUPPORT CHANNEL"),"zero-channel battery cannot launch supported active missile");

  Properties p=new Properties();p.setProperty("ai.trackingCue","true");g=new Game(1,new Economy(new Economy.MemoryStore()),Equipment.defaults(),new CombatRules(p));g.start();g.contacts.clear();g.random=new AIBehaviorTest.Certain();t=GameTest.target(g,0);t.radarTracked=false;g.ai.assign(t,5,1,2);g.track(t);
  check(t.pilot.cues.isEmpty(),"internal TWS allocation never invents a tracking warning");g.illuminate(t);
  check(t.pilot.cues.size()==1&&t.pilot.cues.get(0).kind==PilotAI.Warning.LOCK,"compatible receiver can detect explicit hard lock");g.release(t);check(t.pilot.cues.size()==1&&!g.hardLocked(t),"unlock invents no warning");

  // Integrate actual projectile motion through search, acquisition and geometric intercept.
  g=active(1);t=GameTest.target(g,0);g.launch(t);m=g.missiles.get(0);boolean handoff=false;
  for(int i=0;i<2000&&m.alive;i++){g.fly(m,.02);handoff|=m.autonomous;check(g.midcourseUsed()<=1,"flight support cap");}
  check(handoff&&g.kills==1&&!t.alive&&g.midcourseUsed()==0,"active flight acquires its assigned target, intercepts and frees support");
  System.out.println("PASS: independent illumination/midcourse pools, per-missile reservations, acquisition release, transient recovery, SARH sharing and separate warning events");
 }
}

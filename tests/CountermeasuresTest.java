package com.jb.radar;
import java.util.Properties;
/** Conditional countermeasure effects: a deployment is not a magic missile delete or lock break. */
public class CountermeasuresTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static Game make(Properties properties){Game g=new Game(205,new Economy(new Economy.MemoryStore()),new Equipment(properties));g.start();g.contacts.clear();g.nextSpawn=9999;return g;}
 static Game.Contact target(Game g){Game.Contact t=GameTest.target(g,0);t.x=t.px=4;t.y=t.py=0;t.alt=t.palt=2000;t.heading=Math.PI/2;return t;}
 static Game.Missile missile(Game g,Game.Contact t,Equipment.Weapon w){Game.Missile m=new Game.Missile();m.profile=w;m.target=t;m.z=2;m.vx=1;m.estimateValid=true;m.estimateX=4;m.estimateZ=2;return m;}
 public static void main(String[] args){
  Game g=make(new Properties());Game.Contact t=target(g);Game.Missile m=missile(g,t,g.equipment.primary);
  int logSize=g.log.size();g.ir.emitCountermeasure(t,3);Infrared.Flare f=g.ir.flares.get(0);double factor=g.ir.radarDecoyFactor(m);
  check(factor<1&&factor>.25,"fresh, overlapping radar decoy reduces rather than zeros guidance quality");
  check(m.alive&&t.alive&&m.target==t,"countermeasure does not destroy missile or force target reassignment");
  check(g.log.size()==logSize,"unobserved enemy deployment does not leak through player log");
  f.y=2;check(g.ir.radarDecoyFactor(m)==1,"angularly separated decoy has no effect");f.y=0;
  f.age=2;check(g.ir.radarDecoyFactor(m)>factor,"countermeasure effect fades with age");
  f.age=3;check(g.ir.radarDecoyFactor(m)==1,"expired radar decoy has no effect");f.age=0;
  g.running=false;g.ir.tick(1);check(f.age==0,"pause freezes countermeasure lifetime");g.running=true;
  g.ir.tick(3.01);check(g.ir.flares.isEmpty(),"cloud expires after configured lifetime");
  Properties p=new Properties();p.setProperty("missile.rampart.radarDecoyResistance","1");p.setProperty("missile.ir6.irDecoyResistance","1");
  g=make(p);t=target(g);m=missile(g,t,g.equipment.primary);g.ir.emitCountermeasure(t,3);f=g.ir.flares.get(0);
  check(g.ir.radarDecoyFactor(m)==1,"radar seeker resistance is respected");m.profile=g.equipment.infrared;
  check(!g.ir.validFlare(m,m.profile,f),"IR seeker resistance is respected independently");
  p=new Properties();p.setProperty("missile.ir6.irDecoyResistance","0");g=make(p);t=target(g);m=missile(g,t,g.equipment.infrared);m.infrared=true;g.ir.emitCountermeasure(t,3);f=g.ir.flares.get(0);
  check(g.ir.validFlare(m,m.profile,f),"same combined deployment presents detectable heat");f.y=8;
  check(!g.ir.validFlare(m,m.profile,f),"heat decoy still requires seeker field of view");f.y=0;f.age=3;
  check(!g.ir.validFlare(m,m.profile,f),"expired heat decoy cannot be acquired");
  System.out.println("PASS: combined countermeasure geometry, resistance, finite life, pause and no hidden event leak");
 }
}

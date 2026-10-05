package com.jb.radar;

/** Integration boundaries for new aircraft, fixed presets and persistent ballistic bombs. */
public final class FlightUpdateTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static Game game(){Game g=new Game(402);g.start();g.contacts.clear();g.nextSpawn=9999;g.random=new AIBehaviorTest.Certain();return g;}
 static Game.Contact spawn(Game g,int type){g.spawn(type,15);return g.contacts.get(g.contacts.size()-1);}
 public static void main(String[] args){
  Game g=game();
  for(int type=0;type<AircraftProfiles.COUNT;type++){
   Game.Contact t=spawn(g,type);AircraftProfiles.Profile p=g.aircraft(t);t.x=15;t.y=0;t.heading=Math.PI/2;t.speed=1200;t.desiredAlt=p.spawnAltitudeM;
   check(t.weapon==-1&&t.pilot.bombs==p.preset.bombs,"spawn uses only fixed WP0/WP1 and no AGM");
   double prior=t.speed;
   for(int i=0;i<200;i++){double before=t.speed;g.moveAircraft(t,.025);check(t.actualG<=p.commandedG+1e-8&&t.actualG<=p.structuralG+1e-8,"integrated G bound for "+p.name);check(Math.abs(t.speed-before)<4,"finite speed change for "+p.name);check(t.alt<=p.ceilingM&&t.alt>=Game.terrain(t.x,t.y)+39.99,"ceiling and terrain bounds");}
   check(t.speed<prior,"turn and finite deceleration consume speed");
   t.pilot.health=40;t.heading=Math.PI/2;t.speed=900;g.moveAircraft(t,.025);check(t.actualG<=p.availableG(900,.4)+1e-8,"damage reduces actual control authority");
  }
  g=game();Game.Contact intrusion=spawn(g,AircraftProfiles.MIG25);check(intrusion.pilot.intrusion&&intrusion.pilot.bombs==0,"MiG25 gets legitimate WP0 task");
  for(int i=0;i<50;i++){g.elapsed+=.1;g.ai.tick(intrusion,.1);g.moveAircraft(intrusion,.1);}
  check(!g.ai.isExiting(intrusion)&&!g.releaseBombs(intrusion),"WP0 follows intrusion route rather than aborting for no bombs");
  intrusion.x=g.aircraft.route.intrusionXKm;intrusion.y=g.aircraft.route.intrusionYKm;g.elapsed+=.3;g.ai.tick(intrusion,.1);check(g.ai.isExiting(intrusion),"intrusion completion starts configured exit route");
  check(Math.abs(Game.delta(g.ai.heading(intrusion),Game.angle(g.aircraft.route.exitXKm-intrusion.x,g.aircraft.route.exitYKm-intrusion.y)))<1e-9,"exit uses mission coordinates");
  g=game();Game.Contact attacker=spawn(g,AircraftProfiles.SU27);AircraftProfiles.Preset preset=g.aircraft(attacker).preset;attacker.speed=700;attacker.alt=Game.terrain(0,0)+2000;attacker.x=preset.leadKm(2000,700);attacker.y=0;attacker.heading=-Math.PI/2;attacker.aimStableSeconds=2;
  check(g.releaseBombs(attacker)&&g.bombs.size()==1&&attacker.pilot.bombs==3,"valid spawn load releases exactly one bomb");
  Game.Bomb bomb=g.bombs.get(0);check(bomb.weaponId.equals(preset.weaponDefinitionId)&&bomb.explosiveKg==38&&bomb.explosiveKg!=preset.bombMassKg,"bomb uses selected charge rather than full mass");
  check(!g.releaseBombs(attacker),"integrated release cooldown");g.flyBomb(bomb,.1);check(g.battery.condition()==100&&bomb.alive,"release is not immediate damage");
  g.confirmIntercept(attacker,false);check(!attacker.alive&&bomb.alive&&g.kills==1,"parent destruction leaves released bomb alive");
  for(int i=0;i<2500&&bomb.alive;i++)g.flyBomb(bomb,.01);
  check(!bomb.alive&&g.battery.condition()<100,"released bomb still impacts after parent destroyed");
  g=new Game(303);g.start();for(int i=0;i<7000&&!g.finished;i++){g.tick(.05);check(g.enemyMissiles.isEmpty(),"campaign does not activate reference-catalogue hostile missiles");for(Game.Contact t:g.contacts)check(t.type>=0&&t.type<4&&t.weapon==-1,"only the four updated default aircraft spawn");}
  check(g.spawned>=4,"default campaign exercised later spawn schedule");
  System.out.println("PASS: integrated aircraft G/energy/damage limits, four-aircraft fixed presets, WP0 route, ballistic release cooldown and parent-independent bomb flight, no default AGMs");
 }
}

package com.jb.radar;

import java.util.*;

/** Physical emission/visual sampling boundary. Only receiver observations reach pilots. */
public final class GameSensors {
 public final Game game;
 public final RwrReceiver.Config config;
 private static final String BATTERY_TOKEN="battery-emitter";
 private static final class Fit {
  final PilotAI.Pilot pilot;
  final RwrReceiver receiver;
  Fit(PilotAI.Pilot pilot,RwrReceiver receiver){this.pilot=pilot;this.receiver=receiver;}
 }
 private final IdentityHashMap<Game.Contact,Fit> fits=new IdentityHashMap<>();
 private final IdentityHashMap<Game.Contact,String> overrides=new IdentityHashMap<>();
 private final IdentityHashMap<Game.Missile,String> missileTokens=new IdentityHashMap<>();
 private long nextToken;

 public GameSensors(Game game,RwrReceiver.Config config){this.game=game;this.config=config==null?RwrReceiver.Config.defaults():config;}
 public void reset(){fits.clear();overrides.clear();missileTokens.clear();nextToken=0;}
 /** Explicit development/loadout fit override; a profile ID is never delivered to AI. */
 public void assign(Game.Contact contact,String profileId){if(contact==null)return;overrides.put(contact,profileId);fits.remove(contact);}
 public RwrReceiver receiver(Game.Contact contact){
  if(contact==null||contact.pilot==null||contact instanceof Game.EnemyMissile)return null;
  Fit fit=fits.get(contact);
  if(fit==null||fit.pilot!=contact.pilot){
   String profileId=overrides.get(contact);
   if(profileId==null)profileId=profileId(contact);
   fit=new Fit(contact.pilot,new RwrReceiver(config.profile(profileId).withMaws(game.aircraft(contact).maws),game.random));fits.put(contact,fit);
  }
  return fit.receiver;
 }
 private String profileId(Game.Contact contact){
  PilotAI.WarningProfile legacy=contact.pilot.warnings;
  if(!legacy.searchRadar&&!legacy.fireControl&&!legacy.activeRadar)return "GEN-0";
  return game.aircraft(contact).rwrId;
 }
 private String token(Game.Missile missile){String token=missileTokens.get(missile);if(token==null){token="airborne-emitter-"+(++nextToken);missileTokens.put(missile,token);}return token;}
 private RwrReceiver.Pose pose(Game.Contact t){double speed=t.speed/3600;return new RwrReceiver.Pose(t.x,t.y,t.alt/1000,t.heading,Math.atan2(t.climb/1000,speed),Math.sin(t.heading)*speed,-Math.cos(t.heading)*speed,t.climb/1000);}
 private RwrReceiver.Pose pose(Game.Missile m){return new RwrReceiver.Pose(m.x,m.y,m.z,Game.angle(m.vx,m.vy),Math.atan2(m.vz,Math.hypot(m.vx,m.vy)),m.vx*m.speed,m.vy*m.speed,m.vz*m.speed);}
 private double radarZ(){return (Game.terrain(0,0)+35)/1000;}
 private boolean line(double x,double y,double z,Game.Contact t){return game.ir.clearLine(x,y,z,t.x,t.y,t.alt/1000);}
 private void receive(Game.Contact t,RwrReceiver.Emission emission,double x,double y,double z){RwrReceiver receiver=receiver(t);if(receiver!=null)receiver.receive(game.elapsed,pose(t),emission,line(x,y,z,t));}
 private void broadcast(RwrReceiver.Emission emission,double x,double y,double z){for(Game.Contact t:game.contacts)if(t.alive)receive(t,emission,x,y,z);}
 private boolean sarhIllumination(Game.Contact track){for(Game.Missile m:game.missiles)if(m.alive&&!m.infrared&&m.target==track&&m.linkReserved&&!m.supportReleased&&!Game.activeHoming(game.profile(m)))return true;return false;}

 /** Called once after world movement with the same simulation delta; real time is unused. */
 public void tick(double dt){
  if(dt<=0||!game.running||game.finished)return;
  double now=game.elapsed,radarZ=radarZ();
  if(game.radarReady()){
   double advance=Math.min(Game.TAU,dt*Game.TAU/game.equipment.radar.sweepSeconds());
   for(Game.Contact t:game.contacts)if(t.alive&&t.pilot!=null){
    double bearing=(Game.angle(t.x,t.y)+Game.TAU)%Game.TAU;
    // Reception does not consult radar detection range, stored plots, or TWS assignment.
    if(config.search.pattern==RwrReceiver.Pattern.OMNI||(game.sweep-bearing+Game.TAU)%Game.TAU<advance)
     // The pulse occurred at the bearing crossing within this timestep, before its end angle.
     receive(t,new RwrReceiver.Emission(BATTERY_TOKEN,config.search,0,0,radarZ,bearing,0),0,0,radarZ);
   }
   for(Game.Contact illuminated:game.targets())if(game.hardLocked(illuminated)){
    double heading=Game.angle(illuminated.x,illuminated.y),pitch=Math.atan2(illuminated.alt/1000-radarZ,illuminated.range());
    // Other aircraft can receive the same beam if its geometry/strength reaches them.
    broadcast(new RwrReceiver.Emission(BATTERY_TOKEN,sarhIllumination(illuminated)?config.illumination:config.fireControl,0,0,radarZ,heading,pitch),0,0,radarZ);
   }
  }
  for(Game.Missile m:game.missiles)if(m.alive){
   if(!m.infrared&&Game.activeHoming(game.profile(m))&&m.seekerState!=Game.SeekerState.MIDCOURSE){
    broadcast(new RwrReceiver.Emission(token(m),config.activeSeeker,m.x,m.y,m.z,Game.angle(m.vx,m.vy),Math.atan2(m.vz,Math.hypot(m.vx,m.vy))),m.x,m.y,m.z);
   }
   for(Game.Contact t:game.contacts)if(t.alive&&t.pilot!=null&&(t.pilot.warnings.visual||t.pilot.warnings.missileWarning||game.aircraft(t).maws)){
    RwrReceiver receiver=receiver(t);
    receiver.observeMissile(now,token(m),pose(t),pose(m),line(m.x,m.y,m.z,t),config.nightVisibility,config.weatherVisibility*game.ir.visibility);
   }
  }
  for(Game.Contact t:game.contacts)if(t.alive&&t.pilot!=null){
   RwrReceiver receiver=receiver(t);
   for(RwrReceiver.Observation observation:receiver.observations(now)){
    PilotAI.Warning kind=warning(observation.kind);
    game.ai.observe(t,kind,observation.emitterId,observation.bearing,observation.confidence,observation.expiresAt);
   }
  }
  for(Iterator<Map.Entry<Game.Contact,Fit>> i=fits.entrySet().iterator();i.hasNext();)if(!i.next().getKey().alive)i.remove();
  for(Iterator<Map.Entry<Game.Missile,String>> i=missileTokens.entrySet().iterator();i.hasNext();)if(!i.next().getKey().alive)i.remove();
 }
 private static PilotAI.Warning warning(RwrReceiver.Kind kind){switch(kind){
  case SEARCH:return PilotAI.Warning.SEARCH;
  case FIRE_CONTROL:return PilotAI.Warning.LOCK;
  case ILLUMINATION:return PilotAI.Warning.ILLUMINATION;
  case ACTIVE_SEEKER:return PilotAI.Warning.ACTIVE_SEEKER;
  case VISUAL_MISSILE:case MAWS_MISSILE:return PilotAI.Warning.VISUAL_MISSILE;
  default:return PilotAI.Warning.UNKNOWN_RADAR;
 }}
}

package com.jb.radar;
import java.io.*;
import java.util.*;

/** Flight and loadout boundaries, not snapshots of implementation internals. */
public final class AircraftProfilesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static boolean near(double a,double b){return Math.abs(a-b)<1e-8;}
 static AircraftProfiles configuration(String... entries){Properties p=new Properties();for(int i=0;i<entries.length;i+=2)p.setProperty(entries[i],entries[i+1]);return new AircraftProfiles(p);}
 static void bad(String... entries){try{configuration(entries);throw new AssertionError("Invalid config accepted: "+Arrays.toString(entries));}catch(IllegalArgumentException expected){}}
 public static void main(String[] args)throws Exception{
  AircraftProfiles profiles=AircraftProfiles.defaults();File file=new File("assets/aircraft.properties");if(!file.exists())file=new File("radar/assets/aircraft.properties");
  AircraftProfiles loaded;try(InputStream in=new FileInputStream(file)){loaded=AircraftProfiles.load(in);}
  check(profiles.all.length==4&&loaded.all.length==4,"four generic aircraft only");
  String[] names={"Su-27","MiG-29","MiG-25","Tu-160"};int[] bombs={4,2,0,8},cm={96,60,64,128};
  for(int i=0;i<4;i++){
   AircraftProfiles.Profile p=profiles.profile(i),a=loaded.profile(i);
   check(p.name.equals(names[i])&&p.preset.bombs==bombs[i]&&p.countermeasureInventory==cm[i],"guide roster, loads and CM counts");
   check(!p.maws&&p.stealthLevel==0,"no default MAWS or stealth");
   check(a.name.equals(p.name)&&near(a.maxSpeedKmh,p.maxSpeedKmh)&&near(a.ceilingM,p.ceilingM)&&near(a.maneuverability,p.maneuverability)&&a.preset.id.equals(p.preset.id)&&a.preset.bombs==p.preset.bombs&&near(a.ai.reactionMaxSeconds,p.ai.reactionMaxSeconds),"asset and fallback agree");
   check(near(p.maxSpeedKmh(0,1,0),p.lowAltitudeSpeedKmh),"low-altitude cap");
   check(near(p.maxSpeedKmh(p.referenceAltitudeM/2,1,0),(p.lowAltitudeSpeedKmh+p.maxSpeedKmh)/2),"altitude interpolation");
   check(near(p.maxSpeedKmh(p.ceilingM+5000,1,0),p.maxSpeedKmh),"cap above reference altitude");
   check(p.maxSpeedKmh(p.referenceAltitudeM,.5,1)<p.maxSpeedKmh(p.referenceAltitudeM,1,0),"damage and load reduce cap");
   for(double speed=100;speed<4000;speed+=100){
    double g=p.availableG(speed,1),omega=p.turnRateRad(speed,1),physicalG=Math.sqrt(1+Math.pow(omega*(speed/3.6)/9.80665,2));
    check(g<=p.commandedG&&g<=p.structuralG&&physicalG<=g+1e-9,"turn respects commanded and structural G at every speed");
   }
   check(p.availableG(300,1)<p.availableG(p.cornerSpeedKmh,1),"low energy cannot sustain maximum commanded G");
   check(p.availableG(p.cornerSpeedKmh,.3)<p.availableG(p.cornerSpeedKmh,1),"damage loses control authority");
   double initial=600,plain=p.advanceSpeedKmh(initial,2000,5000,1,0,1,.1),turn=p.advanceSpeedKmh(initial,2000,5000,1,0,5,.1);
   check(plain>initial&&plain<=initial+p.accelerationKmhPerSecond*.1+1e-9,"finite acceleration");
   check(turn<plain,"loaded turn loses speed");
   check(p.advanceSpeedKmh(2000,400,0,.1,1,1,.1)>1900,"damage/cap drop does not teleport velocity");
   check(p.turnRadiusKm(1600,1)>p.turnRadiusKm(1200,1),"high speed has larger turn radius above corner");
  }
  check(profiles.profile(3).turnRadiusKm(1000,1)>profiles.profile(0).turnRadiusKm(1000,1)*3,"bomber cannot copy fighter turn");
  check(profiles.profile(2).turnRadiusKm(1000,1)>profiles.profile(1).turnRadiusKm(1000,1),"MiG25 uses wider turns");
  check(profiles.profile(2).rwrGeneration==1&&profiles.profile(3).provisional,"basic MiG25 and explicitly provisional Tu160");
  AircraftProfiles.Preset b=profiles.profile(0).preset;double lead=b.leadKm(2500,850);
  check(lead>5&&b.releaseEnvelope(lead,0,2500,850,2,1),"valid ballistic release occurs before overflying objective");
  check(!b.releaseEnvelope(0,0,2500,850,2,1),"overhead high-speed drop cannot damage objective immediately");
  check(!b.releaseEnvelope(lead,.5,2500,850,2,1),"wrong heading invalidates release");
  check(!b.releaseEnvelope(lead,0,2500,1500,2,1),"too fast invalidates release");
  check(!b.releaseEnvelope(lead,0,2500,850,.5,1),"unstable aim invalidates release");
  check(!b.releaseEnvelope(lead,0,2500,850,2,.1),"cooldown prevents repeated release");
  check(!profiles.profile(2).preset.armed()&&!profiles.profile(2).preset.releaseEnvelope(lead,0,2500,850,2,1),"WP0 cannot bomb");
  check(!b.releaseEnvelope(Double.NaN,0,2500,850,2,1),"bad geometry rejected");
  AircraftProfiles.CountermeasureConfig c=profiles.profile(0).countermeasures;
  check(c.burstAvailable(96,false)==3&&c.burstAvailable(10,false)==0,"regular bursts respect combined reserve");
  check(c.burstAvailable(2,true)==2&&c.burstAvailable(0,true)==0,"urgent partial burst can consume reserve without inventing rounds");
  AircraftProfiles edited=configuration("ai.skill","0.2","aircraft.su27.ai.aggression","0.8","aircraft.su27.countermeasures.inventory","4");
  check(edited.profile(0).ai.skill==.2&&edited.profile(1).ai.skill==.2&&edited.profile(0).ai.aggression==.8&&edited.profile(1).ai.aggression==.55,"global defaults and per-aircraft overrides");
  check(edited.profile(0).countermeasures.inventory==4,"inventory editable");
  bad("aircraft.su27.commandedG","12");bad("aircraft.su27.stealthLevel","8");bad("aircraft.su27.maneuverability","11");
  bad("aircraft.su27.preset.id","WP-3");bad("aircraft.mig25.preset.bombs","2");bad("aircraft.su27.preset.bombs","0");
  bad("aircraft.su27.preset.explosiveKg","115");bad("aircraft.su27.rwrGeneration","4");bad("aircraft.su27.countermeasures.inventory","-1");
  bad("aircraft.su27.maxSpeedKmh","NaN");bad("aircraft.su27.ceilingM","11000");bad("ai.reactionMaxSeconds","1");bad("ai.skill","1.1");
  bad("ai.failedApproachLimit","0");bad("ai.decisionIntervalSeconds","0");bad("aircraft.su27.unrecognisedKey","1");
  System.out.println("PASS: editable four-aircraft profiles, flight envelope/G/energy, fixed WP0/WP1 and ballistic release, combined CM reserve, strict configuration validation");
 }
}

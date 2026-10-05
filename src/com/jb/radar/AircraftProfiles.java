package com.jb.radar;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Editable fictional game profiles from the October 2026 aircraft update.
 * Speeds are km/h, heights meters, time seconds; health/load inputs are fractions.
 * These profiles are game balance, not certified aircraft performance data. */
public final class AircraftProfiles {
 public static final int SU27=0, MIG29=1, MIG25=2, TU160=3;
 public static final int COUNT=4;
 private static final double G=9.80665;

 /** Mission knowledge is explicit and independent of any player's sensor state. */
 public static final class Route {
  public final double objectiveXKm,objectiveYKm,exitXKm,exitYKm,intrusionXKm,intrusionYKm,arrivalKm;
  Route(Reader r){
   objectiveXKm=r.number("mission.objectiveXKm",0,-200,200);objectiveYKm=r.number("mission.objectiveYKm",0,-200,200);
   exitXKm=r.number("mission.exitXKm",65,-200,200);exitYKm=r.number("mission.exitYKm",-45,-200,200);
   intrusionXKm=r.number("mission.intrusionXKm",55,-200,200);intrusionYKm=r.number("mission.intrusionYKm",35,-200,200);
   arrivalKm=r.number("mission.routeArrivalKm",2,.1,20);
  }
 }

 public static final class AIConfig {
  public final double skill,aggression,reactionMinSeconds,reactionMaxSeconds,emergencyMinSeconds,emergencyMaxSeconds;
  public final double observedMissileMemorySeconds,abortHealthFraction,minimumStateHoldSeconds,safeClearSeconds,decisionIntervalSeconds;
  public final int failedApproachLimit;
  AIConfig(Reader r,String k,AIConfig fallback){
   skill=r.number(k+"skill",fallback==null?.40:fallback.skill,0,1);
   aggression=r.number(k+"aggression",fallback==null?.55:fallback.aggression,0,1);
   reactionMinSeconds=r.number(k+"reactionMinSeconds",fallback==null?2:fallback.reactionMinSeconds,0,30);
   reactionMaxSeconds=r.number(k+"reactionMaxSeconds",fallback==null?4:fallback.reactionMaxSeconds,reactionMinSeconds,60);
   emergencyMinSeconds=r.number(k+"emergencyMinSeconds",fallback==null?1:fallback.emergencyMinSeconds,0,30);
   emergencyMaxSeconds=r.number(k+"emergencyMaxSeconds",fallback==null?2:fallback.emergencyMaxSeconds,emergencyMinSeconds,60);
   observedMissileMemorySeconds=r.number(k+"observedMissileMemorySeconds",fallback==null?12:fallback.observedMissileMemorySeconds,0,120);
   abortHealthFraction=r.number(k+"abortHealthFraction",fallback==null?.35:fallback.abortHealthFraction,0,1);
   minimumStateHoldSeconds=r.number(k+"minimumStateHoldSeconds",fallback==null?2:fallback.minimumStateHoldSeconds,0,30);
   safeClearSeconds=r.number(k+"safeClearSeconds",fallback==null?6:fallback.safeClearSeconds,0,120);
   failedApproachLimit=r.integer(k+"failedApproachLimit",fallback==null?2:fallback.failedApproachLimit,1,20);
   decisionIntervalSeconds=r.number(k+"decisionIntervalSeconds",fallback==null?.25:fallback.decisionIntervalSeconds,.01,5);
  }
 }

 public static final class CountermeasureConfig {
  public final int inventory,burstUnits;
  public final double cooldownSeconds,effectSeconds,reserveFraction;
  CountermeasureConfig(Reader r,String k,int count,CountermeasureConfig fallback){
   inventory=r.integer(k+"inventory",count,0,2000);
   burstUnits=r.integer(k+"burstUnits",fallback==null?3:fallback.burstUnits,1,100);
   cooldownSeconds=r.number(k+"cooldownSeconds",fallback==null?1.5:fallback.cooldownSeconds,.05,60);
   effectSeconds=r.number(k+"effectSeconds",fallback==null?3:fallback.effectSeconds,.05,30);
   reserveFraction=r.number(k+"reserveFraction",fallback==null?.1:fallback.reserveFraction,0,1);
  }
  public int reserveUnits(){return (int)Math.ceil(inventory*reserveFraction);}
  /** The last partial burst is allowed; immediate missile danger may consume the reserve. */
  public int burstAvailable(int remaining,boolean immediateThreat){
   return Math.min(burstUnits,Math.max(0,remaining-(immediateThreat?0:reserveUnits())));
  }
 }

 /** Fixed spawn preset. Only WP-0 and WP-1 can be activated by this build. */
 public static final class Preset {
  public final String id,weaponDefinitionId;
  public final int bombs,bombClass;
  public final double bombMassKg,explosiveKg,blastRadiusKm;
  public final double minReleaseAglM,maxReleaseAglM,minReleaseSpeedKmh,maxReleaseSpeedKmh;
  public final double headingToleranceDeg,stableAimSeconds,cooldownSeconds,releaseToleranceKm;
  Preset(Reader r,String k,boolean intrusion,int count){
   id=r.text(k+"id",intrusion?"WP-0":"WP-1");
   if(!"WP-0".equals(id)&&!"WP-1".equals(id))throw new IllegalArgumentException(k+"id only WP-0/WP-1 are enabled");
   bombs=r.integer(k+"bombs",armed()?count:0,0,64);
   if(!armed()&&bombs!=0)throw new IllegalArgumentException(k+"bombs must be zero for WP-0");
   if(armed()&&bombs==0)throw new IllegalArgumentException(k+"bombs must be positive for WP-1");
   weaponDefinitionId=r.text(k+"weaponDefinitionId",armed()?"OFAB-100":"NONE");
   if(!armed()&&!"NONE".equals(weaponDefinitionId))throw new IllegalArgumentException(k+"WP-0 cannot carry a ground weapon");
   bombClass=r.integer(k+"bombClass",1,0,6);
   bombMassKg=r.number(k+"bombMassKg",114,1,10000);explosiveKg=r.number(k+"explosiveKg",38,0,bombMassKg);
   blastRadiusKm=r.number(k+"blastRadiusKm",.13,.01,5);
   minReleaseAglM=r.number(k+"minReleaseAglM",500,50,30000);
   maxReleaseAglM=r.number(k+"maxReleaseAglM",8000,minReleaseAglM,30000);
   minReleaseSpeedKmh=r.number(k+"minReleaseSpeedKmh",300,1,4000);
   maxReleaseSpeedKmh=r.number(k+"maxReleaseSpeedKmh",1200,minReleaseSpeedKmh,4000);
   headingToleranceDeg=r.number(k+"headingToleranceDeg",12,.1,90);
   stableAimSeconds=r.number(k+"stableAimSeconds",1.2,0,30);
   cooldownSeconds=r.number(k+"cooldownSeconds",.35,.01,30);
   releaseToleranceKm=r.number(k+"releaseToleranceKm",.35,.01,5);
  }
  public boolean armed(){return "WP-1".equals(id);}
  /** Provisional vacuum ballistic solution; game movement must use the same fall model. */
  public double fallSeconds(double altitudeAglM){return Math.sqrt(2*Math.max(0,altitudeAglM)/G);}
  public double leadKm(double altitudeAglM,double speedKmh){return Math.max(0,speedKmh)/3600*fallSeconds(altitudeAglM);}
  public boolean releaseEnvelope(double rangeKm,double headingErrorRad,double altitudeAglM,double speedKmh,double stableSeconds,double sinceReleaseSeconds){
   if(!armed()||!finite(rangeKm,headingErrorRad,altitudeAglM,speedKmh,stableSeconds,sinceReleaseSeconds))return false;
   if(altitudeAglM<minReleaseAglM||altitudeAglM>maxReleaseAglM||speedKmh<minReleaseSpeedKmh||speedKmh>maxReleaseSpeedKmh)return false;
   if(stableSeconds<stableAimSeconds||sinceReleaseSeconds<cooldownSeconds||rangeKm<0)return false;
   double heading=Math.abs(Math.atan2(Math.sin(headingErrorRad),Math.cos(headingErrorRad)));
   double lead=leadKm(altitudeAglM,speedKmh);
   double miss=Math.sqrt(Math.max(0,rangeKm*rangeKm+lead*lead-2*rangeKm*lead*Math.cos(heading)));
   return heading<=Math.toRadians(headingToleranceDeg)&&miss<=releaseToleranceKm;
  }
 }

 public static final class Profile {
  public final int index,rwrGeneration,countermeasureInventory;
  public final String id,name,originalRole,missionTask,referenceVariant,referenceMode,rwrId;
  public final boolean provisional,maws;
  public final double maxSpeedKmh,lowAltitudeSpeedKmh,referenceAltitudeM,ceilingM,structuralG,commandedG,maneuverability,stealthLevel,baselineSignature,maxHealth;
  public final double cruiseSpeedKmh,spawnAltitudeM,lengthM,spanM,accelerationKmhPerSecond,decelerationKmhPerSecond;
  public final double turnEnergyKmhPerSecondPerG,minimumSpeedKmh,cornerSpeedKmh,damageSpeedFraction,loadSpeedFraction,climbRateMps,descentRateMps;
  public final AIConfig ai;
  public final CountermeasureConfig countermeasures;
  public final Preset preset;
  Profile(Reader r,int i,String key,String label,String role,String task,String variant,double max,double low,double ref,double ceiling,double structural,double command,double handling,int count,int cm,int gen,AIConfig defaults,CountermeasureConfig cmDefaults){
   index=i;id=key;String k="aircraft."+key+".";
   name=r.text(k+"name",label);originalRole=r.text(k+"originalRole",role);missionTask=r.text(k+"missionTask",task);
   referenceVariant=r.text(k+"referenceVariant",variant);referenceMode=r.text(k+"referenceMode",i==TU160?"PROVISIONAL GAME PROFILE":"ROUNDED UPGRADED REALISTIC INSPIRATION");
   provisional=r.bool(k+"provisional",i==TU160);
   maxSpeedKmh=r.number(k+"maxSpeedKmh",max,100,6000);lowAltitudeSpeedKmh=r.number(k+"lowAltitudeSpeedKmh",low,100,maxSpeedKmh);
   referenceAltitudeM=r.number(k+"referenceAltitudeM",ref,1,40000);ceilingM=r.number(k+"ceilingM",ceiling,referenceAltitudeM,40000);
   structuralG=r.number(k+"structuralG",structural,1,20);commandedG=r.number(k+"commandedG",command,1,structuralG);
   maneuverability=r.number(k+"maneuverability",handling,1,10);stealthLevel=r.number(k+"stealthLevel",0,0,7);
   baselineSignature=r.number(k+"baselineSignature",i==TU160?1.5:i==MIG25?1.15:1,.01,10);
   maxHealth=r.number(k+"maxHealth",100,1,10000);maws=r.bool(k+"maws",false);
   rwrGeneration=r.integer(k+"rwrGeneration",gen,0,3);rwrId=r.text(k+"rwrId",new String[]{"RWR-S27","RWR-M29","RWR-M25","RWR-T160"}[i]);
   countermeasures=new CountermeasureConfig(r,k+"countermeasures.",cm,cmDefaults);countermeasureInventory=countermeasures.inventory;
   ai=new AIConfig(r,k+"ai.",defaults);preset=new Preset(r,k+"preset.",i==MIG25,count);
   cruiseSpeedKmh=r.number(k+"cruiseSpeedKmh",new double[]{850,900,1200,800}[i],100,lowAltitudeSpeedKmh);
   spawnAltitudeM=r.number(k+"spawnAltitudeM",new double[]{2500,3000,12000,4500}[i],100,ceilingM);
   lengthM=r.number(k+"lengthM",new double[]{21.9,17.3,23.8,54.1}[i],1,100);
   spanM=r.number(k+"spanM",new double[]{14.7,11.4,14,40}[i],1,100);
   accelerationKmhPerSecond=r.number(k+"accelerationKmhPerSecond",new double[]{30,35,22,12}[i],.1,200);
   decelerationKmhPerSecond=r.number(k+"decelerationKmhPerSecond",25,.1,200);
   minimumSpeedKmh=r.number(k+"minimumSpeedKmh",new double[]{240,230,300,300}[i],50,cruiseSpeedKmh);
   cornerSpeedKmh=r.number(k+"cornerSpeedKmh",new double[]{750,700,1000,750}[i],minimumSpeedKmh,maxSpeedKmh);
   turnEnergyKmhPerSecondPerG=r.number(k+"turnEnergyKmhPerSecondPerG",7,.01,100);
   damageSpeedFraction=r.number(k+"damageSpeedFraction",.4,0,.95);loadSpeedFraction=r.number(k+"loadSpeedFraction",.08,0,.8);
   climbRateMps=r.number(k+"climbRateMps",new double[]{45,50,35,15}[i],.1,200);descentRateMps=r.number(k+"descentRateMps",new double[]{60,65,45,20}[i],.1,200);
  }
  /** A ceiling on intended speed, not an instantaneous velocity change. */
  public double maxSpeedKmh(double altitudeM,double healthFraction,double loadFraction){
   double altitude=clamp(altitudeM/referenceAltitudeM,0,1);
   double cap=lowAltitudeSpeedKmh+(maxSpeedKmh-lowAltitudeSpeedKmh)*altitude;
   return Math.max(0,cap*(1-damageSpeedFraction*(1-clamp(healthFraction,0,1)))*(1-loadSpeedFraction*clamp(loadFraction,0,1)));
  }
  /** Lift/energy limits reduce commanded G below corner speed; damage reduces authority. */
  public double availableG(double speedKmh,double healthFraction){
   double health=clamp(healthFraction,0,1),energy=clamp(speedKmh/cornerSpeedKmh,0,1);
   double safe=1+(Math.min(structuralG,commandedG)-1)*(.25+.75*health)*energy*energy;
   return Math.min(structuralG,Math.min(commandedG,safe));
  }
  /** Coordinated horizontal turn rate; handling modifies response without exceeding G. */
  public double turnRateRad(double speedKmh,double healthFraction){
   if(speedKmh<=0||!finite(speedKmh,healthFraction))return 0;
   double limit=availableG(speedKmh,healthFraction),response=.65+.35*(maneuverability-1)/9;
   return G*Math.sqrt(Math.max(0,limit*limit-1))/(speedKmh/3.6)*response;
  }
  public double turnRadiusKm(double speedKmh,double healthFraction){double rate=turnRateRad(speedKmh,healthFraction);return rate>0?speedKmh/3600/rate:Double.POSITIVE_INFINITY;}
  /** Finite acceleration/deceleration, with induced-drag energy cost during loaded turns.
   * A suddenly reduced cap is approached by deceleration rather than teleporting speed. */
  public double advanceSpeedKmh(double current,double desired,double altitudeM,double healthFraction,double loadFraction,double actualG,double dt){
   if(!finite(current,desired,altitudeM,healthFraction,loadFraction,actualG,dt)||dt<=0)return Math.max(0,current);
   double health=clamp(healthFraction,0,1),load=clamp(loadFraction,0,1);
   double target=clamp(desired,0,maxSpeedKmh(altitudeM,health,load));
   double accel=accelerationKmhPerSecond*(.25+.75*health)*(1-.15*load);
   double response=clamp(target-current,-decelerationKmhPerSecond*dt,accel*dt);
   double usedG=clamp(Math.abs(actualG),1,availableG(current,health));
   double handlingLoss=1.3-.5*(maneuverability-1)/9;
   double turnLoss=(usedG-1)*turnEnergyKmhPerSecondPerG*handlingLoss*dt;
   return Math.max(0,current+response-turnLoss);
  }
 }

 public final Profile[] all;
 public final Route route;
 public final AIConfig ai;
 public final CountermeasureConfig countermeasures;
 public AircraftProfiles(Properties properties){
  Reader r=new Reader(properties);route=new Route(r);ai=new AIConfig(r,"ai.",null);countermeasures=new CountermeasureConfig(r,"countermeasures.",0,null);
  all=new Profile[]{
   new Profile(r,SU27,"su27","Su-27","AIR-SUPERIORITY FIGHTER","BOMB STRIKE","Su-27",2400,1400,12000,16000,11,9,8,4,96,2,ai,countermeasures),
   new Profile(r,MIG29,"mig29","MiG-29","FRONTLINE FIGHTER","BOMB STRIKE","MiG-29 (9-13)",2350,1450,14000,16000,13,9,8.5,2,60,2,ai,countermeasures),
   new Profile(r,MIG25,"mig25","MiG-25","HIGH-ALTITUDE INTERCEPTOR","INTRUSION / DIVERSION","MiG-25PD",2940,1200,18000,25000,7,5,3,0,64,1,ai,countermeasures),
   new Profile(r,TU160,"tu160","Tu-160","STRATEGIC BOMBER / MISSILE CARRIER","SIMPLIFIED BOMB STRIKE","NO VERIFIED WT FLIGHT RECORD",2200,1000,12000,16000,3,2,1.5,8,128,2,ai,countermeasures)
  };
  r.finish();
 }
 public Profile profile(int index){if(index<0||index>=all.length)throw new IllegalArgumentException("Unknown aircraft index: "+index);return all[index];}
 public Profile profile(String id){for(Profile p:all)if(p.id.equals(id))return p;throw new IllegalArgumentException("Unknown aircraft: "+id);}
 public static AircraftProfiles defaults(){return new AircraftProfiles(new Properties());}
 public static AircraftProfiles load(InputStream in)throws IOException{Properties p=new Properties();p.load(in);return new AircraftProfiles(p);}
 private static double clamp(double value,double low,double high){return Math.max(low,Math.min(high,value));}
 private static boolean finite(double... values){for(double x:values)if(Double.isNaN(x)||Double.isInfinite(x))return false;return true;}
 private static final class Reader {
  final Properties p;final Set<String> used=new HashSet<String>();
  Reader(Properties p){this.p=p;}
  double number(String k,double d,double min,double max){used.add(k);return Equipment.number(p,k,d,min,max);}
  int integer(String k,int d,int min,int max){used.add(k);return Equipment.integer(p,k,d,min,max);}
  boolean bool(String k,boolean d){used.add(k);return Equipment.bool(p,k,d);}
  String text(String k,String d){used.add(k);String s=p.getProperty(k,d).trim();if(s.length()==0||s.length()>100)throw new IllegalArgumentException(k+" needs 1-100 characters");return s;}
  void finish(){for(String k:p.stringPropertyNames())if(!used.contains(k))throw new IllegalArgumentException("Unknown aircraft property: "+k);}
 }
}

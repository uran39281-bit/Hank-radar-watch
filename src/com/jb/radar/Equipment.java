package com.jb.radar;
import java.io.*;
import java.util.*;
/** Editable fictional game balance. Units are explicit; invalid profiles fail as a whole. */
public final class Equipment {
 public enum Guidance { RADAR, INFRARED, COMMAND, LASER }
 public enum RadarMode { NONE, ACTIVE, SEMI_ACTIVE, PASSIVE }
 public enum WarheadType { BLAST_FRAGMENTATION, FRAGMENTATION, KINETIC }
 public static final class Radar {
  public final String name,role,nctrMemory;
  public final int launcherCapacity,reserveRounds,irstCapacity;
  public final double switchSeconds,lockAcquireSeconds,lockLossSeconds,freshSeconds,trackTimeoutSeconds,contactTimeoutSeconds,reloadSeconds,sweepSlowSeconds,sweepFastSeconds,irstRangeKm,irstUpdateSeconds,irstAcquireSeconds,irstTrackTimeoutSeconds,irstSwitchSeconds,irstSensitivity;
  public final boolean retainTracks;
  public final int detectionKm,tracks,channels,illuminationChannels,midcourseChannels,supportedMissiles,acquireObservations;
  public final double trackingKm,lockKm,irLockKm,scanSpeed,detectionAbsoluteKm,trackingAbsoluteKm,lockAbsoluteKm,trackQualityMinimum,lockQualityMinimum;
  public final boolean infrared;
  Radar(Properties p){name=name(p,"radar.name","ADS-201 Watchpost");detectionKm=integer(p,"radar.detectionKm",30,1,100);trackingKm=number(p,"radar.trackingKm",24,1,detectionKm);lockKm=number(p,"radar.lockKm",16,1,trackingKm);tracks=integer(p,"radar.tracks",5,1,72);channels=integer(p,"radar.channels",2,0,72);scanSpeed=number(p,"radar.scanSpeed",1.00,1,6);infrared=bool(p,"radar.infrared",false);irLockKm=number(p,"radar.irLockKm",0,0,100);
   detectionAbsoluteKm=number(p,"radar.detectionAbsoluteKm",detectionKm*1.2,detectionKm,120);
   trackingAbsoluteKm=number(p,"radar.trackingAbsoluteKm",Math.min(detectionAbsoluteKm,trackingKm*1.25),trackingKm,detectionAbsoluteKm);
   lockAbsoluteKm=number(p,"radar.lockAbsoluteKm",Math.min(trackingAbsoluteKm,lockKm*1.25),lockKm,trackingAbsoluteKm);
   illuminationChannels=integer(p,"radar.illuminationChannels",Math.min(channels,tracks),0,tracks);
   midcourseChannels=integer(p,"radar.midcourseChannels",channels,0,72);
   supportedMissiles=integer(p,"radar.supportedMissiles",6,1,72);
   acquireObservations=integer(p,"radar.acquireObservations",2,2,8);
   trackQualityMinimum=number(p,"radar.trackQualityMinimum",.25,.05,1);
   lockQualityMinimum=number(p,"radar.lockQualityMinimum",.30,trackQualityMinimum,1);
   role=name(p,"radar.role","LOCAL AIR DEFENSE");nctrMemory=name(p,"radar.nctrMemory","CAMPAIGN AIRCRAFT");
   switchSeconds=number(p,"radar.switchSeconds",1.5,0,30);lockAcquireSeconds=number(p,"radar.lockAcquireSeconds",1.2,0,30);lockLossSeconds=number(p,"radar.lockLossSeconds",1,0,30);
   sweepSlowSeconds=number(p,"radar.sweepSlowSeconds",10,.2,60);sweepFastSeconds=number(p,"radar.sweepFastSeconds",5,.1,sweepSlowSeconds);
   freshSeconds=number(p,"radar.freshSeconds",sweepSeconds()*1.15,.1,120);trackTimeoutSeconds=number(p,"radar.trackTimeoutSeconds",Math.max(22,sweepSeconds()*2.2),freshSeconds,180);contactTimeoutSeconds=number(p,"radar.contactTimeoutSeconds",Math.max(32,sweepSeconds()*3.2),trackTimeoutSeconds,300);
   retainTracks=bool(p,"radar.retainTracks",true);launcherCapacity=integer(p,"radar.launcherCapacity",3,1,12);reserveRounds=integer(p,"radar.reserveRounds",12,0,500);reloadSeconds=number(p,"radar.reloadSeconds",90,.1,600);
   irstRangeKm=number(p,"radar.irstRangeKm",irLockKm>0?irLockKm:12,.1,200);irstUpdateSeconds=number(p,"radar.irstUpdateSeconds",1,.05,30);irstCapacity=integer(p,"radar.irstCapacity",4,1,72);irstAcquireSeconds=number(p,"radar.irstAcquireSeconds",1.5,0,30);irstTrackTimeoutSeconds=number(p,"radar.irstTrackTimeoutSeconds",4,irstUpdateSeconds,180);irstSwitchSeconds=number(p,"radar.irstSwitchSeconds",.6,0,30);irstSensitivity=number(p,"radar.irstSensitivity",1,.01,10);
  }
  public double sweepSeconds(){return sweepSlowSeconds+(sweepFastSeconds-sweepSlowSeconds)*(scanSpeed-1)/5;}
 }
 public static final class Weapon {
  public final String id,name,role;
  public final double maneuverability;public final WarheadType warheadType;
  public final double maneuverG,warheadKg,fuzeKm,directHitKm,damageScale,damageExponent,kineticScale,seekerAcquireSeconds,seekerSensitivity;
  public final boolean irLockAfterLaunch;
  public final Guidance guidance;
  public final RadarMode radarMode;
  public final double guidanceSeconds,maxSpeedKmh,maxG,massKg,maxAoADeg,thrustN,burnSeconds,rangeKm,minRangeKm,ceilingM,seekerKm,explosiveKg,blastRadiusKm,supportRecoverySeconds,seekerSearchSeconds,seekerFovDeg;
  public final boolean earlyActivation,retargeting;
  Weapon(Properties p,String key,String title,Guidance guide,RadarMode mode,double time,double speed,double g,double mass,double aoa,double thrust,double burn,double range,double min,double ceiling){
   id=key;String k="missile."+key+".";name=name(p,k+"name",title);guidance=enumeration(p,k+"guidance",guide,Guidance.class);radarMode=enumeration(p,k+"radarMode",mode,RadarMode.class);
   if((guidance==Guidance.RADAR)==(radarMode==RadarMode.NONE))throw new IllegalArgumentException(k+"radarMode must match guidance");
   guidanceSeconds=number(p,k+"guidanceSeconds",time,1,300);maxSpeedKmh=number(p,k+"maxSpeedKmh",speed,100,10000);maxG=number(p,k+"maxG",g,.1,100);massKg=number(p,k+"massKg",mass,1,10000);maxAoADeg=number(p,k+"maxAoADeg",aoa,1,70);thrustN=number(p,k+"thrustN",thrust,0,1000000);burnSeconds=number(p,k+"burnSeconds",burn,0,guidanceSeconds);rangeKm=number(p,k+"rangeKm",range,.1,200);minRangeKm=number(p,k+"minRangeKm",min,0,rangeKm);ceilingM=number(p,k+"ceilingM",ceiling,100,40000);seekerKm=number(p,k+"seekerKm",key.equals("ir6")?9:8,.1,200);
   supportRecoverySeconds=number(p,k+"supportRecoverySeconds",4,.1,30);
   seekerSearchSeconds=number(p,k+"seekerSearchSeconds",6,.1,30);
   seekerFovDeg=number(p,k+"seekerFovDeg",60,5,180);
   earlyActivation=bool(p,k+"earlyActivation",true);retargeting=bool(p,k+"retargeting",true);
   // Fictional warhead charge and blast footprint; independent of launch mass.
   explosiveKg=number(p,k+"explosiveKg",key.equals("kh29")||key.equals("kh58")?87.1:key.equals("kh23")||key.equals("kh27")?44.4:key.equals("kh25")?24.5:15,0,10000);
   blastRadiusKm=number(p,k+"blastRadiusKm",key.equals("kh29")||key.equals("kh58")?.32:.20,.01,5);
   role=name(p,k+"role",guide==Guidance.INFRARED?"SHORT-RANGE DEFENSE":"AIR DEFENSE");
   maneuverability=number(p,k+"maneuverability",Math.max(1,Math.min(10,(g-2)/2)),1,10);
   double turnLow=number(p,"missile.turnGAtOne",4,.1,100),turnHigh=number(p,"missile.turnGAtTen",22,turnLow,100);
   maneuverG=turnLow+(turnHigh-turnLow)*(maneuverability-1)/9.0;
   warheadType=enumeration(p,k+"warheadType",WarheadType.BLAST_FRAGMENTATION,WarheadType.class);warheadKg=number(p,k+"warheadKg",explosiveKg,0,10000);
   directHitKm=number(p,k+"directHitKm",.025,.001,1);fuzeKm=number(p,k+"fuzeKm",.12,directHitKm,5);damageScale=number(p,k+"damageScale",12,0,1000);damageExponent=number(p,k+"damageExponent",1.2,.1,10);kineticScale=number(p,k+"kineticScale",.002,.000001,100);
   irLockAfterLaunch=bool(p,k+"irLockAfterLaunch",true);seekerAcquireSeconds=number(p,k+"seekerAcquireSeconds",.35,0,10);seekerSensitivity=number(p,k+"seekerSensitivity",.5,.01,10);
  }
  public String guidanceLabel(){return guidance==Guidance.RADAR?"RADAR / "+radarMode.toString().replace('_',' '):guidance.toString();}
 }
 public final Radar radar;
 public final Weapon primary,stonebolt,active,infrared;
 public final Weapon[] enemy,all;
 public Equipment(Properties p){
  radar=new Radar(p);
  primary=new Weapon(p,"rampart","MIM-301 Rampart",Guidance.RADAR,RadarMode.SEMI_ACTIVE,35,2400,14,140,12,18000,5,24,0,13700);
  stonebolt=new Weapon(p,"stonebolt","FIM-352 Stonebolt",Guidance.RADAR,RadarMode.SEMI_ACTIVE,40,2700,12,180,10,22000,6,28,0,13700);
  active=new Weapon(p,"active","MIM-303 Sentinel",Guidance.RADAR,RadarMode.ACTIVE,40,2600,16,160,16,21000,6,26,.8,13700);
  infrared=new Weapon(p,"ir6","FIM-306 Ember",Guidance.INFRARED,RadarMode.NONE,28,2592,22,90,35,14400,4,12,.6,6000);
  if(primary.guidance==Guidance.INFRARED||primary.guidance==Guidance.LASER)throw new IllegalArgumentException("Primary slot supports RADAR or COMMAND");
  if(infrared!=null&&infrared.guidance!=Guidance.INFRARED)throw new IllegalArgumentException("IR slot requires INFRARED");
  enemy=new Weapon[]{
   new Weapon(p,"kh25","Kh-25ML",Guidance.LASER,RadarMode.NONE,45,2448,8,300,20,36000,6,10,3,20000),
   new Weapon(p,"kh29","Kh-29L",Guidance.LASER,RadarMode.NONE,40,2196,7,650,18,78000,5,13,3,20000),
   new Weapon(p,"kh23","Kh-23M",Guidance.COMMAND,RadarMode.NONE,25,2448,9,280,22,33600,6,10,3,20000),
   new Weapon(p,"kh27","Kh-27PS",Guidance.RADAR,RadarMode.PASSIVE,70,2592,8,320,20,38400,6,26,3,20000),
   new Weapon(p,"kh58","Kh-58U",Guidance.RADAR,RadarMode.PASSIVE,80,3060,9,640,22,76800,7,32,3,20000)};
  all=new Weapon[]{primary,stonebolt,active,infrared,enemy[0],enemy[1],enemy[2],enemy[3],enemy[4]};
  HashSet<String> allowed=new HashSet<>();for(String k:new String[]{"name","detectionKm","trackingKm","lockKm","tracks","channels","scanSpeed","infrared","irLockKm","detectionAbsoluteKm","trackingAbsoluteKm","lockAbsoluteKm","illuminationChannels","midcourseChannels","supportedMissiles","acquireObservations","trackQualityMinimum","lockQualityMinimum","role","nctrMemory","switchSeconds","lockAcquireSeconds","lockLossSeconds","freshSeconds","trackTimeoutSeconds","contactTimeoutSeconds","retainTracks","launcherCapacity","reserveRounds","reloadSeconds","sweepSlowSeconds","sweepFastSeconds","irstRangeKm","irstUpdateSeconds","irstCapacity","irstAcquireSeconds","irstTrackTimeoutSeconds","irstSwitchSeconds","irstSensitivity"})allowed.add("radar."+k);
  for(Weapon w:all)for(String k:new String[]{"name","guidance","radarMode","guidanceSeconds","maxSpeedKmh","maxG","massKg","maxAoADeg","thrustN","burnSeconds","rangeKm","minRangeKm","ceilingM","seekerKm","explosiveKg","blastRadiusKm","supportRecoverySeconds","seekerSearchSeconds","seekerFovDeg","earlyActivation","retargeting","role","maneuverability","warheadType","warheadKg","fuzeKm","directHitKm","damageScale","damageExponent","kineticScale","irLockAfterLaunch","seekerAcquireSeconds","seekerSensitivity"})allowed.add("missile."+w.id+"."+k);
  allowed.add("missile.turnGAtOne");allowed.add("missile.turnGAtTen");
  for(String k:p.stringPropertyNames())if(!allowed.contains(k))throw new IllegalArgumentException("Unknown equipment property: "+k);
 }
 public Weapon playerWeapon(String id){return "stonebolt".equals(id)?stonebolt:"active".equals(id)?active:"ir6".equals(id)?infrared:primary;}
 public static Equipment defaults(){return new Equipment(new Properties());}
 public static Equipment load(InputStream in)throws IOException{Properties p=new Properties();p.load(in);return new Equipment(p);}
 static String name(Properties p,String key,String def){String s=p.getProperty(key,def).trim();if(s.length()==0||s.length()>28)throw new IllegalArgumentException(key+" needs 1-28 characters");return s;}
 static double number(Properties p,String key,double def,double min,double max){double d;try{d=Double.parseDouble(p.getProperty(key,Double.toString(def)));}catch(NumberFormatException e){throw new IllegalArgumentException(key+" needs a number");}if(Double.isNaN(d)||Double.isInfinite(d)||d<min||d>max)throw new IllegalArgumentException(key+" outside "+min+".."+max);return d;}
 static int integer(Properties p,String key,int def,int min,int max){double d=number(p,key,def,min,max);if(d!=(int)d)throw new IllegalArgumentException(key+" needs a whole number");return (int)d;}
 static boolean bool(Properties p,String key,boolean def){String s=p.getProperty(key,Boolean.toString(def)).trim();if(!s.equals("true")&&!s.equals("false"))throw new IllegalArgumentException(key+" needs true or false");return Boolean.parseBoolean(s);}
 static <T extends Enum<T>> T enumeration(Properties p,String key,T def,Class<T> type){try{return Enum.valueOf(type,p.getProperty(key,def.name()).trim());}catch(IllegalArgumentException e){throw new IllegalArgumentException(key+" has an unsupported mode");}}
}

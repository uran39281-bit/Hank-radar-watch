package com.jb.radar;
import java.io.*;
import java.util.*;
/** Editable fictional game balance. Units are explicit; invalid profiles fail as a whole. */
public final class Equipment {
 public enum Guidance { RADAR, INFRARED, COMMAND, LASER }
 public enum RadarMode { NONE, ACTIVE, SEMI_ACTIVE, PASSIVE }
 public static final class Radar {
  public final String name;
  public final int detectionKm,tracks,channels,scanSpeed;
  public final double trackingKm,lockKm,irLockKm;
  public final boolean infrared;
  Radar(Properties p){name=name(p,"radar.name","HAWK BATTERY");detectionKm=integer(p,"radar.detectionKm",40,1,100);trackingKm=number(p,"radar.trackingKm",32,1,detectionKm);lockKm=number(p,"radar.lockKm",25,1,trackingKm);tracks=integer(p,"radar.tracks",12,1,72);channels=integer(p,"radar.channels",2,0,tracks);scanSpeed=integer(p,"radar.scanSpeed",6,1,10);infrared=bool(p,"radar.infrared",true);irLockKm=number(p,"radar.irLockKm",9,0,100);}
  public double sweepSeconds(){return 11-scanSpeed;}
 }
 public static final class Weapon {
  public final String id,name;
  public final Guidance guidance;
  public final RadarMode radarMode;
  public final double guidanceSeconds,maxSpeedKmh,maxG,massKg,maxAoADeg,thrustN,burnSeconds,rangeKm,minRangeKm,ceilingM,seekerKm;
  Weapon(Properties p,String key,String title,Guidance guide,RadarMode mode,double time,double speed,double g,double mass,double aoa,double thrust,double burn,double range,double min,double ceiling){
   id=key;String k="missile."+key+".";name=name(p,k+"name",title);guidance=enumeration(p,k+"guidance",guide,Guidance.class);radarMode=enumeration(p,k+"radarMode",mode,RadarMode.class);
   if((guidance==Guidance.RADAR)==(radarMode==RadarMode.NONE))throw new IllegalArgumentException(k+"radarMode must match guidance");
   guidanceSeconds=number(p,k+"guidanceSeconds",time,1,300);maxSpeedKmh=number(p,k+"maxSpeedKmh",speed,100,10000);maxG=number(p,k+"maxG",g,.1,100);massKg=number(p,k+"massKg",mass,1,10000);maxAoADeg=number(p,k+"maxAoADeg",aoa,1,70);thrustN=number(p,k+"thrustN",thrust,0,1000000);burnSeconds=number(p,k+"burnSeconds",burn,0,guidanceSeconds);rangeKm=number(p,k+"rangeKm",range,.1,200);minRangeKm=number(p,k+"minRangeKm",min,0,rangeKm);ceilingM=number(p,k+"ceilingM",ceiling,100,40000);seekerKm=number(p,k+"seekerKm",key.equals("ir6")?9:8,.1,200);
  }
  public String guidanceLabel(){return guidance==Guidance.RADAR?"RADAR / "+radarMode.toString().replace('_',' '):guidance.toString();}
 }
 public final Radar radar;
 public final Weapon primary,infrared;
 public final Weapon[] enemy,all;
 public Equipment(Properties p){
  radar=new Radar(p);
  primary=new Weapon(p,"hawk","MIM-23",Guidance.RADAR,RadarMode.SEMI_ACTIVE,65,3000,18,580,28,84000,5,32,0,13700);
  infrared=new Weapon(p,"ir6","IR-6",Guidance.INFRARED,RadarMode.NONE,28,2592,22,90,35,14400,4,12,.6,6000);
  if(primary.guidance==Guidance.INFRARED||primary.guidance==Guidance.LASER)throw new IllegalArgumentException("Primary slot supports RADAR or COMMAND");
  if(infrared.guidance!=Guidance.INFRARED)throw new IllegalArgumentException("IR slot requires INFRARED");
  enemy=new Weapon[]{
   new Weapon(p,"kh25","Kh-25ML",Guidance.LASER,RadarMode.NONE,45,2448,8,300,20,36000,6,10,3,20000),
   new Weapon(p,"kh29","Kh-29L",Guidance.LASER,RadarMode.NONE,40,2196,7,650,18,78000,5,13,3,20000),
   new Weapon(p,"kh23","Kh-23M",Guidance.COMMAND,RadarMode.NONE,25,2448,9,280,22,33600,6,10,3,20000),
   new Weapon(p,"kh27","Kh-27PS",Guidance.RADAR,RadarMode.PASSIVE,70,2592,8,320,20,38400,6,26,3,20000),
   new Weapon(p,"kh58","Kh-58U",Guidance.RADAR,RadarMode.PASSIVE,80,3060,9,640,22,76800,7,32,3,20000)};
  all=new Weapon[]{primary,infrared,enemy[0],enemy[1],enemy[2],enemy[3],enemy[4]};
  HashSet<String> allowed=new HashSet<>();for(String k:new String[]{"name","detectionKm","trackingKm","lockKm","tracks","channels","scanSpeed","infrared","irLockKm"})allowed.add("radar."+k);
  for(Weapon w:all)for(String k:new String[]{"name","guidance","radarMode","guidanceSeconds","maxSpeedKmh","maxG","massKg","maxAoADeg","thrustN","burnSeconds","rangeKm","minRangeKm","ceilingM","seekerKm"})allowed.add("missile."+w.id+"."+k);
  for(String k:p.stringPropertyNames())if(!allowed.contains(k))throw new IllegalArgumentException("Unknown equipment property: "+k);
 }
 public static Equipment defaults(){return new Equipment(new Properties());}
 public static Equipment load(InputStream in)throws IOException{Properties p=new Properties();p.load(in);return new Equipment(p);}
 static String name(Properties p,String key,String def){String s=p.getProperty(key,def).trim();if(s.length()==0||s.length()>28)throw new IllegalArgumentException(key+" needs 1-28 characters");return s;}
 static double number(Properties p,String key,double def,double min,double max){double d;try{d=Double.parseDouble(p.getProperty(key,Double.toString(def)));}catch(NumberFormatException e){throw new IllegalArgumentException(key+" needs a number");}if(Double.isNaN(d)||Double.isInfinite(d)||d<min||d>max)throw new IllegalArgumentException(key+" outside "+min+".."+max);return d;}
 static int integer(Properties p,String key,int def,int min,int max){double d=number(p,key,def,min,max);if(d!=(int)d)throw new IllegalArgumentException(key+" needs a whole number");return (int)d;}
 static boolean bool(Properties p,String key,boolean def){String s=p.getProperty(key,Boolean.toString(def)).trim();if(!s.equals("true")&&!s.equals("false"))throw new IllegalArgumentException(key+" needs true or false");return Boolean.parseBoolean(s);}
 static <T extends Enum<T>> T enumeration(Properties p,String key,T def,Class<T> type){try{return Enum.valueOf(type,p.getProperty(key,def.name()).trim());}catch(IllegalArgumentException e){throw new IllegalArgumentException(key+" has an unsupported mode");}}
}

package com.jb.radar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/**
 * Passive game receivers. World geometry stops at this boundary: AI receives only
 * anonymous, delayed, imperfect evidence, never a target object or its true range.
 * Distances are kilometres, headings use north=0/east=PI/2, and time is simulation time.
 * These are fictional balancing profiles, not specifications of real receivers.
 */
public final class RwrReceiver {
 public enum Kind { SEARCH, UNKNOWN_RADAR, FIRE_CONTROL, ILLUMINATION, ACTIVE_SEEKER, VISUAL_MISSILE, MAWS_MISSILE }
 public enum Waveform { SEARCH, PULSE_DOPPLER, FIRE_CONTROL, ILLUMINATION, ACTIVE_SEEKER }
 public enum Pattern { ROTATING, STEERED, OMNI }

 /** Sensor-side geometry. This object is never delivered to the pilot. */
 public static final class Pose {
  public final double x,y,z,heading,pitch,vx,vy,vz;
  public Pose(double x,double y,double z,double heading,double pitch){this(x,y,z,heading,pitch,0,0,0);}
  public Pose(double x,double y,double z,double heading,double pitch,double vx,double vy,double vz){
   this.x=x;this.y=y;this.z=z;this.heading=heading;this.pitch=pitch;this.vx=vx;this.vy=vy;this.vz=vz;
  }
 }

 public static final class Emitter {
  public final String band;
  public final Waveform waveform;
  public final Pattern pattern;
  /** Relative power at 1 km, beam widths in degrees, and out-of-beam power fraction. */
  public final double strength,horizontalDegrees,verticalDegrees,sideLobeGain;
  public Emitter(String band,Waveform waveform,Pattern pattern,double strength,double horizontalDegrees,double verticalDegrees,double sideLobeGain){
   if(band==null||waveform==null||pattern==null||!finite(strength)||strength<0||horizontalDegrees<=0||horizontalDegrees>360||verticalDegrees<=0||verticalDegrees>180||sideLobeGain<0||sideLobeGain>1)throw new IllegalArgumentException("Invalid emitter configuration");
   this.band=band.toUpperCase(Locale.US);this.waveform=waveform;this.pattern=pattern;this.strength=strength;this.horizontalDegrees=horizontalDegrees;this.verticalDegrees=verticalDegrees;this.sideLobeGain=sideLobeGain;
  }
 }

 /** The opaque source token is private to the sensor layer and is never returned. */
 public static final class Emission {
  private final String sourceToken;
  public final Emitter emitter;
  public final double x,y,z,heading,pitch;
  public Emission(String opaqueSourceToken,Emitter emitter,double x,double y,double z,double heading,double pitch){
   if(opaqueSourceToken==null||emitter==null)throw new IllegalArgumentException("Emission source and profile required");
   sourceToken=opaqueSourceToken;this.emitter=emitter;this.x=x;this.y=y;this.z=z;this.heading=heading;this.pitch=pitch;
  }
 }

 public static final class Profile {
  public final String id;
  public final int generation,capacity;
  public final Set<String> bands;
  public final Set<Waveform> waveforms;
  public final Set<Kind> library;
  public final boolean enabled,searchWarning,fireControlRecognition,illuminationRecognition,activeSeekerRecognition,visual,maws;
  public final double sensitivity,horizontalDegrees,verticalDegrees,rearBlindDegrees,bearingErrorDegrees,sectorDegrees;
  public final double delaySeconds,memorySeconds,missChance,ambiguityChance,sampleSeconds;
  public final double visualRangeKm,visualHorizontalDegrees,visualVerticalDegrees,visualAcquireSeconds,visualMemorySeconds,visualMissChance,mawsRangeKm,mawsDelaySeconds;
  private Profile(Properties p,String id,int generation,Profile inherited){
   this.id=id;String k="profile."+id+".";int g=integer(p,k+"generation",inherited==null?generation:inherited.generation,0,3);this.generation=g;
   enabled=bool(p,k+"enabled",inherited==null?g>0:inherited.enabled);
   bands=Collections.unmodifiableSet(strings(p.getProperty(k+"bands",inherited==null?(g==1?"H,I,J":g==2?"G,H,I":g==3?"C,D,E,F,G,H,I,J":""):join(inherited.bands))));
   waveforms=Collections.unmodifiableSet(enums(p.getProperty(k+"waveforms",inherited==null?"SEARCH,PULSE_DOPPLER,FIRE_CONTROL,ILLUMINATION,ACTIVE_SEEKER":join(inherited.waveforms)),Waveform.class));
   library=Collections.unmodifiableSet(enums(p.getProperty(k+"library",inherited==null?(g>=2?"SEARCH,FIRE_CONTROL,ILLUMINATION,ACTIVE_SEEKER":"SEARCH,FIRE_CONTROL"):join(inherited.library)),Kind.class));
   searchWarning=bool(p,k+"searchWarning",inherited==null?g>0:inherited.searchWarning);
   fireControlRecognition=bool(p,k+"fireControlRecognition",inherited==null?g>0:inherited.fireControlRecognition);
   illuminationRecognition=bool(p,k+"illuminationRecognition",inherited==null?g>=2:inherited.illuminationRecognition);
   activeSeekerRecognition=bool(p,k+"activeSeekerRecognition",inherited==null?g>=2:inherited.activeSeekerRecognition);
   sensitivity=num(p,k+"sensitivity",inherited==null?(g==3?.0001:.0002):inherited.sensitivity,.000000001,100);
   horizontalDegrees=num(p,k+"horizontalDegrees",inherited==null?360:inherited.horizontalDegrees,1,360);
   verticalDegrees=num(p,k+"verticalDegrees",inherited==null?120:inherited.verticalDegrees,1,180);
   rearBlindDegrees=num(p,k+"rearBlindDegrees",inherited==null?0:inherited.rearBlindDegrees,0,360);
   bearingErrorDegrees=num(p,k+"bearingErrorDegrees",inherited==null?(g==3?5:15):inherited.bearingErrorDegrees,0,180);
   sectorDegrees=num(p,k+"sectorDegrees",inherited==null?(g==1?90:0):inherited.sectorDegrees,0,360);
   delaySeconds=num(p,k+"delaySeconds",inherited==null?(g==1?1.2:g==3?.3:.7):inherited.delaySeconds,0,30);
   memorySeconds=num(p,k+"memorySeconds",inherited==null?(g==1?6:g==3?10:8):inherited.memorySeconds,delaySeconds+.01,120);
   capacity=integer(p,k+"capacity",inherited==null?(g==0?0:g==1?4:g==3?16:8):inherited.capacity,g==0?0:1,64);
   missChance=num(p,k+"missChance",inherited==null?(g==1?.05:g==3?.01:.03):inherited.missChance,0,1);
   ambiguityChance=num(p,k+"ambiguityChance",inherited==null?(g==1?.08:g==3?.01:.04):inherited.ambiguityChance,0,1);
   sampleSeconds=num(p,k+"sampleSeconds",inherited==null?.25:inherited.sampleSeconds,.02,5);
   visual=bool(p,k+"visual",inherited==null?true:inherited.visual);
   maws=bool(p,k+"maws",inherited==null?false:inherited.maws);
   visualRangeKm=num(p,k+"visualRangeKm",inherited==null?3.5:inherited.visualRangeKm,.01,20);
   visualHorizontalDegrees=num(p,k+"visualHorizontalDegrees",inherited==null?120:inherited.visualHorizontalDegrees,1,360);
   visualVerticalDegrees=num(p,k+"visualVerticalDegrees",inherited==null?90:inherited.visualVerticalDegrees,1,180);
   visualAcquireSeconds=num(p,k+"visualAcquireSeconds",inherited==null?1:inherited.visualAcquireSeconds,.05,30);
   visualMemorySeconds=num(p,k+"visualMemorySeconds",inherited==null?2:inherited.visualMemorySeconds,.05,60);
   visualMissChance=num(p,k+"visualMissChance",inherited==null?.12:inherited.visualMissChance,0,1);
   mawsRangeKm=num(p,k+"mawsRangeKm",inherited==null?6:inherited.mawsRangeKm,.01,30);
   mawsDelaySeconds=num(p,k+"mawsDelaySeconds",inherited==null?.5:inherited.mawsDelaySeconds,0,30);
  }
  /** Installed MAWS is a separate aircraft fit, never a benefit inferred from RWR generation. */
  public Profile withMaws(boolean installed){if(maws==installed)return this;Properties p=new Properties();p.setProperty("profile."+id+".maws",Boolean.toString(installed));return new Profile(p,id,generation,this);}
 }

 public static final class Config {
  public final Emitter search,fireControl,illumination,activeSeeker;
  public final double nightVisibility,weatherVisibility;
  private final Map<String,Profile> profiles=new LinkedHashMap<>();
  public Config(Properties p){
   for(int i=0;i<=3;i++)profiles.put("GEN-"+i,new Profile(p,"GEN-"+i,i,null));
   String[][] aliases={{"RWR-S27","GEN-2"},{"RWR-M29","GEN-2"},{"RWR-M25","GEN-1"},{"RWR-T160","GEN-2"}};
   for(String[] pair:aliases)profiles.put(pair[0],new Profile(p,pair[0],0,profiles.get(pair[1])));
   search=emitter(p,"search",Waveform.SEARCH,Pattern.ROTATING,1,8,80);
   fireControl=emitter(p,"fireControl",Waveform.FIRE_CONTROL,Pattern.STEERED,1,6,60);
   illumination=emitter(p,"illumination",Waveform.ILLUMINATION,Pattern.STEERED,1,6,60);
   activeSeeker=emitter(p,"activeSeeker",Waveform.ACTIVE_SEEKER,Pattern.STEERED,.025,60,60);
   nightVisibility=num(p,"visual.nightVisibility",.45,0,1);weatherVisibility=num(p,"visual.weatherVisibility",.9,0,1);
   validateKeys(p,profiles.keySet());
  }
  public Profile profile(String id){Profile profile=profiles.get(id);if(profile==null)throw new IllegalArgumentException("Unknown RWR profile: "+id);return profile;}
  public static Config defaults(){return new Config(new Properties());}
  public static Config load(InputStream in)throws IOException{Properties p=new Properties();p.load(in);return new Config(p);}
  private static Emitter emitter(Properties p,String name,Waveform w,Pattern pattern,double power,double h,double v){String k="emitter."+name+".";
   return new Emitter(p.getProperty(k+"band","I"),Waveform.valueOf(p.getProperty(k+"waveform",w.name()).toUpperCase(Locale.US)),Pattern.valueOf(p.getProperty(k+"pattern",pattern.name()).toUpperCase(Locale.US)),num(p,k+"strength",power,0,10000),num(p,k+"horizontalDegrees",h,1,360),num(p,k+"verticalDegrees",v,1,180),num(p,k+"sideLobeGain",0,0,1));}
 }

 /** Immutable evidence. No hidden world identity, position, range, launch count or weapon type. */
 public static final class Observation {
  public final String emitterId;
  public final Kind kind;
  public final double bearing,confidence,observedAt,expiresAt;
  private Observation(String id,Signal s){emitterId=id;kind=s.kind;bearing=s.bearing;confidence=s.confidence;observedAt=s.observedAt;expiresAt=s.expiresAt;}
 }
 private static final class Signal {
  final Kind kind;final double due;double bearing,confidence,observedAt,expiresAt;
  Signal(Kind kind,double now,double delay){this.kind=kind;due=now+delay;}
 }
 private static final class Source {
  final String anonymousId;
  final EnumMap<Kind,Signal> signals=new EnumMap<>(Kind.class);
  Source(String id){anonymousId=id;}
 }
 private static final class RfSample {double last;final EnumMap<Waveform,Double> sampled=new EnumMap<>(Waveform.class);}
 private static final class VisualSample {double sampled=-1e6,first=-1,last=-1;}

 public final Profile profile;
 private final Random random;
 private int serial;
 private final LinkedHashMap<String,Source> sources=new LinkedHashMap<>();
 private final HashMap<String,RfSample> rfSamples=new HashMap<>();
 private final HashMap<String,VisualSample> visualSamples=new HashMap<>();
 public RwrReceiver(Profile profile,Random random){if(profile==null||random==null)throw new IllegalArgumentException("Receiver profile and seeded random required");this.profile=profile;this.random=random;}
 public void reset(){sources.clear();rfSamples.clear();visualSamples.clear();serial=0;}

 /** Return true only for an actually received sample; delivery still waits for receiver delay. */
 public boolean receive(double now,Pose own,Emission e,boolean lineOfSight){
  if(!finite(now)||own==null||e==null||!lineOfSight||!profile.enabled||profile.capacity==0||!profile.bands.contains(e.emitter.band)||!profile.waveforms.contains(e.emitter.waveform))return false;
  prune(now);
  double dx=e.x-own.x,dy=e.y-own.y,dz=e.z-own.z,distanceSquared=dx*dx+dy*dy+dz*dz;
  double bearing=angle(dx,dy),elevation=Math.atan2(dz,Math.hypot(dx,dy));
  double relative=delta(bearing,own.heading);
  if(!within(relative,profile.horizontalDegrees)||!within(delta(elevation,own.pitch),profile.verticalDegrees)||profile.rearBlindDegrees>0&&Math.abs(Math.abs(relative)-Math.PI)<Math.toRadians(profile.rearBlindDegrees/2))return false;
  Emitter tx=e.emitter;double gain=1;
  if(tx.pattern!=Pattern.OMNI&&(!within(delta(angle(-dx,-dy),e.heading),tx.horizontalDegrees)||!within(delta(-elevation,e.pitch),tx.verticalDegrees)))gain=tx.sideLobeGain;
  double strength=tx.strength*gain/Math.max(.01,distanceSquared);
  if(!finite(strength)||strength<profile.sensitivity)return false;
  String token="rf:"+e.sourceToken;RfSample sampled=rfSamples.get(token);
  if(sampled==null){sampled=new RfSample();rfSamples.put(token,sampled);}
  Double previous=sampled.sampled.get(tx.waveform);if(previous!=null&&now-previous<profile.sampleSeconds-1e-9)return false;
  sampled.sampled.put(tx.waveform,now);sampled.last=now;
  if(chance(profile.missChance))return false;
  Kind kind=classify(tx.waveform);
  if(kind==null)return false;
  boolean ambiguous=chance(profile.ambiguityChance);
  if(ambiguous)kind=Kind.UNKNOWN_RADAR;
  Source source=source(token,priority(kind),now);if(source==null)return false;
  double confidence=clamp(.5+.4*(1-profile.sensitivity/Math.max(profile.sensitivity,strength)),.25,.9);
  if(ambiguous||kind==Kind.UNKNOWN_RADAR)confidence*=.65;
  add(source,kind,now,profile.delaySeconds,profile.memorySeconds,estimatedBearing(bearing,own.heading),confidence);
  return true;
 }

 private Kind classify(Waveform waveform){
  switch(waveform){
   case SEARCH:case PULSE_DOPPLER:return !profile.searchWarning?null:profile.library.contains(Kind.SEARCH)?Kind.SEARCH:Kind.UNKNOWN_RADAR;
   case FIRE_CONTROL:return profile.fireControlRecognition&&profile.library.contains(Kind.FIRE_CONTROL)?Kind.FIRE_CONTROL:Kind.UNKNOWN_RADAR;
   case ILLUMINATION:return profile.illuminationRecognition&&profile.library.contains(Kind.ILLUMINATION)?Kind.ILLUMINATION:profile.fireControlRecognition&&profile.library.contains(Kind.FIRE_CONTROL)?Kind.FIRE_CONTROL:Kind.UNKNOWN_RADAR;
   case ACTIVE_SEEKER:return profile.activeSeekerRecognition&&profile.library.contains(Kind.ACTIVE_SEEKER)?Kind.ACTIVE_SEEKER:Kind.UNKNOWN_RADAR;
   default:return Kind.UNKNOWN_RADAR;
  }
 }

 /**
  * Independent optical/optional MAWS sensing. Passing a missile here is not evidence:
  * it must be visible in the finite field of view for an acquisition period. Night and
  * weather reduce reach, slow acquisition and increase misses. No guidance type inferred.
  */
 public boolean observeMissile(double now,String opaqueToken,Pose own,Pose missile,boolean lineOfSight,double nightFactor,double weatherVisibility){
  if(!finite(now)||opaqueToken==null||own==null||missile==null)return false;
  prune(now);
  double dx=missile.x-own.x,dy=missile.y-own.y,dz=missile.z-own.z,distance=Math.sqrt(dx*dx+dy*dy+dz*dz),bearing=angle(dx,dy),elevation=Math.atan2(dz,Math.hypot(dx,dy));
  double closing=dx*(missile.vx-own.vx)+dy*(missile.vy-own.vy)+dz*(missile.vz-own.vz);
  double conditions=clamp(nightFactor,0,1)*clamp(weatherVisibility,0,1);
  boolean maws=profile.maws&&lineOfSight&&distance<=profile.mawsRangeKm&&closing<0;
  boolean optical=profile.visual&&lineOfSight&&conditions>0&&distance<=profile.visualRangeKm*conditions&&within(delta(bearing,own.heading),profile.visualHorizontalDegrees)&&within(delta(elevation,own.pitch),profile.visualVerticalDegrees);
  String key="visual:"+opaqueToken;VisualSample sample=visualSamples.get(key);
  if(sample==null){if(!maws&&!optical)return false;sample=new VisualSample();visualSamples.put(key,sample);}
  if(now-sample.sampled<profile.sampleSeconds-1e-9)return false;
  sample.sampled=now;
  if(!maws&&!optical){sample.first=-1;sample.last=-1;return false;}
  if(sample.last<0||now-sample.last>profile.sampleSeconds*1.6)sample.first=now;
  sample.last=now;
  if(maws){Source source=source(key,4,now);if(source==null)return false;add(source,Kind.MAWS_MISSILE,now,profile.mawsDelaySeconds,profile.visualMemorySeconds,estimatedBearing(bearing,own.heading),.65);return true;}
  if(now-sample.first<profile.visualAcquireSeconds/Math.max(.15,conditions))return false;
  double missed=clamp(profile.visualMissChance+(1-conditions)*.2,0,.95);
  if(chance(missed))return false;
  Source source=source(key,4,now);if(source==null)return false;
  // Optics provide an uncertain bearing; this simplified model grants no exact range/TTI.
  double estimate=bearing+Math.toRadians(8+(1-conditions)*15)*(random.nextDouble()*2-1);
  add(source,Kind.VISUAL_MISSILE,now,0,profile.visualMemorySeconds,wrap(estimate),.4+.4*conditions);
  return true;
 }

 /** One strongest still-valid mode per emitter; search cannot instantly clear a lock warning. */
 public List<Observation> observations(double now){
  prune(now);ArrayList<Observation> result=new ArrayList<>();
  for(Source source:sources.values()){Signal strongest=null;for(Signal signal:source.signals.values())if(now+1e-9>=signal.due&&(strongest==null||priority(signal.kind)>priority(strongest.kind)||priority(signal.kind)==priority(strongest.kind)&&signal.observedAt>strongest.observedAt))strongest=signal;
   if(strongest!=null)result.add(new Observation(source.anonymousId,strongest));
  }
  return Collections.unmodifiableList(result);
 }
 private void add(Source source,Kind kind,double now,double delay,double memory,double bearing,double confidence){
  Signal s=source.signals.get(kind);if(s==null){s=new Signal(kind,now,delay);source.signals.put(kind,s);}s.bearing=bearing;s.confidence=confidence;s.observedAt=now;s.expiresAt=now+memory;
 }
 private Source source(String token,int priority,double now){
  Source source=sources.get(token);if(source!=null)return source;
  boolean rf=token.startsWith("rf:");int count=0;for(String key:sources.keySet())if(key.startsWith("rf:")==rf)count++;
  // Receiver emitter capacity does not erase an independently seen optical missile.
  if(count>=(rf?profile.capacity:8)){String worstKey=null;int worstPriority=Integer.MAX_VALUE;double oldest=Double.POSITIVE_INFINITY;
   for(Map.Entry<String,Source> entry:sources.entrySet()){if(entry.getKey().startsWith("rf:")!=rf)continue;int p=0;double newest=-1e6;for(Signal s:entry.getValue().signals.values()){p=Math.max(p,priority(s.kind));newest=Math.max(newest,s.observedAt);}if(p<worstPriority||p==worstPriority&&newest<oldest){worstKey=entry.getKey();worstPriority=p;oldest=newest;}}
   if(worstKey==null||worstPriority>priority)return null;sources.remove(worstKey);
  }
  source=new Source("E"+(++serial));sources.put(token,source);return source;
 }
 private void prune(double now){
  for(Iterator<Source> it=sources.values().iterator();it.hasNext();){Source source=it.next();for(Iterator<Signal> s=source.signals.values().iterator();s.hasNext();)if(now>=s.next().expiresAt)s.remove();
   if(source.signals.isEmpty())it.remove();
  }
  for(Iterator<RfSample> it=rfSamples.values().iterator();it.hasNext();)if(now-it.next().last>Math.max(2,profile.sampleSeconds*4))it.remove();
  for(Iterator<VisualSample> it=visualSamples.values().iterator();it.hasNext();)if(now-it.next().sampled>Math.max(2,profile.sampleSeconds*4))it.remove();
 }
 private double estimatedBearing(double bearing,double ownHeading){
  if(profile.sectorDegrees>0){double sector=Math.toRadians(profile.sectorDegrees);return wrap(ownHeading+Math.round(delta(bearing,ownHeading)/sector)*sector);}
  return wrap(bearing+Math.toRadians(profile.bearingErrorDegrees)*(random.nextDouble()*2-1));
 }
 public static int priority(Kind kind){switch(kind){case VISUAL_MISSILE:case MAWS_MISSILE:return 4;case ILLUMINATION:case ACTIVE_SEEKER:return 3;case FIRE_CONTROL:return 2;default:return 1;}}
 private boolean chance(double probability){return probability>=1||probability>0&&random.nextDouble()>1-probability;}
 private static boolean within(double offset,double degrees){return Math.abs(offset)<=Math.toRadians(degrees/2)+1e-9;}
 private static double angle(double x,double y){return Math.atan2(x,-y);}
 private static double delta(double a,double b){return Math.atan2(Math.sin(a-b),Math.cos(a-b));}
 private static double wrap(double a){double tau=Math.PI*2;return (a%tau+tau)%tau;}
 private static double clamp(double n,double lo,double hi){return Math.max(lo,Math.min(hi,n));}
 private static boolean finite(double n){return !Double.isNaN(n)&&!Double.isInfinite(n);}
 private static double num(Properties p,String key,double def,double min,double max){double n;try{n=Double.parseDouble(p.getProperty(key,Double.toString(def)));}catch(NumberFormatException e){throw new IllegalArgumentException(key+" requires a number");}if(!finite(n)||n<min||n>max)throw new IllegalArgumentException(key+" outside "+min+".."+max);return n;}
 private static int integer(Properties p,String key,int def,int min,int max){double n=num(p,key,def,min,max);if(n!=Math.floor(n))throw new IllegalArgumentException(key+" requires an integer");return(int)n;}
 private static boolean bool(Properties p,String key,boolean def){String value=p.getProperty(key,Boolean.toString(def));if(!"true".equalsIgnoreCase(value)&&!"false".equalsIgnoreCase(value))throw new IllegalArgumentException(key+" requires true or false");return Boolean.parseBoolean(value);}
 private static Set<String> strings(String raw){HashSet<String> set=new HashSet<>();for(String value:raw.split("[,\\s]+"))if(!value.isEmpty())set.add(value.toUpperCase(Locale.US));return set;}
 private static <E extends Enum<E>> Set<E> enums(String raw,Class<E> type){EnumSet<E> result=EnumSet.noneOf(type);for(String value:strings(raw))result.add(Enum.valueOf(type,value));return result;}
 private static String join(Iterable<?> values){StringBuilder result=new StringBuilder();for(Object value:values){if(result.length()>0)result.append(',');result.append(value);}return result.toString();}
 private static void validateKeys(Properties p,Set<String> ids){
  Set<String> suffixes=new HashSet<>(Arrays.asList("generation","enabled","bands","waveforms","library","searchWarning","fireControlRecognition","illuminationRecognition","activeSeekerRecognition","sensitivity","horizontalDegrees","verticalDegrees","rearBlindDegrees","bearingErrorDegrees","sectorDegrees","delaySeconds","memorySeconds","capacity","missChance","ambiguityChance","sampleSeconds","visual","maws","visualRangeKm","visualHorizontalDegrees","visualVerticalDegrees","visualAcquireSeconds","visualMemorySeconds","visualMissChance","mawsRangeKm","mawsDelaySeconds"));
  Set<String> allowed=new HashSet<>(Arrays.asList("visual.nightVisibility","visual.weatherVisibility"));
  for(String id:ids)for(String suffix:suffixes)allowed.add("profile."+id+"."+suffix);
  for(String emitter:new String[]{"search","fireControl","illumination","activeSeeker"})for(String suffix:new String[]{"band","waveform","pattern","strength","horizontalDegrees","verticalDegrees","sideLobeGain"})allowed.add("emitter."+emitter+"."+suffix);
  for(String key:p.stringPropertyNames())if(!allowed.contains(key))throw new IllegalArgumentException("Unknown RWR property: "+key);
 }
}

package com.jb.radar;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/** Receiver boundary tests: physical reception is independent of the player's contact list. */
public final class RwrTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static Properties clear(){Properties p=new Properties();for(int g=0;g<4;g++){p.setProperty("profile.GEN-"+g+".missChance","0");p.setProperty("profile.GEN-"+g+".ambiguityChance","0");p.setProperty("profile.GEN-"+g+".visualMissChance","0");}return p;}
 static RwrReceiver.Config config(){return new RwrReceiver.Config(clear());}
 static RwrReceiver.Pose own(double range){return new RwrReceiver.Pose(0,-range,1,Math.PI,0);}
 static RwrReceiver.Emission emission(RwrReceiver.Emitter emitter,String token){return new RwrReceiver.Emission(token,emitter,0,0,.1,0,0);}
 static RwrReceiver receiver(RwrReceiver.Config c,String profile){return new RwrReceiver(c.profile(profile),new Random(16));}
 static RwrReceiver.Observation only(RwrReceiver r,double now){List<RwrReceiver.Observation> all=r.observations(now);check(all.size()==1,"one perceived emitter at "+now+", got "+all.size());return all.get(0);}

 static void defaultsAndOverrides()throws Exception{
  RwrReceiver.Config c;try(FileInputStream in=new FileInputStream("assets/rwr.properties")){c=RwrReceiver.Config.load(in);}
  check(c.profile("RWR-M25").generation==1&&c.profile("RWR-M25").capacity==4,"MiG-25 basic profile");
  check(c.profile("GEN-0").capacity==0,"absent RF receiver has zero RF emitter capacity");
  for(String id:new String[]{"RWR-S27","RWR-M29","RWR-T160"})check(c.profile(id).generation==2&&!c.profile(id).maws,"improved RWR never silently grants MAWS");
  check(c.profile("GEN-3").capacity==16&&c.profile("GEN-3").bands.contains("C"),"reserved generation remains configurable");
  check(c.profile("RWR-M25").withMaws(true).maws&&!c.profile("RWR-M25").maws&&c.profile("RWR-M25").withMaws(true).generation==1,"separate installed MAWS override does not mutate receiver or generation");
  Properties p=clear();p.setProperty("profile.RWR-S27.activeSeekerRecognition","false");c=new RwrReceiver.Config(p);check(c.profile("RWR-S27").generation==2&&!c.profile("RWR-S27").activeSeekerRecognition&&c.profile("RWR-M29").activeSeekerRecognition,"capability overrides independent of generation");
  p.setProperty("profile.GEN-2.delaySecond","3");boolean rejected=false;try{new RwrReceiver.Config(p);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"misspelled balance keys rejected");
 }

 static void independentReception(){
  RwrReceiver.Config c=config();RwrReceiver r=receiver(c,"GEN-2");
  check(r.receive(0,own(40),emission(c.search,"hidden-battery-coordinate-token"),true),"RWR hears search at 40 km, beyond player's 30 km effective detection");
  check(r.observations(.69).isEmpty(),"receiver delay before evidence delivered");
  RwrReceiver.Observation o=only(r,.7);check(o.kind==RwrReceiver.Kind.SEARCH&&o.observedAt==0&&o.expiresAt==8,"received search with immutable evidence timestamp and finite memory");
  check(!o.emitterId.contains("battery")&&o.emitterId.startsWith("E"),"AI identity is anonymous, not a world lookup key");
  for(Field f:RwrReceiver.Observation.class.getFields())check(!f.getName().equals("range")&&!f.getName().equals("target")&&!f.getName().equals("sourceToken")&&!f.getName().equals("position")&&!f.getName().equals("missile"),"no world information in perception API");
  for(int i=0;i<50;i++)check(r.observations(.7).get(0).observedAt==0,"paused query never refreshes evidence or advances time");
  check(r.observations(8).isEmpty(),"no continuing live warning after reception memory expires");
  r=receiver(c,"GEN-0");check(!r.receive(0,own(1),emission(c.search,"radar"),true)&&r.observations(4).isEmpty(),"no RWR means no RF evidence");
 }

 static void physicalReception(){
  RwrReceiver.Config c=config();RwrReceiver.Pose p=own(10);
  RwrReceiver r=receiver(c,"GEN-2");check(!r.receive(0,p,emission(c.search,"radar"),false),"terrain blocks receiver independently of player tracking");
  RwrReceiver.Emitter incompatible=new RwrReceiver.Emitter("C",RwrReceiver.Waveform.SEARCH,RwrReceiver.Pattern.OMNI,1,360,180,0);
  check(!r.receive(0,p,emission(incompatible,"radar"),true),"unsupported band cannot alert");
  check(!r.receive(0,own(90),emission(c.search,"radar"),true),"received power below sensitivity cannot alert");
  check(!r.receive(0,p,new RwrReceiver.Emission("radar",c.search,0,0,.1,Math.PI,0),true),"opposite transmitter beam cannot alert");
  Properties restricted=clear();restricted.setProperty("profile.GEN-2.waveforms","SEARCH");RwrReceiver.Config rc=new RwrReceiver.Config(restricted);r=receiver(rc,"GEN-2");check(!r.receive(0,p,emission(c.fireControl,"radar"),true),"unsupported waveform cannot alert despite band match");
  restricted=clear();restricted.setProperty("profile.GEN-2.horizontalDegrees","120");rc=new RwrReceiver.Config(restricted);r=receiver(rc,"GEN-2");check(!r.receive(0,new RwrReceiver.Pose(0,-10,1,0,0),emission(c.search,"radar"),true),"receiver antenna sector blocks rear emitter");
  restricted=clear();restricted.setProperty("profile.GEN-2.rearBlindDegrees","45");rc=new RwrReceiver.Config(restricted);r=receiver(rc,"GEN-2");check(!r.receive(0,new RwrReceiver.Pose(0,-10,1,0,0),emission(c.search,"radar"),true),"configurable antenna blind area respected");
  restricted=clear();restricted.setProperty("profile.GEN-2.verticalDegrees","10");rc=new RwrReceiver.Config(restricted);r=receiver(rc,"GEN-2");RwrReceiver.Emitter omni=new RwrReceiver.Emitter("I",RwrReceiver.Waveform.SEARCH,RwrReceiver.Pattern.OMNI,1,360,180,0);check(!r.receive(0,new RwrReceiver.Pose(0,-1,5,Math.PI,0),emission(omni,"radar"),true),"vertical antenna coverage respected");
 }

 static void classificationAndMemory(){
  RwrReceiver.Config c=config();RwrReceiver one=receiver(c,"GEN-1"),two=receiver(c,"GEN-2");RwrReceiver.Pose p=own(5);
  check(one.receive(0,p,emission(c.activeSeeker,"missile-not-an-AI-object"),true),"basic receiver can physically receive active seeker");
  check(one.observations(1.19).isEmpty()&&only(one,1.2).kind==RwrReceiver.Kind.UNKNOWN_RADAR,"GEN-1 reception does not grant active-seeker classification");
  check(two.receive(0,p,emission(c.activeSeeker,"missile"),true)&&only(two,.7).kind==RwrReceiver.Kind.ACTIVE_SEEKER,"GEN-2 recognizes compatible known active-seeker pattern");
  RwrReceiver r=receiver(c,"GEN-2");r.receive(0,p,emission(c.fireControl,"battery"),true);r.receive(.25,p,emission(c.fireControl,"battery"),true);r.receive(.5,p,emission(c.fireControl,"battery"),true);
  RwrReceiver.Observation fire=only(r,.7);check(fire.kind==RwrReceiver.Kind.FIRE_CONTROL,"returning emission refreshes memory without endlessly restarting warning delay");
  r.receive(1,p,emission(c.search,"battery"),true);check(only(r,1.8).kind==RwrReceiver.Kind.FIRE_CONTROL,"search does not immediately erase held fire-control warning");
  check(only(r,8.5).kind==RwrReceiver.Kind.SEARCH,"expired higher-priority mode downgrades to separately remembered search");
  check(r.observations(9).isEmpty(),"radar-off memory ends only after individual signals expire");
  r=receiver(c,"GEN-1");r.receive(0,p,emission(c.illumination,"battery"),true);check(only(r,1.2).kind==RwrReceiver.Kind.FIRE_CONTROL,"GEN-1 illumination is fire control, not perfect missile-launch warning");
  Properties unknown=clear();unknown.setProperty("profile.GEN-2.library","SEARCH,FIRE_CONTROL");r=receiver(new RwrReceiver.Config(unknown),"GEN-2");r.receive(0,p,emission(c.activeSeeker,"missile"),true);check(only(r,.7).kind==RwrReceiver.Kind.UNKNOWN_RADAR,"hardware reception does not override incomplete emitter library");
 }

 static void capacityAndCadence(){
  Properties p=clear();p.setProperty("profile.GEN-2.capacity","2");RwrReceiver.Config c=new RwrReceiver.Config(p);RwrReceiver r=receiver(c,"GEN-2");RwrReceiver.Pose own=own(5);
  r.receive(0,own,emission(c.fireControl,"one"),true);r.receive(0,own,emission(c.illumination,"two"),true);check(!r.receive(0,own,emission(c.search,"three"),true),"new search cannot replace higher-priority received threats");
  r.receive(.25,own,emission(c.activeSeeker,"four"),true);List<RwrReceiver.Observation> all=r.observations(1);check(all.size()==2,"bounded receiver capacity");for(RwrReceiver.Observation o:all)check(o.kind==RwrReceiver.Kind.ILLUMINATION||o.kind==RwrReceiver.Kind.ACTIVE_SEEKER,"stronger perceived threat replaces lowest priority emitter");
  p=clear();p.setProperty("profile.GEN-2.capacity","1");p.setProperty("profile.GEN-2.activeSeekerRecognition","false");c=new RwrReceiver.Config(p);r=receiver(c,"GEN-2");r.receive(0,own,emission(c.fireControl,"one"),true);check(!r.receive(0,own,emission(c.activeSeeker,"two"),true)&&only(r,.7).kind==RwrReceiver.Kind.FIRE_CONTROL,"an unidentified seeker cannot use hidden true type to evict perceived lock");
  RwrReceiver slow=receiver(config(),"GEN-2"),fast=receiver(config(),"GEN-2");
  for(int i=0;i<=40;i++)slow.receive(i/20.0,own,emission(c.search,"radar"),true);
  for(int i=0;i<=120;i++)fast.receive(i/60.0,own,emission(c.search,"radar"),true);
  RwrReceiver.Observation a=only(slow,2),b=only(fast,2);check(Math.abs(a.bearing-b.bearing)<1e-9&&a.observedAt==b.observedAt,"uncertainty sampled at sensor cadence, not render frame rate");
 }

 static void visualEvidence(){
  RwrReceiver.Config c=config();RwrReceiver r=receiver(c,"GEN-0");RwrReceiver.Pose own=new RwrReceiver.Pose(0,0,1,0,0),front=new RwrReceiver.Pose(0,-1,1,Math.PI,0,0,.5,0),rear=new RwrReceiver.Pose(0,1,1,0,0,0,-.5,0);
  for(int i=0;i<=8;i++)r.observeMissile(i*.25,"rear",own,rear,true,1,1);check(r.observations(2).isEmpty(),"no RWR and default MAWS cannot see missile behind visual field");
  for(int i=0;i<4;i++)r.observeMissile(3+i*.25,"front",own,front,true,1,1);check(r.observations(3.75).isEmpty(),"visible missile still requires finite acquisition time");
  check(r.observeMissile(4,"front",own,front,true,1,1)&&only(r,4).kind==RwrReceiver.Kind.VISUAL_MISSILE,"visual evidence independent of RWR equipment");
  check(r.observations(6).isEmpty(),"unobserved visual evidence expires without knowing missile fate");
  r=receiver(c,"GEN-0");RwrReceiver.Pose distant=new RwrReceiver.Pose(0,-2,1,Math.PI,0,0,.5,0);for(int i=0;i<=20;i++)r.observeMissile(i*.25,"dark",own,distant,true,.45,1);check(r.observations(5).isEmpty(),"night penalty reduces optical range");
  r=receiver(c,"GEN-0");for(int i=0;i<=20;i++)r.observeMissile(i*.25,"occluded",own,front,false,1,1);check(r.observations(5).isEmpty(),"visual sensor also requires line of sight");
  Properties p=clear();p.setProperty("profile.GEN-0.maws","true");c=new RwrReceiver.Config(p);r=receiver(c,"GEN-0");r.observeMissile(0,"rear",own,rear,true,0,0);check(r.observations(.49).isEmpty()&&only(r,.5).kind==RwrReceiver.Kind.MAWS_MISSILE,"separately equipped MAWS has independent delay and works outside optical view");
  r=receiver(c,"GEN-0");RwrReceiver.Pose receding=new RwrReceiver.Pose(0,1,1,Math.PI,0,0,.5,0);r.observeMissile(0,"receding",own,receding,true,0,0);check(r.observations(1).isEmpty(),"MAWS does not invent an approaching threat for a receding missile");
 }

 public static void main(String[] args)throws Exception{defaultsAndOverrides();independentReception();physicalReception();classificationAndMemory();capacityAndCadence();visualEvidence();System.out.println("PASS: passive RF geometry/bands/waveforms/strength, delayed anonymous evidence, receiver tiers/libraries, mode memory, capacity/cadence, independent finite visual acquisition and optional MAWS");}
}

package com.jb.radar;
import java.io.*;import java.util.*;
/** Fictional balance values. Explosive kilograms are never inferred from total weapon mass. */
public final class CombatRules {
 public static final String[] BOMB_NAMES={"SUPER-SMALL","SMALL","MEDIUM","LARGE","SUPER-LARGE","MEGA-LARGE","NUCLEAR (FICTIONAL)"};
 public static final double[] KG={24.5,44.4,87.1,201.8,428.6,898.8,1554.5};
 public final double aircraftHealth,missileHealth,aircraftVulnerability,missileVulnerability;
 public final double[] aircraftHealthByType=new double[5],aircraftVulnerabilityByType=new double[5];
 public final int mission1SmartLevel;public final double multiplier,releaseKm;public final boolean sharedMemory,trackingCue;
 public final double[] explosiveKg=new double[7],radiusKm=new double[7],protection=new double[7];
 public CombatRules(Properties p){mission1SmartLevel=Equipment.integer(p,"mission1.smartLevel",1,1,5);multiplier=Equipment.number(p,"damage.multiplier",.55,.001,10);releaseKm=Equipment.number(p,"bomb.releaseAreaKm",.35,.05,1);sharedMemory=Equipment.bool(p,"ai.sharedMemory",true);trackingCue=Equipment.bool(p,"ai.trackingCue",false);
  double[] radii={.08,.13,.20,.32,.48,.68,.90},armor={.65,.75,.75,.75,.75,.80,.55};Set<String> keys=new HashSet<>(Arrays.asList("mission1.smartLevel","damage.multiplier","bomb.releaseAreaKm","ai.sharedMemory","ai.trackingCue"));
  for(int i=0;i<7;i++){String k="bomb."+i;explosiveKg[i]=Equipment.number(p,k+".explosiveKg",KG[i],0,10000);radiusKm[i]=Equipment.number(p,k+".radiusKm",radii[i],.01,5);keys.add(k+".explosiveKg");keys.add(k+".radiusKm");}
  for(int i=0;i<7;i++){String k="component."+i+".protection";protection[i]=Equipment.number(p,k,armor[i],0,1);keys.add(k);}
  aircraftHealth=Equipment.number(p,"target.aircraftHealth",100,1,10000);missileHealth=Equipment.number(p,"target.missileHealth",60,1,10000);aircraftVulnerability=Equipment.number(p,"target.aircraftVulnerability",1,.01,100);missileVulnerability=Equipment.number(p,"target.missileVulnerability",1,.01,100);
  keys.addAll(Arrays.asList("target.aircraftHealth","target.missileHealth","target.aircraftVulnerability","target.missileVulnerability"));
  for(int i=0;i<5;i++){String base="target.aircraft."+i;aircraftHealthByType[i]=Equipment.number(p,base+".health",aircraftHealth,1,10000);aircraftVulnerabilityByType[i]=Equipment.number(p,base+".vulnerability",aircraftVulnerability,.01,100);keys.add(base+".health");keys.add(base+".vulnerability");}
  for(String k:p.stringPropertyNames())if(!keys.contains(k))throw new IllegalArgumentException("Unknown combat property: "+k);
 }
 public static CombatRules defaults(){return new CombatRules(new Properties());}
 public static CombatRules load(InputStream in)throws IOException{Properties p=new Properties();p.load(in);return new CombatRules(p);}
}

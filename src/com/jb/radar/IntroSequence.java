package com.jb.radar;

/** Story clock: no mission state, wall-clock timers, or delayed callbacks. */
public final class IntroSequence {
 public static final String[] SECTIONS={
  "6 OCTOBER 1973\n0445 HOURS",
  "The first explosion throws the sleeping city into darkness.\nFor a moment, no one understands what has happened.",
  "Then the sirens begin.\nParents carry children into stairwells. Some never reach the shelters.",
  "An apartment block collapses.\nSurvivors call into the rubble. Some hear answers. Others hear nothing.",
  "At the airfield, hangars burn with crews still inside.\nRescuers reach one doorway just as the roof gives way.",
  "Runways are torn apart. Fighters burn on the ground.\nAn ambulance crew lifts a wounded pilot onto its last stretcher.",
  "Across the defense network, voices plead for help.\nOne transmission ends in a scream. The next is swallowed by static.",
  "Several batteries have fallen silent.\nNo one knows how many crews are still alive.",
  "Your cabin shudders. Dust falls onto the radar screen.\nA young operator wipes it away with a shaking hand.",
  "\"All remaining batteries... fighter support is unavailable.\nHold until reinforcements arrive.\"",
  "Beyond the wire, the city is burning.\nYour crew looks to you.",
  "\"Bring the battery to combat readiness.\"\nIt is time to fire back."
 };
 public static final double PRE_DELAY=.3, HOLD=2.1, FIRST_HOLD=1.8, FINAL_HOLD=2.8;
 public static final double DURATION=duration();
 public final double[] starts=new double[SECTIONS.length], ends=new double[SECTIONS.length];
 public final double[][] reveal=new double[SECTIONS.length][];
 public double elapsed; public boolean paused,music=true,active;
 static double hold(int n){return n==0?FIRST_HOLD:n==SECTIONS.length-1?FINAL_HOLD:HOLD;}
 static double duration(){double t=0;for(int n=0;n<SECTIONS.length;n++){t+=PRE_DELAY+hold(n);for(int i=0;i<SECTIONS[n].length();i++)t+=delay(SECTIONS[n].charAt(i));}return t;}
 public IntroSequence(){
  double t=0;for(int n=0;n<SECTIONS.length;n++){starts[n]=t;t+=PRE_DELAY;String s=SECTIONS[n];reveal[n]=new double[s.length()];for(int i=0;i<s.length();i++){reveal[n][i]=t;t+=delay(s.charAt(i));}t+=hold(n);ends[n]=t;}
 }
 static double delay(char c){if(c=='\n')return .220;if(c==' ')return .026;if(c==','||c==':'||c==';')return .110;if(c=='.'||c=='!'||c=='?')return .190;return .038;}
 public void start(){elapsed=0;paused=false;active=true;}
 public void advance(double dt){if(active&&!paused&&Double.isFinite(dt)&&dt>0)elapsed=Math.min(DURATION,elapsed+dt);}
 public void sync(double seconds){if(active&&!paused&&Double.isFinite(seconds))elapsed=Math.max(elapsed,Math.min(DURATION,seconds));}
 public boolean done(){return elapsed>=DURATION-1e-6;}
 public int section(){for(int i=0;i<ends.length;i++)if(elapsed<ends[i])return i;return ends.length-1;}
 public int visible(){int n=section(),count=0;while(count<reveal[n].length&&reveal[n][count]<=elapsed)count++;return count;}
 public void stop(){active=false;paused=false;}
}

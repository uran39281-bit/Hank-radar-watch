package com.jb.radar;
import java.util.*;
/** Bounded, mission-local game decisions based on delivered cues, never future player input. */
public final class PilotAI {
 public enum State { APPROACH,EVADE,NOTCH,ABORT,REPOSITION,EXIT,RETREAT }
 public enum Warning { TRACK,LOCK,LAUNCH,SEARCH,ACTIVE_SEEKER }
 public enum Awareness { UNAWARE,ALERTED,DEFENSIVE }
 /** Fictional receiver fits, separate from pilot skill. These are game profiles, not aircraft specifications. */
 public static final class WarningProfile {
  public final boolean searchRadar,fireControl,activeRadar,missileWarning,visual;
  public WarningProfile(boolean search,boolean lock,boolean active,boolean missile,boolean visual){searchRadar=search;fireControl=lock;activeRadar=active;missileWarning=missile;this.visual=visual;}
 }
 public static final WarningProfile NO_WARNING_EQUIPMENT=new WarningProfile(false,false,false,false,false);
 public static final WarningProfile BASIC_RWR=new WarningProfile(true,true,true,false,true);
 public static final WarningProfile LIMITED_RWR=new WarningProfile(true,true,false,false,true);
 public static final WarningProfile FULL_WARNING_FIT=new WarningProfile(true,true,true,true,true);
 public static final WarningProfile[] AIRCRAFT_WARNING_PROFILES={BASIC_RWR,LIMITED_RWR,BASIC_RWR,FULL_WARNING_FIT,LIMITED_RWR};
 public enum Action { CONTINUE,EVADE,NOTCH,ABORT,RETREAT }
 public static class Memory {
  public int early,late,repeated,runs,damageRuns,failedRuns;public final int[] success=new int[5],failure=new int[5];
  static int inc(int x){return Math.min(32,x+1);}
  void fire(double range,boolean repeat){if(range>10)early=inc(early);else late=inc(late);if(repeat)repeated=inc(repeated);}
  void outcome(Action a,boolean ok){int[] v=ok?success:failure;v[a.ordinal()]=inc(v[a.ordinal()]);}
  void bomb(boolean hit){runs=inc(runs);if(hit)damageRuns=inc(damageRuns);else failedRuns=inc(failedRuns);}
 }
 public static class Cue {final Warning kind;final double due,range;Cue(Warning k,double due,double range){kind=k;this.due=due;this.range=range;}}
 public static class Pilot {
  public final int smart;public int bombs,category,passes,recognizedLaunches,pendingBombs;public double health=100,maneuverUntil,outcomeAt,wayX,wayY,closest=999,lastVisual=-999,lastCue=-999,bombDamage;
  public State state=State.APPROACH;public Awareness awareness=Awareness.UNAWARE;public WarningProfile warnings=BASIC_RWR;public double lastPresence=-999,lastActive=-999;public Action lastAction=Action.CONTINUE;public boolean continuePass=true,learnReaction,retreatRecorded,enteredApproach;
  public final Memory memory=new Memory();public final ArrayList<Cue> cues=new ArrayList<>();
  public Pilot(int level,int bombs,int category){smart=(int)Game.clamp(level,1,5);this.bombs=Math.max(0,bombs);this.category=(int)Game.clamp(category,0,6);}
 }
 public final Game game;public Memory side=new Memory();public int bombRuns,damagingRuns,abortedPasses,retreats;
 public PilotAI(Game g){game=g;}
 public void reset(){side=new Memory();bombRuns=damagingRuns=abortedPasses=retreats=0;}
 public void assign(Game.Contact t,int level,int bombs,int category){assign(t,level,bombs,category,AIRCRAFT_WARNING_PROFILES[(int)Game.clamp(t.type,0,AIRCRAFT_WARNING_PROFILES.length-1)]);}
 public void assign(Game.Contact t,int level,int bombs,int category,WarningProfile profile){t.pilot=new Pilot(level,bombs,category);t.pilot.warnings=profile==null?NO_WARNING_EQUIPMENT:profile;}
 public static double recognitionChance(int level,Warning w){double[][] chance={{0,0,0,0,0},{.28,.50,.76,.91,.97},{.35,.60,.82,.94,.99},{.20,.40,.65,.82,.94},{.30,.55,.80,.94,.99}};return chance[w.ordinal()][(int)Game.clamp(level,1,5)-1];}
 public static double reactionDelay(int level,Warning w){double base=new double[]{5,3.8,2.5,1.4,.7}[(int)Game.clamp(level,1,5)-1];return base*(w==Warning.TRACK||w==Warning.SEARCH?1.2:w==Warning.LAUNCH||w==Warning.ACTIVE_SEEKER?.8:1);}
 boolean equipmentCanWarn(Pilot p,Warning w){WarningProfile q=p.warnings;switch(w){case SEARCH:return q.searchRadar;case LOCK:return q.fireControl;case ACTIVE_SEEKER:return q.activeRadar;case LAUNCH:return q.missileWarning||q.visual;default:return false;}}
 public boolean warn(Game.Contact t,Warning w,boolean detectable){Pilot p=t==null?null:t.pilot;if(p==null||!t.alive||!detectable||!equipmentCanWarn(p,w))return false;for(Cue cue:p.cues)if(cue.kind==w)return false;if(p.cues.size()>=6||game.random.nextDouble()>recognitionChance(p.smart,w))return false;
  p.cues.add(new Cue(w,game.elapsed+reactionDelay(p.smart,w)*(.85+game.random.nextDouble()*.3),t.range()));return true;
 }
 public void radarPresence(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||!p.warnings.searchRadar||game.elapsed-p.lastPresence<5||!game.battery.radarOnline(game.elapsed)||game.signal(t)<=0)return;p.lastPresence=game.elapsed;warn(t,Warning.SEARCH,true);}
 /** A launch itself has no universal radar cue; optical/visual fits need an observable nearby launch. */
 public void launchWarning(Game.Contact t,boolean infrared){Pilot p=t==null?null:t.pilot;if(p==null)return;double reach=p.warnings.missileWarning?10:p.warnings.visual?3.5:0;if(reach>0&&Math.hypot(t.range(),t.alt/1000)<=reach&&game.ir.lineOfSight(0,0,(Game.terrain(0,0)+5)/1000,t))warn(t,Warning.LAUNCH,true);}
 public void activeSeekerWarning(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||game.elapsed-p.lastActive<5)return;p.lastActive=game.elapsed;warn(t,Warning.ACTIVE_SEEKER,true);}
 public void activeSeekerWarning(Game.Contact t,Game.Missile m){if(t==null||m==null||!game.ir.lineOfSight(m.x,m.y,m.z,t))return;Equipment.Weapon w=m.profile==null?game.weapon:m.profile;if(Math.sqrt(Math.pow(t.x-m.x,2)+Math.pow(t.y-m.y,2)+Math.pow(t.alt/1000-m.z,2))<=w.seekerKm*1.5)activeSeekerWarning(t);}
 public void visualWarning(Game.Contact t,Game.Missile m){Pilot p=t==null?null:t.pilot;if(p==null||!t.alive||game.elapsed-p.lastVisual<5)return;double reach=p.warnings.missileWarning?10:p.warnings.visual?3.5:0;if(reach<=0||Math.sqrt(Math.pow(t.x-m.x,2)+Math.pow(t.y-m.y,2)+Math.pow(t.alt/1000-m.z,2))>reach||!game.ir.lineOfSight(m.x,m.y,m.z,t))return;p.lastVisual=game.elapsed;warn(t,Warning.LAUNCH,true);}
 Memory memory(Pilot p){return game.rules.sharedMemory?side:p.memory;}
 public double caution(Pilot p){Memory m=memory(p);double samples=m.early+m.late;return (p.smart-1)/4.0*(.18*Math.min(1,samples/8)*m.early/Math.max(1,samples)+.07*Math.min(1,m.repeated/5.0)+.06*Math.min(1,m.failedRuns/5.0));}
 public double[] weights(Pilot p,Warning warning){boolean launch=warning==Warning.LAUNCH||warning==Warning.ACTIVE_SEEKER;double l=p.smart,c=caution(p);double[] w=launch?new double[]{1.2-.19*l,.2+.14*l,l>=3?.1+.08*l:.025,.06+.05*l+c,p.health<50?.35:.015}:new double[]{1.6-.15*l,.04*l,l>=3?.03*l:.01,.04*l+c,p.health<50?.20:.005};
  Memory m=memory(p);for(int i=0;i<w.length;i++){double samples=m.success[i]+m.failure[i],learn=(p.smart-1)/4.0*Math.min(.18,samples*.015);double rate=(m.success[i]+1.0)/(samples+2);w[i]*=1+learn*2*(rate-.5);}return w;
 }
 Action choose(Pilot p,Warning warning){double[] w=weights(p,warning);double sum=0;for(double x:w)sum+=x;double pick=game.random.nextDouble()*sum;for(int i=0;i<w.length;i++){pick-=w[i];if(pick<=0)return Action.values()[i];}return Action.CONTINUE;}
 void record(Pilot p,Action a,boolean success){p.memory.outcome(a,success);if(game.rules.sharedMemory)side.outcome(a,success);}
 void recordFire(Pilot p,double range){boolean repeat=p.recognizedLaunches++>0;p.memory.fire(range,repeat);if(game.rules.sharedMemory)side.fire(range,repeat);}
 public void react(Game.Contact t,Warning warning){Pilot p=t.pilot;if(p==null||warning==Warning.TRACK)return;if(p.awareness==Awareness.UNAWARE)p.awareness=Awareness.ALERTED;if(warning==Warning.SEARCH)return;if(p.health<=30){retreat(t);return;}Action action=choose(p,warning);p.lastAction=action;p.learnReaction=warning==Warning.LAUNCH||warning==Warning.ACTIVE_SEEKER;p.outcomeAt=game.elapsed+12;p.continuePass=game.random.nextDouble()>.1+.025*p.smart+caution(p);
  if(action==Action.RETREAT){retreat(t);return;}if(action==Action.ABORT){abort(t);return;}
  if(action==Action.CONTINUE){p.state=State.APPROACH;t.evasion=0;return;}
  p.awareness=Awareness.DEFENSIVE;p.state=action==Action.NOTCH?State.NOTCH:State.EVADE;p.maneuverUntil=game.elapsed+3+p.smart*.7;t.evasion=0;
 }
 public void retreat(Game.Contact t){Pilot p=t.pilot;if(p==null)return;p.state=State.RETREAT;t.evasion=0;if(!p.retreatRecorded){p.retreatRecorded=true;retreats++;}}
 public void abort(Game.Contact t){Pilot p=t.pilot;if(p==null)return;if(p.state!=State.ABORT){abortedPasses++;p.passes++;p.memory.failedRuns=Memory.inc(p.memory.failedRuns);if(game.rules.sharedMemory)side.failedRuns=Memory.inc(side.failedRuns);}p.state=State.ABORT;p.maneuverUntil=game.elapsed+6;t.evasion=0;}
 void reposition(Game.Contact t){Pilot p=t.pilot;if(p.passes>=3||p.bombs<=0){retreat(t);return;}double bearing=Game.angle(t.x,t.y)+t.side*(.5+game.random.nextDouble()*.9);double distance=5+game.random.nextDouble()*4;p.wayX=Math.sin(bearing)*distance;p.wayY=-Math.cos(bearing)*distance;p.state=State.REPOSITION;p.maneuverUntil=game.elapsed+70;p.closest=999;p.enteredApproach=false;}
 public void tick(Game.Contact t,double dt){Pilot p=t.pilot;if(p==null)return;
  if(p.health<=30){retreat(t);p.cues.clear();}else if(p.bombs<=0&&p.state!=State.EXIT&&p.state!=State.RETREAT){retreat(t);p.cues.clear();}
  for(Iterator<Cue> it=p.cues.iterator();it.hasNext();){Cue cue=it.next();if(game.elapsed<cue.due)continue;it.remove();if(cue.kind==Warning.LAUNCH)recordFire(p,cue.range);if(p.state!=State.RETREAT&&p.state!=State.EXIT)react(t,cue.kind);}
  if(p.learnReaction&&game.elapsed>=p.outcomeAt){record(p,p.lastAction,true);p.learnReaction=false;}
  if(p.state==State.EVADE||p.state==State.NOTCH){if(game.elapsed>=p.maneuverUntil){if(p.continuePass){p.state=State.APPROACH;p.closest=999;p.enteredApproach=false;}else abort(t);}}
  if(p.state==State.ABORT&&game.elapsed>=p.maneuverUntil)reposition(t);
  if(p.state==State.REPOSITION&&(Math.hypot(t.x-p.wayX,t.y-p.wayY)<1||game.elapsed>=p.maneuverUntil)){p.state=State.APPROACH;p.closest=999;p.enteredApproach=false;}
  if(p.state==State.APPROACH){double r=t.range();if(r<4)p.enteredApproach=true;p.closest=Math.min(p.closest,r);if(p.enteredApproach&&r>p.closest+.3&&r>game.rules.releaseKm)abort(t);}
 }
 public double heading(Game.Contact t){Pilot p=t.pilot;double inbound=Game.angle(-t.x,-t.y);if(p==null)return t.weaponFired&&!game.guiding(t)?Game.angle(t.x,t.y):inbound;
  switch(p.state){case RETREAT:case EXIT:case ABORT:return Game.angle(t.x,t.y);case REPOSITION:return Game.angle(p.wayX-t.x,p.wayY-t.y);case NOTCH:return Game.angle(t.x,t.y)+t.side*Math.PI/2;case EVADE:return inbound+t.side*1.45;default:return inbound;}
 }
 public boolean defensive(Game.Contact t){return t.pilot!=null&&(t.pilot.state==State.NOTCH||t.pilot.state==State.EVADE);}
 public boolean notchBreak(Game.Contact t){return t.pilot!=null&&t.pilot.state==State.NOTCH&&Math.abs(Math.cos(Game.delta(t.heading,Game.angle(t.x,t.y))))<.25&&(game.elapsed+t.id)%2.7<1.5;}
 public boolean canBomb(Game.Contact t){Pilot p=t.pilot;return t.alive&&p!=null&&p.health>30&&p.bombs>0&&p.state==State.APPROACH&&t.range()<=game.rules.releaseKm;}
 public void released(Game.Contact t,int count){Pilot p=t.pilot;p.pendingBombs+=count;p.bombs-=count;p.state=State.EXIT;p.passes++;p.bombDamage=0;p.cues.clear();}
 public void bombOutcome(Game.Contact t,double damage){Pilot p=t.pilot;if(p==null||p.pendingBombs<=0)return;p.bombDamage+=damage;if(game.finished)p.pendingBombs=1;if(--p.pendingBombs==0){boolean hit=p.bombDamage>0;p.memory.bomb(hit);if(game.rules.sharedMemory)side.bomb(hit);bombRuns++;if(hit)damagingRuns++;}}
 public void destroyed(Game.Contact t){Pilot p=t.pilot;if(p==null)return;if(p.retreatRecorded){record(p,Action.RETREAT,false);p.learnReaction=false;}else if(p.learnReaction){record(p,p.lastAction,false);p.learnReaction=false;}else record(p,p.lastAction,false);p.cues.clear();}
 public void exited(Game.Contact t){Pilot p=t.pilot;if(p!=null&&p.retreatRecorded)record(p,Action.RETREAT,true);}
}

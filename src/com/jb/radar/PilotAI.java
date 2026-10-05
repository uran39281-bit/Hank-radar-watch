package com.jb.radar;
import java.util.*;
/** Mission-local decisions. The decision layer consumes perceived evidence, never hidden weapon positions. */
public final class PilotAI {
 public enum State { APPROACH,EVADE,NOTCH,ABORT,REPOSITION,EXIT,RETREAT,CAUTIOUS,DEFENSIVE,MISSILE_EVASION,REASSESS,ATTACK_RUN,DISENGAGE,EXITED,DESTROYED }
 public enum Warning { TRACK,LOCK,LAUNCH,SEARCH,ACTIVE_SEEKER,ILLUMINATION,UNKNOWN_RADAR,VISUAL_MISSILE }
 public enum Awareness { UNAWARE,ALERTED,DEFENSIVE }
 /** Legacy fit adapter; full RF profiles and visual sensing live in the receiver layer. */
 public static final class WarningProfile {
  public final boolean searchRadar,fireControl,activeRadar,missileWarning,visual;
  public WarningProfile(boolean search,boolean lock,boolean active,boolean missile,boolean visual){searchRadar=search;fireControl=lock;activeRadar=active;missileWarning=missile;this.visual=visual;}
 }
 public static final WarningProfile NO_WARNING_EQUIPMENT=new WarningProfile(false,false,false,false,false);
 public static final WarningProfile BASIC_RWR=new WarningProfile(true,true,true,false,true);
 public static final WarningProfile LIMITED_RWR=new WarningProfile(true,true,false,false,true);
 public static final WarningProfile FULL_WARNING_FIT=new WarningProfile(true,true,true,true,true);
 public static final WarningProfile[] AIRCRAFT_WARNING_PROFILES={BASIC_RWR,BASIC_RWR,LIMITED_RWR,BASIC_RWR};
 public enum Action { CONTINUE,EVADE,NOTCH,ABORT,RETREAT }
 public static class Memory {
  public int early,late,repeated,runs,damageRuns,failedRuns;public final int[] success=new int[5],failure=new int[5];
  static int inc(int x){return Math.min(32,x+1);}
  void fire(double range,boolean repeat){if(Double.isFinite(range)){if(range>10)early=inc(early);else late=inc(late);}if(repeat)repeated=inc(repeated);}
  void outcome(Action a,boolean ok){int[] v=ok?success:failure;v[a.ordinal()]=inc(v[a.ordinal()]);}
  void bomb(boolean hit){runs=inc(runs);if(hit)damageRuns=inc(damageRuns);else failedRuns=inc(failedRuns);}
 }
 /** A queued pilot decision. range is retained for source compatibility, and is never populated from world truth. */
 public static class Cue {
  public final Warning kind;public final double due,range;final Evidence evidence;
  Cue(Warning k,double due,double range){this(k,due,range,null);}
  Cue(Warning k,double due,double range,Evidence e){kind=k;this.due=due;this.range=range;evidence=e;}
 }
 public static final class Evidence {
  public final String emitterId;public Warning kind;public double bearing,confidence,expiresAt,lastObserved,firstObserved,sensorExpiresAt=-999;public boolean accepted,reacted,reactionRecorded;
  Evidence(String id,Warning warning,double now){emitterId=id;kind=warning;firstObserved=lastObserved=now;}
 }
 public static class Pilot {
  public final int smart;public int bombs,category,passes,recognizedLaunches,pendingBombs;
  public double health=100,maneuverUntil,outcomeAt,wayX,wayY,closest=999,lastVisual=-999,lastCue=-999,bombDamage,observedBombDamage;
  public boolean bombOutcomeObserved;public int bombOutcomes,bombObservedOutcomes,bombReleasedCount;
  public State state=State.APPROACH;public Awareness awareness=Awareness.UNAWARE;public WarningProfile warnings=BASIC_RWR;
  public double lastPresence=-999,lastActive=-999;public Action lastAction=Action.CONTINUE;
  public boolean continuePass=true,learnReaction,retreatRecorded,enteredApproach;
  public final Memory memory=new Memory();public final ArrayList<Cue> cues=new ArrayList<>();
  public final LinkedHashMap<String,Evidence> evidence=new LinkedHashMap<>();
  public double skill=.4,aggression=.55,reactionMinSeconds=2,reactionMaxSeconds=4,emergencyMinSeconds=1,emergencyMaxSeconds=2;
  public double observedMissileMemorySeconds=12,abortHealthFraction=.35,minimumStateHoldSeconds=2,safeClearSeconds=6,decisionIntervalSeconds=.25;
  public int failedApproachLimit=2;public boolean intrusion,disengaging,criticalControl,reduceHeat,passDisrupted;
  public double objectiveX,objectiveY,exitX=65,exitY=-45,intrusionX=55,intrusionY=35;
  public double stateSince,clearSince=-1,lastDecision=-999,nextAssessment,maneuverHeading=Double.NaN,courseOffset;
  public int respondingUrgency;public String selectedEmitter="";public double selectedBearing=Double.NaN,selectedConfidence;
  public int countermeasures=60,countermeasureCapacity=60,countermeasureBurstUnits=3;
  public double countermeasureEffectSeconds=3,countermeasureCooldownSeconds=1.5,reserveFraction=.1,nextCountermeasure;
  public boolean countermeasureRequested;public double releaseMinAltM=100,releaseMaxAltM=16000,releaseMaxSpeedKmh=1500,releaseHeadingTolerance=.5;
  public final ArrayList<String> developmentTrace=new ArrayList<>();
  public Pilot(int level,int bombs,int category){smart=(int)Game.clamp(level,1,5);this.bombs=Math.max(0,bombs);this.category=(int)Game.clamp(category,0,6);skill=.2+.1*smart;}
 }
 public final Game game;public Memory side=new Memory();public int bombRuns,damagingRuns,abortedPasses,retreats;public boolean developmentTraceEnabled;
 public PilotAI(Game g){game=g;}
 public void reset(){side=new Memory();bombRuns=damagingRuns=abortedPasses=retreats=0;}
 public void assign(Game.Contact t,int level,int bombs,int category){assign(t,level,bombs,category,AIRCRAFT_WARNING_PROFILES[(int)Game.clamp(t.type,0,AIRCRAFT_WARNING_PROFILES.length-1)]);}
 public void assign(Game.Contact t,int level,int bombs,int category,WarningProfile profile){t.pilot=new Pilot(level,bombs,category);t.pilot.warnings=profile==null?NO_WARNING_EQUIPMENT:profile;t.pilot.stateSince=game.elapsed;}
 public void configure(Game.Contact t,AircraftProfiles.Profile profile){
  Pilot p=t==null?null:t.pilot;if(p==null||profile==null)return;AircraftProfiles.AIConfig a=profile.ai;
  p.skill=a.skill;p.aggression=a.aggression;p.reactionMinSeconds=a.reactionMinSeconds;p.reactionMaxSeconds=a.reactionMaxSeconds;p.emergencyMinSeconds=a.emergencyMinSeconds;p.emergencyMaxSeconds=a.emergencyMaxSeconds;
  p.observedMissileMemorySeconds=a.observedMissileMemorySeconds;p.abortHealthFraction=a.abortHealthFraction;p.minimumStateHoldSeconds=a.minimumStateHoldSeconds;p.safeClearSeconds=a.safeClearSeconds;p.failedApproachLimit=a.failedApproachLimit;p.decisionIntervalSeconds=a.decisionIntervalSeconds;
  p.intrusion=!profile.preset.armed();p.bombs=profile.preset.bombs;p.category=profile.preset.bombClass;
  p.countermeasures=p.countermeasureCapacity=profile.countermeasures.inventory;p.countermeasureBurstUnits=profile.countermeasures.burstUnits;p.countermeasureCooldownSeconds=profile.countermeasures.cooldownSeconds;p.countermeasureEffectSeconds=profile.countermeasures.effectSeconds;p.reserveFraction=profile.countermeasures.reserveFraction;
 }
 public void setRoute(Game.Contact t,double ox,double oy,double ex,double ey,double ix,double iy){Pilot p=t==null?null:t.pilot;if(p==null)return;p.objectiveX=ox;p.objectiveY=oy;p.exitX=ex;p.exitY=ey;p.intrusionX=ix;p.intrusionY=iy;}
 public static double recognitionChance(int level,Warning w){
  if(w==Warning.TRACK)return 0;double l=Game.clamp(level,1,5)-1;
  return w==Warning.LOCK?.28+.1725*l:w==Warning.SEARCH||w==Warning.UNKNOWN_RADAR?.20+.185*l:.30+.1725*l;
 }
 public static double reactionDelay(int level,Warning w){double novice=(5-Game.clamp(level,1,5))/4;return urgency(w)>=3?1+novice:2+2*novice;}
 static int urgency(Warning w){return w==Warning.TRACK?0:w==Warning.SEARCH||w==Warning.UNKNOWN_RADAR?1:w==Warning.LOCK?2:3;}
 boolean equipmentCanWarn(Pilot p,Warning w){WarningProfile q=p.warnings;switch(w){case SEARCH:case UNKNOWN_RADAR:return q.searchRadar;case LOCK:case ILLUMINATION:return q.fireControl;case ACTIVE_SEEKER:return q.activeRadar;case LAUNCH:case VISUAL_MISSILE:return q.missileWarning||q.visual;default:return false;}}
 /** Receiver delay has already elapsed. Re-observations refresh memory, not pilot reaction timers. */
 public boolean observe(Game.Contact t,Warning kind,String emitterId,double bearing,double confidence,double expiresAt){
  Pilot p=t==null?null:t.pilot;double now=game.elapsed;if(p==null||!t.alive||kind==null||kind==Warning.TRACK||emitterId==null||terminal(p)||!Double.isFinite(confidence)||confidence<.15||expiresAt<now)return false;
  confidence=Game.clamp(confidence,0,1);String evidenceKey=emitterId+"/"+kind;Evidence e=p.evidence.get(evidenceKey);int oldUrgency=e==null?0:urgency(e.kind);
  boolean fresh=e==null||e.expiresAt<now,escalates=!fresh&&urgency(kind)>oldUrgency;
  if(fresh){if(e!=null)removeCues(p,e);if(p.evidence.size()>=16)evict(p);e=new Evidence(emitterId,kind,now);p.evidence.put(evidenceKey,e);}
  boolean newMeasurement=fresh||expiresAt>e.sensorExpiresAt+.000001;if(newMeasurement)e.lastObserved=now;e.sensorExpiresAt=Math.max(e.sensorExpiresAt,expiresAt);e.bearing=Double.isFinite(bearing)?bearing:Double.NaN;e.confidence=confidence;
  // Never downgrade a still remembered guidance warning because an ordinary search pulse follows it.
  if(fresh||escalates||urgency(kind)>=urgency(e.kind))e.kind=kind;
  double expiry=urgency(kind)>=3?Math.max(expiresAt,e.lastObserved+p.observedMissileMemorySeconds):expiresAt;
  e.expiresAt=fresh?expiry:Math.max(e.expiresAt,expiry);
  if(fresh||escalates){removeCues(p,e);e.reacted=false;
   // This roll occurs only at a new warning episode, never once per rendered frame.
   e.accepted=game.random.nextDouble()<=Game.clamp(recognitionChance(p.smart,kind)*(.7+.75*p.skill)*(.65+.35*confidence),0,1);
   if(e.accepted){double delay=sampleDelay(p,kind);p.cues.add(new Cue(kind,now+delay,Double.NaN,e));trace(p,"EVIDENCE "+emitterId+" "+kind+" confidence="+round(confidence)+" decision in "+round(delay)+"s");}
   else trace(p,"MISSED "+emitterId+" "+kind);
  }
  return e.accepted;
 }
 double sampleDelay(Pilot p,Warning warning){boolean emergency=urgency(warning)>=3;double low=emergency?p.emergencyMinSeconds:p.reactionMinSeconds,high=emergency?p.emergencyMaxSeconds:p.reactionMaxSeconds;double novice=(5-p.smart)/4.0;double f=Game.clamp(.65*novice+.35*game.random.nextDouble(),0,1);return low+(high-low)*f;}
 void removeCues(Pilot p,Evidence e){for(Iterator<Cue> it=p.cues.iterator();it.hasNext();)if(it.next().evidence==e)it.remove();}
 void evict(Pilot p){Evidence lowest=null;for(Evidence e:p.evidence.values())if(lowest==null||urgency(e.kind)<urgency(lowest.kind)||urgency(e.kind)==urgency(lowest.kind)&&e.lastObserved<lowest.lastObserved)lowest=e;if(lowest!=null){removeCues(p,lowest);p.evidence.values().remove(lowest);}}
 public boolean warn(Game.Contact t,Warning kind,boolean detectable){Pilot p=t==null?null:t.pilot;if(p==null||!detectable||!equipmentCanWarn(p,kind))return false;return observe(t,kind,"legacy-"+kind,Double.NaN,1,game.elapsed+8);}
 // Compatibility entry points. Production dispatch uses GameSensors/RwrReceiver rather than these adapters.
 public void radarPresence(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||game.elapsed-p.lastPresence<5||!game.battery.radarOnline(game.elapsed)||game.signal(t)<=0)return;p.lastPresence=game.elapsed;warn(t,Warning.SEARCH,true);}
 public void launchWarning(Game.Contact t,boolean infrared){} // Launch button events are never evidence.
 public void activeSeekerWarning(Game.Contact t){warn(t,Warning.ACTIVE_SEEKER,true);}
 public void activeSeekerWarning(Game.Contact t,Game.Missile m){} // The receiver must test an actual emitted signal.
 public void visualWarning(Game.Contact t,Game.Missile m){} // Visual acquisition and its delay are handled by GameSensors.
 Memory memory(Pilot p){return game.rules.sharedMemory?side:p.memory;}
 public double caution(Pilot p){Memory m=memory(p);double samples=m.early+m.late;return (p.smart-1)/4.0*(.18*Math.min(1,samples/8)*m.early/Math.max(1,samples)+.07*Math.min(1,m.repeated/5.0)+.06*Math.min(1,m.failedRuns/5.0));}
 public double[] weights(Pilot p,Warning warning){double l=p.smart;return new double[]{1.3-.18*l,.25+.15*l,l>=3?.1+.08*l:0,.1+caution(p),p.health<35?.8:.01};}
 void record(Pilot p,Action a,boolean success){p.memory.outcome(a,success);if(game.rules.sharedMemory)side.outcome(a,success);}
 void recordFire(Pilot p,double range){boolean repeat=p.recognizedLaunches++>0;p.memory.fire(range,repeat);if(game.rules.sharedMemory)side.fire(range,repeat);}
 /** Explicit delivered-event hook retained for tests/tools; normal play enters through observe(). */
 public void react(Game.Contact t,Warning warning){Pilot p=t==null?null:t.pilot;if(p==null||warning==Warning.TRACK)return;Evidence e=new Evidence("delivered-"+warning,warning,game.elapsed);e.accepted=e.reacted=true;e.bearing=Double.NaN;e.confidence=1;e.expiresAt=game.elapsed+(urgency(warning)>=3?p.observedMissileMemorySeconds:8);p.evidence.put(e.emitterId,e);respond(t,e,true);}
 boolean terminal(Pilot p){return p.state==State.DESTROYED||p.state==State.EXITED;}
 void transition(Pilot p,State next){if(p.state==next)return;trace(p,"STATE "+p.state+" -> "+next);p.state=next;p.stateSince=game.elapsed;}
 void trace(Pilot p,String text){if(!developmentTraceEnabled)return;if(p.developmentTrace.size()>=80)p.developmentTrace.remove(0);p.developmentTrace.add(String.format(Locale.US,"%.2f %s",game.elapsed,text));}
 static String round(double n){return String.format(Locale.US,"%.2f",n);}
 double objectiveRange(Game.Contact t){Pilot p=t.pilot;return Math.hypot(t.x-p.objectiveX,t.y-p.objectiveY);}
 void disrupt(Game.Contact t){Pilot p=t.pilot;if(p.intrusion||p.disengaging||p.passDisrupted||objectiveRange(t)>12)return;p.passDisrupted=true;p.passes++;p.memory.failedRuns=Memory.inc(p.memory.failedRuns);if(game.rules.sharedMemory)side.failedRuns=Memory.inc(side.failedRuns);abortedPasses++;}
 void respond(Game.Contact t,Evidence primary,boolean force){
  Pilot p=t.pilot;int u=urgency(primary.kind);p.awareness=u>=2?Awareness.DEFENSIVE:Awareness.ALERTED;p.selectedEmitter=primary.emitterId;p.selectedConfidence=primary.confidence;p.selectedBearing=combinedBearing(p,u);
  if(!force&&u<=p.respondingUrgency&&game.elapsed-p.stateSince<p.minimumStateHoldSeconds)return;
  if(u==1){if(!p.disengaging&&!defensive(t)&&p.state!=State.REASSESS&&p.state!=State.REPOSITION&&p.state!=State.ATTACK_RUN){transition(p,State.CAUTIOUS);if(game.elapsed-primary.firstObserved>8&&p.memory.failedRuns>0)p.courseOffset=t.side*.12;}p.respondingUrgency=Math.max(p.respondingUrgency,1);return;}
  if(u==2&&!p.disengaging&&p.health>70&&game.bombSolution(t)&&p.aggression>.5&&game.elapsed-primary.firstObserved<4){transition(p,State.ATTACK_RUN);p.courseOffset=t.side*.08;p.respondingUrgency=2;return;}
  if(u>=3&&!primary.reactionRecorded){primary.reactionRecorded=true;p.learnReaction=true;p.outcomeAt=game.elapsed+p.observedMissileMemorySeconds;recordFire(p,Double.NaN);}
  if(u>p.respondingUrgency||game.elapsed>=p.nextAssessment||force){
   disrupt(t);p.lastAction=p.smart>=3&&u==2&&game.random.nextDouble()<.15+.35*p.skill?Action.NOTCH:Action.EVADE;
   double bearing=p.selectedBearing,error=(1-p.skill)*(.15+game.random.nextDouble()*.3);
   p.maneuverHeading=Double.isFinite(bearing)?bearing+(p.lastAction==Action.NOTCH?t.side*Math.PI/2:Math.PI+t.side*(.25+error)):t.heading+t.side*(.6+error);
   p.nextAssessment=game.elapsed+Math.max(p.minimumStateHoldSeconds,2.5);p.maneuverUntil=p.nextAssessment;
   transition(p,u>=3?State.MISSILE_EVASION:State.DEFENSIVE);p.reduceHeat=u>=3;p.countermeasureRequested=u>=3;p.respondingUrgency=u;t.evasion=0;
   trace(p,"THREAT "+primary.emitterId+" urgency="+u+" maneuver="+round(p.maneuverHeading)+" CM="+p.countermeasures);
  }
 }
 double combinedBearing(Pilot p,int priority){double x=0,y=0;int count=0;for(Evidence e:p.evidence.values())if(e.accepted&&e.reacted&&e.expiresAt>=game.elapsed&&urgency(e.kind)==priority&&Double.isFinite(e.bearing)){x+=Math.sin(e.bearing)*e.confidence;y+=Math.cos(e.bearing)*e.confidence;count++;}return count==0||Math.hypot(x,y)<.2?Double.NaN:Math.atan2(x,y);}
 Evidence primary(Pilot p){Evidence best=null;for(Evidence e:p.evidence.values())if(e.accepted&&e.reacted&&e.expiresAt>=game.elapsed&&(best==null||urgency(e.kind)>urgency(best.kind)||urgency(e.kind)==urgency(best.kind)&&e.confidence>best.confidence))best=e;return best;}
 public void retreat(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||terminal(p))return;p.disengaging=true;t.evasion=0;if(!p.retreatRecorded){p.retreatRecorded=true;retreats++;}if(!defensive(t))transition(p,State.DISENGAGE);}
 public void abort(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||terminal(p))return;disrupt(t);if(!p.passDisrupted&&!p.intrusion){p.passDisrupted=true;p.passes++;p.memory.failedRuns=Memory.inc(p.memory.failedRuns);abortedPasses++;}p.clearSince=game.elapsed;p.maneuverUntil=game.elapsed+p.safeClearSeconds;transition(p,State.ABORT);t.evasion=0;if(p.passes>=p.failedApproachLimit)retreat(t);}
 void reposition(Game.Contact t){Pilot p=t.pilot;if(p.disengaging||p.passes>=p.failedApproachLimit||!p.intrusion&&p.bombs<=0){retreat(t);return;}double a=Game.angle(t.x-p.objectiveX,t.y-p.objectiveY)+t.side*(.4+game.random.nextDouble()*.5),distance=5+game.random.nextDouble()*3;p.wayX=p.objectiveX+Math.sin(a)*distance;p.wayY=p.objectiveY-Math.cos(a)*distance;transition(p,State.REPOSITION);p.maneuverUntil=game.elapsed+70;p.closest=999;p.enteredApproach=false;}
 public void tick(Game.Contact t,double dt){Pilot p=t==null?null:t.pilot;if(p==null||terminal(p))return;if(!t.alive){destroyed(t);return;}
  if(p.state==State.EXIT||p.state==State.RETREAT||p.state==State.DISENGAGE)p.disengaging=true;
  if(p.health<100*p.abortHealthFraction||p.criticalControl||!p.intrusion&&p.bombs<=0||p.passes>=p.failedApproachLimit)retreat(t);
  boolean delivered=false;
  for(Iterator<Cue> it=p.cues.iterator();it.hasNext();){Cue c=it.next();if(game.elapsed<c.due)continue;it.remove();if(c.evidence!=null&&c.evidence.expiresAt>=game.elapsed){c.evidence.reacted=true;delivered=true;p.lastCue=game.elapsed;}}
  if(p.learnReaction&&game.elapsed>=p.outcomeAt){record(p,p.lastAction,true);p.learnReaction=false;}
  if(!delivered&&game.elapsed-p.lastDecision<p.decisionIntervalSeconds)return;p.lastDecision=game.elapsed;
  for(Iterator<Evidence> it=p.evidence.values().iterator();it.hasNext();){Evidence e=it.next();if(e.expiresAt<game.elapsed){removeCues(p,e);it.remove();}}
  Evidence threat=primary(p);
  if(threat!=null){
   if(urgency(threat.kind)<p.respondingUrgency&&defensive(t)){if(p.clearSince<0)p.clearSince=game.elapsed;if(game.elapsed-p.clearSince<p.safeClearSeconds)return;p.respondingUrgency=0;p.reduceHeat=false;p.countermeasureRequested=false;transition(p,p.disengaging?State.DISENGAGE:State.REASSESS);}
   else p.clearSince=-1;
   respond(t,threat,delivered&&urgency(threat.kind)>p.respondingUrgency);if(p.disengaging&&!defensive(t))transition(p,State.DISENGAGE);else if(urgency(threat.kind)==1&&!defensive(t)){if(!recovering(t))missionProgress(t);}return;
  }
  if(p.clearSince<0)p.clearSince=game.elapsed;
  boolean wasDefending=defensive(t)||p.state==State.ABORT;
  if(wasDefending&&game.elapsed-p.clearSince<p.safeClearSeconds)return;
  if(wasDefending){p.reduceHeat=false;p.countermeasureRequested=false;p.respondingUrgency=0;transition(p,p.disengaging?State.DISENGAGE:State.REASSESS);return;}
  if(p.disengaging){p.reduceHeat=false;transition(p,State.DISENGAGE);return;}
  if(recovering(t))return;
  if(p.state==State.CAUTIOUS){if(game.elapsed-p.clearSince<p.safeClearSeconds)return;p.respondingUrgency=0;p.courseOffset=0;transition(p,State.APPROACH);}
  missionProgress(t);
 }
 boolean recovering(Game.Contact t){Pilot p=t.pilot;if(p.state==State.REASSESS){if(game.elapsed-p.stateSince>=p.minimumStateHoldSeconds){if(p.passDisrupted)reposition(t);else transition(p,State.APPROACH);}return true;}if(p.state==State.REPOSITION){if(Math.hypot(t.x-p.wayX,t.y-p.wayY)<1||game.elapsed>=p.maneuverUntil){transition(p,State.APPROACH);p.closest=999;p.enteredApproach=false;p.passDisrupted=false;}return true;}return false;}
 void missionProgress(Game.Contact t){Pilot p=t.pilot;if(p.disengaging||defensive(t)||p.state==State.ABORT)return;if(p.intrusion){if(Math.hypot(t.x-p.intrusionX,t.y-p.intrusionY)<2)retreat(t);return;}
  double r=objectiveRange(t);if(r<4){p.enteredApproach=true;transition(p,State.ATTACK_RUN);}p.closest=Math.min(p.closest,r);
  if(p.enteredApproach&&r>p.closest+.3&&r>game.rules.releaseKm)abort(t);
 }
 public double heading(Game.Contact t){Pilot p=t.pilot;if(p==null)return Game.angle(-t.x,-t.y);if(terminal(p))return t.heading;if(defensive(t)&&Double.isFinite(p.maneuverHeading))return p.maneuverHeading;
  if(p.disengaging||p.state==State.EXIT||p.state==State.RETREAT||p.state==State.ABORT)return Game.angle(p.exitX-t.x,p.exitY-t.y);
  if(p.state==State.REPOSITION)return Game.angle(p.wayX-t.x,p.wayY-t.y);
  if(p.state==State.REASSESS)return t.heading;
  return (p.intrusion?Game.angle(p.intrusionX-t.x,p.intrusionY-t.y):Game.angle(p.objectiveX-t.x,p.objectiveY-t.y))+p.courseOffset;
 }
 public boolean defensive(Game.Contact t){Pilot p=t==null?null:t.pilot;return p!=null&&(p.state==State.NOTCH||p.state==State.EVADE||p.state==State.DEFENSIVE||p.state==State.MISSILE_EVASION);}
 public boolean isExiting(Game.Contact t){Pilot p=t==null?null:t.pilot;return p!=null&&(p.disengaging||p.state==State.EXIT||p.state==State.RETREAT||p.state==State.DISENGAGE);}
 public boolean urgentThreat(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null)return false;for(Evidence e:p.evidence.values())if(e.accepted&&e.reacted&&e.expiresAt>=game.elapsed&&urgency(e.kind)>=3&&e.kind!=Warning.ILLUMINATION&&e.confidence>=.65)return true;return false;}
 boolean guidanceThreat(Game.Contact t){Pilot p=t==null?null:t.pilot;Evidence e=p==null?null:primary(p);return e!=null&&urgency(e.kind)>=3;}
 public boolean reducingHeat(Game.Contact t){return t!=null&&t.pilot!=null&&t.pilot.reduceHeat&&defensive(t);}
 /** Called once per aircraft update. Only a completed evidence-based assessment can request a burst. */
 public boolean shouldCountermeasure(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||!t.alive||!p.countermeasureRequested||!defensive(t)||!guidanceThreat(t)||game.elapsed<p.nextCountermeasure)return false;
  p.countermeasureRequested=false;int units=Math.min(p.countermeasureBurstUnits,p.countermeasures);if(units<=0)return false;boolean immediate=urgentThreat(t);if(!immediate&&p.countermeasures-units<p.countermeasureCapacity*p.reserveFraction)return false;
  // Rookie execution can be mistimed without changing sensor capability or manufacturing extra rounds.
  if(game.random.nextDouble()>Game.clamp((.35+.12*p.smart)*(.8+.5*p.skill),0,1))return false;p.countermeasures-=units;p.nextCountermeasure=game.elapsed+p.countermeasureCooldownSeconds;trace(p,"COUNTERMEASURES burst="+units+" remaining="+p.countermeasures);return true;
 }
 /** A notch is only an attempted maneuver. The sensor/guidance model decides whether geometry weakens support. */
 public boolean notchBreak(Game.Contact t){return false;}
 public boolean attemptingNotch(Game.Contact t){return t!=null&&t.pilot!=null&&t.pilot.smart>=3&&defensive(t)&&t.pilot.lastAction==Action.NOTCH;}
 public boolean canBomb(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||!t.alive||p.intrusion||p.disengaging||p.health<100*p.abortHealthFraction||p.bombs<=0)return false;boolean run=p.state==State.APPROACH||p.state==State.CAUTIOUS||p.state==State.ATTACK_RUN;return run&&game.bombSolution(t);}
 public void released(Game.Contact t,int count){Pilot p=t.pilot;if(p==null||count<=0)return;count=Math.min(count,p.bombs);if(p.pendingBombs==0){p.bombDamage=p.observedBombDamage=0;p.bombOutcomeObserved=false;p.bombOutcomes=p.bombObservedOutcomes=p.bombReleasedCount=0;}p.bombReleasedCount+=count;p.pendingBombs+=count;p.bombs-=count;if(p.bombs==0){p.passes++;p.disengaging=true;transition(p,State.EXIT);}else transition(p,State.ATTACK_RUN);}
 public void bombOutcome(Game.Contact t,double damage){bombOutcome(t,damage,true);}
 /** Actual damage is only for post-mission counters. Crew learning requires an observed result. */
 public void bombOutcome(Game.Contact t,double damage,boolean observed){Pilot p=t==null?null:t.pilot;if(p==null||p.pendingBombs<=0)return;p.bombDamage+=Math.max(0,damage);p.bombOutcomes++;if(observed){p.bombOutcomeObserved=true;p.bombObservedOutcomes++;p.observedBombDamage+=Math.max(0,damage);}if(game.finished)p.pendingBombs=1;
  if(--p.pendingBombs==0){boolean actualHit=p.bombDamage>0;boolean observedHit=p.observedBombDamage>0;boolean observedWholeRun=p.bombObservedOutcomes>=p.bombReleasedCount;
   if(observedHit||observedWholeRun){p.memory.bomb(observedHit);if(game.rules.sharedMemory)side.bomb(observedHit);}bombRuns++;if(actualHit)damagingRuns++;
  }
 }
 public void destroyed(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||terminal(p))return;record(p,p.retreatRecorded?Action.RETREAT:p.lastAction,false);p.learnReaction=false;p.cues.clear();p.evidence.clear();p.countermeasureRequested=false;transition(p,State.DESTROYED);}
 public void exited(Game.Contact t){Pilot p=t==null?null:t.pilot;if(p==null||terminal(p))return;if(p.retreatRecorded)record(p,Action.RETREAT,true);p.cues.clear();p.evidence.clear();p.countermeasureRequested=false;transition(p,State.EXITED);}
}

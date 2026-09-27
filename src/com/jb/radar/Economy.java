package com.jb.radar;
import java.util.*;
/** Offline earned currency. Atomic snapshots keep balances and reward receipts together. */
public final class Economy {
 public static final int AIR_DOLLARS=250,AIR_BP=100,MISSILE_DOLLARS=150,MISSILE_BP=75,WIN_DOLLARS=500,WIN_BP=250,HIT_DOLLARS=100,HIT_BP=50;
 public static class State {
  public long dollars,bp,lifetimeDollars,lifetimeBP,serial,wins,completed,missionDollars,missionBP;
  public int aircraft,missiles,remainingHits;
  public String result="NONE",credited="";
  public final HashMap<String,Integer> research=new HashMap<>();public final HashSet<String> owned=new HashSet<>();public String equippedMissile="rampart";
  public State copy(){State n=new State();n.dollars=dollars;n.bp=bp;n.lifetimeDollars=lifetimeDollars;n.lifetimeBP=lifetimeBP;n.serial=serial;n.wins=wins;n.completed=completed;n.missionDollars=missionDollars;n.missionBP=missionBP;n.aircraft=aircraft;n.missiles=missiles;n.remainingHits=remainingHits;n.result=result;n.credited=credited;n.research.putAll(research);n.owned.addAll(owned);n.equippedMissile=equippedMissile;return n;}
 }
 public interface Store {State load();boolean save(State state);}
 public static class MemoryStore implements Store {private State saved=new State();public State load(){return saved.copy();}public boolean save(State s){saved=s.copy();return true;}}
 private final Store store;
 private State state;
 public boolean saveFailed;
 public Economy(Store store){this.store=store;state=store.load();if(state==null)state=new State();TechTree.normalize(state);if("ACTIVE".equals(state.result)){state.result="INTERRUPTED";flush();}}
 public State snapshot(){return state.copy();}
 static long add(long a,long b){a=Math.max(0,a);b=Math.max(0,b);return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
 public boolean flush(){try{saveFailed=!store.save(state.copy());}catch(RuntimeException e){saveFailed=true;}return !saveFailed;}
 public boolean beginMission(){if(saveFailed&&!flush())return false;State next=state.copy();next.serial=add(next.serial,1);next.aircraft=next.missiles=next.remainingHits=0;next.missionDollars=next.missionBP=0;next.result="ACTIVE";next.credited="";
  try{if(!store.save(next.copy())){saveFailed=true;return false;}}catch(RuntimeException e){saveFailed=true;return false;}state=next;saveFailed=false;return true;
 }
 private void credit(long dollars,long bp){state.dollars=add(state.dollars,dollars);state.bp=add(state.bp,bp);state.lifetimeDollars=add(state.lifetimeDollars,dollars);state.lifetimeBP=add(state.lifetimeBP,bp);state.missionDollars=add(state.missionDollars,dollars);state.missionBP=add(state.missionBP,bp);}
 public boolean intercept(String id,boolean missile){String key="["+id+"]";if(!"ACTIVE".equals(state.result)||state.credited.contains(key))return false;state.credited+=key;if(missile){state.missiles++;credit(MISSILE_DOLLARS,MISSILE_BP);}else{state.aircraft++;credit(AIR_DOLLARS,AIR_BP);}flush();return true;}
 public void finish(boolean victory,int hitsRemaining){if(!"ACTIVE".equals(state.result))return;state.result=victory?"VICTORY":"DEFEAT";state.completed=add(state.completed,1);if(victory){state.wins=add(state.wins,1);state.remainingHits=Math.max(0,Math.min(4,hitsRemaining));credit(WIN_DOLLARS+(long)state.remainingHits*HIT_DOLLARS,WIN_BP+(long)state.remainingHits*HIT_BP);}flush();}
 public void abandon(){if("ACTIVE".equals(state.result)){state.result="WITHDREW";flush();}else if(saveFailed)flush();}
 private String techBlock(){if("ACTIVE".equals(state.result))return "RETURN TO THE MENU BEFORE CHANGING EQUIPMENT";if(saveFailed&&!flush())return "SAVE FAILED / RETRY BEFORE SPENDING";return null;}
 private boolean saveTech(State next){try{if(!store.save(next.copy())){saveFailed=true;return false;}}catch(RuntimeException e){saveFailed=true;return false;}state=next;saveFailed=false;return true;}
 public String research(String id){String blocked=techBlock();if(blocked!=null)return blocked;TechTree.Node n=TechTree.get(id);if(n==null)return "UNKNOWN EQUIPMENT";if(!TechTree.prerequisite(state,n))return "OWN THE PREVIOUS EQUIPMENT FIRST";if(TechTree.researched(state,n))return "RESEARCH ALREADY COMPLETE";if(state.bp<=0)return "NO BP AVAILABLE / EARN BP IN BATTLE";
  State next=state.copy();int used=(int)Math.min(next.bp,n.bp-TechTree.progress(next,n));next.bp-=used;next.research.put(n.id,TechTree.progress(next,n)+used);if(!saveTech(next))return "SAVE FAILED / NO BP SPENT";return TechTree.researched(state,n)?"RESEARCH COMPLETE / BUY TO UNLOCK":"RESEARCH SAVED / +"+used+" BP";
 }
 public String purchase(String id){String blocked=techBlock();if(blocked!=null)return blocked;TechTree.Node n=TechTree.get(id);if(n==null)return "UNKNOWN EQUIPMENT";if(state.owned.contains(id))return "ALREADY OWNED";if(!TechTree.prerequisite(state,n)||!TechTree.researched(state,n))return "COMPLETE RESEARCH FIRST";if(state.dollars<n.dollars)return "NOT ENOUGH DOLLARS";State next=state.copy();next.dollars-=n.dollars;next.owned.add(id);if(!saveTech(next))return "SAVE FAILED / NO DOLLARS SPENT";return "PURCHASED / EQUIP FOR YOUR NEXT MISSION";}
 public String equip(String id){String blocked=techBlock();if(blocked!=null)return blocked;TechTree.Node n=TechTree.get(id);if(n==null||n.battery)return "WATCHPOST IS YOUR STARTER BATTERY";if(!state.owned.contains(id))return "BUY THIS MISSILE BEFORE EQUIPPING";if(id.equals(state.equippedMissile))return "ALREADY EQUIPPED";State next=state.copy();next.equippedMissile=id;if(!saveTech(next))return "SAVE FAILED / LOADOUT UNCHANGED";return n.name.toUpperCase(java.util.Locale.US)+" EQUIPPED";}
}

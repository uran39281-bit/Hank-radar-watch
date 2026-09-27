package com.jb.radar;
/** Offline earned currency. Atomic snapshots keep balances and reward receipts together. */
public final class Economy {
 public static final int AIR_DOLLARS=250,AIR_BP=100,MISSILE_DOLLARS=150,MISSILE_BP=75,WIN_DOLLARS=500,WIN_BP=250,HIT_DOLLARS=100,HIT_BP=50;
 public static class State {
  public long dollars,bp,lifetimeDollars,lifetimeBP,serial,wins,completed,missionDollars,missionBP;
  public int aircraft,missiles,remainingHits;
  public String result="NONE",credited="";
  public State copy(){State n=new State();n.dollars=dollars;n.bp=bp;n.lifetimeDollars=lifetimeDollars;n.lifetimeBP=lifetimeBP;n.serial=serial;n.wins=wins;n.completed=completed;n.missionDollars=missionDollars;n.missionBP=missionBP;n.aircraft=aircraft;n.missiles=missiles;n.remainingHits=remainingHits;n.result=result;n.credited=credited;return n;}
 }
 public interface Store {State load();boolean save(State state);}
 public static class MemoryStore implements Store {private State saved=new State();public State load(){return saved.copy();}public boolean save(State s){saved=s.copy();return true;}}
 private final Store store;
 private State state;
 public boolean saveFailed;
 public Economy(Store store){this.store=store;state=store.load();if(state==null)state=new State();if("ACTIVE".equals(state.result)){state.result="INTERRUPTED";flush();}}
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
}

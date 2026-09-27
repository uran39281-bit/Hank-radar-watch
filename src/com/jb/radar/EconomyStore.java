package com.jb.radar;
import android.content.SharedPreferences;
/** One synchronous preference transaction saves currency and its duplicate-payout markers. */
public final class EconomyStore implements Economy.Store {
 private final SharedPreferences prefs;
 public EconomyStore(SharedPreferences prefs){this.prefs=prefs;}
 private long read(String key){return Math.max(0,prefs.getLong("economy_"+key,0));}
 public Economy.State load(){Economy.State s=new Economy.State();s.dollars=read("dollars");s.bp=read("bp");s.lifetimeDollars=read("lifetime_dollars");s.lifetimeBP=read("lifetime_bp");s.serial=read("serial");s.wins=read("wins");s.completed=read("completed");s.missionDollars=read("mission_dollars");s.missionBP=read("mission_bp");s.aircraft=Math.max(0,prefs.getInt("economy_aircraft",0));s.missiles=Math.max(0,prefs.getInt("economy_missiles",0));s.remainingHits=Math.max(0,Math.min(4,prefs.getInt("economy_hits",0)));s.result=prefs.getString("economy_result","NONE");s.credited=prefs.getString("economy_credited","");TechTree.decode(s,prefs.getString("economy_tech_research",""),prefs.getString("economy_tech_owned",""),prefs.getString("economy_equipped_missile","rampart"));return s;}
 public boolean save(Economy.State s){return prefs.edit().putInt("economy_schema",2)
 .putString("economy_tech_research",TechTree.encodeResearch(s)).putString("economy_tech_owned",TechTree.encodeOwned(s)).putString("economy_equipped_missile",s.equippedMissile)
 .putLong("economy_dollars",s.dollars).putLong("economy_bp",s.bp).putLong("economy_lifetime_dollars",s.lifetimeDollars).putLong("economy_lifetime_bp",s.lifetimeBP)
 .putLong("economy_serial",s.serial).putLong("economy_wins",s.wins).putLong("economy_completed",s.completed).putLong("economy_mission_dollars",s.missionDollars).putLong("economy_mission_bp",s.missionBP)
 .putInt("economy_aircraft",s.aircraft).putInt("economy_missiles",s.missiles).putInt("economy_hits",s.remainingHits).putString("economy_result",s.result).putString("economy_credited",s.credited).commit();}
}

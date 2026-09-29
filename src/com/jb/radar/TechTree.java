package com.jb.radar;
import java.util.*;
/** USA research catalog. Prices are initial game balance, not real currency. */
public final class TechTree {
 public static final String USA="USA";
 public static final class Node {
  public final String id,name,parent;public final boolean battery;public final int rank,bp,dollars;
  Node(String id,String name,String parent,boolean battery,int bp,int dollars){this.id=id;this.name=name;this.parent=parent;this.battery=battery;this.rank=1;this.bp=bp;this.dollars=dollars;}
  public boolean starter(){return bp==0&&dollars==0;}
 }
 public static final Node WATCHPOST=new Node("watchpost","ADS-201 Watchpost",null,true,0,0);
 public static final Node RAMPART=new Node("rampart","MIM-301 Rampart","watchpost",false,0,0);
 public static final Node STONEBOLT=new Node("stonebolt","FIM-352 Stonebolt","rampart",false,600,1500);
 public static final Node ACTIVE=new Node("active","MIM-303 Sentinel","watchpost",false,0,0);
 public static final Node INFRARED=new Node("ir6","FIM-306 Ember","watchpost",false,0,0);
 public static final Node[] NODES={WATCHPOST,RAMPART,STONEBOLT,ACTIVE,INFRARED};
 private TechTree(){}
 public static Node get(String id){for(Node n:NODES)if(n.id.equals(id))return n;return null;}
 public static int progress(Economy.State s,Node n){Integer p=s.research.get(n.id);return n.starter()?n.bp:p==null?0:Math.max(0,Math.min(n.bp,p));}
 public static boolean researched(Economy.State s,Node n){return progress(s,n)>=n.bp;}
 public static boolean prerequisite(Economy.State s,Node n){return n.parent==null||s.owned.contains(n.parent);}
 public static String status(Economy.State s,Node n){if(n.battery||n.id.equals(s.equippedMissile))return "EQUIPPED";if(s.owned.contains(n.id))return "OWNED";if(!prerequisite(s,n))return "LOCKED";return researched(s,n)?"READY TO BUY":progress(s,n)>0?"RESEARCHING":"AVAILABLE";}
 public static void normalize(Economy.State s){
  s.owned.retainAll(new HashSet<String>(Arrays.asList("watchpost","rampart","stonebolt","active","ir6")));s.research.keySet().retainAll(new HashSet<String>(Arrays.asList("watchpost","rampart","stonebolt","active","ir6")));
  for(Node n:NODES){if(n.starter())s.owned.add(n.id);int p=progress(s,n);if(s.owned.contains(n.id))p=n.bp;s.research.put(n.id,p);}
  Node selected=get(s.equippedMissile);if(selected==null||selected.battery||!s.owned.contains(selected.id))s.equippedMissile=RAMPART.id;
 }
 public static String encodeResearch(Economy.State s){StringBuilder b=new StringBuilder();for(Node n:NODES)b.append(n.id).append('=').append(progress(s,n)).append(';');return b.toString();}
 public static String encodeOwned(Economy.State s){StringBuilder b=new StringBuilder();for(Node n:NODES)if(s.owned.contains(n.id))b.append(n.id).append(';');return b.toString();}
 public static void decode(Economy.State s,String research,String owned,String selected){
  s.research.clear();s.owned.clear();if(research!=null)for(String part:research.split(";")){String[] pair=part.split("=");if(pair.length==2&&get(pair[0])!=null)try{s.research.put(pair[0],Integer.parseInt(pair[1]));}catch(NumberFormatException ignored){}}
  if(owned!=null)for(String id:owned.split(";"))if(get(id)!=null)s.owned.add(id);s.equippedMissile=selected;normalize(s);
 }
}

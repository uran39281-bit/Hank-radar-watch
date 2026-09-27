package com.jb.radar;
public class MissionSmoke {
 public static void main(String[] a){int total=0;
  for(int seed=0;seed<10;seed++){Game g=new Game(seed);g.start();for(int i=0;i<14000&&!g.finished;i++){
   for(Game.Contact t:g.contacts)if(t.alive&&g.liveTrack(t)&&t.measuredRange()<23&&t.palt<13700){g.illuminate(t);if(t.illuminated&&g.inbound(t)==0){for(int n=0;n<3;n++)if(g.launchers[n].ammo>0){g.chosenLauncher=n;g.launch(t);break;}}}
   g.tick(.05);if(g.channels()>2||g.reserve<0||g.ready()+g.reserve+g.shots!=18)throw new AssertionError("Inventory/channel invariant");
  }if(!g.finished)throw new AssertionError("Unresolved mission");total+=g.kills;System.out.println("seed="+seed+" hits="+g.kills+" shots="+g.shots+" site="+g.health);}
  if(total==0)throw new AssertionError("No moving-target intercepts");
 }
}

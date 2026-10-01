package com.jb.radar;
/** Each component has its own health. Aggregate condition is a display average, not a damage pool. */
public final class Battery {
 public static final int RADAR=0,L1=1,L2=2,L3=3,L4=4,POWER=5,COMMAND=6;
 public static class Component {public final String name;public final double x,y,protection;public double hp=100;
  Component(String name,double x,double y,double protection){this.name=name;this.x=x;this.y=y;this.protection=protection;}
  public boolean alive(){return hp>0;}public int percent(){return (int)Math.ceil(hp);}
 }
 public final Component[] parts=new Component[7];public final CombatRules rules;public double outageUntil;
 public Battery(CombatRules r){rules=r;String[] names={"RADAR","LAUNCHER L1","LAUNCHER L2","LAUNCHER L3","LAUNCHER L4","POWER SUPPLY","COMMAND UNIT"};double[] x={0,.2,-.2,0,0,-.12,.12},y={0,-.08,-.08,.22,-.22,.08,.08};for(int i=0;i<parts.length;i++)parts[i]=new Component(names[i],x[i],y[i],r.protection[i]);}
 public void reset(){for(Component c:parts)c.hp=100;outageUntil=0;}
 public boolean powered(double time){return parts[POWER].alive()&&time>=outageUntil;}
 public boolean radarOnline(double time){return powered(time)&&parts[RADAR].alive()&&parts[COMMAND].alive();}
 public double radarFactor(){return parts[RADAR].alive()?.2+.8*parts[RADAR].hp/100:0;}
 public double processingDelay(){return .25+4*(1-parts[COMMAND].hp/100);}
 public double reloadRate(int launcher,double time){Component c=parts[L1+launcher];return c.alive()&&powered(time)?1/(1+2*(1-c.hp/100)):0;}
 public double condition(){double sum=0;for(Component c:parts)sum+=c.hp;return sum/parts.length;}
 public static double distanceFactor(double distance,double radius){return radius>0?Game.clamp(1-distance/radius,0,1):0;}
 public double explode(double x,double y,double kg,double radius,double time){if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(kg)||!Double.isFinite(radius)||kg<=0||radius<=0)return 0;double before=condition();
  for(int i=0;i<parts.length;i++){Component c=parts[i];double damage=kg*rules.multiplier*distanceFactor(Math.hypot(x-c.x,y-c.y),radius)*c.protection,old=c.hp;c.hp=Math.max(0,c.hp-damage);
   if(i==POWER&&old>c.hp)outageUntil=Math.max(outageUntil,time+Math.min(6,.7+(old-c.hp)*.06));}
  return before-condition();
 }
 public String warning(double time){if(!parts[COMMAND].alive())return "COMMAND DESTROYED";if(!powered(time))return "POWER FAILURE";if(!parts[RADAR].alive())return "RADAR DISABLED";for(int i=L1;i<=L4;i++)if(!parts[i].alive())return "LAUNCHER DISABLED";if(parts[RADAR].hp<99.5)return "RADAR DEGRADED";if(parts[COMMAND].hp<99.5)return "COMMAND PROCESSING SLOW";for(int i=L1;i<=L4;i++)if(parts[i].hp<99.5)return "LAUNCHER RELOAD SLOWED";return "BATTERY OPERATIONAL";}
}

package com.jb.radar;
/** Shared arcade flight step. This is deliberately approximate game physics. */
public final class MissileMotion {
 private MissileMotion(){}
 public static void aim(Game.Missile m,double dx,double dy,double dz){double d=Math.sqrt(dx*dx+dy*dy+dz*dz);if(d<1e-9){m.vx=0;m.vy=0;m.vz=1;}else{m.vx=dx/d;m.vy=dy/d;m.vz=dz/d;}}
 public static void step(Game.Missile m,Equipment.Weapon w,double tx,double ty,double tz,boolean guided,double dt){
  if(dt<=0)return;
  double n=Math.sqrt(m.vx*m.vx+m.vy*m.vy+m.vz*m.vz);if(n<1e-9)aim(m,tx-m.x,ty-m.y,tz-m.z);else{m.vx/=n;m.vy/=n;m.vz/=n;}
  double cap=w.maxSpeedKmh/3600;
  double powered=Math.max(0,Math.min(dt,w.burnSeconds-(m.age-dt)));
  // Thrust / mass accelerates; quadratic coast drag and turning consume energy.
  m.speed+=w.thrustN/w.massKg/1000*powered;
  m.speed=Math.min(cap,Math.max(.04,m.speed-.012*Math.pow(m.speed/Math.max(.01,cap),2)*dt));
  double dx=tx-m.x,dy=ty-m.y,dz=tz-m.z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
  if(guided&&d>1e-9){
   dx/=d;dy/=d;dz/=d;double dot=Game.clamp(dx*m.vx+dy*m.vy+dz*m.vz,-1,1),angle=Math.acos(dot);
   double rate=Math.min(w.maneuverG*9.81/(m.speed*1000),Math.toRadians(w.maxAoADeg)*2);
   double turn=Math.min(angle,rate*dt);
   if(turn>1e-9){double px=dx-dot*m.vx,py=dy-dot*m.vy,pz=dz-dot*m.vz,pn=Math.sqrt(px*px+py*py+pz*pz);
    if(pn<1e-9){if(Math.abs(m.vz)<.9){px=-m.vy;py=m.vx;pz=0;}else{px=1;py=0;pz=-m.vx/m.vz;}pn=Math.sqrt(px*px+py*py+pz*pz);}
    double cs=Math.cos(turn),sn=Math.sin(turn);m.vx=m.vx*cs+px/pn*sn;m.vy=m.vy*cs+py/pn*sn;m.vz=m.vz*cs+pz/pn*sn;
    double aoa=Math.min(w.maxAoADeg,Math.toDegrees(turn/dt)*.5);
    m.speed=Math.max(.04,m.speed*(1-.20*Math.pow(aoa/45,2)*dt));
   }
  }
  m.x+=m.vx*m.speed*dt;m.y+=m.vy*m.speed*dt;m.z+=m.vz*m.speed*dt;m.path+=m.speed*dt;
 }
 public static double nearest(double ox,double oy,double oz,Game.Missile m,double tx,double ty,double tz){double sx=m.x-ox,sy=m.y-oy,sz=m.z-oz,len=sx*sx+sy*sy+sz*sz;double f=len==0?0:Game.clamp(((tx-ox)*sx+(ty-oy)*sy+(tz-oz)*sz)/len,0,1);return Math.sqrt(Math.pow(tx-ox-f*sx,2)+Math.pow(ty-oy-f*sy,2)+Math.pow(tz-oz-f*sz,2));}
}

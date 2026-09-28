package com.jb.radar;
public class IntroTest {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){IntroSequence s=new IntroSequence();ok(s.SECTIONS.length==12,"12 exact sections");ok(Math.abs(s.ends[11]-75.64)<1e-8,"ZIP runtime 75.64 seconds");s.start();ok(s.section()==0&&s.visible()==0,"blank preroll");s.advance(.299);ok(s.visible()==0,"300ms pre-section pause");s.advance(.1);ok(s.visible()>0,"typing begins");double t=s.elapsed;s.paused=true;s.advance(300);s.sync(40);ok(s.elapsed==t,"pause holds clock");s.paused=false;s.advance(2);ok(s.elapsed==t+2,"resume continues");
 for(int n=0;n<12;n++){s.elapsed=s.reveal[n][s.reveal[n].length-1]+.001;ok(s.section()==n&&s.visible()==s.SECTIONS[n].length(),"full story revealed "+n);double hold=s.ends[n]-s.reveal[n][s.reveal[n].length-1]-IntroSequence.delay(s.SECTIONS[n].charAt(s.SECTIONS[n].length()-1));ok(Math.abs(hold-(n==0?1.8:n==11?2.8:2.1))<1e-8,"complete hold "+n);}
 s.advance(100);ok(s.done(),"normal completion");s.stop();s.start();ok(s.elapsed==0&&!s.paused&&s.active,"replay resets");s.stop();s.advance(100);ok(s.elapsed==0&&!s.active,"skip cancels sequence");System.out.printf("PASS: intro timing, all sections, pause/resume, skip/replay (ZIP duration %.3fs)%n",s.ends[11]);
 }
}

package com.jb.radar;
import android.graphics.*;

/** Lay out complete sections once, then reveal glyphs at their fixed positions. */
final class IntroScene {
 final Paint paint=new Paint(3);final Typeface font;final float[][] xs=new float[12][],ys=new float[12][];
 final String[][] glyphs=new String[12][];final float[] cursorX=new float[12],cursorY=new float[12];
 static final int GREEN=0xff58ff7e;
 static final float SIZE=41,LEFT=192,RIGHT=1088,TOP=0,LINE=SIZE*1.65f,SPACING=.23f;
 IntroScene(Typeface font){this.font=font;paint.setTypeface(font);paint.setTextSize(SIZE);
  for(int n=0;n<IntroSequence.SECTIONS.length;n++)layout(n);
 }
 String glyph(char c){return c=='…'?"...":String.valueOf(c);}
 float width(String s){return paint.measureText(s)+s.length()*SPACING;}
 void layout(int n){String s=IntroSequence.SECTIONS[n];int len=s.length();xs[n]=new float[len];ys[n]=new float[len];glyphs[n]=new String[len];float x=LEFT,y=TOP;
  for(int i=0;i<len;i++){char ch=s.charAt(i);
   if(ch!=' '&&ch!='\n'&&(i==0||s.charAt(i-1)==' '||s.charAt(i-1)=='\n')){int end=i;StringBuilder word=new StringBuilder();while(end<len&&s.charAt(end)!=' '&&s.charAt(end)!='\n')word.append(glyph(s.charAt(end++)));if(x>LEFT&&x+width(word.toString())>RIGHT){x=LEFT;y+=LINE;}}
   xs[n][i]=x;ys[n][i]=y;glyphs[n][i]=glyph(ch);
   if(ch=='\n'){x=LEFT;y+=LINE;}else{x+=width(glyphs[n][i]);}
  }
  // Center the complete section, not the currently visible prefix.
  float offset=344+SIZE*.34f-y/2;for(int i=0;i<len;i++)ys[n][i]+=offset;
  cursorX[n]=x;cursorY[n]=y+offset;
 }
 void control(Canvas c,String label,float x,float y,float w){paint.setTypeface(font);paint.setTextSize(23);paint.setStyle(Paint.Style.FILL);paint.setColor(GREEN);c.drawText(label,x+(w-paint.measureText(label))/2,y+35,paint);}
 void draw(Canvas c,IntroSequence seq){paint.setTypeface(font);paint.setTextSize(SIZE);paint.setStyle(Paint.Style.FILL);paint.setColor(GREEN);int n=seq.section(),count=seq.visible();for(int i=0;i<count;i++)if(IntroSequence.SECTIONS[n].charAt(i)!='\n')c.drawText(glyphs[n][i],xs[n][i],ys[n][i],paint);
  float x=count<xs[n].length?xs[n][count]:cursorX[n],y=count<ys[n].length?ys[n][count]:cursorY[n];
  // The cursor stays after the latest letter, even before an automatic word wrap.
  if(count>0&&IntroSequence.SECTIONS[n].charAt(count-1)!='\n'){x=xs[n][count-1]+width(glyphs[n][count-1]);y=ys[n][count-1];}
  if(count>0){paint.setStrokeWidth(3);c.drawLine(x+1,y+3,x+SIZE*.55f,y+3,paint);}
 }
}

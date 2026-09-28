package android.graphics;
import java.awt.*;import java.awt.geom.*;import java.awt.image.*;import java.util.*;
public class Canvas {
 public BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();Stack<Graphics2D> stack=new Stack<>();
 public Canvas(){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
 void apply(Paint p){g.setColor(new java.awt.Color(p.color,true));g.setStroke(new BasicStroke(p.stroke));g.setFont(p.typeface.awt.deriveFont(p.size));}
 void shape(Shape s,Paint p){apply(p);if(p.style==Paint.Style.FILL)g.fill(s);else g.draw(s);}
 public void drawColor(int c){g.setColor(new java.awt.Color(c,true));g.fillRect(0,0,1280,720);}
 public void drawText(String s,float x,float y,Paint p){apply(p);g.drawString(s,x,y);}
 public void drawLine(float x,float y,float a,float b,Paint p){apply(p);g.draw(new Line2D.Float(x,y,a,b));}
 public void drawCircle(float x,float y,float r,Paint p){shape(new Ellipse2D.Float(x-r,y-r,2*r,2*r),p);}
 public void drawPath(Path path,Paint p){shape(path.shape,p);}
 public void drawOval(RectF r,Paint p){shape(new Ellipse2D.Float(r.l,r.t,r.r-r.l,r.b-r.t),p);}
 public void drawRoundRect(RectF r,float a,float b,Paint p){shape(new RoundRectangle2D.Float(r.l,r.t,r.r-r.l,r.b-r.t,a,b),p);}
 public void drawArc(RectF r,float a,float b,boolean center,Paint p){shape(new Arc2D.Float(r.l,r.t,r.r-r.l,r.b-r.t,-a,-b,center?Arc2D.PIE:Arc2D.OPEN),p);}
 public void drawBitmap(Bitmap b,Object src,RectF r,Paint p){g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,p.alpha/255f));g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(b.image,(int)r.l,(int)r.t,(int)(r.r-r.l),(int)(r.b-r.t),null);g.setComposite(AlphaComposite.SrcOver);}public void save(){stack.push(g);g=(Graphics2D)g.create();}public void restore(){g.dispose();g=stack.pop();}public void translate(float x,float y){g.translate(x,y);}public void rotate(float a){g.rotate(Math.toRadians(a));}public void scale(float x,float y){g.scale(x,y);}public void clipPath(Path p){g.clip(p.shape);}
}
package com.jb.radar;
import android.graphics.*;import javax.imageio.ImageIO;import java.io.File;
public class MenuPreview{
 public static void main(String[] args)throws Exception{MenuScene scene=new MenuScene();for(int i=0;i<2;i++){Canvas c=new Canvas();scene.draw(c,i*4,"$4,000","1,850",false,false);ImageIO.write(c.image,"png",new File("out/menu-v011-"+i+".png"));}
  MainActivity a=new MainActivity();a.onCreate(null);a.onResume();TechTreeRenderCheck.draw(a.view,"menu-v011-new-save");a.onDestroy();System.out.println("Rendered two animation phases and the actual zero-balance menu.");
 }
}

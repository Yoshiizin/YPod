package com.ypod.minihome;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MiniActivity extends Activity {
  private final ArrayList<App> apps=new ArrayList<App>();
  private final String[] labels={"Music","Files","Settings","Apps"};
  private ViewHome home;
  private SharedPreferences prefs;
  private int battery=-1;
  private boolean listening=false;
  private final BroadcastReceiver batteryReceiver=new BroadcastReceiver(){
    @Override public void onReceive(Context c,Intent i){
      if(Intent.ACTION_BATTERY_CHANGED.equals(i.getAction())){
        int n=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),d=i.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        if(n>=0 && d>0) battery=(n*100)/d;
      }
      if(home!=null)home.invalidate();
    }
  };
  static final class App {
    final String label,pkg,cls;
    App(String l,String p,String c){label=l;pkg=p;cls=c;}
  }
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setFlags(1024,1024);
    prefs=getSharedPreferences("shortcuts",0);
    home=new ViewHome(this);setContentView(home);
  }
  @Override protected void onResume(){
    super.onResume();refresh();
    if(!listening){
      IntentFilter f=new IntentFilter();
      f.addAction(Intent.ACTION_BATTERY_CHANGED);
      f.addAction(Intent.ACTION_TIME_TICK);
      f.addAction(Intent.ACTION_DATE_CHANGED);
      registerReceiver(batteryReceiver,f);listening=true;
    }
    home.invalidate();
  }
  @Override protected void onPause(){
    if(listening){unregisterReceiver(batteryReceiver);listening=false;}
    super.onPause();
  }
  @Override public void onBackPressed(){
    if(home.drawer){home.drawer=false;home.invalidate();}
  }
  @Override public boolean onCreateOptionsMenu(android.view.Menu m){
    m.add("Todos os aplicativos").setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener(){
      public boolean onMenuItemClick(android.view.MenuItem i){home.drawer=true;home.invalidate();return true;}
    });
    m.add("Configurar atalhos").setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener(){
      public boolean onMenuItemClick(android.view.MenuItem i){chooseTile();return true;}
    });
    return true;
  }
  private void refresh(){
    apps.clear();PackageManager pm=getPackageManager();
    Intent intent=new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> got=pm.queryIntentActivities(intent,0);
    if(got!=null)for(ResolveInfo r:got){
      if(r.activityInfo==null || getPackageName().equals(r.activityInfo.packageName))continue;
      CharSequence title=r.loadLabel(pm);
      apps.add(new App(title==null?r.activityInfo.name:title.toString(),r.activityInfo.packageName,r.activityInfo.name));
    }
    Collections.sort(apps,new Comparator<App>(){
      public int compare(App a,App b){return a.label.compareToIgnoreCase(b.label);}
    });
  }
  private void chooseTile(){
    new AlertDialog.Builder(this).setTitle("Configurar atalho").setItems(labels,new DialogInterface.OnClickListener(){
      public void onClick(DialogInterface d,int i){if(i<3)chooseApp(i);else {home.drawer=true;home.invalidate();}}
    }).show();
  }
  private void chooseApp(final int index){
    final String[] names=new String[apps.size()+1];
    names[0]="Limpar / usar padrão";
    for(int i=0;i<apps.size();i++)names[i+1]=apps.get(i).label;
    new AlertDialog.Builder(this).setTitle("Atalho: "+labels[index]).setItems(names,new DialogInterface.OnClickListener(){
      public void onClick(DialogInterface d,int selected){
        SharedPreferences.Editor e=prefs.edit();
        if(selected==0)e.remove("t"+index);
        else {App a=apps.get(selected-1);e.putString("t"+index,a.pkg+"/"+a.cls);}
        e.apply();home.invalidate();
      }
    }).show();
  }
  private void launch(App a){
    try {
      Intent i=new Intent(Intent.ACTION_MAIN);
      i.addCategory(Intent.CATEGORY_LAUNCHER);
      i.setComponent(new ComponentName(a.pkg,a.cls));
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(i);
    }catch(Exception e){Toast.makeText(this,"Erro ao abrir "+a.label,Toast.LENGTH_SHORT).show();}
  }
  private void shortcut(int index){
    if(index==3){home.drawer=true;home.invalidate();return;}
    String bound=prefs.getString("t"+index,null);
    if(bound!=null)for(App a:apps)if(bound.equals(a.pkg+"/"+a.cls)){launch(a);return;}
    if(index==2){
      try{startActivity(new Intent(Settings.ACTION_SETTINGS));return;}catch(Exception e){}
    } else {
      String[] terms=index==0?new String[]{"ypod","música","music","player"}:new String[]{"arquivos","files","file manager"};
      for(String t:terms)for(App a:apps)if(a.label.toLowerCase(Locale.US).contains(t)){launch(a);return;}
    }
    chooseApp(index);
  }
  private final class ViewHome extends View {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r=new RectF();
    final Handler h=new Handler();
    boolean drawer=false,moved=false,longPress=false;
    int downTile=-1;float startX,startY,lastY,scroll=0;
    final int BLUE=0xff0b43c5,WHITE=0xfff8f6f5,LIGHT=0xffbed8ff,DARK=0xff0a2872;
    final Runnable longTask=new Runnable(){
      public void run(){if(!drawer && !moved && downTile>=0 && downTile<3){longPress=true;chooseApp(downTile);}}
    };
    ViewHome(Context c){super(c);}
    void paint(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
    void box(Canvas c,float x,float y,float w,float ht,int col){paint(col);c.drawRect(x,y,x+w,y+ht,p);}
    void round(Canvas c,float x,float y,float w,float ht,float rad,int col){
      paint(col);r.set(x,y,x+w,y+ht);c.drawRoundRect(r,rad,rad,p);
    }
    void circle(Canvas c,float x,float y,float rad,int col){paint(col);c.drawCircle(x,y,rad,p);}
    void line(Canvas c,float x,float y,float xx,float yy,int col,float width){
      paint(col);p.setStrokeWidth(width);c.drawLine(x,y,xx,yy,p);
    }
    void word(Canvas c,String s,float x,float y,float sz,int col,boolean bold){
      paint(col);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextSize(sz);c.drawText(s,x,y,p);
    }
    @Override protected void onDraw(Canvas real){
      real.drawColor(BLUE);real.save();
      real.scale(getWidth()/240f,getHeight()/320f);
      if(drawer)drawApps(real);else drawHome(real);
      real.restore();
    }

    void outline(Canvas c,float x,float y,float rad,int col,float thickness){
      paint(col);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(thickness);
      c.drawCircle(x,y,rad,p);p.setStyle(Paint.Style.FILL);
    }
    void poly(Canvas c,int color,float... points){
      Path path=new Path();path.moveTo(points[0],points[1]);
      for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);
      path.close();paint(color);c.drawPath(path,p);
    }
    void oval(Canvas c,float x1,float y1,float x2,float y2,int col){
      paint(col);r.set(x1,y1,x2,y2);c.drawOval(r,p);
    }
    void retroIcon(Canvas c,int index,float x,float y){
      // All glyph-free icons use vectors supported by Gingerbread.
      if(index==0){
        line(c,x,y-5,x,y+4,WHITE,1.8f);
        line(c,x,y-5,x+6,y-6,WHITE,1.8f);
        line(c,x+6,y-6,x+6,y+2,WHITE,1.8f);
        oval(c,x-5,y+2,x+1,y+6,WHITE);
        oval(c,x+1,y,x+7,y+4,WHITE);
      }else if(index==1){
        round(c,x-7,y-4,14,10,1,WHITE);
        box(c,x-6,y-6,7,3,WHITE);
        round(c,x-5,y-1,10,5,0.8f,BLUE);
      }else if(index==2){
        outline(c,x,y,4,WHITE,1.5f);
        circle(c,x,y,1.4f,WHITE);
        for(int i=0;i<8;i++){
          double angle=i*Math.PI/4d;
          float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
          line(c,x+dx*5.3f,y+dy*5.3f,x+dx*7.5f,y+dy*7.5f,WHITE,1.5f);
        }
      }else{
        for(int row=0;row<2;row++)for(int col=0;col<2;col++)
          round(c,x-6+col*7,y-6+row*7,5,5,1,WHITE);
      }
    }
    void drawHome(Canvas c){
      Calendar cal=Calendar.getInstance();
      word(c,"MINI HOME / YPOD",15,12,6,LIGHT,false);
      for(int i=-1;i<=1;i++){
        Calendar d=(Calendar)cal.clone();d.add(Calendar.DAY_OF_YEAR,i);
        String name=new SimpleDateFormat("EEEE",Locale.ENGLISH).format(d.getTime());
        circle(c,18,28+(i+1)*13,3,i==0?WHITE:LIGHT);
        if(i!=0)circle(c,18,28+(i+1)*13,2,BLUE);
        word(c,name,27,31+(i+1)*13,8,i==0?WHITE:LIGHT,i==0);
      }
      word(c,"Hi",132,37,32,WHITE,true);
      round(c,175,21,48,18,9,0xffe4484a);
      word(c,":)",190,34,11,WHITE,true);
      word(c,"Welcome",112,61,23,WHITE,true);
      String date=new SimpleDateFormat("EEE, d MMM",Locale.ENGLISH).format(new Date());
      word(c,date+"   /   "+(battery<0?"--":battery)+"%",18,83,8,WHITE,false);
      line(c,17,90,224,90,0xff729be3,0.65f);
      for(int i=0;i<4;i++){
        float y=108+i*25;
        circle(c,29,y-4,10,0xff2b6cda);
        retroIcon(c,i,29,y-4);
        word(c,labels[i],46,y,10,WHITE,i==0);
      }

      // Carefully layered 2D objects to match the original 240x320 preview.
      poly(c,0xff03194d,30,278,209,272,239,289,54,297);
      poly(c,0xff143075,42,283,208,282,217,286,55,292);
      // Tilted vintage Macintosh computer, cyan monitor and floppy drive.
      poly(c,0xff9295a6,116,190,182,201,188,265,124,255);
      poly(c,0xffd8d5d7,112,185,179,196,177,262,108,251);
      poly(c,0xfff0ece8,114,186,179,196,174,248,109,240);
      poly(c,0xff999caf,123,195,166,201,165,230,121,226);
      poly(c,0xff123a66,125,197,164,203,163,227,122,223);
      poly(c,0xff73cee6,128,199,161,204,160,223,126,220);
      word(c,"404",132,215,16,0xff23435b,true);
      word(c,"ERROR",137,221,3.8f,0xff23435b,true);
      line(c,124,231,162,237,0xffb5b4bb,1);
      round(c,137,241,20,4,1,0xff484b5a);
      circle(c,168,245,1.7f,0xff4e5261);
      poly(c,0xff888c9f,131,258,170,262,182,270,122,268);
      poly(c,0xffd8d5d7,129,255,169,258,175,266,124,264);
      line(c,135,261,166,263,0xff656c82,1.4f);
      // Wired retro controller.
      poly(c,0xff666a81,78,261,105,267,105,280,78,275);
      poly(c,0xffa1a5ba,81,259,106,265,104,276,80,272);
      line(c,87,265,95,266,0xff2f354e,1.4f);
      line(c,91,262,91,270,0xff2f354e,1.3f);
      circle(c,100,268,1.2f,0xffe4484a);
      paint(0xff111c3d);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
      r.set(59,236,122,271);c.drawArc(r,182,180,false,p);
      p.setStyle(Paint.Style.FILL);
      // Ashtray.
      poly(c,0xff212b47,28,274,48,266,78,273,75,283,47,289);
      poly(c,0xff8089a6,29,266,50,259,77,267,72,278,45,283);
      oval(c,36,263,71,278,0xffb8b9c5);
      oval(c,42,265,65,274,0xff4c5978);
      line(c,56,273,71,258,0xffc8b48c,2.6f);
      // Red soda.
      poly(c,0xffae202d,186,264,207,267,220,293,197,292);
      poly(c,0xffe4484a,185,261,203,262,217,290,198,287);
      poly(c,0xffef6870,185,262,196,266,206,288,199,286);
      oval(c,184,258,203,267,0xff9aa2b2);
      oval(c,188,260,200,264,0xffdedade);
      c.save();c.rotate(-20,203,278);
      word(c,"COKE",187,280,10,WHITE,true);c.restore();
      circle(c,209,289,1.5f,WHITE);
      // Small blue alien and desk props.
      round(c,55,208,27,26,10,0xff3474d1);
      circle(c,62,219,3.4f,0xff12398a);
      circle(c,74,219,3.4f,0xff12398a);
      poly(c,0xff12398a,66,227,70,227,68,230);
      round(c,98,289,10,3,2,0xffead9cb);
      circle(c,109,289,2.1f,0xffef4e5c);
      round(c,107,293,26,24,12,0xff1e5aca);
      paint(LIGHT);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);
      r.set(115,300,125,309);c.drawRoundRect(r,1,1,p);
      p.setStyle(Paint.Style.FILL);
      word(c,"APPS",136,307,6.5f,LIGHT,true);
    }
    void drawApps(Canvas c){
      box(c,0,0,240,320,DARK);
      word(c,"ALL APPS",15,34,24,WHITE,true);
      word(c,"Toque para abrir  /  deslize para rolar",15,51,7,LIGHT,false);
      round(c,194,15,31,29,12,0xff285fc4);
      word(c,"<",203,35,17,WHITE,true);
      c.save();c.clipRect(9,60,231,320);
      for(int i=0;i<apps.size();i++){
        float y=65+i*33-scroll;
        if(y<30||y>335)continue;
        App a=apps.get(i);
        round(c,15,y,211,29,7,0xff1957b6);
        circle(c,32,y+15,10,0xff3a7add);
        word(c,a.label.length()>0?a.label.substring(0,1).toUpperCase(Locale.getDefault()):"?",28,y+19,12,WHITE,true);
        String s=a.label.length()>26?a.label.substring(0,25)+"…":a.label;
        word(c,s,50,y+19,11,WHITE,false);
      }
      c.restore();
    }
    float bound(float v){return Math.max(0,Math.min(v,Math.max(0,apps.size()*33-251)));}
    int tile(float x,float y){
      if(x<14||x>153)return -1;
      for(int i=0;i<4;i++)if(y>=94+i*25&&y<=116+i*25)return i;
      return -1;
    }
    @Override public boolean onTouchEvent(MotionEvent ev){
      float x=ev.getX()*240/getWidth(),y=ev.getY()*320/getHeight();
      switch(ev.getAction()){
        case MotionEvent.ACTION_DOWN:
          startX=x;startY=y;lastY=y;moved=false;longPress=false;
          downTile=drawer?-1:tile(x,y);
          if(downTile>=0&&downTile<3)h.postDelayed(longTask,650);
          return true;
        case MotionEvent.ACTION_MOVE:
          if(Math.abs(x-startX)>7||Math.abs(y-startY)>7){moved=true;h.removeCallbacks(longTask);}
          if(drawer&&moved){scroll=bound(scroll+lastY-y);invalidate();}
          lastY=y;return true;
        case MotionEvent.ACTION_CANCEL:h.removeCallbacks(longTask);return true;
        case MotionEvent.ACTION_UP:
          h.removeCallbacks(longTask);
          if(moved||longPress)return true;
          if(drawer){
            if(y<55&&x>181){drawer=false;invalidate();return true;}
            int idx=(int)((y-65+scroll)/33);
            if(y>=60 && idx>=0 && idx<apps.size())launch(apps.get(idx));
          }else{
            int ix=tile(x,y);
            if(ix>=0&&ix==downTile)shortcut(ix);
            else if(y>=290){drawer=true;scroll=0;invalidate();}
          }
          return true;
      }
      return true;
    }
  }
}

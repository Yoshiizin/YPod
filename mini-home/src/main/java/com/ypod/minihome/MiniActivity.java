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
      round(c,178,21,47,18,9,0xffdd4550);
      word(c,":)",190,34,11,WHITE,true);
      word(c,"Welcome",112,61,23,WHITE,true);
      String date=new SimpleDateFormat("EEE, d MMM",Locale.ENGLISH).format(new Date());
      word(c,date+"   /   "+(battery<0?"--":battery)+"%",18,83,8,WHITE,false);
      line(c,18,91,224,91,0xff5087d9,0.7f);
      for(int i=0;i<4;i++){
        float y=108+i*25;
        circle(c,29,y-4,10,0xff2b6cda);
        word(c,new String[]{"♫","F","⚙","▦"}[i],23,y+1,12,WHITE,true);
        word(c,labels[i],46,y,10,WHITE,i==0);
      }
      // Low-cost static retro artwork, no textures, bitmaps or animations.
      box(c,25,279,203,9,0xff08225a);
      round(c,103,190,77,67,5,0xffabb1be);
      round(c,109,185,68,66,5,0xffdad9da);
      round(c,117,194,50,37,2,0xff5a798d);
      round(c,122,198,40,29,1,0xff82d9e9);
      word(c,"404",128,218,18,0xff30536b,true);
      box(c,133,241,31,4,0xff666a7b);
      round(c,121,253,57,13,3,0xffbcb9bc);
      line(c,135,261,166,262,0xff676979,1);
      round(c,76,265,37,14,4,0xff8e94aa);
      word(c,"+",82,275,14,0xff1a2c50,true);
      circle(c,102,272,2,0xffe85a60);
      circle(c,47,272,25,0xff717eac);
      circle(c,47,272,17,0xffc1c4cf);
      circle(c,47,272,11,0xff5d6885);
      line(c,53,274,67,254,0xffd6b8a1,3);
      round(c,184,260,24,32,4,0xffd73543);
      round(c,184,259,24,6,3,0xffaab3c2);
      c.save();c.rotate(-19,193,280);
      word(c,"COLA",180,281,8,WHITE,true);c.restore();
      round(c,61,209,21,24,8,0xff337edc);
      circle(c,67,221,3,DARK);circle(c,76,221,3,DARK);
      line(c,17,290,221,290,0xff092463,2);
      round(c,106,294,28,24,14,0xff285fce);
      word(c,"□",114,310,14,LIGHT,true);
      word(c,"APPS",138,310,7,LIGHT,true);
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

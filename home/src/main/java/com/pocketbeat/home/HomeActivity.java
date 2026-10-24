package com.pocketbeat.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

/**
 * YPod Home Classic: a small, self-contained Android 2.3 launcher.
 * No AndroidX, networking, ads, services or imported iOS graphical assets.
 * User can opt in as their default HOME screen and change it later.
 */
public final class HomeActivity extends Activity {
    private static final int WHITE=Color.WHITE, BLUE=0xff75d4ff;
    private static final String[] MAIN_TYPES={
        "music","photos","camera","clock",
        "settings","files","web","bluetooth",
        "gallery","calc","contacts","apps"
    };
    private static final String[] MAIN_LABELS={
        "Música","Fotos","Câmera","Relógio",
        "Ajustes","Arquivos","Internet","Bluetooth",
        "Galeria","Calculadora","Contatos","Apps"
    };
    private static final String[] DOCK_TYPES={"phone","messages","music","apps"};
    private static final String[] DOCK_LABELS={"Telefone","Mensagens","Música","Apps"};
    private static final int SCREEN_HOME=0, SCREEN_APPS=1, SCREEN_SETTINGS=2;
    private int screen=SCREEN_HOME, palette=0;
    private FrameLayout frame;
    private LinearLayout root;
    private TextView statusClock;
    private ListView appsList;
    private AppsAdapter appAdapter;
    private EditText appSearch;
    private final ArrayList<ResolveInfo> installed=new ArrayList<ResolveInfo>();
    private final ArrayList<ResolveInfo> filtered=new ArrayList<ResolveInfo>();
    private final Handler clockHandler=new Handler();
    private final Runnable refreshTime=new Runnable(){
        public void run() {
            if(statusClock!=null)statusClock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));
            clockHandler.postDelayed(this,30000);
        }
    };

    private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams params(int width,int height){
        return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));
    }
    private TextView label(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.CENTER);t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);return t;
    }
    private GradientDrawable gradient(int top,int bottom,int corners){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
        d.setCornerRadius(dp(corners));return d;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        palette=getPreferences(0).getInt("palette",0);
        build();clockHandler.post(refreshTime);
        if(!getPreferences(0).getBoolean("first_run_complete",false)){
            getPreferences(0).edit().putBoolean("first_run_complete",true).commit();
            new AlertDialog.Builder(this).setTitle("Bem-vindo ao YPod Home")
                .setMessage("Seu Samsung agora pode ter uma tela inicial inspirada nos iPods clássicos.\n\nPressione o botão Home do aparelho e escolha YPod Home para ativá-la. Você pode voltar ao launcher Samsung depois pelas configurações do Android.")
                .setPositiveButton("Começar",null).show();
        }
    }
    @Override protected void onResume(){super.onResume();if(screen==SCREEN_APPS)loadApplications();}
    private void build(){
        frame=new FrameLayout(this);frame.setBackgroundColor(0xff0a1020);
        frame.addView(new Wallpaper(this),new FrameLayout.LayoutParams(-1,-1));
        root=column();frame.addView(root,new FrameLayout.LayoutParams(-1,-1));
        setContentView(frame);
        showStatus();
        if(screen==SCREEN_HOME)showHome();
        else if(screen==SCREEN_APPS)showApps();
        else showSettings();
    }
    private void showStatus(){
        LinearLayout bar=row();bar.setPadding(dp(7),0,dp(7),0);
        bar.setBackgroundDrawable(gradient(0xaa000000,0x33000000,0));
        TextView brand=label("●●●   YPod",10,WHITE,true);
        brand.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        bar.addView(brand,new LinearLayout.LayoutParams(0,dp(20),1));
        statusClock=label("",11,WHITE,true);
        bar.addView(statusClock,params(60,20));
        statusClock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));
        String battery="?";
        try {
            Intent info=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(info!=null){
                int level=info.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                int scale=info.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
                if(level>=0&&scale>0)battery=(level*100/scale)+"%";
            }
        }catch(Exception ignored){}
        TextView charge=label(battery,10,WHITE,false);
        charge.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        bar.addView(charge,new LinearLayout.LayoutParams(0,dp(20),1));
        root.addView(bar,params(-1,20));
    }
    private void showHome(){
        final float density=getResources().getDisplayMetrics().density;
        int dpWidth=(int)(getResources().getDisplayMetrics().widthPixels/density);
        int dpHeight=(int)(getResources().getDisplayMetrics().heightPixels/density);
        boolean compact=dpHeight<355;
        int iconSize=Math.max(38,Math.min(compact?49:55,(dpWidth-40)/4-8));
        int rowHeight=compact?70:82;
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout section=column();section.setGravity(Gravity.CENTER);section.setPadding(dp(5),dp(7),dp(5),dp(2));
        scroll.addView(section);
        for(int r=0;r<3;r++){
            LinearLayout strip=row();strip.setGravity(Gravity.CENTER);
            section.addView(strip,params(-1,rowHeight));
            for(int c=0;c<4;c++){
                final int index=r*4+c;
                LinearLayout item=shortcut(MAIN_TYPES[index],MAIN_LABELS[index],iconSize);
                strip.addView(item,new LinearLayout.LayoutParams(0,-1,1));
                item.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v){openShortcut(MAIN_TYPES[index]);}
                });
            }
        }
        TextView dots=label("●  •  •",13,0xffeef5ff,true);
        dots.setShadowLayer(dp(2),0,dp(1),0xff000000);
        section.addView(dots,params(-1,compact?16:27));
        LinearLayout dock=row();dock.setPadding(dp(6),dp(3),dp(6),dp(2));
        dock.setGravity(Gravity.CENTER);
        dock.setBackgroundDrawable(gradient(0x998e9cad,0x88616a7b,18));
        LinearLayout.LayoutParams dockParams=params(-1,compact?65:76);
        dockParams.leftMargin=dp(5);dockParams.rightMargin=dp(5);dockParams.bottomMargin=dp(5);
        root.addView(dock,dockParams);
        for(int i=0;i<DOCK_TYPES.length;i++){
            final String type=DOCK_TYPES[i];
            LinearLayout item=shortcut(type,DOCK_LABELS[i],iconSize);
            dock.addView(item,new LinearLayout.LayoutParams(0,-1,1));
            item.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){openShortcut(type);}
            });
        }
    }
    private LinearLayout shortcut(String type,String name,int iconSize){
        LinearLayout item=column();item.setGravity(Gravity.CENTER);
        ShortcutIcon icon=new ShortcutIcon(this,type);
        LinearLayout.LayoutParams pic=params(iconSize,iconSize);pic.gravity=Gravity.CENTER_HORIZONTAL;
        item.addView(icon,pic);
        TextView title=label(name,10,WHITE,false);
        title.setShadowLayer(dp(2),0,dp(1),0xff05050a);
        LinearLayout.LayoutParams caption=params(-1,18);caption.topMargin=dp(2);
        item.addView(title,caption);
        return item;
    }
    private void openShortcut(String type){
        if("apps".equals(type)){screen=SCREEN_APPS;loadApplications();build();return;}
        if("settings".equals(type)){screen=SCREEN_SETTINGS;build();return;}
        if("music".equals(type)){
            if(launchKnown("com.pocketbeat.ypodclassic","com.pocketbeat.ypod2","com.pocketbeat.player"))return;
            new AlertDialog.Builder(this).setTitle("YPod Music")
                .setMessage("Instale primeiro o YPod Music 3.0. O YPod Home abrirá o player automaticamente.")
                .setPositiveButton("Entendi",null).show();return;
        }
        if("phone".equals(type)){launch(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:")),"Telefone");return;}
        if("messages".equals(type)){launch(new Intent(Intent.ACTION_VIEW,Uri.parse("sms:")),"Mensagens");return;}
        if("camera".equals(type)){launch(new Intent("android.media.action.IMAGE_CAPTURE"),"Câmera");return;}
        if("photos".equals(type)||"gallery".equals(type)){
            if(launchKnown("com.cooliris.media","com.sec.android.gallery3d","com.android.gallery3d"))return;
            Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");
            launch(i,"Galeria");return;
        }
        if("clock".equals(type)){
            if(launchKnown("com.sec.android.app.clockpackage","com.android.deskclock"))return;
            launch(new Intent("android.intent.action.SHOW_ALARMS"),"Relógio");return;
        }
        if("files".equals(type)){
            if(launchKnown("com.sec.android.app.myfiles","com.android.filemanager","com.estrongs.android.pop"))return;
            Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("*/*");
            launch(i,"Arquivos");return;
        }
        if("web".equals(type)){
            if(launchKnown("com.android.browser"))return;
            launch(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")),"Navegador");return;
        }
        if("bluetooth".equals(type)){
            launch(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),"Bluetooth");return;
        }
        if("calc".equals(type)){
            if(launchKnown("com.sec.android.app.popupcalculator","com.android.calculator2"))return;
            Toast.makeText(this,"Abra a Calculadora pelo menu Apps",Toast.LENGTH_LONG).show();return;
        }
        if("contacts".equals(type)){
            Intent i=new Intent(Intent.ACTION_VIEW,ContactsContract.Contacts.CONTENT_URI);
            launch(i,"Contatos");return;
        }
    }
    private boolean launchKnown(String... packages){
        PackageManager pm=getPackageManager();
        for(String pkg:packages)try{
            Intent i=pm.getLaunchIntentForPackage(pkg);
            if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);return true;}
        }catch(Exception ignored){}
        return false;
    }
    private void launch(Intent intent,String name){
        try{startActivity(intent);}
        catch(Exception e){Toast.makeText(this,"Não foi possível abrir "+name+". Veja o menu Apps.",Toast.LENGTH_LONG).show();}
    }
    private void titleRow(String title){
        LinearLayout bar=row();
        bar.setPadding(dp(8),0,dp(8),0);
        bar.setBackgroundDrawable(gradient(0xeeedf1f6,0xeeb6bdc8,0));
        TextView back=label("‹ Início",12,0xff214e75,true);
        back.setBackgroundDrawable(gradient(0xfffafafa,0xffdce4eb,7));
        bar.addView(back,params(67,30));
        back.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){screen=SCREEN_HOME;build();}
        });
        TextView titleText=label(title,16,0xff252c39,true);
        bar.addView(titleText,new LinearLayout.LayoutParams(0,dp(42),1));
        TextView space=label("",10,WHITE,false);bar.addView(space,params(67,30));
        root.addView(bar,params(-1,42));
    }
    private void loadApplications(){
        installed.clear();filtered.clear();
        try{
            Intent intent=new Intent(Intent.ACTION_MAIN);intent.addCategory(Intent.CATEGORY_LAUNCHER);
            ArrayList<ResolveInfo> found=new ArrayList<ResolveInfo>(getPackageManager().queryIntentActivities(intent,0));
            HashSet<String> seen=new HashSet<String>();
            for(ResolveInfo info:found){
                if(info.activityInfo==null)continue;
                String key=info.activityInfo.packageName+"/"+info.activityInfo.name;
                if(!seen.contains(key)&&!info.activityInfo.packageName.equals(getPackageName())){
                    seen.add(key);installed.add(info);
                }
            }
            Collections.sort(installed,new Comparator<ResolveInfo>(){
                public int compare(ResolveInfo a,ResolveInfo b){
                    String aa=String.valueOf(a.loadLabel(getPackageManager()));
                    String bb=String.valueOf(b.loadLabel(getPackageManager()));
                    return aa.compareToIgnoreCase(bb);
                }
            });
            filtered.addAll(installed);
        }catch(Exception e){Toast.makeText(this,"Não foi possível carregar os aplicativos",Toast.LENGTH_SHORT).show();}
    }
    private void filterApplications(String query){
        if(appAdapter==null)return;
        String lower=query.toLowerCase(Locale.getDefault()).trim();
        filtered.clear();
        for(ResolveInfo info:installed){
            CharSequence title=info.loadLabel(getPackageManager());
            String name=title==null?"":title.toString().toLowerCase(Locale.getDefault());
            if(name.contains(lower)||info.activityInfo.packageName.toLowerCase(Locale.getDefault()).contains(lower))
                filtered.add(info);
        }
        appAdapter.notifyDataSetChanged();
    }
    private void showApps(){
        titleRow("Aplicativos");
        LinearLayout searchBar=row();searchBar.setPadding(dp(9),dp(5),dp(9),dp(5));
        searchBar.setBackgroundColor(0xfff0f1f3);
        appSearch=new EditText(this);appSearch.setTextSize(13);appSearch.setSingleLine(true);
        appSearch.setHint("Buscar aplicativos");
        appSearch.setBackgroundDrawable(gradient(WHITE,0xffe4e8eb,7));
        appSearch.setPadding(dp(9),0,dp(9),0);
        searchBar.addView(appSearch,params(-1,32));root.addView(searchBar,params(-1,43));
        appsList=new ListView(this);appsList.setBackgroundColor(0xfff5f5f6);
        appsList.setCacheColorHint(0xfff5f5f6);appsList.setDividerHeight(dp(1));
        appAdapter=new AppsAdapter();appsList.setAdapter(appAdapter);
        root.addView(appsList,new LinearLayout.LayoutParams(-1,0,1));
        appSearch.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){filterApplications(s.toString());}
            public void afterTextChanged(Editable e){}
        });
        appsList.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener(){
            public void onItemClick(android.widget.AdapterView<?> parent,View view,int position,long id){
                if(position<0||position>=filtered.size())return;
                ResolveInfo info=filtered.get(position);
                Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setClassName(info.activityInfo.packageName,info.activityInfo.name);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launch(i,String.valueOf(info.loadLabel(getPackageManager())));
            }
        });
    }
    private final class AppsAdapter extends BaseAdapter{
        public int getCount(){return filtered.size();}
        public Object getItem(int p){return filtered.get(p);}
        public long getItemId(int p){return p;}
        public View getView(int pos,View convert,ViewGroup parent){
            LinearLayout cell=row();cell.setPadding(dp(10),0,dp(10),0);
            cell.setBackgroundColor(0xfffafafa);
            ImageView icon=new ImageView(HomeActivity.this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ip=params(39,39);ip.rightMargin=dp(12);cell.addView(icon,ip);
            ResolveInfo info=filtered.get(pos);
            try{icon.setImageDrawable(info.loadIcon(getPackageManager()));}catch(Exception ignored){}
            TextView name=label(String.valueOf(info.loadLabel(getPackageManager())),14,0xff252525,true);
            name.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            cell.addView(name,new LinearLayout.LayoutParams(0,dp(54),1));
            TextView arrow=label("›",26,0xff969ca2,false);cell.addView(arrow,params(20,54));
            return cell;
        }
    }
    private void showSettings(){
        titleRow("Ajustes YPod");
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout content=column();content.setPadding(dp(11),dp(12),dp(11),dp(10));scroll.addView(content);
        TextView heading=label("APARÊNCIA",12,WHITE,true);heading.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        content.addView(heading,params(-1,25));
        String[] options={"Azul clássico","Preto minimalista","Roxo noturno"};
        for(int i=0;i<options.length;i++){
            final int select=i;
            TextView option=label((palette==i?"✓  ":"    ")+options[i],15,0xff222a36,palette==i);
            option.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            option.setPadding(dp(11),0,dp(5),0);
            option.setBackgroundDrawable(gradient(WHITE,0xffd8dee8,7));
            LinearLayout.LayoutParams lp=params(-1,44);lp.bottomMargin=dp(5);
            content.addView(option,lp);
            option.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){palette=select;getPreferences(0).edit().putInt("palette",select).commit();build();}
            });
        }
        TextView tools=label("SISTEMA",12,WHITE,true);tools.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        content.addView(tools,params(-1,35));
        settingButton(content,"Abrir configurações do Android",new View.OnClickListener(){
            public void onClick(View v){launch(new Intent(Settings.ACTION_SETTINGS),"Configurações");}
        });
        settingButton(content,"Ver todos os aplicativos",new View.OnClickListener(){
            public void onClick(View v){screen=SCREEN_APPS;loadApplications();build();}
        });
        settingButton(content,"Como voltar ao launcher Samsung",new View.OnClickListener(){
            public void onClick(View v){
                new AlertDialog.Builder(HomeActivity.this)
                    .setTitle("Voltar à tela original")
                    .setMessage("Abra Ajustes do Android → Aplicações → Gerenciar aplicações → YPod Home → Limpar padrões.\n\nDepois pressione o botão Home e selecione a tela inicial Samsung.")
                    .setPositiveButton("Abrir Ajustes",new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface d,int n){launch(new Intent(Settings.ACTION_APPLICATION_SETTINGS),"Aplicações");}
                    }).setNegativeButton("Fechar",null).show();
            }
        });
        TextView tip=label("YPod Home 1.0 • Para o Galaxy Y Duos\nAndroid 2.3.6 • Funciona offline\n\nNão modifica a ROM nem exige root.",12,0xffdde3ec,false);
        tip.setSingleLine(false);tip.setEllipsize(null);tip.setGravity(Gravity.LEFT|Gravity.TOP);
        LinearLayout.LayoutParams p=params(-1,-2);p.topMargin=dp(20);content.addView(tip,p);
    }
    private void settingButton(LinearLayout parent,String title,View.OnClickListener action){
        TextView button=label(title,13,0xff213349,true);
        button.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        button.setPadding(dp(9),0,dp(9),0);
        button.setBackgroundDrawable(gradient(0xfffafafa,0xffd0d7e2,7));
        LinearLayout.LayoutParams lp=params(-1,42);lp.bottomMargin=dp(5);parent.addView(button,lp);
        button.setOnClickListener(action);
    }
    @Override public void onBackPressed(){
        if(screen!=SCREEN_HOME){screen=SCREEN_HOME;build();}
        else new AlertDialog.Builder(this).setTitle("Sair do YPod Home?")
            .setMessage("Você pode voltar a ele pressionando o botão Home.")
            .setPositiveButton("Sair",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface d,int which){finish();}
            }).setNegativeButton("Cancelar",null).show();
    }
    @Override public boolean onKeyDown(int keyCode,KeyEvent event){
        if(keyCode==KeyEvent.KEYCODE_MENU){
            if(screen==SCREEN_APPS)screen=SCREEN_HOME;
            else{screen=SCREEN_APPS;loadApplications();}
            build();return true;
        }
        return super.onKeyDown(keyCode,event);
    }
    @Override protected void onDestroy(){clockHandler.removeCallbacks(refreshTime);super.onDestroy();}

    private final class Wallpaper extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Wallpaper(Context context){super(context);}
        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight();
            if(w<=0||h<=0)return;
            int top=palette==1?0xff14151a:palette==2?0xff3a235c:0xff173f65;
            int bottom=palette==1?0xff030305:palette==2?0xff08051b:0xff070d24;
            p.setShader(new LinearGradient(0,0,w,h,top,bottom,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);
            if(palette!=1){
                p.setShader(new RadialGradient(w*.75f,h*.28f,w*.7f,
                    palette==2?0x888652b9:0x8859aeda,0x00111133,Shader.TileMode.CLAMP));
                c.drawRect(0,0,w,h,p);p.setShader(null);
                p.setShader(new RadialGradient(w*.12f,h*.85f,w*.6f,
                    palette==2?0x88462a84:0x884d76af,0x00111133,Shader.TileMode.CLAMP));
                c.drawRect(0,0,w,h,p);p.setShader(null);
            }
            p.setColor(0x18ffffff);p.setStrokeWidth(Math.max(1,w/240f));
            for(int i=0;i<13;i++){
                float y=h*(.12f+i*.069f);
                c.drawLine(0,y,w,y-24,p);
            }
            p.setShader(new LinearGradient(0,0,0,h,0x00ffffff,0x28000000,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);
        }
    }
    private final class ShortcutIcon extends View {
        private final String type;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
        ShortcutIcon(Context context,String kind){super(context);type=kind;}
        private void stroke(int color,float width){
            line.setColor(color);line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(width);
            line.setStrokeJoin(Paint.Join.ROUND);line.setStrokeCap(Paint.Cap.ROUND);
        }
        private void fill(int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);}
        private void round(Canvas c,float x,float y,float xx,float yy,float rad){
            c.drawRoundRect(new RectF(x,y,xx,yy),rad,rad,p);
        }
        private void word(Canvas c,String s,float size,float y){
            fill(WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(size);
            c.drawText(s,28,y,p);p.setTextAlign(Paint.Align.LEFT);
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            int w=getWidth(),h=getHeight();if(w==0||h==0)return;
            canvas.save();canvas.scale(w/56f,h/56f);
            int[] colors=iconColors(type);
            p.setShadowLayer(2.6f,0,2,0x77000000);
            p.setShader(new LinearGradient(0,0,0,56,colors[0],colors[1],Shader.TileMode.CLAMP));
            round(canvas,2,1,54,53,10);p.clearShadowLayer();p.setShader(null);
            fill(0x80ffffff);canvas.drawRoundRect(new RectF(3,2,53,52),10,10,p);
            p.setShader(new LinearGradient(0,2,0,24,0xaaffffff,0x00ffffff,Shader.TileMode.CLAMP));
            canvas.save();canvas.clipRect(3,2,53,24);round(canvas,4,3,52,50,10);canvas.restore();
            p.setShader(null);
            fill(0x44ffffff);canvas.drawRoundRect(new RectF(3,3,53,53),10,10,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0x77ffffff);
            canvas.drawRoundRect(new RectF(3,2,53,52),10,10,p);p.setStyle(Paint.Style.FILL);
            pictogram(canvas);
            canvas.restore();
        }
        private int[] iconColors(String s){
            if(s.equals("music"))return new int[]{0xffffa856,0xffc95113};
            if(s.equals("photos")||s.equals("gallery"))return new int[]{0xff8dccea,0xff306da1};
            if(s.equals("camera"))return new int[]{0xff8c949b,0xff303841};
            if(s.equals("clock"))return new int[]{0xff535d70,0xff141a25};
            if(s.equals("settings"))return new int[]{0xffc7cbd5,0xff626c7a};
            if(s.equals("files"))return new int[]{0xffe5c75b,0xffa77e18};
            if(s.equals("web"))return new int[]{0xff81cef2,0xff1a5fbe};
            if(s.equals("bluetooth"))return new int[]{0xff76bfff,0xff254da7};
            if(s.equals("calc"))return new int[]{0xff555b62,0xff1e252b};
            if(s.equals("contacts"))return new int[]{0xffd5b88b,0xff8b623c};
            if(s.equals("apps"))return new int[]{0xffa7afff,0xff5b47ae};
            if(s.equals("phone"))return new int[]{0xff80e885,0xff168c42};
            if(s.equals("messages"))return new int[]{0xff9af79d,0xff30a355};
            return new int[]{0xff88aacc,0xff345678};
        }
        private void pictogram(Canvas c){
            if(type.equals("music")){
                fill(WHITE);c.drawOval(new RectF(11,34,23,43),p);c.drawOval(new RectF(30,29,42,38),p);
                stroke(WHITE,4);c.drawLine(21,37,21,14,line);c.drawLine(40,32,40,9,line);
                c.drawLine(21,14,40,9,line);stroke(WHITE,5);c.drawLine(22,19,40,14,line);
            }else if(type.equals("photos")){
                int[] petals={0xffe56274,0xffffba3b,0xff82c95a,0xff4ec4c6,0xff689ceb,0xffaf79db};
                for(int i=0;i<6;i++){double a=Math.PI*i/3;
                    fill(petals[i]);c.drawCircle(28+(float)Math.cos(a)*11,26+(float)Math.sin(a)*11,9,p);
                }
                fill(0xffffe17b);c.drawCircle(28,26,7,p);
            }else if(type.equals("camera")){
                fill(0xeeffffff);round(c,10,19,46,40,5);
                round(c,15,14,27,22,2);fill(0xff2c3b49);c.drawCircle(29,29,10,p);
                fill(0xffb4c9d6);c.drawCircle(29,29,6,p);
                fill(WHITE);c.drawCircle(41,23,2,p);
            }else if(type.equals("clock")){
                fill(WHITE);c.drawCircle(28,27,19,p);fill(0xffe8e9ec);c.drawCircle(28,27,16,p);
                stroke(0xff353943,2);
                c.drawLine(28,27,28,16,line);c.drawLine(28,27,37,31,line);
                fill(0xff343943);c.drawCircle(28,27,2,p);
                for(int i=0;i<12;i++){double a=Math.PI*i/6;float xx=28+(float)Math.sin(a)*14,yy=27-(float)Math.cos(a)*14;c.drawCircle(xx,yy,0.8f,p);}
            }else if(type.equals("settings")){
                fill(0xffeceef2);c.drawCircle(28,27,17,p);fill(0xff7d8690);c.drawCircle(28,27,11,p);
                fill(0xfff8f8ff);c.drawCircle(28,27,7,p);
                stroke(WHITE,4);for(int i=0;i<8;i++){double a=Math.PI*i/4;
                    c.drawLine(28+(float)Math.sin(a)*15,27+(float)Math.cos(a)*15,
                        28+(float)Math.sin(a)*21,27+(float)Math.cos(a)*21,line);}
            }else if(type.equals("files")){
                fill(0xfff2d66b);round(c,9,19,47,42,4);
                fill(0xfff9e4a8);round(c,10,14,28,23,2);
                fill(0xffffedb2);round(c,9,23,47,43,4);
            }else if(type.equals("web")){
                fill(WHITE);c.drawCircle(28,27,19,p);
                stroke(0xff3183bb,2);c.drawCircle(28,27,17,line);
                c.drawOval(new RectF(20,10,36,44),line);
                c.drawLine(11,27,45,27,line);c.drawLine(15,18,41,18,line);
                c.drawLine(15,36,41,36,line);
            }else if(type.equals("bluetooth")){
                stroke(WHITE,4);c.drawLine(27,9,27,44,line);
                c.drawLine(27,9,40,20,line);c.drawLine(40,20,17,36,line);
                c.drawLine(17,18,40,36,line);c.drawLine(40,36,27,44,line);
            }else if(type.equals("gallery")){
                fill(WHITE);round(c,10,14,46,42,3);fill(0xff9ed8f3);round(c,12,16,44,40,2);
                fill(0xffffdd76);c.drawCircle(36,23,5,p);
                fill(0xff6dae70);Path mount=new Path();mount.moveTo(12,40);mount.lineTo(24,25);
                mount.lineTo(31,34);mount.lineTo(36,29);mount.lineTo(44,40);mount.close();c.drawPath(mount,p);
            }else if(type.equals("calc")){
                fill(0xffdfe4ea);round(c,13,8,43,46,5);
                fill(0xff485c62);round(c,16,11,40,20,1);
                fill(0xff87939b);for(int r=0;r<3;r++)for(int col=0;col<3;col++)round(c,17+col*8,23+r*7,23+col*8,28+r*7,1);
            }else if(type.equals("contacts")){
                fill(0xfff8ebda);round(c,12,9,44,44,3);
                fill(0xffb48363);c.drawCircle(28,23,9,p);
                fill(0xffbc8864);c.drawOval(new RectF(16,31,40,43),p);
            }else if(type.equals("apps")){
                fill(WHITE);for(int r=0;r<2;r++)for(int col=0;col<2;col++)
                    round(c,12+col*18,11+r*18,26+col*18,25+r*18,3);
            }else if(type.equals("phone")){
                word(c,"☎",37,40);
            }else if(type.equals("messages")){
                fill(WHITE);round(c,9,13,47,38,11);
                Path tail=new Path();tail.moveTo(20,35);tail.lineTo(15,44);tail.lineTo(30,37);
                tail.close();c.drawPath(tail,p);
                fill(0xff60bf75);for(int i=0;i<3;i++)c.drawCircle(18+i*10,26,2,p);
            }else word(c,"★",31,39);
        }
    }
}

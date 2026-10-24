package com.pocketbeat.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;

/**
 * YPod Music: iPod touch-era music interface for Android 2.3 (API 10).
 * All UI elements are native Android Views; no web, Google services or AndroidX.
 */
public class ClassicActivity extends Activity {
    private static final int INK=0xff242424, SUB=0xff777777, BLUE=0xff2374ad,
        LINE=0xffdedede, WHITE=0xffffffff, ORANGE=0xffdd6c24;
    private static final int SONGS=0, ARTISTS=1, ALBUMS=2, LISTS=3, DETAIL=4, PLAYER=5, SETTINGS=6;
    private int screen=SONGS, detailParent=SONGS, playerBack=SONGS, current=-1, queuePosition=-1;
    private int repeat=0; // 0 = off, 1 = all, 2 = one
    private boolean shuffle=false, touchingSeek=false;
    private final ArrayList<Song> songs=new ArrayList<Song>();
    private final ArrayList<Integer> queue=new ArrayList<Integer>();
    private final Random random=new Random();
    private MediaPlayer media;
    private LinearLayout body, shell;
    private TextView miniTitle, miniButton, mainPlay, timeAt, timeLeft, shuffleButton, repeatButton;
    private SeekBar progress;
    private ImageView coverImage;
    private final Handler handler=new Handler();
    private String detailName="", detailType="";
    private Bitmap cachedCover;
    private long cachedAlbum=-1;

    private static class Song {
        String title, artist, album, path;
        long albumId;
        Song(String t,String a,String b,String p,long id) {
            title=t;artist=a;album=b;path=p;albumId=id;
        }
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable gradient(int a,int b,int radius) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{a,b});
        d.setCornerRadius(dp(radius));return d;
    }
    private GradientDrawable bordered(int a,int b,int radius,int stroke) {
        GradientDrawable d=gradient(a,b,radius);d.setStroke(dp(1),stroke);return d;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);
        t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);return t;
    }
    private LinearLayout.LayoutParams lp(int width,int height){return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));}
    private void padding(View v,int h,int vert){v.setPadding(dp(h),dp(vert),dp(h),dp(vert));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        shuffle=getPreferences(0).getBoolean("shuffle",false);
        repeat=getPreferences(0).getInt("repeat",0);
        scan();render();handler.post(ticker);
    }
    private void scan(){
        songs.clear();Cursor c=null;
        try {
            String[] cols={MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.ALBUM_ID};
            c=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,cols,
                MediaStore.Audio.Media.IS_MUSIC+" != 0",null,MediaStore.Audio.Media.TITLE+" COLLATE NOCASE ASC");
            if(c!=null)while(c.moveToNext()){
                String path=c.getString(3);
                if(path!=null&&path.length()>0)
                    songs.add(new Song(safe(c.getString(0),"Sem título"),safe(c.getString(1),"Artista desconhecido"),
                        safe(c.getString(2),"Álbum desconhecido"),path,c.getLong(4)));
            }
        }catch(Exception e){Toast.makeText(this,"Não foi possível ler a biblioteca",Toast.LENGTH_LONG).show();}
        finally{if(c!=null)c.close();}
    }
    private String safe(String s,String fallback){
        return s==null||s.trim().length()==0||s.equals("<unknown>")?fallback:s;
    }
    private void render(){
        miniTitle=null;miniButton=null;mainPlay=null;timeAt=null;timeLeft=null;progress=null;coverImage=null;
        shuffleButton=null;repeatButton=null;
        shell=column();shell.setBackgroundColor(0xfff8f8f8);setContentView(shell);
        statusBar();
        if(screen==PLAYER){topBar("Reproduzindo", "Biblioteca", null, new View.OnClickListener(){
            public void onClick(View v){screen=playerBack==PLAYER?SONGS:playerBack;render();}
        });}
        else if(screen==DETAIL){topBar(detailName,"Voltar",null,new View.OnClickListener(){
            public void onClick(View v){screen=detailParent;render();}
        });}
        else if(screen==SETTINGS){topBar("Ajustes","Voltar",null,new View.OnClickListener(){
            public void onClick(View v){screen=SONGS;render();}
        });}
        else {
            String[] names={"Músicas","Artistas","Álbuns","Playlists"};
            String action=screen==SONGS?"Ajustes":screen==LISTS?"+":null;
            topBar(names[screen],null,action,null);
        }
        body=column();body.setBackgroundColor(WHITE);
        shell.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        if(screen==SONGS)showSongs();
        else if(screen==ARTISTS)showGroups(true);
        else if(screen==ALBUMS)showGroups(false);
        else if(screen==LISTS)showLists();
        else if(screen==DETAIL)showDetail();
        else if(screen==PLAYER)showPlayer();
        else showSettings();
        if(screen!=PLAYER&&screen!=SETTINGS){miniPlayer();tabBar();}
    }
    private void statusBar(){
        LinearLayout bar=row();bar.setBackgroundDrawable(gradient(0xfffafafa,0xffe6e6e6,0));
        TextView name=text("YPod  •  MUSIC",10,INK,true);padding(name,9,0);
        bar.addView(name,new LinearLayout.LayoutParams(0,dp(18),1));
        String clock=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date());
        TextView time=text(clock,10,INK,true);time.setGravity(Gravity.CENTER);bar.addView(time,lp(56,18));
        TextView off=text("OFFLINE",9,0xff555555,false);off.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        padding(off,8,0);bar.addView(off,new LinearLayout.LayoutParams(0,dp(18),1));
        shell.addView(bar,lp(-1,18));
    }
    private TextView barButton(String value){
        TextView t=text(value,12,WHITE,true);t.setGravity(Gravity.CENTER);
        t.setBackgroundDrawable(bordered(0xff8dc7f4,0xff1c6eab,5,0xff3979a6));
        return t;
    }
    private void topBar(String title,String back,String action,View.OnClickListener backAction){
        LinearLayout bar=row();bar.setBackgroundDrawable(bordered(0xfffafafa,0xffc3c7ce,0,0xffa1a5ab));
        LinearLayout left=row();left.setPadding(dp(5),0,0,0);
        if(back!=null){TextView b=barButton("‹ "+back);left.addView(b,lp(70,29));b.setOnClickListener(backAction);}
        bar.addView(left,new LinearLayout.LayoutParams(0,dp(42),1));
        TextView t=text(title,16,INK,true);t.setGravity(Gravity.CENTER);
        t.setShadowLayer(1,0,1,WHITE);bar.addView(t,new LinearLayout.LayoutParams(0,dp(42),2));
        LinearLayout right=row();right.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        right.setPadding(0,0,dp(5),0);
        if(action!=null){
            TextView b=barButton(action);right.addView(b,lp(action.equals("+")?33:57,29));
            b.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){if(screen==LISTS)newPlaylist();else{screen=SETTINGS;render();}}
            });
        }
        bar.addView(right,new LinearLayout.LayoutParams(0,dp(42),1));shell.addView(bar,lp(-1,42));
    }
    private void tabBar(){
        LinearLayout nav=row();nav.setBackgroundDrawable(gradient(0xff55565a,0xff111115,0));
        final String[] marks={"♫","●","▣","☷"};
        final String[] captions={"Músicas","Artistas","Álbuns","Listas"};
        for(int i=0;i<4;i++){
            final int which=i;
            LinearLayout item=column();item.setGravity(Gravity.CENTER);
            if(screen==i)item.setBackgroundDrawable(gradient(0xff6a6d73,0xff282a2f,4));
            TextView symbol=text(marks[i],23,screen==i?0xff88d8ff:0xffc9cbd0,true);
            symbol.setGravity(Gravity.CENTER);item.addView(symbol,lp(-1,25));
            TextView caption=text(captions[i],10,screen==i?WHITE:0xffb3b3b9,screen==i);
            caption.setGravity(Gravity.CENTER);item.addView(caption,lp(-1,17));
            nav.addView(item,new LinearLayout.LayoutParams(0,dp(48),1));
            item.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){screen=which;render();}
            });
        }
        shell.addView(nav,lp(-1,48));
    }
    private void miniPlayer(){
        if(current<0||current>=songs.size())return;
        LinearLayout mini=row();mini.setBackgroundDrawable(bordered(0xfff8f8f8,0xffd9dce1,0,0xffb0b6bd));
        TextView icon=text("♫",23,WHITE,true);icon.setGravity(Gravity.CENTER);
        icon.setBackgroundDrawable(gradient(0xffffc987,0xffc35721,4));
        LinearLayout.LayoutParams ip=lp(33,33);ip.leftMargin=dp(7);ip.rightMargin=dp(7);mini.addView(icon,ip);
        miniTitle=text(songs.get(current).title,12,INK,true);mini.addView(miniTitle,new LinearLayout.LayoutParams(0,dp(41),1));
        miniButton=text(media!=null&&media.isPlaying()?"Ⅱ":"▶",22,BLUE,true);
        miniButton.setGravity(Gravity.CENTER);mini.addView(miniButton,lp(46,41));
        miniButton.setOnClickListener(new View.OnClickListener(){public void onClick(View v){toggle();}});
        View.OnClickListener open=new View.OnClickListener(){public void onClick(View v){
            playerBack=screen;screen=PLAYER;render();
        }};
        icon.setOnClickListener(open);miniTitle.setOnClickListener(open);
        shell.addView(mini,lp(-1,43));
    }
    private void sectionNote(String value){
        TextView t=text(value,11,0xff888888,false);t.setBackgroundColor(0xfff1f1f3);
        padding(t,12,5);body.addView(t,lp(-1,26));
    }
    private ListView list(){
        ListView l=new ListView(this);l.setBackgroundColor(WHITE);l.setCacheColorHint(WHITE);
        l.setDivider(new ColorDrawable(LINE));l.setDividerHeight(dp(1));
        l.setSelector(new ColorDrawable(0xffdceefa));l.setFastScrollEnabled(true);
        body.addView(l,new LinearLayout.LayoutParams(-1,0,1));return l;
    }
    private View cell(String symbol,String name,String sub,int color){
        LinearLayout outer=row();outer.setBackgroundColor(WHITE);
        TextView icon=text(symbol,22,WHITE,true);icon.setGravity(Gravity.CENTER);
        icon.setBackgroundDrawable(gradient(light(color),color,5));
        LinearLayout.LayoutParams iconp=lp(38,38);iconp.leftMargin=dp(10);iconp.rightMargin=dp(10);
        outer.addView(icon,iconp);
        LinearLayout content=column();content.setGravity(Gravity.CENTER_VERTICAL);
        TextView top=text(name,14,INK,true);content.addView(top,lp(-1,21));
        if(sub!=null&&sub.length()>0){TextView bottom=text(sub,11,SUB,false);content.addView(bottom,lp(-1,17));}
        outer.addView(content,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView arrow=text("›",24,0xffb0b0b6,false);arrow.setGravity(Gravity.CENTER);outer.addView(arrow,lp(23,54));
        return outer;
    }
    private int light(int color){
        int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
        return Color.rgb(Math.min(255,r+48),Math.min(255,g+48),Math.min(255,b+48));
    }
    private void showSongs(){
        final ArrayList<Integer> visible=new ArrayList<Integer>();
        for(int i=0;i<songs.size();i++)visible.add(i);
        EditText find=new EditText(this);find.setTextSize(13);find.setSingleLine(true);
        find.setHint("⌕  Buscar músicas");find.setBackgroundDrawable(bordered(0xffeeeeef,0xfffafafa,8,0xffd3d3d6));
        LinearLayout searchbox=row();padding(searchbox,9,6);searchbox.addView(find,lp(-1,31));
        body.addView(searchbox,lp(-1,43));
        sectionNote(songs.size()+" MÚSICAS NO DISPOSITIVO");
        if(songs.size()==0){emptyState("Nenhuma música encontrada","Copie músicas MP3 para o cartão SD e toque em Atualizar nos Ajustes.");return;}
        final ListView list=list();
        final BaseAdapter adapter=new BaseAdapter(){
            public int getCount(){return visible.size();}
            public Object getItem(int p){return songs.get(visible.get(p));}
            public long getItemId(int p){return visible.get(p);}
            public View getView(int p,View old,ViewGroup parent){
                Song s=songs.get(visible.get(p));return cell("♫",s.title,s.artist,ORANGE);
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> a,View v,int pos,long id){
                ArrayList<Integer> selected=new ArrayList<Integer>(visible);
                playSelection(selected,pos);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            public boolean onItemLongClick(AdapterView<?> a,View v,int pos,long id){
                choosePlaylist(visible.get(pos));return true;
            }
        });
        find.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                String term=s.toString().toLowerCase(Locale.getDefault());
                visible.clear();
                for(int i=0;i<songs.size();i++){
                    Song song=songs.get(i);
                    if(song.title.toLowerCase(Locale.getDefault()).contains(term)||
                        song.artist.toLowerCase(Locale.getDefault()).contains(term)||
                        song.album.toLowerCase(Locale.getDefault()).contains(term))visible.add(i);
                }
                adapter.notifyDataSetChanged();
            }
            public void afterTextChanged(Editable e){}
        });
    }
    private void emptyState(String title,String info){
        LinearLayout empty=column();empty.setGravity(Gravity.CENTER);padding(empty,18,20);
        TextView mark=text("♫",52,0xffcacaca,true);mark.setGravity(Gravity.CENTER);empty.addView(mark,lp(-1,70));
        TextView h=text(title,16,INK,true);h.setGravity(Gravity.CENTER);h.setSingleLine(false);empty.addView(h,lp(-1,44));
        TextView desc=text(info,12,SUB,false);desc.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        desc.setSingleLine(false);desc.setEllipsize(null);empty.addView(desc,lp(-1,-2));
        body.addView(empty,new LinearLayout.LayoutParams(-1,0,1));
    }
    private TreeMap<String,ArrayList<Integer>> groups(boolean artists){
        TreeMap<String,ArrayList<Integer>> groups=new TreeMap<String,ArrayList<Integer>>(String.CASE_INSENSITIVE_ORDER);
        for(int i=0;i<songs.size();i++){
            Song s=songs.get(i);String key=artists?s.artist:s.album;
            ArrayList<Integer> ids=groups.get(key);
            if(ids==null){ids=new ArrayList<Integer>();groups.put(key,ids);}
            ids.add(i);
        }
        return groups;
    }
    private void showGroups(final boolean artists){
        final TreeMap<String,ArrayList<Integer>> buckets=groups(artists);
        final ArrayList<String> keys=new ArrayList<String>(buckets.keySet());
        sectionNote(keys.size()+(artists?" ARTISTAS":" ÁLBUNS"));
        if(keys.isEmpty()){emptyState("Sua biblioteca está vazia","Adicione MP3 ao cartão SD e atualize a biblioteca.");return;}
        ListView l=list();l.setAdapter(new BaseAdapter(){
            public int getCount(){return keys.size();}
            public Object getItem(int p){return keys.get(p);}
            public long getItemId(int p){return p;}
            public View getView(int p,View old,ViewGroup parent){
                String key=keys.get(p);int count=buckets.get(key).size();
                return cell(artists?"●":"▣",key,count+(count==1?" música":" músicas"),artists?0xff497eb9:0xff7f66aa);
            }
        });
        l.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> a,View v,int pos,long id){
                detailParent=artists?ARTISTS:ALBUMS;detailType=artists?"artist":"album";
                detailName=keys.get(pos);screen=DETAIL;render();
            }
        });
    }
    private String[] playlistNames(){
        String raw=getPreferences(0).getString("names","Favoritas");
        if(raw.length()==0)raw="Favoritas";
        return raw.split("\n");
    }
    private String playlistData(String name){return getPreferences(0).getString("list_"+name,"");}
    private void newPlaylist(){
        final EditText edit=new EditText(this);edit.setSingleLine(true);edit.setHint("Nome da playlist");
        new AlertDialog.Builder(this).setTitle("Nova playlist").setView(edit).setPositiveButton("Criar",
            new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){
                String n=edit.getText().toString().trim().replace("\n"," ");
                if(n.length()==0)return;
                for(String p:playlistNames())if(p.equalsIgnoreCase(n)){
                    Toast.makeText(ClassicActivity.this,"Essa playlist já existe",Toast.LENGTH_SHORT).show();return;
                }
                getPreferences(0).edit().putString("names",getPreferences(0).getString("names","Favoritas")+"\n"+n).commit();
                screen=LISTS;render();
            }}).setNegativeButton("Cancelar",null).show();
    }
    private void choosePlaylist(final int index){
        final String[] names=playlistNames();
        new AlertDialog.Builder(this).setTitle("Adicionar à playlist").setItems(names,
            new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
                String path=songs.get(index).path;String data=playlistData(names[which]);
                String all="\n"+data+"\n";
                if(!all.contains("\n"+path+"\n")){
                    String value=data.length()==0?path:data+"\n"+path;
                    getPreferences(0).edit().putString("list_"+names[which],value).commit();
                    Toast.makeText(ClassicActivity.this,"Adicionada à "+names[which],Toast.LENGTH_SHORT).show();
                }else Toast.makeText(ClassicActivity.this,"Já está nesta playlist",Toast.LENGTH_SHORT).show();
            }}).setNegativeButton("Cancelar",null).show();
    }
    private void showLists(){
        final String[] names=playlistNames();sectionNote(names.length+" PLAYLISTS  •  SEGURE PARA EXCLUIR");
        ListView l=list();l.setAdapter(new BaseAdapter(){
            public int getCount(){return names.length;}
            public Object getItem(int p){return names[p];}
            public long getItemId(int p){return p;}
            public View getView(int p,View old,ViewGroup parent){
                String data=playlistData(names[p]);
                int count=data.length()==0?0:data.split("\n").length;
                return cell("♫",names[p],count+(count==1?" música":" músicas"),0xff4c9d8d);
            }
        });
        l.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> a,View v,int pos,long id){
                detailParent=LISTS;detailType="list";detailName=names[pos];screen=DETAIL;render();
            }
        });
        l.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            public boolean onItemLongClick(AdapterView<?> a,View v,int pos,long id){
                if(pos==0){Toast.makeText(ClassicActivity.this,"Favoritas não pode ser excluída",Toast.LENGTH_SHORT).show();return true;}
                final String victim=names[pos];
                new AlertDialog.Builder(ClassicActivity.this).setTitle("Excluir "+victim+"?")
                    .setMessage("Apenas a playlist será removida. Suas músicas continuarão no cartão SD.")
                    .setPositiveButton("Excluir",new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface d,int w){
                            String all="";for(String name:playlistNames())if(!name.equals(victim))
                                all+=(all.length()>0?"\n":"")+name;
                            getPreferences(0).edit().putString("names",all).remove("list_"+victim).commit();
                            render();
                        }
                    }).setNegativeButton("Cancelar",null).show();return true;
            }
        });
    }
    private ArrayList<Integer> detailSongs(){
        ArrayList<Integer> ids=new ArrayList<Integer>();
        if(detailType.equals("list")){
            String data=playlistData(detailName);
            for(String path:data.split("\n"))for(int i=0;i<songs.size();i++)
                if(songs.get(i).path.equals(path)){ids.add(i);break;}
        }else for(int i=0;i<songs.size();i++){
            Song s=songs.get(i);
            if((detailType.equals("artist")?s.artist:s.album).equals(detailName))ids.add(i);
        }
        return ids;
    }
    private void showDetail(){
        final ArrayList<Integer> ids=detailSongs();
        sectionNote(ids.size()+(ids.size()==1?" MÚSICA":" MÚSICAS"));
        if(ids.isEmpty()){emptyState("Nenhuma música aqui","Adicione músicas à playlist pela biblioteca (segure uma música).");return;}
        ListView l=list();l.setAdapter(new BaseAdapter(){
            public int getCount(){return ids.size();}
            public Object getItem(int p){return songs.get(ids.get(p));}
            public long getItemId(int p){return ids.get(p);}
            public View getView(int p,View old,ViewGroup parent){
                Song s=songs.get(ids.get(p));return cell("♫",s.title,s.artist,ORANGE);
            }
        });
        l.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> a,View v,int pos,long id){playSelection(ids,pos);}
        });
        l.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            public boolean onItemLongClick(AdapterView<?> a,View v,final int pos,long id){
                if(!detailType.equals("list")){choosePlaylist(ids.get(pos));return true;}
                new AlertDialog.Builder(ClassicActivity.this).setTitle("Remover da playlist?")
                    .setPositiveButton("Remover",new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface d,int w){
                            String selected=songs.get(ids.get(pos)).path,data=playlistData(detailName),updated="";
                            for(String path:data.split("\n"))if(path.length()>0&&!path.equals(selected))
                                updated+=(updated.length()>0?"\n":"")+path;
                            getPreferences(0).edit().putString("list_"+detailName,updated).commit();render();
                        }
                    }).setNegativeButton("Cancelar",null).show();return true;
            }
        });
    }
    private void playSelection(ArrayList<Integer> selected,int position){
        if(selected.isEmpty())return;
        queue.clear();queue.addAll(selected);queuePosition=position;startSong(queue.get(queuePosition));
        playerBack=screen;screen=PLAYER;render();
    }
    private void startSong(int id){
        if(id<0||id>=songs.size())return;
        current=id;
        if(media!=null){try{media.reset();media.release();}catch(Exception e){}media=null;}
        try{
            media=new MediaPlayer();media.setAudioStreamType(AudioManager.STREAM_MUSIC);
            media.setDataSource(songs.get(current).path);
            media.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                public void onCompletion(MediaPlayer p){next(1,false);}
            });
            media.prepare();media.start();
        }catch(Exception e){
            if(media!=null){try{media.release();}catch(Exception ignored){}media=null;}
            Toast.makeText(this,"Não foi possível tocar este arquivo",Toast.LENGTH_LONG).show();
        }
        updateButtons();
    }
    private void toggle(){
        if(media==null){if(!songs.isEmpty()){
            ArrayList<Integer> all=new ArrayList<Integer>();for(int i=0;i<songs.size();i++)all.add(i);
            queue.clear();queue.addAll(all);queuePosition=current<0?0:current;startSong(queue.get(queuePosition));
        }return;}
        try{if(media.isPlaying())media.pause();else media.start();}catch(Exception e){}
        updateButtons();
    }
    private void next(int direction,boolean manual){
        if(queue.isEmpty()){if(songs.isEmpty())return;
            for(int i=0;i<songs.size();i++)queue.add(i);queuePosition=Math.max(0,current);
        }
        if(!manual&&repeat==2&&media!=null){
            try{media.seekTo(0);media.start();}catch(Exception e){}return;
        }
        if(shuffle&&direction>0&&queue.size()>1){
            int next=random.nextInt(queue.size());if(next==queuePosition)next=(next+1)%queue.size();
            queuePosition=next;
        }else{
            int next=queuePosition+direction;
            if(next<0||next>=queue.size()){
                if(!manual&&repeat==0){if(media!=null)try{media.pause();media.seekTo(0);}catch(Exception e){}updateButtons();return;}
                next=(next+queue.size())%queue.size();
            }
            queuePosition=next;
        }
        startSong(queue.get(queuePosition));
        if(screen==PLAYER)render();else updateButtons();
    }
    private Bitmap albumCover(long albumId){
        if(albumId==cachedAlbum)return cachedCover;
        cachedAlbum=albumId;cachedCover=null;
        InputStream in=null;
        try{
            Uri uri=Uri.parse("content://media/external/audio/albumart/"+albumId);
            in=getContentResolver().openInputStream(uri);
            if(in!=null){BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=4;
                opts.inPreferredConfig=Bitmap.Config.RGB_565;
                cachedCover=BitmapFactory.decodeStream(in,null,opts);
            }
        }catch(Exception e){}catch(OutOfMemoryError e){cachedCover=null;}
        finally{if(in!=null)try{in.close();}catch(Exception ignored){}}
        return cachedCover;
    }
    private TextView action(String symbol,int size){
        TextView t=text(symbol,size,BLUE,true);t.setGravity(Gravity.CENTER);return t;
    }
    private void showPlayer(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body.addView(scroll,lp(-1,-1));
        LinearLayout page=column();page.setGravity(Gravity.CENTER_HORIZONTAL);padding(page,12,7);
        scroll.addView(page);
        int heightDp=(int)(getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density);
        int coverSize=Math.max(90,Math.min(134,heightDp-175));
        coverImage=new ImageView(this);coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coverImage.setBackgroundDrawable(bordered(0xfff5f5f5,0xffc5c6cb,6,0xffa5a5a5));
        LinearLayout.LayoutParams art=lp(coverSize,coverSize);art.gravity=Gravity.CENTER_HORIZONTAL;
        art.bottomMargin=dp(8);page.addView(coverImage,art);
        TextView placeholder=text("♫",66,0xffcacaca,true);placeholder.setGravity(Gravity.CENTER);
        // ImageView receives either embedded album artwork or generated placeholder artwork.
        if(current>=0&&current<songs.size()){
            Bitmap bitmap=albumCover(songs.get(current).albumId);
            if(bitmap!=null)coverImage.setImageBitmap(bitmap);
            else{
                android.graphics.Bitmap picture=Bitmap.createBitmap(128,128,Bitmap.Config.RGB_565);
                android.graphics.Canvas canvas=new android.graphics.Canvas(picture);
                canvas.drawColor(0xffe58b40);
                android.graphics.Paint paint=new android.graphics.Paint(3);
                paint.setColor(0xffffd99e);paint.setTextAlign(android.graphics.Paint.Align.CENTER);
                paint.setTextSize(82);canvas.drawText("♫",64,89,paint);
                coverImage.setImageBitmap(picture);
            }
        }
        Song playing=current>=0&&current<songs.size()?songs.get(current):null;
        TextView songName=text(playing==null?"Nenhuma música":playing.title,16,INK,true);
        songName.setGravity(Gravity.CENTER);page.addView(songName,lp(-1,24));
        TextView artistName=text(playing==null?"Selecione uma música":playing.artist,12,SUB,false);
        artistName.setGravity(Gravity.CENTER);page.addView(artistName,lp(-1,21));
        progress=new SeekBar(this);progress.setMax(1000);page.addView(progress,lp(-1,31));
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar s){touchingSeek=true;}
            public void onStopTrackingTouch(SeekBar s){
                if(media!=null)try{media.seekTo((int)((long)media.getDuration()*s.getProgress()/1000));}
                    catch(Exception e){}
                touchingSeek=false;
            }
            public void onProgressChanged(SeekBar s,int value,boolean user){}
        });
        LinearLayout times=row();timeAt=text("0:00",11,SUB,false);timeLeft=text("-0:00",11,SUB,false);
        times.addView(timeAt,new LinearLayout.LayoutParams(0,dp(18),1));
        timeLeft.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);times.addView(timeLeft);
        page.addView(times,lp(-1,18));
        LinearLayout controls=row();controls.setGravity(Gravity.CENTER);
        TextView previous=action("◀◀",23),play=action("▶",32),forward=action("▶▶",23);
        mainPlay=play;play.setText(media!=null&&media.isPlaying()?"Ⅱ":"▶");
        controls.addView(previous,new LinearLayout.LayoutParams(0,dp(46),1));
        controls.addView(play,new LinearLayout.LayoutParams(0,dp(46),1));
        controls.addView(forward,new LinearLayout.LayoutParams(0,dp(46),1));
        page.addView(controls,lp(-1,46));
        previous.setOnClickListener(new View.OnClickListener(){public void onClick(View v){next(-1,true);}});
        play.setOnClickListener(new View.OnClickListener(){public void onClick(View v){toggle();}});
        forward.setOnClickListener(new View.OnClickListener(){public void onClick(View v){next(1,true);}});
        LinearLayout options=row();options.setGravity(Gravity.CENTER);
        shuffleButton=action(shuffle?"Misturar: ON":"Misturar: OFF",11);
        repeatButton=action(repeat==0?"Repetir: OFF":repeat==1?"Repetir: TUDO":"Repetir: 1",11);
        options.addView(shuffleButton,new LinearLayout.LayoutParams(0,dp(35),1));
        options.addView(repeatButton,new LinearLayout.LayoutParams(0,dp(35),1));
        page.addView(options,lp(-1,35));
        shuffleButton.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            shuffle=!shuffle;getPreferences(0).edit().putBoolean("shuffle",shuffle).commit();
            shuffleButton.setText(shuffle?"Misturar: ON":"Misturar: OFF");
        }});
        repeatButton.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            repeat=(repeat+1)%3;getPreferences(0).edit().putInt("repeat",repeat).commit();
            repeatButton.setText(repeat==0?"Repetir: OFF":repeat==1?"Repetir: TUDO":"Repetir: 1");
        }});
        if(playing!=null){
            TextView add=action("＋  Adicionar à playlist",12);page.addView(add,lp(-1,32));
            add.setOnClickListener(new View.OnClickListener(){public void onClick(View v){choosePlaylist(current);}});
        }
    }
    private void showSettings(){
        sectionNote("BIBLIOTECA E REPRODUÇÃO");
        settingsRow("Atualizar músicas", "Buscar novas faixas no cartão SD",new View.OnClickListener(){
            public void onClick(View v){scan();Toast.makeText(ClassicActivity.this,songs.size()+" músicas encontradas",Toast.LENGTH_SHORT).show();render();}
        });
        settingsRow("Sobre o YPod Music", "Versão 3.0 • clássico para Android 2.3",null);
        settingsRow("Sair do aplicativo", "Interromper reprodução e voltar ao Android",new View.OnClickListener(){
            public void onClick(View v){confirmExit();}
        });
        TextView hint=text("Dica: segure uma música para adicioná-la a uma playlist.\n\nO YPod reproduz músicas armazenadas no aparelho. Não requer internet.",12,SUB,false);
        hint.setSingleLine(false);hint.setEllipsize(null);padding(hint,14,18);
        body.addView(hint,lp(-1,-2));
    }
    private void settingsRow(String title,String detail,View.OnClickListener click){
        View v=cell("›",title,detail,0xff8e929a);
        body.addView(v,lp(-1,57));View line=new View(this);line.setBackgroundColor(LINE);body.addView(line,lp(-1,1));
        if(click!=null)v.setOnClickListener(click);
    }
    private void updateButtons(){
        boolean playing=media!=null&&media.isPlaying();
        if(mainPlay!=null)mainPlay.setText(playing?"Ⅱ":"▶");
        if(miniButton!=null)miniButton.setText(playing?"Ⅱ":"▶");
        if(miniTitle!=null&&current>=0&&current<songs.size())miniTitle.setText(songs.get(current).title);
    }
    private String duration(int ms){
        int sec=Math.max(0,ms/1000);return (sec/60)+":"+String.format(Locale.US,"%02d",sec%60);
    }
    private final Runnable ticker=new Runnable(){
        public void run(){
            if(media!=null&&progress!=null&&progress.getParent()!=null){
                try{
                    int total=media.getDuration(),where=media.getCurrentPosition();
                    if(total>0&&!touchingSeek)progress.setProgress((int)((long)where*1000/total));
                    if(timeAt!=null)timeAt.setText(duration(where));
                    if(timeLeft!=null)timeLeft.setText("-"+duration(total-where));
                }catch(Exception ignored){}
            }
            handler.postDelayed(this,600);
        }
    };
    private void confirmExit(){
        new AlertDialog.Builder(this).setTitle("Sair do YPod?")
            .setMessage("A reprodução será interrompida.").setPositiveButton("Sair",
                new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){finish();}})
            .setNegativeButton("Cancelar",null).show();
    }
    @Override public void onBackPressed(){
        if(screen==PLAYER){screen=playerBack==PLAYER?SONGS:playerBack;render();}
        else if(screen==DETAIL){screen=detailParent;render();}
        else if(screen==SETTINGS){screen=SONGS;render();}
        else if(screen!=SONGS){screen=SONGS;render();}
        else confirmExit();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(ticker);
        if(media!=null){try{media.release();}catch(Exception ignored){}media=null;}
        super.onDestroy();
    }
}

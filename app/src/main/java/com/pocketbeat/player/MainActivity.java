package com.pocketbeat.player;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
  private static final int BG=0xff0c1010, PANEL=0xff182523, ACCENT=0xff28d7b1, MUTED=0xffa1b9b3;
  private final ArrayList<Song> songs=new ArrayList<Song>();
  private MediaPlayer player;
  private int index=-1;
  private LinearLayout root, page, mini;
  private TextView nowTitle,nowArtist,timeStart,timeEnd,miniTitle,empty;
  private ImageView cover;
  private SeekBar seek;
  private Button playButton;
  private final Handler handler=new Handler();
  private boolean seeking=false;
  private final ArrayList<Integer> queue=new ArrayList<Integer>();
  private int queuePosition=-1;
  private final int GREEN=0xff1ed5a9;
  private int activeTab=0;
  private String activeTitle="Biblioteca";
  private int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
  private static class Song {String title,artist,album,path;long albumId;Song(String t,String a,String b,String p,long id){title=t;artist=a;album=b;path=p;albumId=id;}}
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setBackgroundDrawableResource(android.R.color.black);scan();showHome();handler.post(ticker);}
  private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
  private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  private void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
  private void setup(String title){
    activeTitle=title;
    root=vertical();root.setBackgroundColor(BG);
    getWindow().setFlags(1024,1024);
    setContentView(root);
    LinearLayout status=horizontal();pad(status,16,5);status.setBackgroundColor(0xff0d1516);
    TextView brand=text("YPod",15,ACCENT,true);status.addView(brand,new LinearLayout.LayoutParams(0,dp(30),1));
    TextView clock=text("●  OFFLINE",10,MUTED,true);status.addView(clock);root.addView(status);
    page=vertical();root.addView(page,new LinearLayout.LayoutParams(-1,0,1));
    mini=horizontal();mini.setBackgroundColor(PANEL);root.addView(mini);
    LinearLayout nav=horizontal();nav.setBackgroundColor(0xff101b1b);root.addView(nav,new LinearLayout.LayoutParams(-1,dp(55)));
    String[] labels={"⌂\\nInício","♫\\nMúsicas","▦\\nPlaylists","◉\\nTocando"};
    for(int i=0;i<labels.length;i++){final int n=i;TextView item=text(labels[i],11,(activeTab==i?ACCENT:MUTED),activeTab==i);item.setGravity(Gravity.CENTER);nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));item.setOnClickListener(new View.OnClickListener(){public void onClick(View v){activeTab=n;if(n==0)showHome();else if(n==1)showLibrary();else if(n==2)showPlaylists();else showPlayer();}});}
  }
  private TextView heading(String title,String subtitle){
    LinearLayout group=vertical();pad(group,18,10);
    TextView h=text(title,27,Color.WHITE,true);group.addView(h);
    TextView sub=text(subtitle,12,MUTED,false);group.addView(sub);
    page.addView(group);return h;
  }
  private TextView tile(String title,String subtitle,int color){
    LinearLayout block=vertical();pad(block,15,11);block.setBackgroundDrawable(bg(color,15));
    TextView h=text(title,17,Color.WHITE,true);block.addView(h);
    TextView sub=text(subtitle,11,0xffd0e4e0,false);block.addView(sub);
    return h;
  }
  private void showHome(){
    activeTab=0;setup("Início");
    ScrollView scroll=new ScrollView(this);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout content=vertical();pad(content,14,6);scroll.addView(content);
    TextView hero=text("Sua música.\\nSeu YPod.",29,Color.WHITE,true);pad(hero,5,15);content.addView(hero);
    TextView tag=text("A experiência de um iPod Touch, sem internet.",12,MUTED,false);pad(tag,5,5);content.addView(tag);
    LinearLayout heroCard=vertical();pad(heroCard,18,18);heroCard.setBackgroundDrawable(bg(0xff205348,19));
    TextView icon=text("♫",39,Color.WHITE,true);heroCard.addView(icon);
    TextView count=text(songs.size()+" músicas no seu bolso",17,Color.WHITE,true);heroCard.addView(count);
    TextView hint=text("Toque para explorar sua biblioteca",12,0xffd0f3e8,false);heroCard.addView(hint);
    LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(145));hp.topMargin=dp(14);content.addView(heroCard,hp);
    heroCard.setOnClickListener(new View.OnClickListener(){public void onClick(View v){activeTab=1;showLibrary();}});
    TextView recent=text("Explorar",19,Color.WHITE,true);pad(recent,5,19);content.addView(recent);
    String[] choices={"▣  Minhas playlists","◉  Álbuns","♬  Artistas","↻  Atualizar músicas"};
    for(int i=0;i<choices.length;i++){final int action=i;TextView row=text(choices[i]+"      ›",15,Color.WHITE,true);pad(row,16,15);row.setBackgroundDrawable(bg(i%2==0?PANEL:0xff16201f,13));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(57));rp.bottomMargin=dp(8);content.addView(row,rp);row.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(action==0){activeTab=2;showPlaylists();}else if(action==1)showCategory(2);else if(action==2)showCategory(1);else{scan();showHome();}}});}
    drawMini();
  }
  private void scan(){songs.clear();Cursor c=null;try{c=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.ALBUM_ID},MediaStore.Audio.Media.IS_MUSIC+" != 0",null,MediaStore.Audio.Media.TITLE+" COLLATE NOCASE ASC");if(c!=null)while(c.moveToNext()){String p=c.getString(3);if(p!=null)songs.add(new Song(c.getString(0),c.getString(1),c.getString(2),p,c.getLong(4)));}}catch(Exception ignored){}finally{if(c!=null)c.close();}}
  private String clean(String s){return s==null||s.length()==0||s.equals("<unknown>")?"Artista desconhecido":s;}
  private void showLibrary(){activeTab=1;setup("BIBLIOTECA");TextView header=text("Músicas",26,Color.WHITE,true);pad(header,18,14);page.addView(header);TextView sub=text(songs.size()+" músicas disponíveis offline",12,MUTED,false);pad(sub,18,4);page.addView(sub);LinearLayout tabs=horizontal();pad(tabs,12,8);page.addView(tabs);String[] names={"MÚSICAS","ARTISTAS","ÁLBUNS","PLAYLISTS"};for(int i=0;i<4;i++){final int mode=i;TextView t=text(names[i],11,ACCENT,true);t.setGravity(Gravity.CENTER);tabs.addView(t,new LinearLayout.LayoutParams(0,dp(40),1));t.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(mode==3)showPlaylists();else showCategory(mode);}});}ListView list=new ListView(this);list.setDividerHeight(0);list.setCacheColorHint(Color.TRANSPARENT);ArrayList<String> labels=new ArrayList<String>();for(Song s:songs)labels.add("♫   "+s.title+"\n       "+clean(s.artist));list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@Override public View getView(int p,View old,android.view.ViewGroup parent){TextView v=(TextView)super.getView(p,old,parent);v.setTextColor(Color.WHITE);v.setTextSize(14);v.setPadding(dp(18),dp(12),dp(12),dp(12));v.setBackgroundColor(p%2==0?BG:0xff191622);return v;}});page.addView(list,new LinearLayout.LayoutParams(-1,0,1));list.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> p,View v,int pos,long id){queue.clear();queuePosition=-1;start(pos);showPlayer();}});
    list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){public boolean onItemLongClick(AdapterView<?> p,View v,int pos,long id){choosePlaylist(pos);return true;}});if(songs.size()==0){TextView hint=text("Nenhuma música encontrada.\nCopie arquivos MP3 para o cartão SD e atualize.",14,MUTED,false);pad(hint,18,20);page.addView(hint);}Button refresh=button("↻  ATUALIZAR BIBLIOTECA");page.addView(refresh);refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){scan();showLibrary();}});drawMini();}
  private Button button(String label){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setBackgroundDrawable(bg(PANEL,12));return b;}
  private void showCategory(final int mode){setup(mode==1?"ARTISTAS":"ÁLBUNS");final ArrayList<String> keys=new ArrayList<String>();for(Song s:songs){String key=mode==1?clean(s.artist):clean(s.album);if(!keys.contains(key))keys.add(key);}ListView list=new ListView(this);ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,keys){@Override public View getView(int p,View old,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,old,parent);t.setTextColor(Color.WHITE);t.setPadding(dp(20),dp(16),dp(10),dp(16));return t;}};list.setAdapter(adapter);page.addView(list,new LinearLayout.LayoutParams(-1,0,1));list.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> a,View v,int pos,long id){showFiltered(mode,keys.get(pos));}});Button back=button("←  TODAS AS MÚSICAS");page.addView(back);back.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showLibrary();}});drawMini();}
  private void showFiltered(final int mode,String key){setup(key.toUpperCase(Locale.getDefault()));final ArrayList<Integer> indexes=new ArrayList<Integer>();ArrayList<String> labels=new ArrayList<String>();for(int i=0;i<songs.size();i++){Song s=songs.get(i);if((mode==1?clean(s.artist):clean(s.album)).equals(key)){indexes.add(i);labels.add("♫   "+s.title);}}ListView list=new ListView(this);list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@Override public View getView(int p,View old,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,old,parent);t.setTextColor(Color.WHITE);t.setPadding(dp(18),dp(14),dp(12),dp(14));return t;}});page.addView(list,new LinearLayout.LayoutParams(-1,0,1));list.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> a,View v,int pos,long id){queue.clear();queuePosition=-1;start(indexes.get(pos));showPlayer();}});Button back=button("←  BIBLIOTECA");page.addView(back);back.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showLibrary();}});drawMini();}
  private String playlistNames(){return getPreferences(0).getString("names","Favoritas");}
  private String[] names(){return playlistNames().split("\\n");}
  private String playlistKey(String name){return "playlist_"+name;}
  private boolean containsSong(String playlist,String path){String data=getPreferences(0).getString(playlistKey(playlist),"");return ("\\n"+data+"\\n").contains("\\n"+path+"\\n");}
  private void addSong(String name,int position){String path=songs.get(position).path;String data=getPreferences(0).getString(playlistKey(name),"");if(!containsSong(name,path)){getPreferences(0).edit().putString(playlistKey(name),data.length()==0?path:data+"\\n"+path).commit();Toast.makeText(this,"Adicionado a "+name,Toast.LENGTH_SHORT).show();}else Toast.makeText(this,"Música já está nesta playlist",Toast.LENGTH_SHORT).show();}
  private void choosePlaylist(final int song){final String[] all=names();new android.app.AlertDialog.Builder(this).setTitle("Adicionar à playlist").setItems(all,new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int which){addSong(all[which],song);}}).setNegativeButton("Cancelar",null).show();}
  private void newPlaylist(){final EditText field=new EditText(this);field.setSingleLine(true);field.setHint("Nome da playlist");new android.app.AlertDialog.Builder(this).setTitle("Nova playlist").setView(field).setPositiveButton("Criar",new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int w){String name=field.getText().toString().trim().replace("\\n"," ");if(name.length()==0)return;for(String old:names())if(old.equals(name)){Toast.makeText(MainActivity.this,"Essa playlist já existe",Toast.LENGTH_SHORT).show();return;}getPreferences(0).edit().putString("names",playlistNames()+"\\n"+name).commit();showPlaylists();}}).setNegativeButton("Cancelar",null).show();}
  private void showPlaylists(){activeTab=2;setup("SUAS PLAYLISTS");TextView h=text("Suas playlists",26,Color.WHITE,true);pad(h,18,16);page.addView(h);final String[] all=names();ListView list=new ListView(this);list.setCacheColorHint(Color.TRANSPARENT);ArrayList<String> labels=new ArrayList<String>();for(String n:all){String data=getPreferences(0).getString(playlistKey(n),"");int count=data.length()==0?0:data.split("\\n").length;labels.add("▣   "+n+"    ·    "+count+" músicas");}list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@Override public View getView(int p,View old,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,old,parent);t.setTextColor(Color.WHITE);t.setPadding(dp(18),dp(18),dp(12),dp(18));return t;}});page.addView(list,new LinearLayout.LayoutParams(-1,0,1));list.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> a,View v,int pos,long id){showPlaylist(all[pos]);}});Button create=button("＋  CRIAR PLAYLIST");page.addView(create);create.setOnClickListener(new View.OnClickListener(){public void onClick(View v){newPlaylist();}});Button back=button("←  BIBLIOTECA");page.addView(back);back.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showLibrary();}});drawMini();}
  private void showPlaylist(final String name){setup(name.toUpperCase(Locale.getDefault()));final ArrayList<Integer> ids=new ArrayList<Integer>();String data=getPreferences(0).getString(playlistKey(name),"");for(String p:data.split("\\n"))for(int i=0;i<songs.size();i++)if(songs.get(i).path.equals(p)){ids.add(i);break;}ArrayList<String> labels=new ArrayList<String>();for(Integer i:ids)labels.add("♫   "+songs.get(i).title+"\\n      "+clean(songs.get(i).artist));ListView list=new ListView(this);list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@Override public View getView(int p,View old,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,old,parent);t.setTextColor(Color.WHITE);t.setPadding(dp(16),dp(14),dp(12),dp(14));return t;}});page.addView(list,new LinearLayout.LayoutParams(-1,0,1));list.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> a,View v,int pos,long id){queue.clear();queue.addAll(ids);queuePosition=pos;start(ids.get(pos));showPlayer();}});list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){public boolean onItemLongClick(AdapterView<?> a,View v,int pos,long id){String path=songs.get(ids.get(pos)).path;String data=getPreferences(0).getString(playlistKey(name),"");ArrayList<String> kept=new ArrayList<String>();for(String p:data.split("\\n"))if(!p.equals(path)&&p.length()>0)kept.add(p);String result="";for(String p:kept)result+=(result.length()>0?"\\n":"")+p;getPreferences(0).edit().putString(playlistKey(name),result).commit();showPlaylist(name);return true;}});TextView hint=text("Toque para reproduzir · Segure para remover",11,MUTED,false);pad(hint,15,10);page.addView(hint);Button back=button("←  PLAYLISTS");page.addView(back);back.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showPlaylists();}});drawMini();}
  private void showPlayer(){activeTab=3;setup("TOCANDO AGORA");ScrollView scroll=new ScrollView(this);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout body=vertical();pad(body,22,12);scroll.addView(body);cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackgroundDrawable(bg(PANEL,18));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(188),dp(188));cp.gravity=Gravity.CENTER_HORIZONTAL;cp.topMargin=dp(12);cp.bottomMargin=dp(18);body.addView(cover,cp);nowTitle=text("",20,Color.WHITE,true);nowTitle.setGravity(Gravity.CENTER);body.addView(nowTitle);nowArtist=text("",13,MUTED,false);nowArtist.setGravity(Gravity.CENTER);body.addView(nowArtist);seek=new SeekBar(this);seek.setMax(1000);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(44));sp.topMargin=dp(12);body.addView(seek,sp);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){if(player!=null)try{player.seekTo(player.getDuration()*s.getProgress()/1000);}catch(Exception ignored){}seeking=false;}public void onProgressChanged(SeekBar s,int p,boolean fromUser){}});LinearLayout times=horizontal();timeStart=text("0:00",11,MUTED,false);timeEnd=text("0:00",11,MUTED,false);times.addView(timeStart,new LinearLayout.LayoutParams(0,-2,1));times.addView(timeEnd);body.addView(times);LinearLayout controls=horizontal();controls.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ctrl=new LinearLayout.LayoutParams(-1,dp(66));ctrl.topMargin=dp(12);body.addView(controls,ctrl);Button prev=button("◀◀");playButton=button("Ⅱ");Button next=button("▶▶");controls.addView(prev,new LinearLayout.LayoutParams(0,dp(60),1));controls.addView(playButton,new LinearLayout.LayoutParams(0,dp(60),1));controls.addView(next,new LinearLayout.LayoutParams(0,dp(60),1));prev.setOnClickListener(new View.OnClickListener(){public void onClick(View v){skip(-1);updatePlayer();}});next.setOnClickListener(new View.OnClickListener(){public void onClick(View v){skip(1);updatePlayer();}});playButton.setOnClickListener(new View.OnClickListener(){public void onClick(View v){toggle();updatePlayer();}});Button back=button("←  VOLTAR À BIBLIOTECA");body.addView(back);back.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showLibrary();}});updatePlayer();}
  private void updatePlayer(){if(index<0||index>=songs.size()||nowTitle==null)return;Song s=songs.get(index);nowTitle.setText(s.title);nowArtist.setText(clean(s.artist)+"  •  "+clean(s.album));if(playButton!=null)playButton.setText(player!=null&&player.isPlaying()?"Ⅱ":"▶");if(cover!=null){Bitmap bmp=null;try{Uri u=Uri.parse("content://media/external/audio/albumart/"+s.albumId);java.io.InputStream in=getContentResolver().openInputStream(u);if(in!=null){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;bmp=BitmapFactory.decodeStream(in,null,o);in.close();}}catch(Exception ignored){}if(bmp!=null)cover.setImageBitmap(bmp);else{cover.setImageDrawable(null);cover.setBackgroundDrawable(bg(PANEL,18));}}}
  private void drawMini(){mini.removeAllViews();if(index<0||index>=songs.size())return;miniTitle=text("♫  "+songs.get(index).title,12,Color.WHITE,true);pad(miniTitle,12,10);mini.addView(miniTitle,new LinearLayout.LayoutParams(0,dp(54),1));miniTitle.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showPlayer();}});Button toggle=button(player!=null&&player.isPlaying()?"Ⅱ":"▶");mini.addView(toggle,new LinearLayout.LayoutParams(dp(60),dp(54)));toggle.setOnClickListener(new View.OnClickListener(){public void onClick(View v){toggle();drawMini();}});}
  private void skip(int direction){if(queue.size()>0){queuePosition=(queuePosition+direction+queue.size())%queue.size();start(queue.get(queuePosition));}else start(index+direction);}
  private void start(int pos){if(songs.size()==0)return;index=(pos+songs.size())%songs.size();if(player!=null){player.release();player=null;}try{player=new MediaPlayer();player.setDataSource(songs.get(index).path);player.prepare();player.start();player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){public void onCompletion(MediaPlayer mp){skip(1);if(nowTitle!=null&&nowTitle.getParent()!=null)updatePlayer();else drawMini();}});}catch(Exception e){Toast.makeText(this,"Não foi possível tocar esta faixa",Toast.LENGTH_SHORT).show();}}
  private void toggle(){if(player==null){if(index<0)start(0);else start(index);return;}try{if(player.isPlaying())player.pause();else player.start();}catch(Exception ignored){}}
  private String stamp(int ms){int s=Math.max(0,ms/1000);return (s/60)+":"+String.format(Locale.US,"%02d",s%60);}
  private final Runnable ticker=new Runnable(){public void run(){if(player!=null&&seek!=null&&seek.getParent()!=null){try{int d=player.getDuration(),p=player.getCurrentPosition();if(d>0&&!seeking)seek.setProgress(p*1000/d);if(timeStart!=null)timeStart.setText(stamp(p));if(timeEnd!=null)timeEnd.setText(stamp(d));}catch(Exception ignored){}}handler.postDelayed(this,700);}};
  @Override protected void onDestroy(){handler.removeCallbacks(ticker);if(player!=null){player.release();player=null;}super.onDestroy();}
  @Override public void onBackPressed(){activeTab=0;showHome();}
}

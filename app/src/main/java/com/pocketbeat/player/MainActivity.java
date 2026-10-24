package com.pocketbeat.player;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
  private final ArrayList<String> titles = new ArrayList<String>();
  private final ArrayList<String> paths = new ArrayList<String>();
  private MediaPlayer player;
  private int index = 0;
  private TextView now;
  private int purple = Color.rgb(171, 105, 255);
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(12, 12, 12, 8);
    root.setBackgroundColor(Color.rgb(15,13,22));
    TextView heading = label("YPod  ♪", 26, purple);
    root.addView(heading);
    now = label("Escolha uma música", 15, Color.WHITE);
    root.addView(now);
    LinearLayout controls = new LinearLayout(this);
    Button prev = button("◀◀"); Button play = button("▶ / ❚❚"); Button next = button("▶▶");
    controls.addView(prev, new LinearLayout.LayoutParams(0, 54, 1));
    controls.addView(play, new LinearLayout.LayoutParams(0, 54, 1));
    controls.addView(next, new LinearLayout.LayoutParams(0, 54, 1));
    root.addView(controls);
    Button refresh = button("Atualizar músicas");
    root.addView(refresh);
    ListView list = new ListView(this);
    list.setCacheColorHint(Color.TRANSPARENT);
    root.addView(list, new LinearLayout.LayoutParams(-1,0,1));
    setContentView(root);
    prev.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startAt(index-1);}});
    next.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startAt(index+1);}});
    play.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(player==null){startAt(index);}else if(player.isPlaying()){player.pause();}else{player.start();}}});
    refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){scan(list);}});
    list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener(){public void onItemClick(android.widget.AdapterView<?> parent,View v,int pos,long id){startAt(pos);}});
    scan(list);
  }
  private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(3,8,3,8);return t;}
  private Button button(String s){Button b=new Button(this);b.setText(s);return b;}
  private void scan(ListView list){
    titles.clear();paths.clear();
    Cursor c=null;
    try{
      c=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.DATA},MediaStore.Audio.Media.IS_MUSIC+" != 0",null,MediaStore.Audio.Media.TITLE+" ASC");
      if(c!=null){while(c.moveToNext()){titles.add(c.getString(0));paths.add(c.getString(1));}}
    }catch(Exception e){now.setText("Não foi possível ler a biblioteca.");}
    finally{if(c!=null)c.close();}
    list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,titles));
    if(titles.size()==0)now.setText("Copie MP3 para o microSD e toque Atualizar.");
  }
  private void startAt(int next){
    if(paths.size()==0)return;
    index=(next+paths.size())%paths.size();
    if(player!=null){player.release();player=null;}
    try{
      player=new MediaPlayer();
      player.setDataSource(paths.get(index));
      player.prepare();player.start();
      now.setText(titles.get(index));
      player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){public void onCompletion(MediaPlayer mp){startAt(index+1);}});
    }catch(Exception e){now.setText("Erro ao reproduzir esta faixa.");}
  }
  @Override public void onDestroy(){if(player!=null){player.release();player=null;}super.onDestroy();}
}

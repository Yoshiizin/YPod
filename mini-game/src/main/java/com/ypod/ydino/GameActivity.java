package com.ypod.ydino;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Y-Dino: tiny 2D runner for Android 2.3.6 (API 10), 240x320.
 * Everything is Canvas vector/pixel art: no images, network or dependencies.
 */
public final class GameActivity extends Activity {
  private GameScreen game;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    game = new GameScreen(this);
    setContentView(game);
  }

  @Override protected void onResume() {
    super.onResume();
    game.resumeLoop();
  }

  @Override protected void onPause() {
    game.pauseLoop();
    super.onPause();
  }

  @Override protected void onDestroy() {
    game.dispose();
    super.onDestroy();
  }

  @Override public void onBackPressed() {
    if (!game.handleBack()) super.onBackPressed();
  }

  private final class GameScreen extends View {
    private static final int READY=0, RUNNING=1, PAUSED=2, FINISHED=3;
    private static final float W=240f, H=320f, GROUND=250f;
    private static final float DINO_X=45f, DINO_H=24f, DINO_W=25f;
    private static final int NAVY=0xff101c4b;
    private static final int BLUE=0xff2445ae;
    private static final int SKY=0xff192d73;
    private static final int CYAN=0xff80f1eb;
    private static final int LIGHT=0xffe9f6ff;
    private static final int PINK=0xffff7ea8;
    private static final int PURPLE=0xffc29bff;
    private static final int INK=0xff081b34;
    private final Paint p=new Paint();
    private final RectF rounded=new RectF();
    private final Handler handler=new Handler();
    private final SharedPreferences pref=getSharedPreferences("ydino_scores",MODE_PRIVATE);
    private final Runnable tick=new Runnable() {
      public void run() {
        if (!active) return;
        long now=SystemClock.uptimeMillis();
        float dt=Math.min(0.05f,Math.max(0f,(now-lastMs)/1000f));
        lastMs=now;
        update(dt);
        invalidate();
        handler.postDelayed(this,34);
      }
    };
    private boolean active=false;
    private long lastMs=0;
    private int state=READY;
    private int score=0,best=0,obstacleType=0;
    private boolean obstacleScored=false,sound=true;
    private float speed=112f,obstacleX=260f;
    private float dinoY=GROUND-DINO_H,dinoVy=0f;
    private float world=0f,clouds=0f;
    private ToneGenerator tones;

    GameScreen(Context ctx) {
      super(ctx);
      setFocusable(true);
      p.setAntiAlias(false); // crisp pixel art on old low-resolution displays
      best=pref.getInt("highscore",0);
      sound=pref.getBoolean("sound",true);
      try { tones=new ToneGenerator(AudioManager.STREAM_MUSIC,30); }
      catch (RuntimeException ignored) { tones=null; }
    }

    void resumeLoop() {
      if (state==RUNNING) state=PAUSED;
      if (active) return;
      active=true;
      lastMs=SystemClock.uptimeMillis();
      handler.postDelayed(tick,34);
      invalidate();
    }

    void pauseLoop() {
      if (state==RUNNING) state=PAUSED;
      active=false;
      handler.removeCallbacks(tick);
    }

    void dispose() {
      pauseLoop();
      if (tones!=null) { tones.release(); tones=null; }
    }

    boolean handleBack() {
      if (state==RUNNING) { state=PAUSED;invalidate();return true; }
      if (state==PAUSED || state==FINISHED) {
        state=READY;invalidate();return true;
      }
      return false;
    }

    void beep(int ms) {
      if (!sound || tones==null) return;
      try { tones.startTone(ToneGenerator.TONE_PROP_BEEP,ms); }
      catch (RuntimeException ignored) {}
    }

    void start() {
      score=0;
      speed=112f;
      dinoY=GROUND-DINO_H;
      dinoVy=0;
      obstacleX=265f;
      obstacleType=0;
      obstacleScored=false;
      world=0f;
      clouds=0f;
      lastMs=SystemClock.uptimeMillis();
      state=RUNNING;
      invalidate();
    }

    void jump() {
      if (state!=RUNNING) return;
      if (dinoY>=GROUND-DINO_H-1f) {
        dinoVy=-265f;
        dinoY-=0.8f;
        beep(45);
      }
    }

    float obstacleWidth() { return obstacleType==0 ? 20f : 27f; }
    float obstacleHeight() { return obstacleType==0 ? 21f : 25f; }

    void update(float dt) {
      clouds=(clouds+dt*11f)%260f;
      if (state!=RUNNING) return;
      world+=dt*speed;
      dinoVy+=680f*dt;
      dinoY+=dinoVy*dt;
      if (dinoY>GROUND-DINO_H) {
        dinoY=GROUND-DINO_H;
        dinoVy=0f;
      }
      obstacleX-=speed*dt;
      if (!obstacleScored && obstacleX+obstacleWidth()<DINO_X) {
        obstacleScored=true;
        score++;
        if (score>best) {
          best=score;
          pref.edit().putInt("highscore",best).apply();
        }
        speed=Math.min(187f,112f+score*4.0f);
      }
      if (obstacleX+obstacleWidth()<-8f) {
        obstacleX=240f+52f+(score%3)*19f;
        obstacleType=(score%2);
        obstacleScored=false;
      }
      // Slightly inset hitboxes to make touch gameplay more forgiving.
      float left=DINO_X+5f,right=DINO_X+DINO_W-3f;
      float top=dinoY+3f,bottom=dinoY+DINO_H-1f;
      float ol=obstacleX+3f,or=obstacleX+obstacleWidth()-3f;
      float ot=GROUND-obstacleHeight()+3f,ob=GROUND-2f;
      if (right>ol && left<or && bottom>ot && top<ob) {
        state=FINISHED;
        beep(180);
      }
    }

    void color(int argb) {p.setColor(argb);p.setStyle(Paint.Style.FILL);}
    void rect(Canvas c,float x,float y,float width,float height,int col) {
      color(col);c.drawRect(x,y,x+width,y+height,p);
    }
    void circle(Canvas c,float x,float y,float r,int col) {
      color(col);c.drawCircle(x,y,r,p);
    }
    void round(Canvas c,float x,float y,float width,float height,float radius,int col) {
      color(col);rounded.set(x,y,x+width,y+height);
      c.drawRoundRect(rounded,radius,radius,p);
    }
    void text(Canvas c,String s,float x,float baseline,float size,int col,boolean bold) {
      color(col);p.setAntiAlias(true);p.setTypeface(Typeface.create(Typeface.MONOSPACE,bold?Typeface.BOLD:Typeface.NORMAL));
      p.setTextSize(size);
      c.drawText(s,x,baseline,p);
      p.setAntiAlias(false);
    }
    void center(Canvas c,String s,float cx,float baseline,float size,int col,boolean bold) {
      color(col);p.setAntiAlias(true);p.setTypeface(Typeface.create(Typeface.MONOSPACE,bold?Typeface.BOLD:Typeface.NORMAL));
      p.setTextSize(size);
      c.drawText(s,cx-p.measureText(s)/2f,baseline,p);
      p.setAntiAlias(false);
    }
    String digits(int n) {
      if (n<10) return "00"+n;
      if (n<100) return "0"+n;
      return String.valueOf(n);
    }

    void scenery(Canvas c) {
      c.drawColor(NAVY);
      rect(c,0,44,W,GROUND-44,SKY);
      for (int i=0;i<19;i++) {
        int sx=7+(i*71)%228;
        int sy=60+(i*29)%111;
        rect(c,sx,sy,2,2,i%3==0?PURPLE:BLUE);
      }
      drawCloud(c,214f-(clouds*.9f)%290f,79f);
      drawCloud(c,92f-(clouds*.4f)%290f,142f);
      rect(c,0,GROUND,W,6,CYAN);
      rect(c,0,GROUND+6,W,64,0xff14265f);
      for (int i=0;i<16;i++) {
        float xx=(i*23f-(world*.55f)%23f);
        rect(c,xx,GROUND+17+(i%2)*14f,10,3,BLUE);
      }
      rect(c,0,GROUND-8,W,3,0xff3b70b3);
      text(c,"YPOD ARCADE // 2026",12,309,7,0xff668cce,false);
      text(c,"240x320 / OFFLINE",147,309,7,0xff668cce,false);
    }

    void drawCloud(Canvas c,float x,float y) {
      for (int wrap=0;wrap<2;wrap++) {
        float px=x+wrap*290f;
        rect(c,px,y+6,34,7,0xff3c55a0);
        rect(c,px+6,y,20,14,0xff3c55a0);
        rect(c,px+10,y+4,9,3,0xff8396d8);
      }
    }

    void dinosaur(Canvas c) {
      float x=DINO_X,y=dinoY;
      boolean step=(int)(world/13)%2==0 && state==RUNNING && dinoVy==0;
      // Pixel dinosaur: body, tail, head, snout, feet and pink cheek.
      rect(c,x+2,y+15,10,5,0xff32b9bc);
      rect(c,x+6,y+10,14,12,CYAN);
      rect(c,x+15,y+1,10,13,CYAN);
      rect(c,x+19,y+10,10,5,CYAN);
      rect(c,x+22,y+4,2,2,INK);
      rect(c,x+23,y+10,3,2,PINK);
      rect(c,x+9,y+7,3,5,0xff4bd4d5);
      rect(c,x+10,y+21,4,step?2:3,CYAN);
      rect(c,x+18,y+21,4,step?3:2,CYAN);
      rect(c,x+1,y+16,4,2,0xff138a9e);
    }

    void cassette(Canvas c) {
      float x=obstacleX,y=GROUND-obstacleHeight();
      if (obstacleType==0) {
        rect(c,x,y,20,21,INK);
        rect(c,x+2,y+1,16,18,PURPLE);
        rect(c,x+3,y+5,14,8,0xfff3eaff);
        rect(c,x+5,y+7,3,3,INK);
        rect(c,x+12,y+7,3,3,INK);
        rect(c,x+3,y+16,14,2,0xff75569f);
      } else {
        rect(c,x,y,27,25,INK);
        rect(c,x+2,y+2,23,21,PINK);
        rect(c,x+4,y+5,19,7,0xfff7dce8);
        rect(c,x+5,y+15,6,6,INK);
        rect(c,x+16,y+15,6,6,INK);
        rect(c,x+7,y+17,2,2,CYAN);
        rect(c,x+18,y+17,2,2,CYAN);
      }
    }

    void button(Canvas c,String label,float x,float y,float w) {
      round(c,x,y,w,26f,7f,BLUE);
      center(c,label,x+w/2f,y+17f,10f,LIGHT,true);
    }

    void hud(Canvas c) {
      text(c,"Y-DINO",12,23,14,CYAN,true);
      text(c,"SCORE "+digits(score),12,39,9,LIGHT,false);
      text(c,"BEST "+digits(best),95,39,9,PURPLE,false);
      button(c,sound?"SFX":"OFF",151,10,35);
      button(c,"II",190,10,38);
    }

    void veil(Canvas c) {
      color(0xbe08132f);
      c.drawRect(0,44,W,GROUND,p);
    }

    void card(Canvas c,String title,String subtitle,String action) {
      veil(c);
      round(c,18,98,204,115,10,INK);
      round(c,20,100,200,110,10,BLUE);
      rect(c,34,110,28,3,CYAN);
      rect(c,178,195,28,3,PINK);
      center(c,title,120,139,24,LIGHT,true);
      center(c,subtitle,120,160,9,CYAN,false);
      round(c,35,172,170,24,7,CYAN);
      center(c,action,120,188,10,INK,true);
    }

    @Override protected void onDraw(Canvas canvas) {
      canvas.save();
      canvas.scale(getWidth()/W,getHeight()/H);
      scenery(canvas);
      cassette(canvas);
      dinosaur(canvas);
      hud(canvas);
      if(state==READY) {
        card(canvas,"Y-DINO","O MINI GAME DO YPOD","TOQUE PARA JOGAR");
        center(canvas,"TOQUE PARA PULAR",120,238,8,LIGHT,false);
      } else if(state==PAUSED) {
        card(canvas,"PAUSADO","SEU RECORD: "+digits(best),"TOQUE PARA VOLTAR");
      } else if(state==FINISHED) {
        card(canvas,"GAME OVER","PONTOS "+digits(score)+" / BEST "+digits(best),"JOGAR NOVAMENTE");
      } else if(state==RUNNING) {
        center(canvas,"TOQUE PARA PULAR",120,292,8,LIGHT,false);
      }
      canvas.restore();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
      if (event.getAction()!=MotionEvent.ACTION_DOWN) return true;
      float x=event.getX()*W/Math.max(1f,getWidth());
      float y=event.getY()*H/Math.max(1f,getHeight());
      if (y<43 && x>=148 && x<188) {
        sound=!sound;
        pref.edit().putBoolean("sound",sound).apply();
        invalidate();
        return true;
      }
      if (y<43 && x>=188) {
        if (state==RUNNING) state=PAUSED;
        else if (state==PAUSED) {state=RUNNING;lastMs=SystemClock.uptimeMillis();}
        invalidate();
        return true;
      }
      if (state==READY || state==FINISHED) start();
      else if (state==PAUSED) {state=RUNNING;lastMs=SystemClock.uptimeMillis();}
      else jump();
      return true;
    }
  }
}

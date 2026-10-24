package com.pocketbeat.player;

import android.app.Service;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.RemoteViews;
import java.util.Random;

/**
 * Small background playback engine compatible with Android 2.3 / API 10.
 * Never holds an Activity reference. The notification keeps playback alive
 * when the user returns to the launcher or locks the screen.
 */
public final class PlaybackService extends Service {
    public static PlaybackService instance;
    public MediaPlayer media;
    public String[] paths = new String[0];
    public String currentPath;
    public int position = -1;
    public int generation = 0;
    public int repeat = 0;
    public boolean shuffle = false;
    private final Random random = new Random();
    private AudioManager audio;
    private boolean notificationShown = false;
    private final AudioManager.OnAudioFocusChangeListener focus =
        new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int change) {
                if(change==AudioManager.AUDIOFOCUS_LOSS ||
                   change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                   change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    pause();
                }
            }
        };
    private final BroadcastReceiver noisy = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) pause();
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        instance=this;
        audio=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent intent) { return null; }

    public void play(String[] newPaths,int newPosition) {
        if(newPaths==null||newPaths.length==0)return;
        paths=newPaths;
        position=Math.max(0,Math.min(newPosition,paths.length-1));
        load();
    }
    private void load() {
        if(position<0||position>=paths.length)return;
        if(media!=null) {
            try{media.reset();media.release();}catch(Exception ignored){}
            media=null;
        }
        currentPath=paths[position];
        try {
            if(audio!=null)audio.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            MediaPlayer next=new MediaPlayer();
            next.setAudioStreamType(AudioManager.STREAM_MUSIC);
            next.setWakeMode(getApplicationContext(),PowerManager.PARTIAL_WAKE_LOCK);
            next.setDataSource(currentPath);
            next.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                public void onCompletion(MediaPlayer player){advance(1,false);}
            });
            next.prepare();
            media=next;
            media.start();
            generation++;
            showNotification();
        }catch(Exception e) {
            if(media!=null){try{media.release();}catch(Exception ignored){}media=null;}
            generation++;
            hideNotification();
        }
    }
    public boolean isPlaying() {
        try{return media!=null && media.isPlaying();}catch(Exception e){return false;}
    }
    public void pause() {
        if(isPlaying())try{media.pause();}catch(Exception ignored){}
        showNotificationIfActive();
    }
    public void toggle() {
        if(media==null) { if(paths.length>0)load();return; }
        try{
            if(media.isPlaying())media.pause();
            else {
                if(audio!=null)audio.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
                media.start();
            }
        }catch(Exception ignored){}
        showNotificationIfActive();
    }
    public void advance(int direction,boolean manual) {
        if(paths.length==0)return;
        if(!manual&&repeat==2&&media!=null){
            try{media.seekTo(0);media.start();}catch(Exception ignored){}
            showNotificationIfActive();return;
        }
        if(shuffle && direction>0 && paths.length>1){
            int target=random.nextInt(paths.length-1);
            if(target>=position)target++;
            position=target;
        }else{
            int target=position+direction;
            if(target<0||target>=paths.length){
                if(!manual && repeat==0){
                    if(media!=null)try{media.pause();media.seekTo(0);}catch(Exception ignored){}
                    showNotificationIfActive();return;
                }
                target=(target+paths.length)%paths.length;
            }
            position=target;
        }
        load();
    }
    private void showNotificationIfActive() {
        if(media!=null)showNotification();else hideNotification();
    }
    private void showNotification() {
        Notification n=new Notification();
        n.icon=android.R.drawable.ic_media_play;
        n.when=System.currentTimeMillis();
        n.tickerText="YPod Midnight";
        n.flags |= Notification.FLAG_ONGOING_EVENT;
        Intent launch=new Intent(this,ClassicActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        n.contentIntent=PendingIntent.getActivity(this,0,launch,PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews views=new RemoteViews(getPackageName(),android.R.layout.simple_list_item_2);
        views.setTextViewText(android.R.id.text1,"YPod Midnight");
        views.setTextViewText(android.R.id.text2,isPlaying()?"♫  Reproduzindo no YPod":"Ⅱ  Música pausada");
        n.contentView=views;
        startForeground(73,n);
        notificationShown=true;
    }
    private void hideNotification() {
        if(notificationShown){stopForeground(true);notificationShown=false;}
    }
    public void stopPlayback() {
        hideNotification();
        stopSelf();
    }
    @Override public void onDestroy() {
        hideNotification();
        try{unregisterReceiver(noisy);}catch(Exception ignored){}
        if(media!=null){try{media.release();}catch(Exception ignored){}media=null;}
        if(audio!=null)audio.abandonAudioFocus(focus);
        if(instance==this)instance=null;
        super.onDestroy();
    }
}

package com.kyser.audiostreamdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.ListItem;
import com.spotify.protocol.types.ListItems;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.sql.SQLOutput;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.spotify.protocol.types.Repeat.OFF;
import static com.spotify.protocol.types.Repeat.ONE;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener, SlidingUpPanelLayout.PanelSlideListener {
    private static final String CLIENT_ID = "43205f60655840d49b53f30ac2226f77";
    private static final String REDIRECT_URI = "http://com.kyser.audiostreamdemo/callback";
    private static final int REQUEST_CODE = 1337;
    private static final String SCOPES = "user-read-recently-played,user-library-modify,user-read-email,user-read-private";
    private SpotifyAppRemote mSpotifyAppRemote;
    private SlidingUpPanelLayout mActivityLayout;
    private RecyclerView mAlbumList;
    private AlbumItemAdaptor mAlbumItemAdapter;
    private ListItems mListItems;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivityLayout = (SlidingUpPanelLayout)findViewById(R.id.activity_layout);
        setListeners();
        setRecyclerList();
        connectToRemote();
        mActivityLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mActivityLayout.addPanelSlideListener(this);
        mProgressBar = (ProgressBar)findViewById(R.id.player_track_progress);
    }

    private void setListeners() {
        findViewById(R.id.mini_btn_play).setOnClickListener(this);
        findViewById(R.id.mini_btn_next).setOnClickListener(this);
        findViewById(R.id.mini_btn_prev).setOnClickListener(this);
        findViewById(R.id.player_cntrls_play).setOnClickListener(this);
        findViewById(R.id.player_cntrls_next).setOnClickListener(this);
        findViewById(R.id.player_cntrls_prev).setOnClickListener(this);
        findViewById(R.id.player_cntrls_shfl).setOnClickListener(this);
        findViewById(R.id.player_cntrls_repeat).setOnClickListener(this);
        findViewById(R.id.player_minimize).setOnClickListener(this);
    }
    private void setRecyclerList(){
        mAlbumList = (RecyclerView) findViewById(R.id.item_list);
        mAlbumList.setLayoutManager(new GridLayoutManager(this,2));
        mAlbumList.addItemDecoration(new SpaceItemDecoration(30,10));
        mAlbumItemAdapter  = new AlbumItemAdaptor(this, new AlbumItemAdaptor.ItemSelection() {
            @Override
            public void onItemSelection(ListItem item, int position) {
                mSpotifyAppRemote.getContentApi().playContentItem( item);
                ((TextView) findViewById(R.id. playlist_name)).setText(item.title);
            }
        });
        mAlbumList.setAdapter(mAlbumItemAdapter);
    }
    private void setSpinner(ListItems listItems){
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.category_spinner);
        for(int i=0;i<listItems.total;i++)
            dataAdapter.add(listItems.items[i].title);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = findViewById(R.id.category_spinner);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void connectToRemote() {
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(this, connectionParams,  new Connector.ConnectionListener() {
                @Override
                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                    mSpotifyAppRemote = spotifyAppRemote;
                    Log.d("MainActivity", "Connected! Yay!");
                    // Now you can start interacting with App Remote
                    connected();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.e("MainActivity", throwable.getMessage(), throwable);
                    // Something went wrong when attempting to connect! Handle errors here
                }
        });
    }

    private void connected() {
        mActivityLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(playerState -> {
                togglePlayPause(playerState);
                updateTrackProgress(playerState);
                if(playerState.track==null) {
                    mActivityLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                }else {
                    setPlayerInfo(playerState.track);
                }

        });

       CallResult<ListItems> callresult=  mSpotifyAppRemote.getContentApi().getRecommendedContentItems("default" );
       callresult.setResultCallback(listItems -> {
           mListItems = listItems;
           setSpinner(listItems);
       });
    }

    private void setPlayerInfo(Track track) {
        ((TextView) findViewById(R.id.mini_track_title)).setText(new StringBuilder().append(track.artist.name).append(" - ").append(track.name).toString());
        ((TextView) findViewById(R.id.player_track)).setText(track.name);
        ((TextView) findViewById(R.id.player_album)).setText(track.artist.name);

        CallResult<Bitmap> bitmapResult =mSpotifyAppRemote.getImagesApi().getImage(track.imageUri, Image.Dimension.THUMBNAIL);
         bitmapResult.setResultCallback(bitmap -> ((ImageView) findViewById(R.id.mini_album_poster)).setImageBitmap(bitmap));
        CallResult<Bitmap> bitmapResult_big =mSpotifyAppRemote.getImagesApi().getImage(track.imageUri, Image.Dimension.LARGE);
        bitmapResult_big.setResultCallback(bitmap -> ((ImageView) findViewById(R.id.player_poster)).setImageBitmap(bitmap));
    }

    private void togglePlayPause(PlayerState playerState) {
        ((ImageButton)findViewById(R.id.mini_btn_play)).setImageDrawable(getDrawable(playerState.isPaused?R.drawable.play:R.drawable.pause));
        ((ImageButton)findViewById(R.id.player_cntrls_play)).setImageDrawable(getDrawable(playerState.isPaused?R.drawable.play_1:R.drawable.pause_1));
        ((ImageButton)findViewById(R.id.player_cntrls_shfl)).setImageDrawable(getDrawable(playerState.playbackOptions.isShuffling?R.drawable.shuffle_2:R.drawable.shuffle_off));
        ((ImageButton)findViewById(R.id.player_cntrls_repeat)).setImageDrawable(getDrawable(playerState.playbackOptions.repeatMode==OFF ?R.drawable.repeat_none:(playerState.playbackOptions.repeatMode==ONE ?R.drawable.repeat_one:R.drawable.repeat_all)));
    }

    private String getTimeFormMilli(long duration){
        return  String.format(Locale.US,"%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }
    boolean isProgressOn = false;
    Track mCurrentTrack;
    String mCurrentTrackTitle;
    int mProgressCount = 0;
    Runnable r = new Runnable() {
        @Override
        public void run() {
            mProgressCount+=1;
            long total_duration = (mCurrentTrack.duration/1000);
            mProgressBar.setProgress((int)(mProgressCount/(total_duration/100) ));
            ((TextView)findViewById(R.id.player_elapsed_duration)).setText(getTimeFormMilli(mProgressCount*1000));
            if(isProgressOn)
                updatePosition();
        }
    };
    private void updateTrackProgress(PlayerState playerState){
        if(!playerState.track.name.equals(mCurrentTrackTitle)){
            mProgressBar.setProgress(0);
            mProgressCount = 0;
            mCurrentTrack= playerState.track;
            mCurrentTrackTitle = mCurrentTrack.name ;
            isProgressOn=false;

            ((TextView)findViewById(R.id.player_total_duration)).setText(getTimeFormMilli(mCurrentTrack.duration));
        }

        if(!playerState.isPaused) {
            if(!isProgressOn) {
                isProgressOn = true;
                updatePosition();
            }
        }
        else isProgressOn =false;
    }
    private void updatePosition(){
        boolean h = new Handler().postDelayed( r, 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (mListItems.items[i].hasChildren) {
            CallResult<ListItems> child_callresult = mSpotifyAppRemote.getContentApi().getChildrenOfItem(mListItems.items[i] , 100,0);
            child_callresult.setResultCallback(childListItems -> mAlbumItemAdapter.setAlbumList(childListItems.items));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.mini_btn_play || view.getId() == R.id.player_cntrls_play){
            CallResult<PlayerState> playerState =  mSpotifyAppRemote.getPlayerApi().getPlayerState();
            playerState.setResultCallback(playerState1 -> {
               if(playerState1.isPaused){
                   mSpotifyAppRemote.getPlayerApi().resume();
               } else{
                   mSpotifyAppRemote.getPlayerApi().pause();
               }
            });
        }else if(view.getId() == R.id.mini_btn_prev || view.getId() == R.id.player_cntrls_prev){
            mSpotifyAppRemote.getPlayerApi().skipPrevious();
        }else if(view.getId() == R.id.mini_btn_next || view.getId() == R.id.player_cntrls_next){
            mSpotifyAppRemote.getPlayerApi().skipNext();
        }else if(view.getId() == R.id.player_minimize){
            mActivityLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }else if(view.getId() == R.id.player_cntrls_shfl){
            mSpotifyAppRemote.getPlayerApi().toggleShuffle();
        }
        else if(view.getId() == R.id.player_cntrls_repeat){
            mSpotifyAppRemote.getPlayerApi().toggleRepeat();
        }
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset) {}

    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
        if(newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            findViewById(R.id.mini_player).setVisibility(View.GONE);
            findViewById(R.id.player_window).setVisibility(View.VISIBLE);
        }
        if(newState == SlidingUpPanelLayout.PanelState.COLLAPSED){
            findViewById(R.id.mini_player).setVisibility(View.VISIBLE);
            findViewById(R.id.player_window).setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if(mActivityLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
            mActivityLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        else
            super.onBackPressed();
    }
}
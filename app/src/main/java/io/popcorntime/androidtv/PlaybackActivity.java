package io.popcorntime.androidtv;


import android.content.Context;
import android.graphics.Color;
import android.media.MediaFormat;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.CaptioningManager;
import android.widget.VideoView;

import pct.droid.base.providers.media.models.Movie;

import pct.droid.base.providers.subs.SubsProvider;
import pct.droid.base.torrent.StreamInfo;


import java.io.File;
import java.io.FileInputStream;

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
public class PlaybackActivity extends PopcornBaseActivity {

    public static final String AUTO_PLAY = "auto_play";
    private static final String TAG = PlaybackActivity.class.getSimpleName();
    private VideoView mVideoView;
    private PlaybackOverlayFragment mPlaybackOverlay;
    private LeanbackPlaybackState mPlaybackState = LeanbackPlaybackState.IDLE;
    private MediaSession mSession;
    private int mPosition = 0;
    private long mStartTimeMillis;
    private long mDuration = -1;
    private MediaPlayer mMediaPlayer;
    private Movie mMovie;
    private StreamInfo mStreamInfo;
    private Context mContext;
    private CaptioningManager mCaptioningManager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createMediaSession();

        setContentView(R.layout.playback_controls);
        mMovie = (Movie) getIntent().getExtras().getParcelable(MovieDetailsActivity.MOVIE);
        mStreamInfo = (StreamInfo) getIntent().getExtras().getParcelable(MovieDetailsActivity.STREAM_INFO);
        mContext = this;

        mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
        mCaptioningManager.addCaptioningChangeListener(new CaptioningManager.CaptioningChangeListener() {
            @Override
            public void onEnabledChanged(boolean enabled) {
                super.onEnabledChanged(enabled);
                // TODO: need a way to re-trigger subtitles when captioning is re-enabled.
//                if (enabled == true)
//                    loadSubs();
            }
        });

        loadViews();
        playPause(true);
        //Example for handling resizing view for overscan
        //Utils.overScan(this, mVideoView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayback();
        mVideoView.suspend();
        mVideoView.setVideoURI(null);
        mSession.release();
    }

    private void setPosition(int position) {
        if (position > mDuration) {
            mPosition = (int) mDuration;
        } else if (position < 0) {
            mPosition = 0;
            mStartTimeMillis = System.currentTimeMillis();
        } else {
            mPosition = position;
        }
        mStartTimeMillis = System.currentTimeMillis();
        Log.d(TAG, "position set to " + mPosition);
    }

    private void createMediaSession() {
        if (mSession == null) {
            mSession = new MediaSession(this, "LeanbackSampleApp");
            mSession.setCallback(new MediaSessionCallback());
            mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

            mSession.setActive(true);

            setMediaController(new MediaController(this, mSession.getSessionToken()));
        }
    }

    private void playPause(boolean doPlay) {
        if (mPlaybackState == LeanbackPlaybackState.IDLE) {
            setupCallbacks();
        }

        if (doPlay && mPlaybackState != LeanbackPlaybackState.PLAYING) {
            mPlaybackState = LeanbackPlaybackState.PLAYING;
            if (mPosition > 0) {
                mVideoView.seekTo(mPosition);
            }
            mVideoView.start();
            mStartTimeMillis = System.currentTimeMillis();
        } else {
            mPlaybackState = LeanbackPlaybackState.PAUSED;
            int timeElapsedSinceStart = (int)(System.currentTimeMillis() - mStartTimeMillis);
            setPosition(mPosition + timeElapsedSinceStart);
            mVideoView.pause();
        }
        updatePlaybackState();
    }


    private void updatePlaybackState() {
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions());
        int state = PlaybackState.STATE_PLAYING;
        if (mPlaybackState == LeanbackPlaybackState.PAUSED || mPlaybackState == LeanbackPlaybackState.IDLE) {
            state = PlaybackState.STATE_PAUSED;
        }
        stateBuilder.setState(state, mPosition, 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH;

        if (mPlaybackState == LeanbackPlaybackState.PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }

        return actions;
    }

    private void updateMetadata(final Movie movie) {
        final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        String title = movie.title.replace("_", " -");

        metadataBuilder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, movie.videoId);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title);
//        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
//                movie.getStudio());
//        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION,
//                movie.getDescription());
//        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
//                movie.getCardImageUrl());
        metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, mDuration);

        // And at minimum the title and artist for legacy support
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, title);
//        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, movie.getStudio());
//
//        Glide.with(this)
//                .load(movie.getImageURI())
//                .asBitmap()
//                .into(new SimpleTarget<Bitmap>(500, 500) {
//                    @Override
//                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
//                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
//                        mSession.setMetadata(metadataBuilder.build());
//                    }
//                });
    }
    private void loadSubs() {
        if (mStreamInfo != null && mStreamInfo.getSubtitleLanguage() != "no-subs") {
            String vttPath = SubsProvider.getStorageLocation(this) + "/" + mMovie.videoId + "-" + mStreamInfo.getSubtitleLanguage() + ".vtt";

            if (!(new File(vttPath).isFile()))
                Utils.srt2vtt(SubsProvider.getStorageLocation(this) + "/" + mMovie.videoId + "-" + mStreamInfo.getSubtitleLanguage() + ".srt");

            File mSubsFile = new File(SubsProvider.getStorageLocation(this), mMovie.videoId + "-" + mStreamInfo.getSubtitleLanguage() + ".vtt");
            try {
                FileInputStream is = new FileInputStream(mSubsFile);
                mVideoView.addSubtitleSource(is, MediaFormat.createSubtitleFormat(MediaFormat.MIMETYPE_TEXT_VTT, mStreamInfo.getSubtitleLanguage()));
            } catch (java.io.FileNotFoundException e) {
                Log.e(TAG, "Can't find selected subtitle file.");
            }


        }
    }
    private void loadViews() {
        mVideoView = (VideoView) findViewById(R.id.videoView);
        mVideoView.setFocusable(false);
        mVideoView.setFocusableInTouchMode(false);

        mPlaybackOverlay = (PlaybackOverlayFragment) getFragmentManager().findFragmentById(R.id.playback_controls_fragment);
        loadSubs();


        setVideoPath(mMovie.getUrl());
        updateMetadata(mMovie);

    }

    private void setupCallbacks() {

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mVideoView.stopPlayback();
                mPlaybackState = LeanbackPlaybackState.IDLE;
                return false;
            }
        });


        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mPlaybackState == LeanbackPlaybackState.PLAYING) {
                    mVideoView.start();
                }
            }
        });


        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlaybackState = LeanbackPlaybackState.IDLE;
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoView.isPlaying()) {
            if (!requestVisibleBehind(true)) {
                // Try to play behind launcher, but if it fails, stop playback.
                playPause(false);
            }
        } else {
            requestVisibleBehind(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        playPause(false);
    }



    @Override
    public void onVisibleBehindCanceled() {
        playPause(false);
        super.onVisibleBehindCanceled();
    }

    private void stopPlayback() {
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
    }

    @Override
    public boolean onSearchRequested() {
//        startActivity(new Intent(this, SearchActivity.class));
        return true;
    }

    /*
     * List of various states that we can be in
     */
    public enum LeanbackPlaybackState {
        PLAYING, PAUSED, IDLE
    }

    private class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            playPause(true);
        }
        @Override
        public void onPause() {
            playPause(false);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
//            Movie movie = VideoProvider.getMovieById(mediaId);
//            if (movie != null) {
//                setVideoPath(movie.getVideoUrl());
//                mPlaybackState = LeanbackPlaybackState.PAUSED;
//                updateMetadata(movie);
//                playPause(extras.getBoolean(AUTO_PLAY));
//            }
        }

        @Override
        public void onSeekTo(long pos) {
            setPosition((int) pos);
            mVideoView.seekTo(mPosition);
            updatePlaybackState();
        }

        @Override
        public void onFastForward() {
            if (mDuration != -1) {
                // Fast forward 10 seconds.
                setPosition(mVideoView.getCurrentPosition() + (10 * 1000));
                mVideoView.seekTo(mPosition);
                updatePlaybackState();
            }
        }

        @Override
        public void onRewind() {
            // rewind 10 seconds
            setPosition(mVideoView.getCurrentPosition() - (10 * 1000));
            mVideoView.seekTo(mPosition);
            updatePlaybackState();
        }
    }

    private void setVideoPath(String videoUrl) {
        setPosition(0);
        mVideoView.setVideoPath(videoUrl);
        mStartTimeMillis = 0;
        mDuration = Utils.getDuration(videoUrl);
    }
    @Override
    protected void onTorrentServiceConnected() {
        super.onTorrentServiceConnected();
        mService.addListener(mPlaybackOverlay);
    }


}

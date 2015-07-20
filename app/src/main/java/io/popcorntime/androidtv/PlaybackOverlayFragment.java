/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.popcorntime.androidtv;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ShuffleAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import pct.droid.base.providers.media.models.Movie;
import pct.droid.base.torrent.DownloadStatus;
import pct.droid.base.torrent.StreamInfo;
import pct.droid.base.torrent.TorrentService;

/*
 * Class for video playback with media control
 */
public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment implements TorrentService.Listener{
    private static final String TAG = "PlaybackOverlayFragment";
    private static final boolean SHOW_DETAIL = true;
    private static final boolean HIDE_MORE_ACTIONS = false;
    private static final int PRIMARY_CONTROLS = 5;
    private static final boolean SHOW_IMAGE = PRIMARY_CONTROLS <= 5;
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_LIGHT;
    private static final int CARD_WIDTH = 150;
    private static final int CARD_HEIGHT = 240;
    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int UPDATE_PERIOD = 16;
    private static final int SIMULATED_BUFFERED_TIME = 10000;
    private static final int CLICK_TRACKING_DELAY = 1000;
    private static final int INITIAL_SPEED = 10000;

    private final Handler mClickTrackingHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private PlayPauseAction mPlayPauseAction;
    private RepeatAction mRepeatAction;
    private ThumbsUpAction mThumbsUpAction;
    private ThumbsDownAction mThumbsDownAction;
    private ShuffleAction mShuffleAction;
    private FastForwardAction mFastForwardAction;
    private RewindAction mRewindAction;
    private PlaybackControlsRow.ClosedCaptioningAction mClosedCaptionAction;
    private SkipNextAction mSkipNextAction;
    private SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow mPlaybackControlsRow;
    private ArrayList<Movie> mItems = new ArrayList<Movie>();
    private int mCurrentItem;
    private Handler mHandler;
    private Runnable mRunnable;
    private Movie mSelectedMovie;
    private StreamInfo mStreamInfo;
    private int mFfwRwdSpeed = INITIAL_SPEED;
    private Timer mClickTrackingTimer;
    private int mClickCount;
    private int mDuration, mStreamerProgress = 0;

    private MediaController mMediaController;
    private MediaController.Callback mMediaControllerCallback = new MediaControllerCallback();
    private int mCurrentPlaybackState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mItems = new ArrayList<>();
        mSelectedMovie = getActivity()
                .getIntent().getParcelableExtra(MovieDetailsActivity.MOVIE);
        mStreamInfo = getActivity()
                .getIntent().getParcelableExtra(MovieDetailsActivity.STREAM_INFO);

        mHandler = new Handler();

        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);

        setupRows();

        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);
            }
        });
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMediaController = getActivity().getMediaController();
        Log.d(TAG, "register callback of mediaController");
        mMediaController.registerCallback(mMediaControllerCallback);
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onStop() {
        stopProgressAutomation();
        mRowsAdapter = null;
        super.onStop();
    }

    @Override
    public void onDetach() {
        if (mMediaController != null) {
            Log.d(TAG, "unregister callback of mediaController");
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
        super.onDetach();
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    private void setupRows() {

        ClassPresenterSelector ps = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        if (SHOW_DETAIL) {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter(
                    new DescriptionPresenter());
        } else {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter();
        }
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (action.getId() == mPlayPauseAction.getId()) {
                    togglePlayback(mPlayPauseAction.getIndex() == PlayPauseAction.PLAY);
//                } else if (action.getId() == mSkipNextAction.getId()) {
//                    next(mCurrentPlaybackState == PlaybackState.STATE_PLAYING);
//                } else if (action.getId() == mSkipPreviousAction.getId()) {
//                    prev(mCurrentPlaybackState == PlaybackState.STATE_PLAYING);
                } else if (action.getId() == mFastForwardAction.getId()) {
                    fastForward();
                } else if (action.getId() == mRewindAction.getId()) {
                    fastRewind();
                } else if (action.getId() == mClosedCaptionAction.getId()) {
                    Intent intent = new Intent(Settings.ACTION_CAPTIONING_SETTINGS);
                    getActivity().startActivity(intent);
                }
                if (action instanceof PlaybackControlsRow.MultiAction) {
                    notifyChanged(action);
                }
            }
        });
        playbackControlsRowPresenter.setSecondaryActionsHidden(HIDE_MORE_ACTIONS);

        ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);

        addPlaybackControlsRow();
//        addOtherRows();

        setAdapter(mRowsAdapter);
    }

    public void togglePlayback(boolean playPause) {
        if (playPause) {
            mMediaController.getTransportControls().play();
        } else {
            mMediaController.getTransportControls().pause();
        }
    }

    private void addPlaybackControlsRow() {
        if (SHOW_DETAIL) {
            mPlaybackControlsRow = new PlaybackControlsRow(mSelectedMovie);
        } else {
            mPlaybackControlsRow = new PlaybackControlsRow();
        }
        mRowsAdapter.add(mPlaybackControlsRow);

        updatePlaybackRow();

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mSecondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);

        mPlayPauseAction = new PlayPauseAction(getActivity());
//        mRepeatAction = new RepeatAction(getActivity());
//        mThumbsUpAction = new ThumbsUpAction(getActivity());
//        mThumbsDownAction = new ThumbsDownAction(getActivity());
//        mShuffleAction = new ShuffleAction(getActivity());
//        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(getActivity());
//        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(getActivity());
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(getActivity());
        mRewindAction = new PlaybackControlsRow.RewindAction(getActivity());
        mClosedCaptionAction = new PlaybackControlsRow.ClosedCaptioningAction(getActivity());

        if (PRIMARY_CONTROLS > 3) {
            mPrimaryActionsAdapter.add(new PlaybackControlsRow.RewindAction(getActivity()));
        }
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        if (PRIMARY_CONTROLS > 3) {
            mPrimaryActionsAdapter.add(new PlaybackControlsRow.FastForwardAction(getActivity()));
        }

        mSecondaryActionsAdapter.add(mClosedCaptionAction);
    }

    private void notifyChanged(Action action) {
        ArrayObjectAdapter adapter = mPrimaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = mSecondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
    }

    private void updatePlaybackRow() {
        mPlaybackControlsRow.setCurrentTime(0);
        mPlaybackControlsRow.setBufferedProgress(0);
        mDuration = (int) Utils.getDuration(mSelectedMovie.getUrl());
        mPlaybackControlsRow.setTotalTime(mDuration);
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
    }

    private void updateMovieView(String title, String studio, String cardImageUrl, long duration) {
        if (mPlaybackControlsRow.getItem() != null) {
            Movie item = (Movie) mPlaybackControlsRow.getItem();
            item.title = (title);
//            item.setStudio(studio);
        }
        mPlaybackControlsRow.setTotalTime((int) duration);

        if (SHOW_IMAGE) {
            updateVideoImage(cardImageUrl);
        }
    }

    private void addOtherRows() {
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        for (Movie movie : mItems) {
            listRowAdapter.add(movie);
        }
        HeaderItem header = new HeaderItem(0, getString(io.popcorntime.androidtv.R.string.related_movies));
        mRowsAdapter.add(new ListRow(header, listRowAdapter));

    }

    private int getUpdatePeriod() {
        if (getView() == null || mPlaybackControlsRow.getTotalTime() <= 0 || getView().getWidth() == 0) {
            Log.w(TAG, "Using default update period.");
            return DEFAULT_UPDATE_PERIOD;
        }
        return Math.max(UPDATE_PERIOD, mPlaybackControlsRow.getTotalTime() / getView().getWidth());
    }

    private void startProgressAutomation() {
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mCurrentPlaybackState == PlaybackState.STATE_PLAYING) {
                        int updatePeriod = getUpdatePeriod();
                        int currentTime = mPlaybackControlsRow.getCurrentTime() + updatePeriod;
                        int totalTime = mPlaybackControlsRow.getTotalTime();
                        mPlaybackControlsRow.setCurrentTime(currentTime);
                        mPlaybackControlsRow.setBufferedProgress(currentTime + mStreamerProgress);

                        if (totalTime > 0 && totalTime <= currentTime) {
                            stopProgressAutomation();
                            getActivity().finish();
//                            next(true);
                        } else {
                            mHandler.postDelayed(this, updatePeriod);
                        }
                    }
                    else if (mCurrentPlaybackState == PlaybackState.STATE_PAUSED) {
                        int updatePeriod = getUpdatePeriod();
                        mPlaybackControlsRow.setBufferedProgress(mPlaybackControlsRow.getCurrentTime() + mStreamerProgress);
                        mHandler.postDelayed(this, updatePeriod);
                    }

                }
            };
            mHandler.postDelayed(mRunnable, getUpdatePeriod());
        }
    }

    private void next(boolean autoPlay) {
        if (++mCurrentItem >= mItems.size()) {
            mCurrentItem = 0;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(PlaybackActivity.AUTO_PLAY, autoPlay);
        if (autoPlay) {
            mCurrentPlaybackState = PlaybackState.STATE_PAUSED;
        }
        mMediaController.getTransportControls().playFromMediaId(mItems.get(mCurrentItem).videoId, bundle);
        mFfwRwdSpeed = INITIAL_SPEED;
    }

    private void prev(boolean autoPlay) {
        if (--mCurrentItem < 0) {
            mCurrentItem = mItems.size() - 1;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(PlaybackActivity.AUTO_PLAY, autoPlay);
        if (autoPlay) {
            mCurrentPlaybackState = PlaybackState.STATE_PAUSED;
        }
        mMediaController.getTransportControls().playFromMediaId(mItems.get(mCurrentItem).videoId, bundle);
        mFfwRwdSpeed = INITIAL_SPEED;
    }

    private void fastForward() {
        startClickTrackingTimer();
        mMediaController.getTransportControls().fastForward();
    }

    private void fastRewind() {
        startClickTrackingTimer();
        mMediaController.getTransportControls().rewind();
    }

    private void stopProgressAutomation() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            mRunnable = null;
        }
    }

    protected void updateVideoImage(String uri) {
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .into(new SimpleTarget<GlideDrawable>(CARD_WIDTH, CARD_HEIGHT) {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        mPlaybackControlsRow.setImageDrawable(resource);
                        mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
                    }
                });
    }

    private void startClickTrackingTimer() {
        if (null != mClickTrackingTimer) {
            mClickCount++;
            mClickTrackingTimer.cancel();
        } else {
            mClickCount = 0;
            mFfwRwdSpeed = INITIAL_SPEED;
        }
        mClickTrackingTimer = new Timer();
        mClickTrackingTimer.schedule(new UpdateFfwRwdSpeedTask(), CLICK_TRACKING_DELAY);
    }

    @Override
    public void onStreamStarted() {

    }

    @Override
    public void onStreamError(Exception e) {

    }

    @Override
    public void onStreamReady(File videoLocation) {

    }

    @Override
    public void onStreamProgress(DownloadStatus status) {
        int newProgress = (int) ((mDuration / 100) * status.progress);
        if (mStreamerProgress < newProgress) {
            mStreamerProgress = newProgress;
        }
    }

    static class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            viewHolder.getTitle().setText(((Movie) item).title);
//            viewHolder.getSubtitle().setText(((Movie) item).getStudio());
        }
    }

    private class UpdateFfwRwdSpeedTask extends TimerTask {

        @Override
        public void run() {
            mClickTrackingHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mClickCount == 0) {
                        mFfwRwdSpeed = INITIAL_SPEED;
                    } else if (mClickCount == 1) {
                        mFfwRwdSpeed *= 2;
                    } else if (mClickCount >= 2) {
                        mFfwRwdSpeed *= 4;
                    }
                    mClickCount = 0;
                    mClickTrackingTimer = null;
                }
            });
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                intent.putExtra(MovieDetailsActivity.MOVIE, movie);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        MovieDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    private class MediaControllerCallback extends MediaController.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            Log.d(TAG, "playback state changed: " + state.getState());
            if (state.getState() == PlaybackState.STATE_PLAYING && mCurrentPlaybackState != PlaybackState.STATE_PLAYING) {
                mCurrentPlaybackState = PlaybackState.STATE_PLAYING;
                startProgressAutomation();
                setFadingEnabled(true);
                mPlayPauseAction.setIndex(PlayPauseAction.PAUSE);
                mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlayPauseAction.PAUSE));
                notifyChanged(mPlayPauseAction);
            } else if (state.getState() == PlaybackState.STATE_PAUSED && mCurrentPlaybackState != PlaybackState.STATE_PAUSED) {
                mCurrentPlaybackState = PlaybackState.STATE_PAUSED;
//                stopProgressAutomation();
                setFadingEnabled(false);
                mPlayPauseAction.setIndex(PlayPauseAction.PLAY);
                mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlayPauseAction.PLAY));
                notifyChanged(mPlayPauseAction);
            }

            int currentTime = (int)state.getPosition();
            mPlaybackControlsRow.setCurrentTime(currentTime);
//            mPlaybackControlsRow.setBufferedProgress(currentTime + SIMULATED_BUFFERED_TIME);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            Log.d(TAG, "received update of media metadata");
            updateMovieView(
                    metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
                    metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
                    metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI),
                    metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
            );
        }
    }
}

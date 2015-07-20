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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import pct.droid.base.providers.media.MediaProvider;
import pct.droid.base.providers.media.YTSProvider;
import pct.droid.base.providers.media.models.Genre;
import pct.droid.base.providers.media.models.Media;

public class MainFragment extends BrowseFragment {
    private static final String TAG = "MainFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final int NUM_ROWS = 6;
    private static final int NUM_COLS = 15;

    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private URI mBackgroundURI;
    Media mMedia;
    CardPresenter mCardPresenter;
//    private ArrayList<MediaList> mediaLists = new ArrayList<MediaList>();
    private HashMap<Integer, MediaList> mediaListMap = new HashMap<Integer, MediaList>();

    private int selectedRowIndex = -1, selectedObjIndex = -1;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    private void loadRows() {

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mCardPresenter = new CardPresenter();


        YTSProvider movieProvider = new YTSProvider();

        ArrayObjectAdapter newReleasesRowAdapter = new ArrayObjectAdapter(mCardPresenter);
        MediaList newReleasesList = new MediaList(newReleasesRowAdapter, movieProvider, MediaProvider.Filters.Sort.DATE, MediaProvider.Filters.Order.DESC, null);
        newReleasesList.setupMedia();

        ArrayObjectAdapter newMoviesRowAdapter = new ArrayObjectAdapter(mCardPresenter);
        MediaList newMoviesList = new MediaList(newMoviesRowAdapter, movieProvider, MediaProvider.Filters.Sort.YEAR, MediaProvider.Filters.Order.DESC, null);
        newMoviesList.setupMedia();

        ArrayObjectAdapter popularMoviesRowAdapter = new ArrayObjectAdapter(mCardPresenter);
        MediaList popularMoviesList = new MediaList(popularMoviesRowAdapter, movieProvider, MediaProvider.Filters.Sort.POPULARITY, MediaProvider.Filters.Order.DESC, null);
        popularMoviesList.setupMedia();


        HeaderItem newMoviesHeader = new HeaderItem(0, getString(io.popcorntime.androidtv.R.string.category_movie_latest));
        HeaderItem newReleasesHeader = new HeaderItem(1, getString(io.popcorntime.androidtv.R.string.category_new_releases));
        HeaderItem popularMoviesHeader = new HeaderItem(2, getString(io.popcorntime.androidtv.R.string.category_movie_popular));


        ListRow newMoviesRow = new ListRow(newMoviesHeader, newMoviesRowAdapter);
        ListRow newReleasesRow = new ListRow(newReleasesHeader, newReleasesRowAdapter);
        ListRow popularMoviesRow = new ListRow(popularMoviesHeader, popularMoviesRowAdapter);


        mRowsAdapter.add(newMoviesRow);
        mRowsAdapter.add(newReleasesRow);
        mRowsAdapter.add(popularMoviesRow);

        mediaListMap.put(mRowsAdapter.indexOf(newMoviesRow), newMoviesList);
        mediaListMap.put(mRowsAdapter.indexOf(newReleasesRow), newReleasesList);
        mediaListMap.put(mRowsAdapter.indexOf(popularMoviesRow), popularMoviesList);

        List<Genre> genreList = movieProvider.getGenres();
        int headerItemId = 3;
        for (int i = 0; i < genreList.size(); i ++) {
            ArrayObjectAdapter genreMoviesRowAdapter = new ArrayObjectAdapter(mCardPresenter);
            MediaList genreMoviesList = new MediaList(genreMoviesRowAdapter, movieProvider, MediaProvider.Filters.Sort.RATING, MediaProvider.Filters.Order.DESC, genreList.get(i).getKey());
            genreMoviesList.setupMedia();
            HeaderItem genreMoviesHeader = new HeaderItem(headerItemId + i, getResources().getString(genreList.get(i).getLabelId()));
            ListRow genreMoviesRow = new ListRow(genreMoviesHeader, genreMoviesRowAdapter);
            mRowsAdapter.add(genreMoviesRow);
            mediaListMap.put(mRowsAdapter.indexOf(genreMoviesRow), genreMoviesList);
        }


        HeaderItem gridHeader = new HeaderItem(1, getResources().getString(R.string.misc_row_header));

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.settings));
        gridRowAdapter.add(getResources().getString(io.popcorntime.androidtv.R.string.grid_view));
        gridRowAdapter.add(getString(io.popcorntime.androidtv.R.string.error_fragment));
        gridRowAdapter.add(getResources().getString(io.popcorntime.androidtv.R.string.personal_settings));
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        setAdapter(mRowsAdapter);

    }

    private void prepareBackgroundManager() {

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(io.popcorntime.androidtv.R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        setTitle(getString(io.popcorntime.androidtv.R.string.browse_title)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(io.popcorntime.androidtv.R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(io.popcorntime.androidtv.R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Media) {
                Media media = (Media) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
                intent.putExtra(MovieDetailsActivity.MOVIE, media);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        MovieDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            } else if (item instanceof String) {
                if (((String) item).indexOf(getString(io.popcorntime.androidtv.R.string.error_fragment)) >= 0) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);

                    startActivity(intent);
                }
                else if (((String) item).indexOf(getResources().getString(R.string.settings)) >= 0) {
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    intent.putExtra(SettingsFragment.SETTING_TARGET, SettingsFragment.TARGET_ALL);
                    startActivity(intent);

                }
                else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Media) {
                mBackgroundURI = Utils.getImageURI(((Media) item).image);
                startBackgroundTimer();

                // Notify the containing row so it gets a chance to load more for example
                selectedRowIndex = mRowsAdapter.indexOf(row);
                ArrayObjectAdapter adapter = (ArrayObjectAdapter)(((ListRow) row).getAdapter());
                selectedObjIndex = adapter.indexOf(item);

                if (selectedRowIndex < mediaListMap.size())
                    mediaListMap.get(selectedRowIndex).notifySelectedMedia(selectedObjIndex);
//                    mediaLists.get(selectedRowIndex).notifySelectedMedia(selectedObjIndex);



            }

        }
    }

    protected void setDefaultBackground(Drawable background) {
        mDefaultBackground = background;
    }

    protected void setDefaultBackground(int resourceId) {
        mDefaultBackground = getResources().getDrawable(resourceId);
    }

    protected void updateBackground(URI uri) {
        Picasso.with(getActivity())
                .load(uri.toString())
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .centerCrop()
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

    protected void updateBackground(Drawable drawable) {
        BackgroundManager.getInstance(getActivity()).setDrawable(drawable);
    }

    protected void clearBackground() {
        BackgroundManager.getInstance(getActivity()).setDrawable(mDefaultBackground);
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundURI != null) {
                        updateBackground(mBackgroundURI);
                    }
                }
            });

        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(io.popcorntime.androidtv.R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}

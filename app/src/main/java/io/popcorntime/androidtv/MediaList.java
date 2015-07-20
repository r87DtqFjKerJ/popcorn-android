package io.popcorntime.androidtv;

import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.squareup.okhttp.Call;

import java.util.ArrayList;
import java.util.List;

import pct.droid.base.providers.media.MediaProvider;
import pct.droid.base.providers.media.YTSProvider;
import pct.droid.base.providers.media.models.Media;
import pct.droid.base.utils.ThreadUtils;
import timber.log.Timber;
import hugo.weaving.DebugLog;

public class MediaList {

    private State mState = State.UNINITIALISED;
    public enum Mode {
        NORMAL, SEARCH
    }

    private enum State {
        UNINITIALISED, LOADING, SEARCHING, LOADING_PAGE, LOADED, LOADING_DETAIL
    }

    public ArrayList<Media> mItems;
    private Call mCurrentCall;
    private int mPage = 1;
    private ArrayObjectAdapter mAdapter;
    private MediaProvider mProvider;
    private MediaProvider.Filters mFilters = new MediaProvider.Filters();
    private MediaProvider.Filters.Sort mSort;
    private MediaProvider.Filters.Order mDefOrder;
    private String mGenre;
    private boolean mEndOfListReached = false;

//    private Integer mColumns = 2, mRetries = 0;
    private int mFirstVisibleItem, mVisibleItemCount, mTotalItemCount = 0, mLoadingTreshold = 6, mPreviousTotal = 0;

    public MediaList(ArrayObjectAdapter adapter, MediaProvider provider, MediaProvider.Filters.Sort sort, MediaProvider.Filters.Order defOrder, String genre) {
        mAdapter = adapter;

        mProvider = provider;
        mSort = sort;
        mDefOrder = defOrder;
        mGenre = genre;

        mFilters.sort = mSort;
        mFilters.order = mDefOrder;
        mFilters.genre = mGenre;
        mFilters.page = mPage;

    }
    public void setQueryTerm(String queryTerm) {
        mFilters.keywords = queryTerm;
    }

    public List<Media> setupMedia() {

        mItems= new ArrayList<Media>();
        loadMedia();

        return mItems;
    }
    public void loadMedia() {
        MediaProvider.Filters filters = new MediaProvider.Filters(mFilters);
//        if (mProvider == null)
//            mProvider = new YTSProvider();

        setState(State.LOADING);
        mCurrentCall = mProvider.getList(filters, mCallback);/* fetch new items */
    }

    private MediaProvider.Callback mCallback = new MediaProvider.Callback() {
        @Override
        @DebugLog
        public void onSuccess(MediaProvider.Filters filters, final ArrayList<pct.droid.base.providers.media.models.Media> items, boolean changed) {
            if (!(mGenre == null ? "" : mGenre).equals(filters.genre == null ? "" : filters.genre)) return; // nothing changed according to the provider, so don't do anything
            if(!changed) {
                setState(State.LOADED);
                return;
            }

            mItems.clear();
            if (null != items)
                mItems.addAll(items);


//            //fragment may be detached, so we dont want to update the UI
//            if (!isAdded())
//                return;

            mEndOfListReached = false;

            mPage = mPage + 1;

            for (int i = mPreviousTotal; i < mItems.size(); i ++)
                mAdapter.add(mItems.get(i));
            setState(State.LOADED);
            // http://s.ynet.io/assets/images/movies/The_Shawshank_Redemption_1994/large-cover.jpg
            // 750x500
            mPreviousTotal = mTotalItemCount = mAdapter.size();
        }

        @Override
        @DebugLog
        public void onFailure(Exception e) {

//
//            if (e.getMessage().equals("Canceled")) {
//                ThreadUtils.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mAdapter == null) {
//                            return;
//                        }
//
//                        mAdapter.removeLoading();
//                        setState(State.LOADED);
//                    }
//                });
//            } else if (e.getMessage() != null && e.getMessage().equals(getString(R.string.movies_error))) {
//                mEndOfListReached = true;
//                ThreadUtils.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mAdapter == null) {
//                            return;
//                        }
//
//                        mAdapter.removeLoading();
//                        setState(State.LOADED);
//                    }
//                });
//            } else {
//                e.printStackTrace();
//                Timber.e(e.getMessage());
//                if (mRetries > 1) {
//                    ThreadUtils.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(mContext, R.string.unknown_error, Toast.LENGTH_SHORT).show();
//                            setState(State.LOADED);
//                        }
//                    });
//                } else {
//                    mCurrentCall = mProvider.getList(mItems, new MediaProvider.Filters(mFilters), this);
//                }
//                mRetries++;
//            }
        }
    };
    private void setState(State state) {
        if (mState == state) return;//do nothing
        mState = state;
//        updateUI();
    }
    public void notifySelectedMedia(int index) {
        if (mState == State.LOADING)
            return;

        if ((mTotalItemCount - index) < mLoadingTreshold && mState != State.LOADING) {
            mFilters.page = mPage;
            mCurrentCall = mProvider.getList(mItems, new MediaProvider.Filters(mFilters), mCallback);
            setState(State.LOADING);

        }

    }


//    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
//        @Override
//        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//            LinearLayoutManager mLayoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
//            mVisibleItemCount = mLayoutManager.getChildCount();
//            mTotalItemCount = mLayoutManager.getItemCount();// - (mAdapter.isLoading() ? 1 : 0);
//            mFirstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
//
//            if (mState == State.LOADING_PAGE) {
//                if (mTotalItemCount > mPreviousTotal) {
//                    mPreviousTotal = mTotalItemCount;
//                    mPreviousTotal = mTotalItemCount = mLayoutManager.getItemCount();
//                    setState(State.LOADED);
//                }
//            }
//
//            if (!mEndOfListReached && !(mState == State.SEARCHING) && !(mState == State.LOADING_PAGE) && !(mState == State.LOADING) && (mTotalItemCount - mVisibleItemCount) <= (mFirstVisibleItem +
//                    mLoadingTreshold)) {
//
//                mFilters.page = mPage;
//                mCurrentCall = mProvider.getList(mItems, new MediaProvider.Filters(mFilters), mCallback);
//
//                mPreviousTotal = mTotalItemCount = mLayoutManager.getItemCount();
//                setState(State.LOADING_PAGE);
//            }
//        }
//    };

}

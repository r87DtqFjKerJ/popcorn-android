package io.popcorntime.androidtv;


import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import pct.droid.base.providers.media.MediaProvider;
import pct.droid.base.providers.media.YTSProvider;
import pct.droid.base.providers.media.models.Media;
import io.popcorntime.androidtv.BuildConfig;

import java.util.ArrayList;
import java.util.List;

/*
 * This class demonstrates how to do in-app search
 */
public class SearchFragment extends android.support.v17.leanback.app.SearchFragment
        implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider {
    private static final String TAG = "SearchFragment";
    private static final boolean DEBUG = true && BuildConfig.DEBUG;
    private static final boolean FINISH_ON_RECOGNIZER_CANCELED = true;
    private static final int REQUEST_SPEECH = 0x00000010;
    private static final long SEARCH_DELAY_MS = 1000L;

    private int selectedRowIndex, selectedObjIndex;

    private ArrayList<MediaList> mediaLists = new ArrayList<MediaList>();

    private final Handler mHandler = new Handler();
    private final Runnable mDelayedLoad = new Runnable() {
        @Override
        public void run() {
            loadRows();
        }
    };
    private ArrayObjectAdapter mRowsAdapter;
    private String mQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            // SpeechRecognitionCallback is not required and if not provided recognition will be handled
            // using internal speech recognizer, in which case you must have RECORD_AUDIO permission
            setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    if (DEBUG) Log.v(TAG, "recognizeSpeech");
                    try {
                        startActivityForResult(getRecognizerIntent(), REQUEST_SPEECH);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "Cannot find activity for speech recognizer", e);
                    }
                }
            });
        }
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.v(TAG, "onActivityResult requestCode=" + requestCode +
                " resultCode=" + resultCode +
                " data=" + data);
        switch (requestCode) {
            case REQUEST_SPEECH:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        setSearchQuery(data, true);
                        break;
                    case Activity.RESULT_CANCELED:
                        // Once recognizer canceled, user expects the current activity to process
                        // the same BACK press as user doesn't know about overlay activity.
                        // However, you may not want this behaviour as it makes harder to
                        // fall back to keyboard input.
//                        if (FINISH_ON_RECOGNIZER_CANCELED) {
//                            if (!hasResults()) {
//                                if (DEBUG) Log.v(TAG, "Delegating BACK press from recognizer");
//                                getActivity().onBackPressed();
//                            }
//                        }
                        break;
                    // the rest includes various recognizer errors, see {@link RecognizerIntent}
                }
                break;
        }
    }
    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        Log.i(TAG, String.format("Search Query Text Change %s", newQuery));
        loadQuery(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i(TAG, String.format("Search Query Text Submit %s", query));
        loadQuery(query);
        return true;
    }

    public boolean hasResults() {
        return mRowsAdapter.size() > 0;
    }

    private boolean hasPermission(final String permission) {
        final Context context = getActivity();
        return PackageManager.PERMISSION_GRANTED == context.getPackageManager().checkPermission(
                permission, context.getPackageName());
    }

    private void loadQuery(String query) {
        mHandler.removeCallbacks(mDelayedLoad);
        if (!TextUtils.isEmpty(query) && !query.equals("nil")) {
            mQuery = query;
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
    }

    private void loadRows() {
        // offload processing from the UI thread
        new AsyncTask<String, Void, ListRow>() {
            private final String query = mQuery;

            @Override
            protected void onPreExecute() {
                mRowsAdapter.clear();
            }

            @Override
            protected ListRow doInBackground(String... params) {
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
                YTSProvider movieProvider = new YTSProvider();
                MediaList movieList = new MediaList(listRowAdapter, movieProvider, MediaProvider.Filters.Sort.RATING, MediaProvider.Filters.Order.DESC, null);
                movieList.setQueryTerm(query);
                mediaLists.clear();
                mediaLists.add(movieList);


                final List<Media> result = movieList.setupMedia();
//                HashMap<String, List<Movie>> movies = VideoProvider.getMovieList();
//                for (Map.Entry<String, List<Movie>> entry : movies.entrySet()) {
//                    for (Movie movie : entry.getValue()) {
//                        if (movie.getTitle().toLowerCase(Locale.ENGLISH)
//                                .contains(query.toLowerCase(Locale.ENGLISH))
//                                || movie.getDescription().toLowerCase(Locale.ENGLISH)
//                                .contains(query.toLowerCase(Locale.ENGLISH))) {
//                            result.add(movie);
//                        }
//                    }
//                }

//                listRowAdapter.addAll(0, result);
                HeaderItem header = new HeaderItem(getString(io.popcorntime.androidtv.R.string.search_results, query));
                return new ListRow(header, listRowAdapter);
            }

            @Override
            protected void onPostExecute(ListRow listRow) {
                mRowsAdapter.clear();
                mRowsAdapter.add(listRow);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Media) {

                // Notify the containing row so it gets a chance to load more for example
                selectedRowIndex = mRowsAdapter.indexOf(row);
                ArrayObjectAdapter adapter = (ArrayObjectAdapter)(((ListRow) row).getAdapter());
                selectedObjIndex = adapter.indexOf(item);

                if (selectedRowIndex < mediaLists.size())
                    mediaLists.get(selectedRowIndex).notifySelectedMedia(selectedObjIndex);



            }

        }
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
            } else {
                Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


}


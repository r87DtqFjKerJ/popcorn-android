package io.popcorntime.androidtv;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import hugo.weaving.DebugLog;
import pct.droid.base.providers.media.MediaProvider;
import pct.droid.base.providers.media.YTSProvider;
import pct.droid.base.providers.media.models.Media;
import pct.droid.base.providers.media.models.Movie;
import pct.droid.base.providers.subs.SubsProvider;
import pct.droid.base.torrent.StreamInfo;
import pct.droid.base.torrent.TorrentHealth;
import pct.droid.base.utils.LocaleUtils;
import pct.droid.base.utils.SortUtils;
import pct.droid.base.youtube.YouTubeData;

public class MovieDetailsFragment extends BaseDetailFragment {
    private static final String TAG = "MovieDetailsFragment";

    private static final int ACTION_WATCH_TRAILER = 1;
    private static final int ACTION_SELECT_SUBTITLE = 2;
    private int[] ACTIONS_PLAY;

    private static final int DETAIL_THUMB_WIDTH = 182;
    private static final int DETAIL_THUMB_HEIGHT = 274;


    private static final String MOVIE = "Movie";

    private Boolean mAttached = false;

    // Movie object from the list
    private Movie mSelectedMovie;
    private String mSelectedSubtitleLanguage, mSelectedQuality;
    private Movie mTrailer;
    private String mSelectedMovieId;
    private String[] mQualities;
    private String[] mSubtitles;
    private String trailerId;
    private AlertDialog.Builder subtitleDialogBuilder;
    private AlertDialog subtitleDialog;
    private DetailsOverviewRow row;
    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private DetailsOverviewRowPresenter mDorPresenter;
    private DetailRowBuilderTask mDetailRowBuilderTask;
    private YTSProvider movieProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);
        movieProvider = new YTSProvider();

        mDorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(io.popcorntime.androidtv.R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        Intent intent = getActivity().getIntent();


        mSelectedMovie = (Movie) intent.getExtras().getParcelable(MOVIE);
        mSelectedMovieId = mSelectedMovie.videoId;

        movieProvider.getDetail(mSelectedMovieId, mDetailsCallback);

        mDorPresenter.setSharedElementEnterTransition(getActivity(),
                MovieDetailsActivity.SHARED_ELEMENT_NAME);

        updateBackground(Utils.getImageURI(mSelectedMovie.backgroundImage));
        setOnItemViewClickedListener(new ItemViewClickedListener());


        if (mSelectedMovie.getSubsProvider() != null) {
            mSelectedMovie.getSubsProvider().getList(mSelectedMovie, new SubsProvider.Callback() {
                @Override
                public void onSuccess(Map<String, String> subtitles) {
                    if (!mAttached) return;

//                    if(subtitles == null) {
//                        ThreadUtils.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
////                                mSubtitles.setText(R.string.no_subs_available);
//                            }
//                        });
//                        return;
//                    }

                    mSelectedMovie.subtitles = subtitles;

                    if (subtitles != null) {
                        String[] languages = subtitles.keySet().toArray(new String[subtitles.size()]);
                        Arrays.sort(languages);
                        final String[] adapterLanguages = new String[languages.length + 1];
                        adapterLanguages[0] = "no-subs";
                        mSelectedSubtitleLanguage = adapterLanguages[0];
                        System.arraycopy(languages, 0, adapterLanguages, 1, languages.length);

                        mSubtitles = new String[adapterLanguages.length];
                        for (int i = 0; i < mSubtitles.length; i++) {
                            String language = adapterLanguages[i];
                            if (language.equals("no-subs")) {
                                mSubtitles[i] = getString(io.popcorntime.androidtv.R.string.no_subs);
                            } else {
                                Locale locale = LocaleUtils.toLocale(language);
                                mSubtitles[i] = locale.getDisplayName(locale);
                            }
                        }

                        subtitleDialogBuilder = new AlertDialog.Builder(getActivity());
                        subtitleDialogBuilder.setTitle(R.string.tip_subtitles)
                                .setItems(mSubtitles, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter) row.getActionsAdapter();
                                        Action action = (Action) adapter.lookup(ACTION_SELECT_SUBTITLE);
                                        action.setLabel2(mSubtitles[which]);
                                        adapter.notifyArrayItemRangeChanged(adapter.indexOf(ACTION_SELECT_SUBTITLE), 1);
                                        mSelectedSubtitleLanguage = adapterLanguages[which];
                                        // The 'which' argument contains the index position
                                        // of the selected item
                                    }
                                });
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                subtitleDialog = subtitleDialogBuilder.create();
                            }
                        });
                    }


                }

                @Override
                public void onFailure(Exception e) {
                    Log.w(TAG, "Unable to get subtitles.");
                }
            });
        } else {
        }



//        if (mCoverImage != null) {
//            Picasso.with(mCoverImage.getContext()).load(sMovie.image).into(mCoverImage);
//        }
    }

    @Override
    public void onStop() {
        if (mDetailRowBuilderTask != null)
            mDetailRowBuilderTask.cancel(true);
        super.onStop();
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAttached = true;
        if (activity instanceof BaseDetailFragment.FragmentListener)
            mCallback = (BaseDetailFragment.FragmentListener) activity;
    }
    private void renderHealth() {
//        if(mHealth.getVisibility() == View.GONE) {
//            mHealth.setVisibility(View.VISIBLE);
//        }

        TorrentHealth health = TorrentHealth.calculate(mSelectedMovie.torrents.get(mSelectedQuality).seeds, mSelectedMovie.torrents.get(mSelectedQuality).peers);
        Resources res = getResources();
        Drawable shape = res.getDrawable(health.getImageResource());

        setBadgeDrawable(shape);
//        mHealth.setImageResource(health.getImageResource());
    }
    private class DetailRowBuilderTask extends AsyncTask<Media, Integer, DetailsOverviewRow> {
        @Override
        protected DetailsOverviewRow doInBackground(Media... movies) {
            if (mSelectedMovie.torrents.size() > 0) {
                mQualities = SortUtils.sortQualities(mSelectedMovie.torrents.keySet().toArray(new String[mSelectedMovie.torrents.size()]));

                ACTIONS_PLAY = new int[mQualities.length];
                for (int i = 0; i < mQualities.length; i ++)
                    ACTIONS_PLAY[i] = ACTION_SELECT_SUBTITLE + 1 + i;
//                renderHealth();
//            mQuality.setData(qualities);
//            mQuality.setListener(new OptionSelector.SelectorListener() {
//                @Override
//                public void onSelectionChanged(int position, String value) {
//                    mSelectedQuality = value;
//                    renderHealth();
//                    updateMagnet();
//                }
//            });
//            mSelectedQuality = qualities[qualities.length - 1];
//            mQuality.setText(mSelectedQuality);
//            mQuality.setDefault(qualities.length - 1);
//            renderHealth();
//            updateMagnet();
            }



            mSelectedMovie = (Movie) movies[0];

            row = new DetailsOverviewRow(mSelectedMovie);
            try {
                Bitmap poster = Picasso.with(getActivity())
                        .load(mSelectedMovie.image)
                        .resize(Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH),
                                Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT))
                        .centerCrop()
                        .get();
                row.setImageBitmap(getActivity(), poster);
            } catch (IOException e) {
            }

            if (null != trailerId) {
                try {
                    String url = YouTubeData.calculateYouTubeUrl("22", true, trailerId);
                    if (url != null) {
                        String decoded = URLDecoder.decode(url);
//                url = "http://r5---sn-nwj7knl7.googlevideo.com/videoplayback?key=yt5&ip=24.23.206.50&initcwndbps=1902500&source=youtube&pl=19&dur=118.862&itag=22&id=o-ADRJxBknucxkvZdk2KxKKrQTgyKcdE18XGE0LAlv7DAi&mime=video%2Fmp4&upn=sYlMn3MRmqc&mt=1435559501&sparams=dur%2Cid%2Cinitcwndbps%2Cip%2Cipbits%2Citag%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Cratebypass%2Csource%2Cupn%2Cexpire&mn=sn-nwj7knl7&ratebypass=yes&ipbits=0&mm=31&mv=m&ms=au&signature=81F2D3B4B9E8E447B102492E3B4900CA0B8E7CA7.C1E58B97F58F24891B93ADC0176FA3F8E848A3B2&fexp=901816%2C9407141%2C9407720%2C9407966%2C9408142%2C9408145%2C9408420%2C9408710%2C9412463%2C9413503%2C9414590%2C9414764%2C9416074%2C9416126%2C9416348%2C9416376%2C9416456%2C952640%2C962739&lmt=1428057780748795&sver=3&expire=1435581186";
                        // Create a simple Movie object for the trailer
                        // This object gets sent to the player to be played
                        mTrailer = new Movie(mSelectedMovie.getMediaProvider(), mSelectedMovie.getSubsProvider());
                        mTrailer.setUrl(decoded);
                        mTrailer.title = mSelectedMovie.title;
                        mTrailer.fullImage = mSelectedMovie.fullImage;
                        mTrailer.image = mSelectedMovie.image;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();
            if (mTrailer != null)
                adapter.set(ACTION_WATCH_TRAILER, new Action(ACTION_WATCH_TRAILER,
                        getResources().getString(io.popcorntime.androidtv.R.string.watch_trailer_1)));

            if (mSubtitles != null) {
                adapter.set(ACTION_SELECT_SUBTITLE, new Action(ACTION_SELECT_SUBTITLE,
                        getResources().getString(io.popcorntime.androidtv.R.string.subtitles)));
            }


            Resources res = getResources();

            for (int i = 0; i < ACTIONS_PLAY.length; i ++) {
                TorrentHealth health = TorrentHealth.calculate(mSelectedMovie.torrents.get(mQualities[i]).seeds, mSelectedMovie.torrents.get(mQualities[i]).peers);
                Drawable shape = res.getDrawable(health.getImageResource());
                adapter.set(ACTIONS_PLAY[i],
                        new Action(ACTIONS_PLAY[i],
                                getResources().getString(io.popcorntime.androidtv.R.string.play),
                                mQualities[i],
                                shape));
            }


            row.setActionsAdapter(adapter);




//                    getResources().getString(R.string.rent_2)))
//                row.addAction(new Action(ACTIONS_PLAY[i], getResources().getString(R.string.play), mQualities[i]));
//                    getResources().getString(R.string.rent_2)));
//            row.addAction(new Action(ACTION_BUY, getResources().getString(R.string.buy_1),
//                    getResources().getString(R.string.buy_2)));
            return row;
        }

        @Override
        protected void onPostExecute(DetailsOverviewRow detailRow) {
            ClassPresenterSelector ps = new ClassPresenterSelector();
            // set detail background and style
            mDorPresenter.setBackgroundColor(getResources().getColor(io.popcorntime.androidtv.R.color.detail_background));
            mDorPresenter.setStyleLarge(true);
            mDorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
                @Override
                public void onActionClicked(Action action) {
                    long actionId = action.getId();
                    if (actionId == ACTION_WATCH_TRAILER) {
                        // comment out for testing
                        Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                        intent.putExtra(MovieDetailsActivity.MOVIE, mTrailer);
                        // Omit StreamInfo for trailers
                        startActivity(intent);
                    }
                    else if (actionId == ACTION_SELECT_SUBTITLE) {
                        subtitleDialog.show();
                    }
                    else {
                        for (int i = 0; i < ACTIONS_PLAY.length; i ++)
                            if (actionId == ACTIONS_PLAY[i]) {
                                String streamUrl = mSelectedMovie.torrents.get(mQualities[i]).url;
                                StreamInfo streamInfo = new StreamInfo(mSelectedMovie, streamUrl, mSelectedSubtitleLanguage, mQualities[i]);
                                mCallback.playStream(streamInfo);
                            }

                    }

                }
            });

            ps.addClassPresenter(DetailsOverviewRow.class, mDorPresenter);
            ps.addClassPresenter(ListRow.class,
                    new ListRowPresenter());

            ArrayObjectAdapter adapter = new ArrayObjectAdapter(ps);
            adapter.add(detailRow);

            String subcategories[] = {
                    getString(io.popcorntime.androidtv.R.string.related_movies)
            };
//            List<Media> list = MediaList.list;
//            Collections.shuffle(list);
//            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
//            for (int j = 0; j < NUM_COLS; j++) {
//                listRowAdapter.add(list.get(j % 5));
//            }

//            HeaderItem header = new HeaderItem(0, subcategories[0]);
//            adapter.add(new ListRow(header, listRowAdapter));

            setAdapter(adapter);
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
//                intent.putExtra(MovieDetailsActivity.MOVIE, media);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        MovieDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    protected void updateBackground(URI uri) {
        Log.d(TAG, "uri" + uri);
        Log.d(TAG, "metrics" + mMetrics.toString());
        Picasso.with(getActivity())
                .load(uri.toString())
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

    // Request for movie details the list doesn't have
    private MediaProvider.Callback mDetailsCallback = new MediaProvider.Callback() {
        @Override
        @DebugLog
        public void onSuccess(MediaProvider.Filters filters, final ArrayList<Media> items, boolean changed) {
            Movie movieDetails = (Movie) items.get(0);
            if (movieDetails.trailer != null && !movieDetails.trailer.isEmpty()) {
                mSelectedMovie.trailer = movieDetails.trailer;
                Pattern p = Pattern.compile("(?<=v=)(\\w+)");
                Matcher m = p.matcher(mSelectedMovie.trailer);
                if (m.find())
                    trailerId = m.group(0);
            }

            if (movieDetails.fullImage != null && !movieDetails.fullImage.isEmpty())
                mSelectedMovie.fullImage = movieDetails.fullImage;
            if (movieDetails.synopsis != null && !movieDetails.synopsis.isEmpty())
                mSelectedMovie.synopsis = movieDetails.synopsis;
            if (movieDetails.tagline != null && !movieDetails.tagline.isEmpty())
                mSelectedMovie.tagline = movieDetails.tagline;



            mDetailRowBuilderTask = (DetailRowBuilderTask) new DetailRowBuilderTask().execute(mSelectedMovie);

        }


        @Override
        @DebugLog
        public void onFailure(Exception e) {
        // TODO: write failure handling for movie details request
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

}

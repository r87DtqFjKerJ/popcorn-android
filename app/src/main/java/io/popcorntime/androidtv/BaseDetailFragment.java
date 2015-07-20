package io.popcorntime.androidtv;

import android.app.Activity;
import android.support.v17.leanback.app.DetailsFragment;
import android.view.View;

import pct.droid.base.torrent.StreamInfo;


public abstract class BaseDetailFragment extends DetailsFragment{

    protected FragmentListener mCallback;
    protected MediaDetailsActivity mActivity;
    protected View mRoot;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof MediaDetailsActivity)
            mActivity = (MediaDetailsActivity) activity;
    }

    public interface FragmentListener {
        public void playStream(StreamInfo streamInfo);
    }

}

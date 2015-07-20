package io.popcorntime.androidtv;

import android.os.Bundle;

import pct.droid.base.providers.media.models.Media;
import pct.droid.base.torrent.StreamInfo;


public class MediaDetailsActivity extends PopcornBaseActivity implements BaseDetailFragment.FragmentListener {
    private static Media sMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(io.popcorntime.androidtv.R.layout.activity_media_details);
    }

    @Override
    public void playStream(StreamInfo streamInfo) {
        if(mService != null) {
            mService.startForeground();
        }
        StreamLoadingActivity.startActivity(this, streamInfo);


    }
}

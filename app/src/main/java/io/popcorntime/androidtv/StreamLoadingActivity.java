package io.popcorntime.androidtv;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.view.View;
import android.view.WindowManager;
import io.popcorntime.androidtv.R;
//import pct.droid.R;
import pct.droid.base.torrent.StreamInfo;
//import pct.droid.fragments.StreamLoadingFragment;

public class StreamLoadingActivity extends PopcornBaseActivity implements StreamLoadingFragment.FragmentListener {

    public final static String EXTRA_INFO = "mInfo";

    private StreamInfo mInfo;
    private StreamLoadingFragment mFragment;

    public static Intent startActivity(Activity activity, StreamInfo info) {
        Intent i = new Intent(activity, StreamLoadingActivity.class);
        i.putExtra(EXTRA_INFO, info);
        activity.startActivity(i);
        return i;
    }

    public static Intent startActivity(Activity activity, StreamInfo info, Pair<View, String>... elements) {
        Intent i = new Intent(activity, StreamLoadingActivity.class);
        i.putExtra(EXTRA_INFO, info);

        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(activity, elements);
        ActivityCompat.startActivity(activity, i, options.toBundle());
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setBackgroundDrawableResource(R.color.bg);

        super.onCreate(savedInstanceState, R.layout.activity_streamloading);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!getIntent().hasExtra(EXTRA_INFO)) finish();

        mInfo = getIntent().getParcelableExtra(EXTRA_INFO);

        mFragment = (StreamLoadingFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
    }

    @Override
    protected void onTorrentServiceConnected() {
        super.onTorrentServiceConnected();
        if (null != mFragment) {
            mFragment.onTorrentServiceConnected();
        }
    }

    @Override
    public void onTorrentServiceDisconnected() {
        super.onTorrentServiceDisconnected();
        if (null != mFragment) {
            mFragment.onTorrentServiceDisconnected();
        }
    }

    @Override
    public StreamInfo getStreamInformation() {
        return mInfo;
    }

    @Override
    public void onBackPressed() {
        if (mFragment != null) {
            mFragment.cancelStream();
        }
        super.onBackPressed();
    }
}
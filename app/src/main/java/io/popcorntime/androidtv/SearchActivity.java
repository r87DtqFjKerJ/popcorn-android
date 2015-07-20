package io.popcorntime.androidtv;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/*
 * SearchActivity for SearchFragment
 */
public class SearchActivity extends Activity {

    private static final String TAG = "SearchActivity";
    private SearchFragment mFragment;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(io.popcorntime.androidtv.R.layout.search);

        mFragment = (SearchFragment) getFragmentManager().findFragmentById(io.popcorntime.androidtv.R.id.search_fragment);
    }

    @Override
    public boolean onSearchRequested() {
        if (mFragment.hasResults()) {
            startActivity(new Intent(this, SearchActivity.class));
        } else {
            mFragment.startRecognition();
        }
        return true;
    }
}
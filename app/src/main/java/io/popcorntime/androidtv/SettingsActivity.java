package io.popcorntime.androidtv;

import android.support.v17.leanback.app.GuidedStepFragment;
import android.os.Bundle;

public class SettingsActivity extends PopcornBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (null == savedInstanceState) {
            SettingsFragment settings = new SettingsFragment();
            Bundle args = new Bundle();
            args.putInt(SettingsFragment.SETTING_TARGET, SettingsFragment.TARGET_ALL);
            settings.setArguments(args);
            GuidedStepFragment.add(getFragmentManager(), settings);
        }
    }



}

package io.popcorntime.androidtv;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import java.util.List;


public class SettingsFragment extends GuidedStepFragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final int ACTION_SETTINGS_VPN = 0;
    public static final int ACTION_SETTINGS_GENERAL = 1;

    public static final String SETTING_TARGET = "target";

    public static final int TARGET_ALL = 0;
    public static final int TARGET_GENERAL = 1;
    public static final int TARGET_VPN = 2;

    private int mTarget = -1;



    public SettingsFragment() {
        // Required empty public constructor
    }
//
//    @Override
//    public static SettingsFragment newInstance(int target) {
//        SettingsFragment mSettingsFragment = new SettingsFragment();
//
//        Bundle args = new Bundle();
//        args.putInt(SETTING_TARGET, target);
//        mSettingsFragment.setArguments(args);
//
//        return mSettingsFragment;
//    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        if (mTarget < 0) {
            Bundle bundle = getArguments();
            mTarget = bundle.getInt(SETTING_TARGET);
        }
        switch (mTarget) {
            case TARGET_ALL:
                actions.add(new GuidedAction.Builder()
                        .id(ACTION_SETTINGS_GENERAL)
                        .infoOnly(true)
                        .title("General Settings")
                        .build());

                actions.add(new GuidedAction.Builder()
                        .id(ACTION_SETTINGS_VPN)
                        .title("VPN")
                        .description("Some action")
                        .build());
                break;
            case TARGET_VPN:
                break;
            default:
                actions.add(new GuidedAction.Builder()
                        .id(ACTION_SETTINGS_GENERAL)
                        .infoOnly(true)
                        .title("Fake Settings")
                        .build());


        }

    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {


        if (mTarget == TARGET_ALL)
            return new GuidanceStylist.Guidance("Settings", null, "Demo", null);
        if (mTarget == TARGET_VPN)
            return new GuidanceStylist.Guidance("VPN", null, "Demo", null);
        else
            return null;
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        switch ((int) action.getId()) {
            case ACTION_SETTINGS_GENERAL:
                // do something (anything, really)
                break;
            case ACTION_SETTINGS_VPN:
                SettingsFragment settings = new SettingsFragment();
                Bundle args = new Bundle();
                args.putInt(SettingsFragment.SETTING_TARGET, SettingsFragment.TARGET_VPN);
                settings.setArguments(args);
                GuidedStepFragment.add(getFragmentManager(), settings);
                break;
            default:
                break;
        }
    }


}

package org.votingsystem.android.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.votingsystem.android.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessagesGridFragment;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessagesMainActivity extends ActivityBase {

    public static final String TAG = MessagesMainActivity.class.getSimpleName();

    WeakReference<MessagesGridFragment> messagesGridRef;
    private AppVS contextVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppVS) getApplicationContext();
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        MessagesGridFragment fragment = new MessagesGridFragment();
        messagesGridRef = new WeakReference<MessagesGridFragment>(fragment);
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,
                ((Object)fragment).getClass().getSimpleName()).commit();
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        getSupportActionBar().setLogo(null);
        getSupportActionBar().setTitle(getString(R.string.messages_lbl));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_MESSAGES;// we only have a nav drawer if we are in top-level
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
    }

}
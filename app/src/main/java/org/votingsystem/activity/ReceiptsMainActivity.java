package org.votingsystem.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.fragment.ReceiptGridFragment;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptsMainActivity extends ActivityBase {

    public static final String TAG = ReceiptsMainActivity.class.getSimpleName();

    WeakReference<ReceiptGridFragment> receiptGridRef;
    private AppVS appVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getApplicationContext();
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        ReceiptGridFragment fragment = new ReceiptGridFragment();
        receiptGridRef = new WeakReference<ReceiptGridFragment>(fragment);
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,
                ((Object)fragment).getClass().getSimpleName()).commit();
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        getSupportActionBar().setLogo(null);
        getSupportActionBar().setTitle(getString(R.string.receipts_lbl));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_RECEIPTS;// we only have a nav drawer if we are in top-level
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
    }

}
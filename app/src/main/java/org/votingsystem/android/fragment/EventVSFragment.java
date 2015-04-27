package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.FieldEventVSDto;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.VoteVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.votingsystem.util.ContextVS.FRAGMENT_KEY;
import static org.votingsystem.util.ContextVS.MAX_SUBJECT_SIZE;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = EventVSFragment.class.getSimpleName();

    private EventVSDto eventVS;
    private VoteVS vote;
    private List<Button> voteOptionsButtonList;
    private Button saveReceiptButton;
    private Button cancelVoteButton;
    private AppContextVS contextVS;
    private View rootView;
    private String broadCastId = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "intentExtras:" + intent.getExtras());
            final ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null)
                launchVoteService(responseVS.getTypeVS());
            else {
                vote = (VoteVS) intent.getSerializableExtra(ContextVS.VOTE_KEY);
                if(responseVS.getTypeVS() == TypeVS.VOTEVS) {
                    if(ResponseVS.SC_OK == responseVS.getStatusCode())  showReceiptScreen(vote);
                    else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()){
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                responseVS.getCaption(), responseVS.getNotificationMessage(),getActivity());
                        builder.setPositiveButton(getString(R.string.open_receipt_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                                    intent.putExtra(ContextVS.URL_KEY, responseVS.getUrl());
                                    intent.putExtra(ContextVS.FRAGMENT_KEY, ReceiptFragment.class.getName());
                                    startActivity(intent);
                                }
                            });
                        UIUtils.showMessageDialog(builder);
                    } else {
                        setOptionButtonsEnabled(true);
                        MessageDialogFragment.showDialog(responseVS, getFragmentManager());
                    }
                } else if(responseVS.getTypeVS() == TypeVS.CANCEL_VOTE){
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        setEventScreen(eventVS);
                        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).
                                setVisibility(View.GONE);
                        setOptionButtonsEnabled(true);
                    } else cancelVoteButton.setEnabled(true);
                    MessageDialogFragment.showDialog(responseVS, getFragmentManager());
                }
                setProgressDialogVisible(false, null);
            }
        }
    };

    private void launchVoteService(TypeVS operation) {
        LOGD(TAG + ".launchVoteService", "operation: " + operation.toString());
        Intent startIntent = new Intent(getActivity(), VoteService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, operation);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.VOTE_KEY, vote);
        String caption = null;
        if(operation == TypeVS.CANCEL_VOTE) {
            cancelVoteButton.setEnabled(false);
            caption = getString(R.string.cancel_vote_lbl);
        } else caption = getString(R.string.sending_vote_lbl);
        setProgressDialogVisible(true, caption);
        setOptionButtonsEnabled(false);
        getActivity().startService(startIntent);
    }

    public static EventVSFragment newInstance(String eventJSONStr) {
        EventVSFragment fragment = new EventVSFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.EVENTVS_KEY, eventJSONStr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        try {
            if(getArguments().getString(ContextVS.EVENTVS_KEY) != null) {
                String dtoStr = getArguments().getString(ContextVS.EVENTVS_KEY);
                eventVS = JSON.getMapper().readValue(dtoStr, EventVSDto.class);
                eventVS.setAccessControlVS(contextVS.getAccessControl());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        rootView = inflater.inflate(R.layout.eventvs_fragment, container, false);
        saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        saveReceiptButton.setOnClickListener(this);
        cancelVoteButton = (Button) rootView.findViewById(R.id.cancel_vote_button);
        cancelVoteButton.setOnClickListener(this);
        setHasOptionsMenu(true);
        TextView eventSubject = (TextView) rootView.findViewById(R.id.event_subject);
        eventSubject.setOnClickListener(this);
        broadCastId = EventVSFragment.class.getSimpleName() + "_" + eventVS.getId();
        String subtTitle = null;
        switch(eventVS.getState()) {
            case ACTIVE:
                ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(
                        getString(R.string.voting_open_lbl,
                        DateUtils.getElapsedTimeStr(eventVS.getDateFinish())));
                break;
            case PENDING:
                ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(
                        getString(R.string.voting_pending_lbl,
                        DateUtils.getElapsedTimeStr(eventVS.getDateBegin())));
                subtTitle = getString(R.string.init_lbl) + ": " +
                        DateUtils.getDayWeekDateStr(eventVS.getDateBegin());
                break;
            default:
                ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.voting_closed_lbl));
        }
        if(subtTitle != null) ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(subtTitle);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if(bundle != null) vote = (VoteVS) bundle.getSerializable(ContextVS.VOTE_KEY);
        if(vote != null && vote.getVoteReceipt() != null) showReceiptScreen(vote);
        else setEventScreen(eventVS);
    }

    @Override public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancel_vote_button:
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.cancel_vote_msg), false, TypeVS.CANCEL_VOTE);
                break;
            case R.id.save_receipt_button:
                ContentValues values = new ContentValues();
                vote.setTypeVS(TypeVS.VOTEVS);
                values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL, ObjectUtils.serializeObject(vote));
                values.put(ReceiptContentProvider.URL_COL, vote.getMessageId());
                values.put(ReceiptContentProvider.TYPE_COL, vote.getTypeVS().toString());
                values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
                Uri uri = getActivity().getContentResolver().insert(ReceiptContentProvider.CONTENT_URI, values);
                LOGD(TAG + ".saveVote", "uri: " + uri.toString());
                saveReceiptButton.setEnabled(false);
                break;
            case R.id.event_subject:
                if(eventVS != null && eventVS.getSubject() != null &&
                        eventVS.getSubject().length() > MAX_SUBJECT_SIZE) {
                    MessageDialogFragment.showDialog(null, getActivity().getString(R.string.subject_lbl),
                            eventVS.getSubject(), getFragmentManager());
                }
                break;
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption,
                    getString(R.string.wait_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.eventvs, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.eventInfo:
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(FRAGMENT_KEY, EventVSStatsFragment.class.getName());
                intent.putExtra(ContextVS.ITEM_ID_KEY, eventVS.getId());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showReceiptScreen(final VoteVS vote) {
        LOGD(TAG + ".showReceiptScreen", "showReceiptScreen");
        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).setVisibility(View.VISIBLE);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        String subject = vote.getEventVS().getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        contentTextView.setText(Html.fromHtml(vote.getEventVS().getContent()) + "\n");
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<FieldEventVSDto> fieldsEventVS = vote.getEventVS().getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(voteOptionsButtonList == null) {
            voteOptionsButtonList = new ArrayList<Button>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 15, 15);
            for (final FieldEventVSDto option:fieldsEventVS) {
                Button optionButton = new Button(getActivity());
                optionButton.setText(option.getContent());
                voteOptionsButtonList.add(optionButton);
                optionButton.setEnabled(false);
                linearLayout.addView(optionButton, paramsButton);
            }
        } else setOptionButtonsEnabled(false);
    }

    private void setEventScreen(final EventVSDto event) {
        LOGD(TAG + ".setEventScreen", "setEventScreen");
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        String subject = event.getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        contentTextView.setText(Html.fromHtml(event.getContent()));
        //contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<FieldEventVSDto> fieldsEventVS = event.getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(voteOptionsButtonList != null) linearLayout.removeAllViews();
        voteOptionsButtonList = new ArrayList<Button>();
        FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paramsButton.setMargins(15, 15, 15, 15);
        for (final FieldEventVSDto option:fieldsEventVS) {
            Button optionButton = new Button(getActivity());
            optionButton.setText(option.getContent());
            optionButton.setOnClickListener(new Button.OnClickListener() {
                FieldEventVSDto optionSelected = option;
                public void onClick(View v) {
                    LOGD(TAG + "- optionButton - optionId: " +
                            optionSelected.getId(), "state: " + contextVS.getState().toString());
                    processSelectedOption(optionSelected);
                }
            });
            voteOptionsButtonList.add(optionButton);
            if (!event.isActive()) optionButton.setEnabled(false);
            linearLayout.addView(optionButton, paramsButton);
        }
    }

    private void processSelectedOption(FieldEventVSDto optionSelected) {
        LOGD(TAG + ".processSelectedOption", "processSelectedOption");
        vote = new VoteVS(eventVS, optionSelected);
        String pinMsgPart = optionSelected.getContent().length() >
                ContextVS.SELECTED_OPTION_MAX_LENGTH ? optionSelected.getContent().substring(0,
                ContextVS.SELECTED_OPTION_MAX_LENGTH) + "..." : optionSelected.getContent();
        PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                getString(R.string.option_selected_msg, pinMsgPart), false, TypeVS.VOTEVS);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void setOptionButtonsEnabled(boolean areEnabled) {
        if(voteOptionsButtonList == null) return;
        for(Button button: voteOptionsButtonList) {
            button.setEnabled(areEnabled);
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.VOTE_KEY, vote);
    }

    public void saveCancelReceipt(VoteVS vote) {
        LOGD(TAG + ".saveCancelReceipt", "saveCancelReceipt");
        ContentValues values = new ContentValues();
        values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL, ObjectUtils.serializeObject(vote));
        values.put(ReceiptContentProvider.TYPE_COL, TypeVS.CANCEL_VOTE.toString());
        values.put(ReceiptContentProvider.URL_COL, vote.getMessageId());
        values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
        getActivity().getContentResolver().insert(ReceiptContentProvider.CONTENT_URI, values);
        MessageDialogFragment.showDialog(null, getString(R.string.msg_lbl),
                getString(R.string.saved_cancel_vote_recepit_msg), getFragmentManager());
    }

}
package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.votingsystem.AppVS;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRGeneratorFormFragment extends Fragment {

    public static final String TAG = QRGeneratorFormFragment.class.getSimpleName();

    private AppVS appVS;
    private String broadCastId = QRGeneratorFormFragment.class.getSimpleName();
    private View rootView;
    private Button btn_plus;
    private Button btn_minus;
    private LinearLayout currency_amount_selector;
    private Spinner operationSpinner;
    private Spinner currencySpinner;
    private EditText amount_text;
    private Handler handler;
    private static final TypeVS[] OPERATION_ARRAY = new TypeVS[]{TypeVS.CURRENCY_CHANGE, TypeVS.FROM_USERVS};
    private AtomicBoolean isLongPressed = new AtomicBoolean(false);

    final Runnable incrementRunnable = new Runnable(){
        @Override public void run() { incrementAmount(); }
    };

    final Runnable decrementRunnable = new Runnable(){
        @Override public void run() {
            decrementAmount();
        }
    };

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getActivity().getApplicationContext();
        rootView = inflater.inflate(R.layout.qr_generator_form_fragment, container, false);
        ((Button) rootView.findViewById(R.id.request_button)).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        generateQR();
                    }
                });
        currency_amount_selector = (LinearLayout) rootView.findViewById(R.id.currency_amount_selector);
        operationSpinner = (Spinner) rootView.findViewById(R.id.operation_spinner);
        amount_text = (EditText) rootView.findViewById(R.id.amount_text);
        List<String> list = new ArrayList<String>();//indices must correspond to OPERATION_ARRAY
        list.add(getString(R.string.currency_ticket_request_operation));
        list.add(getString(R.string.transactionvs_operation));
        ArrayAdapter<String> operationAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        operationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operationSpinner.setAdapter(operationAdapter);
        currencySpinner = (Spinner) rootView.findViewById(R.id.currency_spinner);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_create_lbl));
        btn_plus = (Button) rootView.findViewById(R.id.btn_plus);
        btn_plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                incrementAmount();
            }
        });
        btn_minus = (Button) rootView.findViewById(R.id.btn_minus);
        handler = new Handler();
        btn_minus.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isLongPressed.set(true);
                    decrementAmount();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isLongPressed.set(false);
                    handler.removeCallbacks(decrementRunnable);
                }
                return true;
            };
        });
        btn_plus.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isLongPressed.set(true);
                    incrementAmount();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isLongPressed.set(false);
                    handler.removeCallbacks(incrementRunnable);
                }
                return true;
            };
        });

        if(!appVS.isWithSocketConnection()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.qr_create_lbl), getString(R.string.qr_connection_required_msg),
                    getActivity()).setPositiveButton(getString(R.string.accept_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getActivity().finish();
                        }
                    });
            UIUtils.showMessageDialog(builder);
        }
        return rootView;
    }

    private void generateQR() {
        LOGD(TAG + ".generateQR", "generateQR");
        Integer selectedAmount = Integer.valueOf(amount_text.getText().toString());
        if(selectedAmount <= 0) {
            MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    getString(R.string.min_withdrawal_msg), getFragmentManager());
            return;
        }
        Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
        TransactionVSDto dto = new TransactionVSDto();
        dto.setAmount(new BigDecimal(amount_text.getText().toString()));
        dto.setCurrencyCode(currencySpinner.getSelectedItem().toString());
        dto.setTagVS(new TagVSDto(TagVSDto.WILDTAG));
        intent.putExtra(ContextVS.TRANSACTION_KEY, dto);
        intent.putExtra(ContextVS.FRAGMENT_KEY, QRGeneratorFragment.class.getName());
        startActivity(intent);
    }

    public BigDecimal getValue() {
        BigDecimal result = new BigDecimal(amount_text.getText().toString());
        return result;
    }

    private void incrementAmount() {
        BigDecimal result = getValue().add(new BigDecimal(10));
        amount_text.setText(result.toString());
        if(isLongPressed.get()) handler.postDelayed(incrementRunnable, 200);
    }

    private void decrementAmount() {
        BigDecimal result = getValue().add(new BigDecimal(10).negate());
        if (result.compareTo(new BigDecimal(0)) >= 0) {
            amount_text.setText(result.toString());
        }
        if(isLongPressed.get()) handler.postDelayed(decrementRunnable, 200);
    }

    public class OperationSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            TypeVS selectedOperation = OPERATION_ARRAY[pos];
            switch (selectedOperation) {
                case CURRENCY_CHANGE:

                    break;
                case FROM_USERVS:

                    break;
            }

        }

        @Override public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    }

}

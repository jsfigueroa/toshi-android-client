package com.bakkenbaeck.toshi.view.dialog;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.bakkenbaeck.toshi.R;
import com.bakkenbaeck.toshi.network.rest.model.VerificationSent;
import com.bakkenbaeck.toshi.network.ws.model.VerificationStart;
import com.bakkenbaeck.toshi.network.ws.model.WebSocketError;
import com.bakkenbaeck.toshi.network.ws.model.WebSocketErrors;
import com.bakkenbaeck.toshi.util.LocaleUtil;
import com.bakkenbaeck.toshi.util.OnNextSubscriber;
import com.bakkenbaeck.toshi.view.BaseApplication;
import com.hbb20.CountryCodePicker;

import java.util.Locale;

import rx.Subscriber;

public class PhoneInputDialog extends DialogFragment {

    private String inputtedPhoneNumber;
    private Listener listener;
    private View view;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface Listener {
        void onPhoneInputSuccess(final PhoneInputDialog dialog);
    }

    public String getInputtedPhoneNumber() {
        return this.inputtedPhoneNumber;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement PhoneInputDialog.Listener");
        }

        BaseApplication.get().getSocketObservables().getErrorObservable().subscribe(generateErrorObservable());
        BaseApplication.get().getSocketObservables().getVerificationSentObservable().subscribe(generateVerificationSentObservable());
    }

    private Subscriber<WebSocketError> generateErrorObservable() {
        return new OnNextSubscriber<WebSocketError>() {
            @Override
            public void onNext(final WebSocketError webSocketError) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        view.findViewById(R.id.spinner_view).setVisibility(View.INVISIBLE);
                        setErrorOnPhoneField(webSocketError);
                    }
                });
            }
        };
    }

    private Subscriber<VerificationSent> generateVerificationSentObservable() {
        return new OnNextSubscriber<VerificationSent>() {
            @Override
            public void onNext(final VerificationSent verificationSent) {
                listener.onPhoneInputSuccess(PhoneInputDialog.this);
                dismiss();
            }
        };
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.DialogTheme));
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        this.view = inflater.inflate(R.layout.dialog_phone_input, null);
        builder.setView(this.view);
        initViews(this.view);

        final Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private void initViews(final View view) {
        final Locale currentLocale = LocaleUtil.getLocale();
        ((CountryCodePicker)view.findViewById(R.id.country_code)).setCountryForNameCode(currentLocale.getCountry());
        view.findViewById(R.id.cancelButton).setOnClickListener(this.dismissDialog);
        view.findViewById(R.id.continueButton).setOnClickListener(new ValidateAndContinueDialog(view));
    }

    private final View.OnClickListener dismissDialog = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            dismiss();
        }
    };

    private class ValidateAndContinueDialog implements View.OnClickListener {

        private final View view;

        private ValidateAndContinueDialog(final View view) {
            this.view = view;
        }

        @Override
        public void onClick(final View v) {
            final EditText phoneNumberField = (EditText) this.view.findViewById(R.id.phone_number);
            if (TextUtils.isEmpty(phoneNumberField.getText())) {
                setErrorOnPhoneField();
                return;
            }

            final String countryCode = ((CountryCodePicker)view.findViewById(R.id.country_code)).getSelectedCountryCodeWithPlus();
            inputtedPhoneNumber = countryCode + phoneNumberField.getText();

            final VerificationStart vsFrame = new VerificationStart(inputtedPhoneNumber);
            BaseApplication.get().sendWebSocketMessage(vsFrame.toString());

            this.view.findViewById(R.id.spinner_view).setVisibility(View.VISIBLE);
        }
    }

    private void setErrorOnPhoneField() {
        setErrorOnPhoneField(null);
    }

    private void setErrorOnPhoneField(final WebSocketError error) {
        final EditText phoneNumberField = (EditText) this.view.findViewById(R.id.phone_number);
        phoneNumberField.requestFocus();

        if (error != null && error.getCode().equals(WebSocketErrors.phone_number_already_in_use)) {
            phoneNumberField.setError(getString(R.string.error__phone_number_in_use));
        } else {
            phoneNumberField.setError(getString(R.string.error__invalid_phone_number));
        }
    }



}

package com.fsck.k9.activity.compose;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.view.CryptoModeSelector;
import com.fsck.k9.view.CryptoModeSelector.CryptoModeSelectorState;
import com.fsck.k9.view.CryptoModeSelector.CryptoStatusSelectedListener;
import com.fsck.k9.view.LinearViewAnimator;
import com.fsck.k9.view.OpenPgpKeyStatusPresenter;
import com.fsck.k9.view.OpenPgpKeyStatusPresenter.OpenPgpKeySelectMvpView;
import com.fsck.k9.view.ToolableViewAnimator;


public class CryptoSettingsDialog extends DialogFragment implements CryptoStatusSelectedListener{
    private static final String ARG_CURRENT_MODE = "current_mode";


    private CryptoModeSelector cryptoModeSelector;
    private LinearViewAnimator cryptoStatusText;
    private ToolableViewAnimator openPgpSelectView;
    private TextView openPgpSelectKeyName;

    private CryptoMode currentMode;
    private OpenPgpKeyStatusPresenter openPgpKeyStatusPresenter;


    public static CryptoSettingsDialog newInstance(CryptoMode initialMode) {
        CryptoSettingsDialog dialog = new CryptoSettingsDialog();

        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_MODE, initialMode.toString());
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = savedInstanceState != null ? savedInstanceState : getArguments();
        currentMode = CryptoMode.valueOf(arguments.getString(ARG_CURRENT_MODE));

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.crypto_settings_dialog, null);
        cryptoModeSelector = (CryptoModeSelector) view.findViewById(R.id.crypto_status_selector);
        cryptoStatusText = (LinearViewAnimator) view.findViewById(R.id.crypto_status_text);
        openPgpSelectView = (ToolableViewAnimator) view.findViewById(R.id.crypto_select_view);
        openPgpSelectKeyName = (TextView) view.findViewById(R.id.crypto_key_name);

        cryptoModeSelector.setCryptoStatusListener(this);
        openPgpKeyStatusPresenter.setView(openPgpKeySelectMvpView);

        updateView(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setNegativeButton(R.string.crypto_settings_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.crypto_settings_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                changeCryptoSettings();
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof MessageCompose)) {
            throw new IllegalArgumentException("CryptoSettingsDialog can only be attached to MessageCompose activity!");
        }

        MessageCompose messageComposeActivity = (MessageCompose) context;
        openPgpKeyStatusPresenter = messageComposeActivity.createOpenPgpKeyStatusPresenterForView();
    }

    @Override
    public void onDetach() {
        openPgpKeyStatusPresenter = null;
        super.onDetach();
    }

    private void changeCryptoSettings() {
        CryptoSettingsDialogListener cryptoSettingsDialogListener = getCryptoSettingsDialogListener();
        if (cryptoSettingsDialogListener == null) {
            return;
        }
        cryptoSettingsDialogListener.onCryptoModeChanged(currentMode);
    }

    @Nullable
    private CryptoSettingsDialogListener getCryptoSettingsDialogListener() {
        Activity activity = getActivity();
        if (activity == null) {
            // is this supposed to happen?
            return null;
        }
        boolean activityIsCryptoModeChangedListener = activity instanceof CryptoSettingsDialogListener;
        if (!activityIsCryptoModeChangedListener) {
            throw new AssertionError("This dialog must be called by an OnCryptoModeChangedListener!");
        }

        return (CryptoSettingsDialogListener) activity;
    }

    void updateView(boolean animate) {
        switch (currentMode) {
            case DISABLE:
                cryptoModeSelector.setCryptoStatus(CryptoModeSelectorState.DISABLED);
                cryptoStatusText.setDisplayedChild(0, animate);
                break;
            case SIGN_ONLY:
                throw new IllegalStateException("This state can't be set here!");
            case OPPORTUNISTIC:
                cryptoModeSelector.setCryptoStatus(CryptoModeSelectorState.OPPORTUNISTIC);
                cryptoStatusText.setDisplayedChild(1, animate);
                break;
            case PRIVATE:
                cryptoModeSelector.setCryptoStatus(CryptoModeSelectorState.PRIVATE);
                cryptoStatusText.setDisplayedChild(2, animate);
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_CURRENT_MODE, currentMode.toString());
    }

    @Override
    public void onCryptoStatusSelected(CryptoModeSelectorState status) {
        switch (status) {
            case DISABLED:
                currentMode = CryptoMode.DISABLE;
                break;
            case SIGN_ONLY:
                throw new IllegalStateException("This widget doesn't support sign-only state!");
            case OPPORTUNISTIC:
                currentMode = CryptoMode.OPPORTUNISTIC;
                break;
            case PRIVATE:
                currentMode = CryptoMode.PRIVATE;
                break;
        }
        updateView(true);
    }

    private OpenPgpKeySelectMvpView openPgpKeySelectMvpView = new OpenPgpKeySelectMvpView() {

        @Override
        public void setOpenPgpSelectViewStatus(int displayedChild, String displayUserId) {
            if (displayUserId != null) {
                openPgpSelectKeyName.setText(displayUserId);
            } else {
                openPgpSelectKeyName.setText(R.string.crypto_key_ok_unknown);
            }
            openPgpSelectView.setDisplayedChild(displayedChild);
        }

        @Override
        public void setOnClickListener(View.OnClickListener onClickListener) {
            for (int i = 0, j = openPgpSelectView.getChildCount(); i < j; i++) {
                View childView = openPgpSelectView.getChildAt(i);
                View clickableView = childView.findViewById(R.id.crypto_key_change);
                if (clickableView != null) {
                    clickableView.setOnClickListener(onClickListener);
                } else {
                    childView.setOnClickListener(onClickListener);
                }
            }
        }

    };

    public interface CryptoSettingsDialogListener {
        void onCryptoModeChanged(CryptoMode cryptoMode);
    }

}

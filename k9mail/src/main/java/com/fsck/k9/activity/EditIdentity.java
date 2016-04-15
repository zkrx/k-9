package com.fsck.k9.activity;


import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;


public class EditIdentity extends K9Activity {
    private static final String ARG_IDENTITY = "identity";
    private static final String ARG_IDENTITY_INDEX = "index";
    private static final String ARG_ACCOUNT = "account";


    private Account account;
    private Identity identity;
    private int identityIndex;


    private EditText mDescriptionView;
    private CheckBox mSignatureUse;
    private EditText mSignatureView;
    private LinearLayout mSignatureLayout;
    private EditText mEmailView;
//  private EditText mAlwaysBccView;
    private EditText mNameView;
    private EditText mReplyTo;


    public static Intent newInstance(Context context, String accountUuid) {
        return newInstance(context, null, -1, accountUuid);
    }

    public static Intent newInstance(Context context, Identity identity, int index, String accountUuid) {
        Intent frag = new Intent(context, EditIdentity.class);

        frag.putExtra(EditIdentity.ARG_IDENTITY, identity);
        frag.putExtra(EditIdentity.ARG_IDENTITY_INDEX, index);
        frag.putExtra(EditIdentity.ARG_ACCOUNT, accountUuid);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getIntent().getExtras();

        identity = (Identity) args.getSerializable(ARG_IDENTITY);
        identityIndex = args.getInt(ARG_IDENTITY_INDEX, -1);
        String accountUuid = args.getString(ARG_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);

        if (identityIndex == -1) {
            identity = new Identity();
        }

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_IDENTITY)) {
            identity = (Identity)savedInstanceState.getSerializable(ARG_IDENTITY);
        }

        setContentView(R.layout.edit_identity);

        mDescriptionView = (EditText) findViewById(R.id.description);
        mDescriptionView.setText(identity.getDescription());

        mNameView = (EditText) findViewById(R.id.name);
        mNameView.setText(identity.getName());

        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(identity.getEmail());

        mReplyTo = (EditText) findViewById(R.id.reply_to);
        mReplyTo.setText(identity.getReplyTo());

//      mAccountAlwaysBcc = (EditText)findViewById(R.id.bcc);
//      mAccountAlwaysBcc.setText(mIdentity.getAlwaysBcc());

        mSignatureLayout = (LinearLayout) findViewById(R.id.signature_layout);
        mSignatureUse = (CheckBox) findViewById(R.id.signature_use);
        mSignatureView = (EditText) findViewById(R.id.signature);
        mSignatureUse.setChecked(identity.getSignatureUse());
        mSignatureUse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSignatureLayout.setVisibility(View.VISIBLE);
                    mSignatureView.setText(identity.getSignature());
                } else {
                    mSignatureLayout.setVisibility(View.GONE);
                }
            }
        });

        if (mSignatureUse.isChecked()) {
            mSignatureView.setText(identity.getSignature());
        } else {
            mSignatureLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        saveIdentity();
        super.onBackPressed();
    }

    private void saveIdentity() {
        identity.setDescription(mDescriptionView.getText().toString());
        identity.setEmail(mEmailView.getText().toString());
        //      identity.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());
        identity.setName(mNameView.getText().toString());
        identity.setSignatureUse(mSignatureUse.isChecked());
        identity.setSignature(mSignatureView.getText().toString());

        if (mReplyTo.getText().length() == 0) {
            identity.setReplyTo(null);
        } else {
            identity.setReplyTo(mReplyTo.getText().toString());
        }

        List<Identity> identities = account.getIdentities();
        if (identityIndex == -1) {
            identities.add(identity);
        } else {
            identities.remove(identityIndex);
            identities.add(identityIndex, identity);
        }

        account.save(Preferences.getPreferences(getApplication().getApplicationContext()));

        finish();
    }
}

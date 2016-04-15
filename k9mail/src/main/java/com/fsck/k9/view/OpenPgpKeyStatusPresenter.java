/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9.view;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Address;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;


public class OpenPgpKeyStatusPresenter implements OnClickListener {
    private static final int REQUEST_CODE_CHECK = 1;
    private static final int REQUEST_CODE_USER_INTERACTION = 2;


    private final Context context;
    private final OpenPgpKeyStatusPendingIntentCallback pendingIntentCallback;
    private final Identity identity;


    private OpenPgpKeySelectMvpView view;

    private String mDefaultUserId;

    private State currentState;
    private String cryptoProvider;
    private OpenPgpServiceConnection openPgpServiceConnection;
    private PendingIntent pendingUserInteractionIntent;


    private enum State {
        PROVIDER_SEARCHING (0),
        PROVIDER_UNCONFIGURED(1),
        PROVIDER_ERROR (2),
        KEY_UNCONFIGURED (3),
        KEY_DISABLED (4),
        KEY_ERROR (5),
        KEY_OK (6),
        ;

        final int displayChildId;

        State(int displayChildId) {
            this.displayChildId = displayChildId;
        }
    }

    public OpenPgpKeyStatusPresenter(
            Context context, OpenPgpKeyStatusPendingIntentCallback pendingIntentCallback, Identity identity) {
        this.context = context;
        this.pendingIntentCallback = pendingIntentCallback;
        this.identity = identity;

        setupCryptoProvider();
    }

    public OpenPgpKeyStatusPresenter(
            Context context, Bundle savedInstanceState, OpenPgpKeyStatusPendingIntentCallback pendingIntentCallback, Identity identity) {
        this.context = context;
        this.pendingIntentCallback = pendingIntentCallback;
        this.identity = identity;

        setupCryptoProvider();
    }

    public void setView(OpenPgpKeySelectMvpView view) {
        this.view = view;

        view.setOnClickListener(this);
    }

    private void setupCryptoProvider() {
        String cryptoProvider = K9.getCryptoProvider();

        boolean providerIsBound = openPgpServiceConnection != null && openPgpServiceConnection.isBound();
        boolean isSameProvider = cryptoProvider != null && cryptoProvider.equals(this.cryptoProvider);
        if (isSameProvider && providerIsBound) {
            cryptoProviderBindOrCheckPermission();
            return;
        }

        if (providerIsBound) {
            openPgpServiceConnection.unbindFromService();
            openPgpServiceConnection = null;
        }

        this.cryptoProvider = cryptoProvider;

        if (cryptoProvider == null) {
            currentState = State.PROVIDER_UNCONFIGURED;
            return;
        }

        currentState = State.PROVIDER_SEARCHING;
        openPgpServiceConnection = new OpenPgpServiceConnection(context, cryptoProvider, new OnBound() {
            @Override
            public void onBound(IOpenPgpService2 service) {
                cryptoProviderBindOrCheckPermission();
            }

            @Override
            public void onError(Exception e) {
                onCryptoProviderError(e);
            }
        });
        cryptoProviderBindOrCheckPermission();
    }

    private void cryptoProviderBindOrCheckPermission() {
        if (openPgpServiceConnection == null) {
            currentState = State.PROVIDER_UNCONFIGURED;
            return;
        }

        if (!openPgpServiceConnection.isBound()) {
            pendingUserInteractionIntent = null;
            openPgpServiceConnection.bindToService();
            return;
        }

        Intent intent = new Intent(OpenPgpApi.ACTION_CHECK_IDENTITY);
        intent.putExtra(OpenPgpApi.EXTRA_API_IDENTITY, identity.getEmail());
        getOpenPgpApi().executeApiAsync(intent, null, null, new MyCallback(REQUEST_CODE_CHECK));
    }

    private void launchUserInteractionPendingIntent() {
        pendingIntentCallback.launchCryptoSettingsDialogPendingIntent(pendingUserInteractionIntent, REQUEST_CODE_USER_INTERACTION);
        pendingUserInteractionIntent = null;
    }

    private void onCryptoProviderError(Exception e) {
        Log.e(K9.LOG_TAG, "error connecting to crypto provider!", e);
        updateViewState(State.PROVIDER_ERROR);
    }

    private void onPgpPermissionCheckResult(Intent result) {
        int resultCode = result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
        switch (resultCode) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                pendingUserInteractionIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                String userId = result.getStringExtra(OpenPgpApi.RESULT_USER_ID);
                String displayUserId = Address.getDisplayNameFromAddress(userId);
                updateViewState(State.KEY_OK, displayUserId);
                break;

            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                pendingUserInteractionIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                updateViewState(State.PROVIDER_ERROR);
                break;

            case OpenPgpApi.RESULT_CODE_ERROR:
            default:
                pendingUserInteractionIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                switch (error.getErrorId()) {
                    case OpenPgpError.KEY_ERROR:
                        updateViewState(State.KEY_ERROR);
                        break;
                    case OpenPgpError.IDENTITY_KEY_NOT_CONFIGURED:
                        updateViewState(State.KEY_UNCONFIGURED);
                        break;
                    case OpenPgpError.IDENTITY_DISABLED:
                        updateViewState(State.KEY_DISABLED);
                        break;
                    default:
                        updateViewState(State.PROVIDER_ERROR);
                        break;
                }
                break;
        }
    }

    public void setDefaultUserId(String userId) {
        mDefaultUserId = userId;
    }

    @Override
    public void onClick(View view) {
        if (pendingUserInteractionIntent != null) {
            launchUserInteractionPendingIntent();
            return;
        }

        switch (currentState) {
            case PROVIDER_UNCONFIGURED:
                // TODO
                break;

            case PROVIDER_ERROR:
                setupCryptoProvider();
                break;

            case KEY_OK:
            case KEY_UNCONFIGURED:
            case KEY_ERROR:
                // nothing to do here?
                break;
        }
    }

    private OpenPgpApi getOpenPgpApi() {
        if (openPgpServiceConnection == null || !openPgpServiceConnection.isBound()) {
            Log.e(K9.LOG_TAG, "obtained openpgpapi object, but service is not bound! inconsistent state?");
        }
        return new OpenPgpApi(context, openPgpServiceConnection.getService());
    }

    private void updateViewState(State newState) {
        updateViewState(newState, null);
    }

    private void updateViewState(State newState, String userId) {
        currentState = newState;
        view.setOpenPgpSelectViewStatus(currentState.displayChildId, userId);
    }

    private class MyCallback implements OpenPgpApi.IOpenPgpCallback {
        int requestCode;

        private MyCallback(int requestCode) {
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            switch (requestCode) {
                case REQUEST_CODE_CHECK:
                    onPgpPermissionCheckResult(result);
                    break;
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        setupCryptoProvider();
    }

    public interface OpenPgpKeySelectMvpView {
        void setOpenPgpSelectViewStatus(int displayedChild, String userId);
        void setOnClickListener(OnClickListener onClickListener);
    }

    public interface OpenPgpKeyStatusPendingIntentCallback {
        void launchCryptoSettingsDialogPendingIntent(PendingIntent pendingIntent, int requestCode);
    }
}
package ch.epfl.bogocoders.bogocoffee;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.PromptBehavior;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PowerBi {

    private Activity mActivity;
    private Handler mAcquireTokenHandler;
    private AuthenticationContext mAuthContext;
    private AuthenticationResult mAuthResult;

    private static AtomicBoolean sIntSignInInvoked = new AtomicBoolean();
    private static final int MSG_INTERACTIVE_SIGN_IN_PROMPT_AUTO = 1;
    private static final int MSG_INTERACTIVE_SIGN_IN_PROMPT_ALWAYS = 2;

    private static final String url_capsule = "https://api.powerbi.com/v1.0/myorg/groups/f465833c-2d2c-40a0-ab99-955c04908d50/datasets/d4753956-95e1-4f84-9869-56a91917c228/tables/Capsule/rows";
    private static final String url_tank = "https://api.powerbi.com/v1.0/myorg/groups/f465833c-2d2c-40a0-ab99-955c04908d50/datasets/d4753956-95e1-4f84-9869-56a91917c228/tables/Tank/rows";
    private static final String url_container = "https://api.powerbi.com/v1.0/myorg/groups/f465833c-2d2c-40a0-ab99-955c04908d50/datasets/d4753956-95e1-4f84-9869-56a91917c228/tables/Container/rows";

    private static final String json_capsule = "{\"rows\": [{\"CoffeeType\": \"%s\", \"Quantity\":%d}]}";
    private static final String json_tank = "{\"rows\": [{\"ml\": %d}]}";
    private static final String json_container = "{\"rows\": [{\"UsedCapsule\": %d}]}";

    private static final String clientID = "68b7a579-f21f-409d-9620-0a14ea65e814";
    private static final String redirectUri = "https://dev.powerbi.com/Apps/SignInRedirect";
    private static final String resourceUri = "https://analysis.windows.net/powerbi/api";
    private static final String authorityUri = "https://login.windows.net/common/oauth2/authorize";

    private static final String LOG_TAG = "PowerBi";

    public PowerBi(Activity activity) {
        this.mActivity = activity;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }

    public void connectToPowerBi() {
        Logger.getInstance().setExternalLogger(new Logger.ILogger() {
            @Override
            public void Log(String tag, String message, String additionalMessage,
                            Logger.LogLevel level, ADALError errorCode) {
                Log.e(LOG_TAG, message + " " + additionalMessage);
            }
        });

        mAuthContext = new AuthenticationContext(mActivity.getApplicationContext(),
                authorityUri,false);

        mAcquireTokenHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if( sIntSignInInvoked.compareAndSet(false, true)) {
                    if (msg.what == MSG_INTERACTIVE_SIGN_IN_PROMPT_AUTO){
                        mAuthContext.acquireToken(mActivity, resourceUri, clientID, redirectUri, PromptBehavior.Auto, getAuthInteractiveCallback());
                    }else if(msg.what == MSG_INTERACTIVE_SIGN_IN_PROMPT_ALWAYS){
                        mAuthContext.acquireToken(mActivity, resourceUri, clientID, redirectUri, PromptBehavior.Always, getAuthInteractiveCallback());
                    }
                }
            }
        };
        mAcquireTokenHandler.sendEmptyMessage(MSG_INTERACTIVE_SIGN_IN_PROMPT_AUTO);
    }

    public void sendToPowerBi(String coffeetype, int coffeeSize) {
        new Thread(new Runnable() {

            @Override
            public void run() {

                OkHttpClient client = new OkHttpClient();

                String json = String.format(Locale.FRANCE, json_tank,  300);
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

                Request request = new Request.Builder()
                        .url(url_tank)
                        .addHeader("Authorization", "Bearer " + mAuthResult.getAccessToken())
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();

                Response response = null;
                try {
                    Call call = client.newCall(request);
                    response = call.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (response == null) {
                    Log.e(LOG_TAG, "Unable to upload to server. (null)");
                } else if(!response.isSuccessful()) {
                    Log.e(LOG_TAG, "Unable to upload to server. (not Successful)");
                } else {
                    Log.e(LOG_TAG, "Upload was successful.");
                }
            }
        }).start();
    }

    private AuthenticationCallback<AuthenticationResult> getAuthInteractiveCallback() {
        return new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                if (authenticationResult == null || TextUtils.isEmpty(authenticationResult.getAccessToken())
                        || authenticationResult.getStatus() != AuthenticationResult.AuthenticationStatus.Succeeded) {
                    Log.e(LOG_TAG, "Authentication Result is invalid");
                    mAcquireTokenHandler.sendEmptyMessage(MSG_INTERACTIVE_SIGN_IN_PROMPT_AUTO);
                    return;
                }
                /* Successfully got a token, call graph now */
                Log.e(LOG_TAG, "Successfully authenticated");
                Log.e(LOG_TAG, "ID Token: " + authenticationResult.getIdToken());
                Log.e(LOG_TAG, "Access Token: " + authenticationResult.getAccessToken());
                mAuthResult = authenticationResult;
                sIntSignInInvoked.set(false);
                sendToPowerBi("", 1);
            }

            @Override
            public void onError(Exception exc) {
                Log.e(LOG_TAG, "Authentication failed: " + exc.toString());
                sIntSignInInvoked.set(false);
            }
        };
    }
}

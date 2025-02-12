package com.example.nuvolapos;

import static com.clover.sdk.internal.util.BitmapUtils.TAG;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.clover.remote.Challenge;
import com.clover.remote.client.CloverConnector;
import com.clover.remote.client.DefaultCloverConnectorListener;
import com.clover.remote.client.ICloverConnector;
import com.clover.remote.client.messages.ConfirmPaymentRequest;
import com.clover.remote.client.messages.SaleRequest;
import com.clover.remote.client.messages.SaleResponse;
import com.clover.remote.client.messages.VerifySignatureRequest;
import com.clover.sdk.v3.payments.Payment;

import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private SaleRequest pendingSale;

    private Account cloverAccount;
//    private static final String TARGET_URL = "http://10.0.2.2:3000/";
//     private static final String TARGET_URL = "https://nuvolapos.vercel.app/";
     private static final String TARGET_URL = "http://192.168.0.69:3000/";
//     private static final String TARGET_URL = "https://nuvolapos-git-page-cache-android-scott-microtillcos-projects.vercel.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize WebView
        webView = findViewById(R.id.webView);

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);


        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);


        webSettings.setDatabaseEnabled(true);

        // Custom WebViewClient to handle offline mode
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (!isNetworkAvailable()) {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                }
                webView.loadUrl(TARGET_URL);
            }
        });

        // Set initial cache mode based on network availability
        if (!isNetworkAvailable()) {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }

        // Load the URL
        webView.loadUrl(TARGET_URL);

        // Handle system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add JavaScript interface with WebView reference
        webView.addJavascriptInterface(new AndroidBridge(webView), "AndroidBridge");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    public class AndroidBridge {
        private static final String TAG = "AndroidBridge";
        private WebView webView;
        private static final String CLOVER_CLOUD_URL = "https://sandbox.dev.clover.com/connect/v1/payments";
        private static final String AUTH_TOKEN = "Bearer c513d90d-b1a6-35d2-28fd-84d9c50ddee1";

        public AndroidBridge(WebView webView) {
            this.webView = webView;
        }

        @JavascriptInterface
        public void printData(final String printData, final String printerIp) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                boolean success = false;
                try {
                    System.out.println("printerIp " + printerIp);
                    // Use the provided printer IP, default port is 9100
                    Socket socket = new Socket(printerIp, 9100);
                    OutputStreamWriter writer = new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8
                    );

                    // Send the print data
                    writer.write(printData);
                    writer.flush();

                    // Close the connections
                    writer.close();
                    socket.close();
                    success = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final boolean finalSuccess = success;

                // Update UI on main thread
                handler.post(() -> {
                    if (finalSuccess) {
                        Toast.makeText(MainActivity.this,
                                "Print job sent successfully to " + printerIp,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Failed to send print job to " + printerIp,
                                Toast.LENGTH_SHORT).show();
                    }
                });

                // Shutdown the executor
                executor.shutdown();
            });
        }

        @JavascriptInterface
        public void openCashDrawerRJ11(final String type) {
            runOnUiThread(() -> {
                try {
                    // Send broadcast intent to open cash drawer
                    Intent intent = new Intent("android.intent.action.CASHBOX");
                    intent.putExtra("cashbox_open", true);
                    sendBroadcast(intent);

                    Toast.makeText(MainActivity.this,
                            "Cash drawer command sent",
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,
                            "Failed to open cash drawer: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void processCloverCloudPayment(final String jsonData) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                try {
                    // Parse input JSON
                    JSONObject inputData = new JSONObject(jsonData);
                    double amount = inputData.getDouble("amount");
                    String orderId = inputData.getString("orderId");

                    // Setup HTTP client
                    OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                    // Prepare request body
                    JSONObject deviceOptions = new JSONObject();
                    deviceOptions.put("disableCashback", false);
                    deviceOptions.put("cardEntryMethods", new JSONArray()
                        .put("MAG_STRIPE")
                        .put("EMV")
                        .put("NFC"));
                    deviceOptions.put("cardNotPresent", false);

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("capture", true);
                    jsonBody.put("deviceOptions", deviceOptions);
                    jsonBody.put("amount", String.valueOf((int)(amount * 100)));
                    jsonBody.put("externalPaymentId", orderId);
                    jsonBody.put("externalReferenceId", orderId);

                    // Create request
                    MediaType mediaType = MediaType.parse("application/json");
                    RequestBody body = RequestBody.create(mediaType, jsonBody.toString());

                    Request request = new Request.Builder()
                        .url(CLOVER_CLOUD_URL)
                        .post(body)
                        .addHeader("accept", "application/json")
                        .addHeader("X-Clover-Device-Id", getCloverDeviceSerial())
                        .addHeader("X-POS-Id", orderId)
                        .addHeader("Idempotency-Key", orderId)
                        .addHeader("content-type", "application/json")
                        .addHeader("authorization", AUTH_TOKEN)
                        .build();

                    // Execute request
                    okhttp3.Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    JSONObject responseJson = new JSONObject(responseData);

                    // Handle response on main thread
                    handler.post(() -> {
                        try {
                            JSONObject paymentJson = responseJson.optJSONObject("payment");
                            if (paymentJson != null && "SUCCESS".equalsIgnoreCase(paymentJson.optString("result", ""))) {
                                // Success response
                                JSONObject successResponse = new JSONObject();
                                successResponse.put("success", true);
                                successResponse.put("orderId", orderId);
                                successResponse.put("amount", amount);
                                successResponse.put("paymentId", paymentJson.optString("id"));
                                
                                // Call window.postMessage instead of direct function call
                                String jsCode = "window.postMessage(" + 
                                    successResponse.toString() + 
                                    ", '*');";
                                
                                webView.evaluateJavascript(jsCode, null);
                                
                                Toast.makeText(MainActivity.this, 
                                    "Payment successful: $" + String.format("%.2f", amount), 
                                    Toast.LENGTH_SHORT).show();
                            } else {
                                // Failure response
                                JSONObject failureResponse = new JSONObject();
                                failureResponse.put("success", false);
                                failureResponse.put("reason", "Transaction failed");
                                
                                // Call window.postMessage instead of direct function call
                                String jsCode = "window.postMessage(" + 
                                    failureResponse.toString() + 
                                    ", '*');";
                                
                                webView.evaluateJavascript(jsCode, null);
                                
                                Toast.makeText(MainActivity.this, 
                                    "Payment failed", 
                                    Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error processing response", e);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error processing payment", e);
                    handler.post(() -> {
                        try {
                            JSONObject errorResponse = new JSONObject();
                            errorResponse.put("success", false);
                            errorResponse.put("reason", "Error: " + e.getMessage());
                            
                            // Call window.postMessage instead of direct function call
                            String jsCode = "window.postMessage(" + 
                                errorResponse.toString() + 
                                ", '*');";
                            
                            webView.evaluateJavascript(jsCode, null);
                            
                            Toast.makeText(MainActivity.this, 
                                "Payment error: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        } catch (JSONException jsonEx) {
                            Log.e(TAG, "Error creating error response", jsonEx);
                        }
                    });
                }
            });

            executor.shutdown();
        }

        private String getCloverDeviceSerial() {
            // Get the stored Clover device serial from SharedPreferences
            SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences(
                "CloverPrefs", Context.MODE_PRIVATE);
            return sharedPreferences.getString("clover_device_serial", "");
        }
    }

    private final class SaleListener extends DefaultCloverConnectorListener {
        public SaleListener(ICloverConnector cloverConnector) {
            super(cloverConnector);
        }

        @Override
        public void onConfirmPaymentRequest(ConfirmPaymentRequest request) {
            Log.d(TAG,"Confirm Payment Request");

            Challenge[] challenges = request.getChallenges();
            if (challenges != null && challenges.length > 0)
            {
                for (Challenge challenge : challenges) {
                    Log.d(TAG,"Received a challenge: " + challenge.type);
                }
            }

            Log.d(TAG, "Automatically processing challenges");
            cloverConnector.acceptPayment(request.getPayment());
        }

        @Override
        public void onSaleResponse(SaleResponse response) {
            try {
                if (response.isSuccess()) {
                    Payment payment = response.getPayment();
                    if (payment.getExternalPaymentId().equals(pendingSale.getExternalId())) {
                        String saleResult = "Sale Successful\n" +
                            "Order ID: " + payment.getExternalPaymentId() + "\n" +
                            "Amount: $" + String.format("%.2f", (double) pendingSale.getAmount() / 100.0) + "\n" +
                            "Card Type: " + payment.getCardTransaction().getCardType() + "\n" +
                            "Last 4: " + payment.getCardTransaction().getLast4();
                        
                        Log.d(TAG, "sales result: " + saleResult);
                        Toast.makeText(MainActivity.this, saleResult, Toast.LENGTH_LONG).show();
                        
                        // Create success response JSON
                        JSONObject successResponse = new JSONObject();
                        try {
                            successResponse.put("success", true);
                            successResponse.put("orderId", payment.getExternalPaymentId());
                            successResponse.put("amount", (double) pendingSale.getAmount() / 100.0);
                            successResponse.put("cardType", payment.getCardTransaction().getCardType());
                            successResponse.put("last4", payment.getCardTransaction().getLast4());
                            
                            webView.evaluateJavascript(
                                "javascript:onPaymentSuccess(" + successResponse.toString() + ")", 
                                null
                            );
                        } catch (JSONException e) {
                            Log.d(TAG, "Error creating success JSON", e);
                        }
                    }
                } else {
                    String failureMessage = "Sale Failed - " + response.getReason();
                    Log.d(TAG, failureMessage);
                    Toast.makeText(MainActivity.this, failureMessage, Toast.LENGTH_SHORT).show();
                    
                    // Create failure response JSON
                    JSONObject failureResponse = new JSONObject();
                    try {
                        failureResponse.put("success", false);
                        failureResponse.put("reason", response.getReason());
                        
                        webView.evaluateJavascript(
                            "javascript:onPaymentFailure(" + failureResponse.toString() + ")", 
                            null
                        );
                    } catch (JSONException e) {
                        Log.d(TAG, "Error creating failure JSON", e);
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Error handling sale response", e);
                
                // Handle unexpected errors
                try {
                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("success", false);
                    errorResponse.put("reason", "Unexpected error: " + e.getMessage());
                    
                    webView.evaluateJavascript(
                        "javascript:onPaymentFailure(" + errorResponse.toString() + ")", 
                        null
                    );
                } catch (JSONException jsonEx) {
                    Log.d(TAG, "Error creating error JSON", jsonEx);
                }
            }
        }

        @Override
        public void onVerifySignatureRequest(VerifySignatureRequest request) {
            super.onVerifySignatureRequest(request);
            Log.d(TAG,"Verify Signature Request - Signature automatically accepted by default");
        }
    }

}
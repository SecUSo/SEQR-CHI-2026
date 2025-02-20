package com.secuso.privacyfriendlycodescanner.qrscanner.ui.resultfragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.zxing.client.result.URIParsedResult;
import com.secuso.privacyfriendlycodescanner.qrscanner.R;
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.Utils;

public class URLResultFragment extends ResultFragment {
    public static final String VALID_RFC3986_PROTOCOL_SCHEME = "^[a-zA-Z][a-zA-Z0-9+.-]*:.*$";

    URIParsedResult result;

    private boolean checked = false;
    private final boolean trust = false;
    private String qrurl;

    public URLResultFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_result_url, container, false);

        result = (URIParsedResult) parsedResult;

        qrurl = result.getURI();

        TextView resultText = (TextView) v.findViewById(R.id.textDomain);
        TextView furtherInfo = (TextView) v.findViewById(R.id.textLink);
        furtherInfo.setMovementMethod(LinkMovementMethod.getInstance());

        Spannable urlWithHighlighting = Utils.getHostHighlightingURI(qrurl, requireContext());

        resultText.setText(urlWithHighlighting);

        // checked = trust = getBoolean("trust", false);

        final CheckBox knowDomain = (CheckBox) v.findViewById(R.id.checkBoxKnowRisks);

        // wenn bereits vertraut wurde, checkbox setzen
        if (trust)
            knowDomain.setChecked(true);

        knowDomain.setOnClickListener(v1 -> checked = knowDomain.isChecked());

        return v;
    }

    public void onProceedPressed(Context context) {
        if (!checked) {
            Toast.makeText(context, R.string.conform_url, Toast.LENGTH_LONG).show();
        } else {
            String urlForIntentData = qrurl;
            if (!qrurl.matches(VALID_RFC3986_PROTOCOL_SCHEME)) {
                urlForIntentData = "http://" + qrurl;
            }
            Intent url = new Intent(Intent.ACTION_VIEW);/// !!!!
            url.setData(Uri.parse(urlForIntentData).normalizeScheme());
            String caption = getResources().getStringArray(R.array.url_array)[0];
            startActivity(Intent.createChooser(url, caption));
        }
    }

    @Override
    public String getProceedButtonTitle(Context context) {
        return context.getString(R.string.action_open);
    }
}

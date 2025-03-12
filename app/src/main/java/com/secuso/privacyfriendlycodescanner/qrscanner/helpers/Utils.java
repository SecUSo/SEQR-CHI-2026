/*
    Privacy Friendly QR Scanner
    Copyright (C) 2020-2025 Privacy Friendly QR Scanner authors and SECUSO

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.secuso.privacyfriendlycodescanner.qrscanner.helpers;

import static com.google.zxing.EncodeHintType.ERROR_CORRECTION;
import static com.google.zxing.ResultMetadataType.ERROR_CORRECTION_LEVEL;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.DrawableRes;

import com.google.common.net.InternetDomainName;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.secuso.privacyfriendlycodescanner.qrscanner.R;
import com.secuso.privacyfriendlycodescanner.qrscanner.database.entities.HistoryItem;

import java.util.EnumMap;
import java.util.Map;

public class Utils {

    public static final int DEFAULT_CODE_WIDTH = 100;
    public static final int DEFAULT_CODE_HEIGHT = 100;
    private static final String TAG = "helpers.Utils";

    private static BarcodeFormat getFormat(BarcodeFormat format) {
        switch (format) {
            //These are the formats supported by MultiFormatWriter. We encode as QR-Code for everything else.
            case EAN_8:
            case UPC_E:
            case EAN_13:
            case UPC_A:
            case QR_CODE:
            case CODE_39:
            case CODE_93:
            case CODE_128:
            case ITF:
            case PDF_417:
            case CODABAR:
            case DATA_MATRIX:
            case AZTEC:
                return format;
            default:
                return BarcodeFormat.QR_CODE;
        }
    }

    @DrawableRes
    public static Integer getBarcodeFormatIcon(BarcodeFormat format) {
        return switch (format) {
            case QR_CODE -> R.drawable.ic_baseline_qr_code_24dp;
            case MAXICODE -> R.drawable.ic_maxicode_24dp;
            case CODABAR, CODE_39, CODE_93, CODE_128, EAN_8, EAN_13, ITF, RSS_14, RSS_EXPANDED, UPC_A, UPC_E, UPC_EAN_EXTENSION -> R.drawable.ic_barcode_24dp;
            case PDF_417 -> R.drawable.ic_pdf_417_code_24dp;
            case DATA_MATRIX -> R.drawable.ic_data_matrix_code_24dp;
            case AZTEC -> R.drawable.ic_aztec_code_24dp;
        };
    }

    public static Bitmap generateCode(String data, BarcodeFormat format, Map<EncodeHintType, Object> hints, Map<ResultMetadataType, Object> metadata) {
        return generateCode(data, format, DEFAULT_CODE_WIDTH, DEFAULT_CODE_HEIGHT, hints, metadata);
    }

    public static Bitmap generateCode(String data, BarcodeFormat format, Map<EncodeHintType, Object> hints) {
        return generateCode(data, format, DEFAULT_CODE_WIDTH, DEFAULT_CODE_HEIGHT, hints, null);
    }

    public static Bitmap generateCode(String data, BarcodeFormat original_format, int imgWidth, int imgHeight, Map<EncodeHintType, Object> hints, Map<ResultMetadataType, Object> metadata) {
        BarcodeFormat format = getFormat(original_format);
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            if (hints == null || !format.equals(original_format)) {
                hints = new EnumMap<>(EncodeHintType.class);
            }
            if (!hints.containsKey(ERROR_CORRECTION) && metadata != null && metadata.containsKey(ERROR_CORRECTION_LEVEL) && format.equals(original_format)) {
                Object ec = metadata.get(ERROR_CORRECTION_LEVEL);
                if (ec != null) {
                    String errorCorrection = ec.toString();
                    errorCorrection = errorCorrection.replace("%", ""); // Sometimes the error correction value contains a percent sign
                    hints.put(ERROR_CORRECTION, errorCorrection);
                }
            }
            if (!hints.containsKey(ERROR_CORRECTION) && format != BarcodeFormat.AZTEC && format != BarcodeFormat.PDF_417) {
                hints.put(ERROR_CORRECTION, ErrorCorrectionLevel.L.name());
            }
            if (!hints.containsKey(EncodeHintType.CHARACTER_SET)) {
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            }
            BitMatrix result = writer.encode(data, format, imgWidth, imgHeight, hints);
            int width = result.getWidth();
            int height = result.getHeight();
            int[] pixels = new int[width * height];
            // All are 0, or black, by default
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }

    public static HistoryItem createHistoryItem(Bitmap mCodeImage, BarcodeResult currentBarcodeResult, boolean prefSaveRealImage) {
        HistoryItem currentHistoryItem = new HistoryItem();

        Bitmap image;
        if (prefSaveRealImage) {
            float height;
            float width;
            if (mCodeImage.getWidth() == 0 || mCodeImage.getWidth() == 0) {
                height = 200f;
                width = 200f;
            } else if (mCodeImage.getWidth() > mCodeImage.getHeight()) {
                height = (float) mCodeImage.getHeight() / (float) mCodeImage.getWidth() * 200f;
                width = 200f;
            } else {
                width = (float) mCodeImage.getWidth() / (float) mCodeImage.getHeight() * 200f;
                height = 200f;
            }
            image = Bitmap.createScaledBitmap(mCodeImage, (int) width, (int) height, false);
        } else {
            image = Utils.generateCode(currentBarcodeResult.getText(), currentBarcodeResult.getBarcodeFormat(), null, currentBarcodeResult.getResult().getResultMetadata());
        }
        currentHistoryItem.setImage(image);

        currentHistoryItem.setFormat(currentBarcodeResult.getResult().getBarcodeFormat());
        currentHistoryItem.setNumBits(currentBarcodeResult.getResult().getNumBits());
        currentHistoryItem.setRawBytes(currentBarcodeResult.getResult().getRawBytes());
        currentHistoryItem.setResultPoints(currentBarcodeResult.getResult().getResultPoints());
        currentHistoryItem.setText(currentBarcodeResult.getResult().getText());
        currentHistoryItem.setTimestamp(currentBarcodeResult.getResult().getTimestamp());

        return currentHistoryItem;
    }

    public static String extractHostFromURI(String uriString) {
        Uri uri = Uri.parse(uriString);
        String host = uri.getHost();
        if (host == null) {
            host = uriString;
        }
        return host;
    }

    public static String extractBaseDomainFromURI(String uriString) {
        String host = extractHostFromURI(uriString);
        try {
            return InternetDomainName.from(host).topDomainUnderRegistrySuffix().toString();
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.d(TAG, "Could not parse URI " + uriString, e);
        }
        return null;
    }

    public static Spannable getHostHighlightingURI(String uriString, Context context) {
        String baseDomain = extractBaseDomainFromURI(uriString);
        if (baseDomain == null) {
            return new SpannableString(uriString);
        }

        int start = uriString.indexOf(baseDomain);
        int end = start + baseDomain.length();

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorURLHighlight, typedValue, true);
        int highlightColor = typedValue.data;

        Spannable resultSpannable = new SpannableString(uriString);
        resultSpannable.setSpan(new ForegroundColorSpan(highlightColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        resultSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return resultSpannable;
    }
}

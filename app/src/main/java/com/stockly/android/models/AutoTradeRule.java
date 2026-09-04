package com.stockly.android.models;

import android.text.TextUtils;

import androidx.annotation.ColorRes;

import com.google.gson.annotations.SerializedName;
import com.stockly.android.R;
import com.stockly.android.utils.CommonUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AutoTradeRule {
    @SerializedName(value = "id", alternate = {"rule_id"})
    public String id;
    public String symbol;
    @SerializedName(value = "notional", alternate = {"amount"})
    public String notional;
    @SerializedName(value = "frequency", alternate = {"cadence", "interval"})
    public String frequency;
    @SerializedName(value = "status", alternate = {"state"})
    public String status;
    public Boolean enabled;
    @SerializedName(value = "side", alternate = {"order_side"})
    public String side;
    @SerializedName(value = "type", alternate = {"order_type"})
    public String type;
    @SerializedName(value = "next_run_at", alternate = {"next_execution_at", "nextExecutionAt"})
    public String nextRunAt;
    @SerializedName(value = "last_run_at", alternate = {"last_execution_at", "lastExecutionAt"})
    public String lastRunAt;
    @SerializedName(value = "last_run_status", alternate = {"last_execution_status", "lastExecutionStatus"})
    public String lastRunStatus;
    @SerializedName(value = "last_run_message", alternate = {"failure_reason", "last_error", "lastError"})
    public String lastRunMessage;

    private static final SimpleDateFormat[] INPUT_FORMATS = new SimpleDateFormat[]{
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    };
    private static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    static {
        for (SimpleDateFormat format : INPUT_FORMATS) {
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        OUTPUT_FORMAT.setTimeZone(TimeZone.getDefault());
    }

    public boolean isPaused() {
        return !Boolean.TRUE.equals(enabled) || "paused".equalsIgnoreCase(status);
    }

    public boolean isActiveRule() {
        return !isPaused() && !"canceled".equalsIgnoreCase(status) && !"cancelled".equalsIgnoreCase(status);
    }

    public String getTitle() {
        return TextUtils.isEmpty(symbol) ? "" : symbol;
    }

    public String getAmountLabel() {
        if (TextUtils.isEmpty(notional)) {
            return "$0.00";
        }
        try {
            return String.format(Locale.getDefault(), "$%s", CommonUtils.round(Double.parseDouble(notional), 2));
        } catch (Exception ignored) {
            return "$" + notional;
        }
    }

    public String getFrequencyLabel() {
        if (TextUtils.isEmpty(frequency)) {
            return "Recurring Buy";
        }
        String normalized = frequency.replace("_", " ").trim().toLowerCase(Locale.getDefault());
        if (TextUtils.isEmpty(normalized)) {
            return "Recurring Buy";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault()) + normalized.substring(1);
    }

    public String getScheduleSummary() {
        return getAmountLabel() + " • " + getFrequencyLabel();
    }

    public String getStatusLabel() {
        if (isPaused()) {
            return "Paused";
        }
        if (TextUtils.isEmpty(status)) {
            return "Scheduled";
        }
        String normalized = status.replace("_", " ").trim().toLowerCase(Locale.getDefault());
        if (TextUtils.isEmpty(normalized)) {
            return "Scheduled";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault()) + normalized.substring(1);
    }

    @ColorRes
    public int getStatusColor() {
        if (isPaused()) {
            return R.color.hintColor;
        }
        if ("failed".equalsIgnoreCase(status)) {
            return R.color.colorError;
        }
        if ("running".equalsIgnoreCase(status)) {
            return R.color.colorPrimary;
        }
        return R.color.greenColor;
    }

    public String getNextRunLabel() {
        String next = formatDisplayDate(nextRunAt);
        return TextUtils.isEmpty(next) ? "Next run: Not scheduled" : "Next run: " + next;
    }

    public String getLastRunLabel() {
        String last = formatDisplayDate(lastRunAt);
        if (TextUtils.isEmpty(last)) {
            return "Last run: Not available";
        }
        if (TextUtils.isEmpty(lastRunStatus)) {
            return "Last run: " + last;
        }
        return "Last run: " + last + " • " + getNormalizedLabel(lastRunStatus);
    }

    public String getLastRunMessageLabel() {
        if (TextUtils.isEmpty(lastRunMessage)) {
            return "";
        }
        return lastRunMessage;
    }

    public boolean hasLastRunMessage() {
        return !TextUtils.isEmpty(lastRunMessage);
    }

    public String getActionLabel() {
        return isPaused() ? "Resume" : "Pause";
    }

    private static String formatDisplayDate(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        for (SimpleDateFormat format : INPUT_FORMATS) {
            try {
                Date parsed = format.parse(value);
                if (parsed != null) {
                    return OUTPUT_FORMAT.format(parsed);
                }
            } catch (ParseException ignored) {
            }
        }
        return value;
    }

    private static String getNormalizedLabel(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String normalized = value.replace("_", " ").trim().toLowerCase(Locale.getDefault());
        if (TextUtils.isEmpty(normalized)) {
            return "";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault()) + normalized.substring(1);
    }
}

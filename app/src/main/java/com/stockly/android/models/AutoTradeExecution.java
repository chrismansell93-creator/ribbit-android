package com.stockly.android.models;

import com.google.gson.annotations.SerializedName;

public class AutoTradeExecution {
    public String id;
    @SerializedName(value = "rule_id", alternate = {"auto_trade_rule_id"})
    public String ruleId;
    public String symbol;
    @SerializedName(value = "notional", alternate = {"amount"})
    public String notional;
    public String status;
    @SerializedName(value = "created_at", alternate = {"submitted_at"})
    public String createdAt;
    @SerializedName(value = "executed_at", alternate = {"completed_at"})
    public String executedAt;
    @SerializedName(value = "message", alternate = {"failure_reason"})
    public String message;
}

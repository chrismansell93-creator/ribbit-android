package com.stockly.android.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.stockly.android.R;
import com.stockly.android.adapter.AutoTradeRulesAdapter;
import com.stockly.android.databinding.FragmentAutoTradeRulesBinding;
import com.stockly.android.models.AutoTradeRule;
import com.stockly.android.models.Positions;
import com.stockly.android.models.RetrofitError;
import com.stockly.android.models.Success;
import com.stockly.android.utils.ActivityUtils;

import java.util.HashMap;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AutoTradeRulesFragment extends NetworkFragment {
    private FragmentAutoTradeRulesBinding mBinding;
    private AutoTradeRulesAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = requireActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_auto_trade_rules;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding = FragmentAutoTradeRulesBinding.bind(view);
        setUpToolBar(mBinding.toolbar.toolbar, true);
        mBinding.toolbar.title.setText(R.string.auto_trades_title);
        mBinding.toolbar.save.setVisibility(View.GONE);
        mBinding.rulesList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mAdapter = new AutoTradeRulesAdapter(getLayoutInflater(), new AutoTradeRulesAdapter.ActionListener() {
            @Override
            public void onRuleClicked(AutoTradeRule rule, int position) {
                openTicker(rule.symbol);
            }

            @Override
            public void onToggleClicked(AutoTradeRule rule, int position) {
                updateRuleState(rule, rule.isPaused());
            }

            @Override
            public void onDeleteClicked(AutoTradeRule rule, int position) {
                deleteRule(rule);
            }
        });
        mBinding.rulesList.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRules();
    }

    private void loadRules() {
        mBinding.progressBar.progressBar.setVisibility(View.VISIBLE);
        enqueue(getApi().getAutoTradeRules(), new CallBack<List<AutoTradeRule>>() {
            @Override
            public void onSuccess(List<AutoTradeRule> autoTradeRules) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                mAdapter.setData(autoTradeRules, true);
                boolean isEmpty = autoTradeRules == null || autoTradeRules.isEmpty();
                mBinding.rulesList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                mBinding.noRecord.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                if (isEmpty) {
                    mBinding.noRecord.setText(R.string.no_auto_trade_rules);
                }
            }

            @Override
            public boolean onError(RetrofitError error, boolean isInternetIssue) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                mBinding.rulesList.setVisibility(View.GONE);
                mBinding.noRecord.setVisibility(View.VISIBLE);
                mBinding.noRecord.setText(R.string.no_auto_trade_rules);
                return true;
            }
        });
    }

    private void updateRuleState(AutoTradeRule rule, boolean enabled) {
        if (rule == null || rule.id == null) {
            return;
        }
        HashMap<String, Object> body = new HashMap<>();
        body.put("enabled", enabled);
        mBinding.progressBar.progressBar.setVisibility(View.VISIBLE);
        enqueue(getApi().updateAutoTradeRule(rule.id, body), new CallBack<AutoTradeRule>() {
            @Override
            public void onSuccess(AutoTradeRule autoTradeRule) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireActivity(), enabled ? R.string.auto_trade_resumed : R.string.auto_trade_paused, Toast.LENGTH_SHORT).show();
                loadRules();
            }

            @Override
            public boolean onError(RetrofitError error, boolean isInternetIssue) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireActivity(), error.message, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void deleteRule(AutoTradeRule rule) {
        if (rule == null || rule.id == null) {
            return;
        }
        mBinding.progressBar.progressBar.setVisibility(View.VISIBLE);
        enqueue(getApi().deleteAutoTradeRule(rule.id), new CallBack<Success>() {
            @Override
            public void onSuccess(Success success) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireActivity(), R.string.auto_trade_deleted, Toast.LENGTH_SHORT).show();
                loadRules();
            }

            @Override
            public boolean onError(RetrofitError error, boolean isInternetIssue) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireActivity(), error.message, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void openTicker(String symbol) {
        if (symbol == null) {
            return;
        }
        mBinding.progressBar.progressBar.setVisibility(View.VISIBLE);
        enqueue(getApi().getAssetsList(symbol), new CallBack<List<Positions>>() {
            @Override
            public void onSuccess(List<Positions> positions) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                if (positions != null) {
                    for (Positions position : positions) {
                        if (position != null && symbol.equalsIgnoreCase(position.symbol)) {
                            Bundle bundle = new Bundle();
                            bundle.putParcelable("asset", position);
                            ActivityUtils.launchFragment(requireActivity(), TickerDetailFragment.class, bundle);
                            return;
                        }
                    }
                }
                Toast.makeText(requireActivity(), R.string.auto_trade_symbol_not_found, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onError(RetrofitError error, boolean isInternetIssue) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireActivity(), error.message, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
}

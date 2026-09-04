package com.stockly.android.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stockly.android.R;
import com.stockly.android.constants.CommonKeys;
import com.stockly.android.databinding.FragmentAutoTradeSetupBinding;
import com.stockly.android.fragments.plaid.BankIntroFragment;
import com.stockly.android.fragments.wallet.AddFundsFragment;
import com.stockly.android.models.AutoTradeRule;
import com.stockly.android.models.BankAccount;
import com.stockly.android.models.Positions;
import com.stockly.android.models.RetrofitError;
import com.stockly.android.models.TradingProfile;
import com.stockly.android.models.User;
import com.stockly.android.session.UserSession;
import com.stockly.android.utils.ActivityUtils;
import com.stockly.android.utils.CommonUtils;
import com.stockly.android.utils.DecimalDigitsInputFilter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;

@AndroidEntryPoint
public class AutoTradeSetupFragment extends NetworkFragment {
    private FragmentAutoTradeSetupBinding mBinding;
    private Positions asset;
    private User mUser;
    private BankAccount accounts;
    private TradingProfile profile;
    private final List<AutoTradeRule> existingRules = new ArrayList<>();
    @Inject
    UserSession mUserSession;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = requireActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));
        Bundle bundle = getArguments();
        if (bundle != null) {
            asset = bundle.getParcelable("asset");
        }
        if (asset == null) {
            throw new IllegalArgumentException("Asset null");
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_auto_trade_setup;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding = FragmentAutoTradeSetupBinding.bind(view);
        setUpToolBar(mBinding.toolbar.toolbar, true);
        mBinding.toolbar.title.setText(R.string.auto_trade_title);
        mBinding.toolbar.save.setVisibility(View.GONE);

        updateImage(mBinding.winIcon, asset);
        mBinding.title.setText(asset.symbol);
        mBinding.desc.setText(asset.getTitleDescription());
        if (asset.marketValue != null) {
            mBinding.tickerPrice.setText(String.format(Locale.getDefault(), "$%s", CommonUtils.round(Double.parseDouble(asset.marketValue), 2)));
        } else {
            mBinding.tickerPrice.setText(asset.getStockPrice());
        }
        mBinding.amount.setFilters(new android.text.InputFilter[]{new DecimalDigitsInputFilter(10, 2)});
        mBinding.manageRules.setOnClickListener(v -> ActivityUtils.launchFragment(requireActivity(), AutoTradeRulesFragment.class));
        mBinding.addFund.setOnClickListener(v -> checkAccounts());
        mBinding.createRule.setOnClickListener(v -> submitRule());
    }

    @Override
    public void onResume() {
        super.onResume();
        getUser();
        getBankAccounts();
        getTradingProfile();
        getAutoTradeRules();
    }

    private void getUser() {
        Single<User> userById = userDao.getUserById(mUserSession.getUserID());
        requestSingle(userById, new CallBackSingle<User>() {
            @Override
            public void onSuccess(@NotNull User user) {
                mUser = user;
                boolean isApproved = CommonKeys.ACCOUNT_APPROVED.equalsIgnoreCase(user.account_status);
                mBinding.accountStatus.setText(isApproved ? R.string.account_ready_auto_trade : R.string.auto_trade_requires_approval);
                mBinding.accountStatus.setTextColor(getResources().getColor(isApproved ? R.color.greenColor : R.color.colorError, null));
            }
        });
    }

    private void getBankAccounts() {
        requestSingle(accountDao.getBankAccount(), new CallBackSingle<BankAccount>() {
            @Override
            public void onSuccess(@NotNull BankAccount account) {
                accounts = account;
                updateBankUi(account);
            }

            @Override
            public void onError(@NotNull Throwable e) {
                accounts = null;
                updateBankUi(null);
            }
        });
    }

    private void getTradingProfile() {
        requestSingle(profileDao.getTradingProfile(), new CallBackSingle<TradingProfile>() {
            @Override
            public void onSuccess(@NotNull TradingProfile tradingProfile) {
                profile = tradingProfile;
                if (tradingProfile != null && !TextUtils.isEmpty(tradingProfile.cash)) {
                    mBinding.balance.setText(String.format(Locale.getDefault(), "$%s", CommonUtils.round(Double.parseDouble(tradingProfile.cash), 2)));
                }
            }
        });
    }

    private void getAutoTradeRules() {
        enqueue(getApi().getAutoTradeRules(), new CallBack<List<AutoTradeRule>>() {
            @Override
            public void onSuccess(List<AutoTradeRule> autoTradeRules) {
                existingRules.clear();
                if (autoTradeRules != null) {
                    existingRules.addAll(autoTradeRules);
                }
            }

            @Override
            public boolean onError(RetrofitError error, boolean isInternetIssue) {
                existingRules.clear();
                return false;
            }
        });
    }

    private void submitRule() {
        String amountText = CommonUtils.getValue(mBinding.amount.getText().toString()).trim();
        if (TextUtils.isEmpty(amountText)) {
            mBinding.amount.setError(getString(R.string.error_non_empty_field));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (Exception e) {
            mBinding.amount.setError(getString(R.string.error_enter_valid_value));
            return;
        }
        if (amount <= 0) {
            mBinding.amount.setError(getString(R.string.error_enter_valid_value));
            return;
        }
        if (mUser == null || !CommonKeys.ACCOUNT_APPROVED.equalsIgnoreCase(mUser.account_status)) {
            showErrorMessage(mBinding.errorMessage, getString(R.string.auto_trade_requires_approval));
            return;
        }
        if (accounts == null) {
            showErrorMessage(mBinding.errorMessage, getString(R.string.auto_trade_requires_bank));
            return;
        }
        if (!TextUtils.isEmpty(asset.status) && !"active".equalsIgnoreCase(asset.status)) {
            showErrorMessage(mBinding.errorMessage, getString(R.string.auto_trade_requires_active_asset));
            return;
        }
        if (profile != null && !TextUtils.isEmpty(profile.cash)) {
            try {
                if (amount > Double.parseDouble(profile.cash)) {
                    showErrorMessage(mBinding.errorMessage, getString(R.string.auto_trade_insufficient_buying_power));
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        String frequency = getSelectedFrequency();
        for (AutoTradeRule rule : existingRules) {
            if (rule != null && rule.isActiveRule()
                    && asset.symbol.equalsIgnoreCase(rule.symbol)
                    && frequency.equalsIgnoreCase(rule.frequency)) {
                showErrorMessage(mBinding.errorMessage, getString(R.string.auto_trade_duplicate_rule));
                return;
            }
        }
        HashMap<String, Object> body = new HashMap<>();
        body.put("symbol", asset.symbol);
        body.put("side", "buy");
        body.put("type", "market");
        body.put("time_in_force", "day");
        body.put("frequency", frequency);
        body.put("notional", amountText);
        body.put("enabled", true);
        mBinding.progressBar.progressBar.setVisibility(View.VISIBLE);
        enqueue(getApi().createAutoTradeRule(body), new CallBack<AutoTradeRule>() {
            @Override
            public void onSuccess(AutoTradeRule autoTradeRule) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireActivity(), R.string.auto_trade_created, Toast.LENGTH_SHORT).show();
                ActivityUtils.launchFragment(requireActivity(), AutoTradeRulesFragment.class);
                requireActivity().finish();
            }

            @Override
            public boolean onError(RetrofitError error, boolean isInternetIssue) {
                mBinding.progressBar.progressBar.setVisibility(View.GONE);
                showErrorMessage(mBinding.errorMessage, error.message);
                return true;
            }
        });
    }

    private String getSelectedFrequency() {
        int checkedId = mBinding.frequencyGroup.getCheckedRadioButtonId();
        RadioButton radioButton = mBinding.getRoot().findViewById(checkedId);
        if (radioButton == null) {
            return "weekly";
        }
        return String.valueOf(radioButton.getTag());
    }

    private void checkAccounts() {
        Bundle bundle = new Bundle();
        if (accounts != null) {
            bundle.putString(CommonKeys.KEY_FUNDS, "transaction");
            ActivityUtils.launchFragment(requireActivity(), AddFundsFragment.class, bundle);
        } else {
            bundle.putString("path", "funds");
            ActivityUtils.launchFragment(requireActivity(), BankIntroFragment.class, bundle);
        }
    }

    private void updateBankUi(@Nullable BankAccount account) {
        if (account == null) {
            mBinding.bankName.setText(R.string.no_account_attached);
            mBinding.accountNo.setText("");
            return;
        }
        mBinding.bankName.setText(account.nickname);
        if (!TextUtils.isEmpty(account.bankAccountNumber) && account.bankAccountNumber.length() >= 4) {
            mBinding.accountNo.setText(getString(R.string.account_xxx, account.bankAccountNumber.substring(account.bankAccountNumber.length() - 4)));
        }
    }
}

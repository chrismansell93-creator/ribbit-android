package com.stockly.android.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stockly.android.databinding.AutoTradeRuleItemBinding;
import com.stockly.android.models.AutoTradeRule;

import java.util.ArrayList;
import java.util.List;

public class AutoTradeRulesAdapter extends RecyclerView.Adapter<AutoTradeRulesAdapter.Holder> {
    private final LayoutInflater mLayoutInflater;
    private final ActionListener mListener;
    private final List<AutoTradeRule> mData = new ArrayList<>();

    public interface ActionListener {
        void onRuleClicked(AutoTradeRule rule, int position);

        void onToggleClicked(AutoTradeRule rule, int position);

        void onDeleteClicked(AutoTradeRule rule, int position);
    }

    public AutoTradeRulesAdapter(LayoutInflater layoutInflater, ActionListener listener) {
        mLayoutInflater = layoutInflater;
        mListener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(AutoTradeRuleItemBinding.inflate(mLayoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AutoTradeRule rule = mData.get(position);
        holder.mBinding.setRule(rule);
        holder.itemView.setOnClickListener(v -> mListener.onRuleClicked(rule, position));
        holder.mBinding.toggleRule.setOnClickListener(v -> mListener.onToggleClicked(rule, position));
        holder.mBinding.deleteRule.setOnClickListener(v -> mListener.onDeleteClicked(rule, position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setData(List<AutoTradeRule> data, boolean isClear) {
        if (isClear) {
            mData.clear();
        }
        if (data != null) {
            mData.addAll(data);
        }
        notifyDataSetChanged();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        private final AutoTradeRuleItemBinding mBinding;

        public Holder(@NonNull AutoTradeRuleItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
    }
}

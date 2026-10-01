package com.walletapp.android.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.walletapp.android.R;
import com.walletapp.android.data.remote.dto.TransactionHistoryItem;
import com.walletapp.android.databinding.ItemTransactionBinding;
import com.walletapp.android.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<TransactionHistoryItem> transactions = new ArrayList<>();

    public void setTransactions(List<TransactionHistoryItem> newTransactions) {
        this.transactions.clear();
        if (newTransactions != null) {
            this.transactions.addAll(newTransactions);
        }
        notifyDataSetChanged();
    }

    public void addTransactions(List<TransactionHistoryItem> moreTransactions) {
        if (moreTransactions != null && !moreTransactions.isEmpty()) {
            int start = this.transactions.size();
            this.transactions.addAll(moreTransactions);
            notifyItemRangeInserted(start, moreTransactions.size());
        }
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(transactions.get(position));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        public TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TransactionHistoryItem item) {
            Context context = itemView.getContext();
            String type = item.getType();

            binding.tvTxTitle.setText(item.getDescription() != null && !item.getDescription().isEmpty()
                    ? item.getDescription() : "Giao dịch ví");

            binding.tvTxDate.setText(FormatUtils.formatIsoDateTime(item.getCreatedAt()));

            boolean isCredit = "TOP_UP".equalsIgnoreCase(type);
            String formattedAmount = FormatUtils.formatVnd(item.getAmount());

            if (isCredit) {
                binding.tvTxAmount.setText("+ " + formattedAmount);
                binding.tvTxAmount.setTextColor(ContextCompat.getColor(context, R.color.status_success));
                binding.ivTxIcon.setImageResource(R.drawable.ic_add_card);
                binding.ivTxIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_success));
            } else if ("QR_PAYMENT".equalsIgnoreCase(type)) {
                binding.tvTxAmount.setText("- " + formattedAmount);
                binding.tvTxAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                binding.ivTxIcon.setImageResource(R.drawable.ic_qr_scan);
                binding.ivTxIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary));
            } else {
                binding.tvTxAmount.setText("- " + formattedAmount);
                binding.tvTxAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                binding.ivTxIcon.setImageResource(R.drawable.ic_send);
                binding.ivTxIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary));
            }

            String status = item.getStatus();
            if ("SUCCESS".equalsIgnoreCase(status)) {
                binding.tvTxStatus.setText("Thành công");
                binding.tvTxStatus.setTextColor(ContextCompat.getColor(context, R.color.status_success));
            } else if ("FAILED".equalsIgnoreCase(status)) {
                binding.tvTxStatus.setText("Thất bại");
                binding.tvTxStatus.setTextColor(ContextCompat.getColor(context, R.color.status_error));
            } else {
                binding.tvTxStatus.setText("Đang xử lý");
                binding.tvTxStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning));
            }
        }
    }
}

package com.walletapp.android.ui.home;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.walletapp.android.R;
import com.walletapp.android.WalletApplication;
import com.walletapp.android.data.local.SecureStorage;
import com.walletapp.android.databinding.FragmentHomeBinding;
import com.walletapp.android.ui.auth.LoginActivity;
import com.walletapp.android.ui.common.ErrorDialog;
import com.walletapp.android.ui.common.LoadingDialog;
import com.walletapp.android.ui.history.TransactionAdapter;
import com.walletapp.android.util.FormatUtils;

import java.math.BigDecimal;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TransactionAdapter adapter;
    private LoadingDialog loadingDialog;
    private boolean isBalanceHidden = false;
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WalletApplication app = (WalletApplication) requireActivity().getApplication();
        viewModel = new HomeViewModel(app.getWalletRepository(), app.getTransferRepository());
        loadingDialog = new LoadingDialog(requireContext());

        initViews();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadDashboard();
    }

    private void initViews() {
        WalletApplication app = (WalletApplication) requireActivity().getApplication();
        SecureStorage storage = app.getSecureStorage();

        String fullName = storage.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            binding.tvUserName.setText(fullName);
        }

        // Setup RecyclerView
        adapter = new TransactionAdapter();
        binding.rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentTransactions.setAdapter(adapter);

        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadDashboard());

        // Balance toggle
        binding.btnToggleBalance.setOnClickListener(v -> {
            isBalanceHidden = !isBalanceHidden;
            updateBalanceDisplay();
        });

        // Copy wallet ID
        binding.tvWalletId.setOnClickListener(v -> {
            String walletId = storage.getWalletId();
            if (walletId != null) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Wallet ID", walletId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "Đã sao chép mã ví", Toast.LENGTH_SHORT).show();
            }
        });

        // Quick actions navigation
        binding.actionTransfer.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_transfer)
        );

        binding.actionTopup.setOnClickListener(v -> showTopupDialog());

        binding.actionScanQr.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_qrScanner)
        );

        binding.actionMyQr.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_qrGenerate)
        );

        binding.tvSeeAll.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_history)
        );

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        app.getAuthRepository().logout();
                        Intent intent = new Intent(requireActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.swipeRefresh.setRefreshing(loading);
        });

        viewModel.getWalletData().observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null) {
                currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
                updateBalanceDisplay();

                if (wallet.getWalletId() != null) {
                    binding.tvWalletId.setText("Mã ví: " + wallet.getWalletId());
                }
                if (wallet.getStatus() != null) {
                    binding.tvWalletStatus.setText(wallet.getStatus());
                }
            }
        });

        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                binding.tvEmptyTransactions.setVisibility(View.VISIBLE);
                binding.rvRecentTransactions.setVisibility(View.GONE);
            } else {
                binding.tvEmptyTransactions.setVisibility(View.GONE);
                binding.rvRecentTransactions.setVisibility(View.VISIBLE);
                adapter.setTransactions(list);
            }
        });

        viewModel.getTopupSuccess().observe(getViewLifecycleOwner(), res -> {
            if (res != null) {
                Toast.makeText(requireContext(), "Nạp tiền thành công! Số dư mới: " + FormatUtils.formatVnd(res.getNewBalance()), Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                ErrorDialog.show(requireContext(), err);
            }
        });
    }

    private void updateBalanceDisplay() {
        if (isBalanceHidden) {
            binding.tvBalance.setText("•••••••• đ");
        } else {
            binding.tvBalance.setText(FormatUtils.formatVnd(currentBalance));
        }
    }

    private void showTopupDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_topup);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etAmount = dialog.findViewById(R.id.etTopupAmount);
        View btnConfirm = dialog.findViewById(R.id.btnConfirmTopup);
        View btnCancel = dialog.findViewById(R.id.btnCancelTopup);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                dialog.dismiss();
                viewModel.topup(amount, "SIMULATED_BANK");
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

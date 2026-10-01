package com.walletapp.android.ui.qrscanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.walletapp.android.R;
import com.walletapp.android.WalletApplication;
import com.walletapp.android.databinding.FragmentQrResultBinding;
import com.walletapp.android.domain.model.QrCodeInfo;
import com.walletapp.android.ui.common.ErrorDialog;
import com.walletapp.android.ui.common.LoadingDialog;
import com.walletapp.android.ui.transfer.TransferViewModel;
import com.walletapp.android.util.FormatUtils;

import java.math.BigDecimal;

public class QrResultFragment extends Fragment {

    private FragmentQrResultBinding binding;
    private QrScannerViewModel qrViewModel;
    private TransferViewModel transferViewModel;
    private LoadingDialog loadingDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentQrResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WalletApplication app = (WalletApplication) requireActivity().getApplication();
        qrViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new QrScannerViewModel(app.getQrRepository());
            }
        }).get(QrScannerViewModel.class);

        transferViewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TransferViewModel(app.getTransferRepository());
            }
        }).get(TransferViewModel.class);

        loadingDialog = new LoadingDialog(requireContext());

        initViews();
        observeViewModel();
    }

    private void initViews() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        QrCodeInfo qrInfo = qrViewModel.getScannedQrInfo();
        if (qrInfo != null) {
            BigDecimal amount = qrInfo.getAmount();
            binding.tvQrResultAmount.setText(amount != null ? FormatUtils.formatVnd(amount) : "Tùy chọn");
            binding.tvQrResultWalletId.setText(qrInfo.getWalletId() != null ? qrInfo.getWalletId() : "--");
            binding.tvQrResultType.setText(qrInfo.isDynamic() ? "QR Động (Thanh toán 1 lần)" : "QR Tĩnh");
            binding.tvQrResultDesc.setText(qrInfo.getDescription() != null ? qrInfo.getDescription() : "Thanh toán VietQR");
        }

        binding.btnPayQr.setOnClickListener(v -> {
            String pin = binding.etPin.getText() != null ? binding.etPin.getText().toString().trim() : "";
            if (pin.length() != 6) {
                Toast.makeText(requireContext(), "Vui lòng nhập mã PIN 6 số", Toast.LENGTH_SHORT).show();
                return;
            }
            qrViewModel.payQr(pin);
        });
    }

    private void observeViewModel() {
        qrViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading) {
                loadingDialog.show();
            } else {
                loadingDialog.dismiss();
            }
        });

        qrViewModel.getPaymentSuccess().observe(getViewLifecycleOwner(), response -> {
            if (response != null && getView() != null) {
                // Populate transferViewModel so receipt displays
                transferViewModel.setTransferData(
                        response.getRecipientName() != null ? response.getRecipientName() : "Người nhận QR",
                        response.getAmount(),
                        "Thanh toán VietQR"
                );
                Toast.makeText(requireContext(), "Thanh toán QR thành công!", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(getView()).navigate(R.id.action_qrResult_to_transferResult);
            }
        });

        qrViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                ErrorDialog.show(requireContext(), "Thanh toán QR thất bại", error);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.walletapp.android.ui.qrgenerate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.walletapp.android.WalletApplication;
import com.walletapp.android.data.local.SecureStorage;
import com.walletapp.android.databinding.FragmentQrGenerateBinding;
import com.walletapp.android.ui.common.ErrorDialog;
import com.walletapp.android.util.FormatUtils;

import java.math.BigDecimal;

public class QrGenerateFragment extends Fragment {

    private FragmentQrGenerateBinding binding;
    private QrGenerateViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentQrGenerateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WalletApplication app = (WalletApplication) requireActivity().getApplication();
        viewModel = new QrGenerateViewModel(app.getQrRepository());

        initViews();
        observeViewModel();

        // Generate default static QR on entry
        viewModel.generateQr(null, "Chuyển tiền");
    }

    private void initViews() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        WalletApplication app = (WalletApplication) requireActivity().getApplication();
        SecureStorage storage = app.getSecureStorage();
        if (storage.getFullName() != null) {
            binding.tvQrOwnerName.setText(storage.getFullName());
        }

        binding.btnGenerateDynamic.setOnClickListener(v -> {
            String amountStr = binding.etGenAmount.getText() != null ? binding.etGenAmount.getText().toString().trim() : "";
            String desc = binding.etGenDesc.getText() != null ? binding.etGenDesc.getText().toString().trim() : "Thanh toán";

            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                BigDecimal amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    Toast.makeText(requireContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.generateQr(amount, desc);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.pbQrLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.ivQrCode.setAlpha(loading ? 0.3f : 1.0f);
        });

        viewModel.getQrResponse().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.getAmount() != null) {
                binding.tvQrAmount.setText(FormatUtils.formatVnd(response.getAmount()));
            } else {
                binding.tvQrAmount.setText("Chuyển khoản VietQR");
            }
        });

        viewModel.getQrBitmap().observe(getViewLifecycleOwner(), bitmap -> {
            if (bitmap != null) {
                binding.ivQrCode.setImageBitmap(bitmap);
                binding.ivQrCode.setAlpha(1.0f);
            }
        });

        viewModel.getRemainingTimeText().observe(getViewLifecycleOwner(), text -> {
            binding.tvCountdown.setText(text);
        });

        viewModel.getIsExpired().observe(getViewLifecycleOwner(), expired -> {
            if (expired) {
                binding.ivQrCode.setAlpha(0.2f);
                Toast.makeText(requireContext(), "Mã QR đã hết hạn, vui lòng tạo mã mới", Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                ErrorDialog.show(requireContext(), error);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

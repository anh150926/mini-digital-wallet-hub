package com.walletapp.android.ui.transfer;

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
import com.walletapp.android.databinding.FragmentTransferConfirmBinding;
import com.walletapp.android.security.BiometricHelper;
import com.walletapp.android.security.KeystoreManager;
import com.walletapp.android.ui.common.ErrorDialog;
import com.walletapp.android.ui.common.LoadingDialog;
import com.walletapp.android.util.FormatUtils;

public class TransferConfirmFragment extends Fragment {

    private FragmentTransferConfirmBinding binding;
    private TransferViewModel viewModel;
    private LoadingDialog loadingDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransferConfirmBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WalletApplication app = (WalletApplication) requireActivity().getApplication();
        viewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.Factory() {
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

        // Display summary
        binding.tvConfirmAmount.setText(FormatUtils.formatVnd(viewModel.getAmount()));
        binding.tvConfirmRecipient.setText(viewModel.getDestPhoneNumber());
        binding.tvConfirmDescription.setText(
                viewModel.getDescription() != null ? viewModel.getDescription() : "Chuyển tiền"
        );

        // Biometric support check
        boolean hasBiometric = BiometricHelper.isBiometricAvailable(requireContext());
        binding.btnBiometric.setVisibility(hasBiometric ? View.VISIBLE : View.GONE);

        binding.btnBiometric.setOnClickListener(v -> {
            BiometricHelper.showBiometricPrompt(
                    requireActivity(),
                    "Xác thực chuyển tiền",
                    "Quét vân tay để xác thực giao dịch chuyển tiền",
                    "Nhập mã PIN",
                    new BiometricHelper.BiometricCallback() {
                        @Override
                        public void onSuccess() {
                            // Biometric verified -> sign payload using Android Keystore ECDSA
                            try {
                                String payloadToSign = viewModel.getDestPhoneNumber() + "|" + viewModel.getAmount();
                                String signature = KeystoreManager.signData(payloadToSign);
                                Toast.makeText(requireContext(), "Xác thực vân tay thành công! Vui lòng nhập mã PIN", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Ký giao dịch thất bại", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(int errorCode, String errString) {
                            Toast.makeText(requireContext(), errString, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailed() {
                            Toast.makeText(requireContext(), "Vân tay không khớp, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        binding.btnConfirmPay.setOnClickListener(v -> {
            String pin = binding.etPin.getText() != null ? binding.etPin.getText().toString().trim() : "";
            if (pin.length() != 6) {
                Toast.makeText(requireContext(), "Vui lòng nhập đúng mã PIN 6 số", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.executeTransfer(pin);
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading) {
                loadingDialog.show();
            } else {
                loadingDialog.dismiss();
            }
        });

        viewModel.getTransferSuccess().observe(getViewLifecycleOwner(), response -> {
            if (response != null && getView() != null) {
                Navigation.findNavController(getView()).navigate(R.id.action_confirm_to_result);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                ErrorDialog.show(requireContext(), "Chuyển tiền thất bại", error);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

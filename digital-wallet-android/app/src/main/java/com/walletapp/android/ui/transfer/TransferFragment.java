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
import com.walletapp.android.databinding.FragmentTransferBinding;

import java.math.BigDecimal;

public class TransferFragment extends Fragment {

    private FragmentTransferBinding binding;
    private TransferViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransferBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WalletApplication app = (WalletApplication) requireActivity().getApplication();
        // Activity-scoped ViewModel to share data with Confirm Fragment
        viewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TransferViewModel(app.getTransferRepository());
            }
        }).get(TransferViewModel.class);

        initViews();
    }

    private void initViews() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Chips
        binding.chip50k.setOnClickListener(v -> binding.etAmount.setText("50000"));
        binding.chip100k.setOnClickListener(v -> binding.etAmount.setText("100000"));
        binding.chip200k.setOnClickListener(v -> binding.etAmount.setText("200000"));
        binding.chip500k.setOnClickListener(v -> binding.etAmount.setText("500000"));
        binding.chip1m.setOnClickListener(v -> binding.etAmount.setText("1000000"));

        binding.btnContinue.setOnClickListener(v -> {
            String destPhone = binding.etDestPhone.getText() != null ? binding.etDestPhone.getText().toString().trim() : "";
            String amountStr = binding.etAmount.getText() != null ? binding.etAmount.getText().toString().trim() : "";
            String description = binding.etDescription.getText() != null ? binding.etDescription.getText().toString().trim() : "Chuyển tiền";

            if (destPhone.length() < 9) {
                Toast.makeText(requireContext(), "Vui lòng nhập số điện thoại hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

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

                viewModel.setTransferData(destPhone, amount, description);
                Navigation.findNavController(v).navigate(R.id.action_transfer_to_confirm);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

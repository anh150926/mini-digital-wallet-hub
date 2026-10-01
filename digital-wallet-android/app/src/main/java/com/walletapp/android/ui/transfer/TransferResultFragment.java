package com.walletapp.android.ui.transfer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.walletapp.android.R;
import com.walletapp.android.WalletApplication;
import com.walletapp.android.data.remote.dto.TransferResponse;
import com.walletapp.android.databinding.FragmentTransferResultBinding;
import com.walletapp.android.util.FormatUtils;

public class TransferResultFragment extends Fragment {

    private FragmentTransferResultBinding binding;
    private TransferViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransferResultBinding.inflate(inflater, container, false);
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

        initViews();

        // Prevent going back to confirm screen
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.action_result_to_home);
                }
            }
        });
    }

    private void initViews() {
        TransferResponse response = viewModel.getTransferSuccess().getValue();
        if (response != null) {
            binding.tvResultAmount.setText(FormatUtils.formatVnd(response.getAmount()));
            binding.tvResultTxId.setText(response.getTransactionId());
            binding.tvResultRecipient.setText(viewModel.getDestPhoneNumber());
            binding.tvResultTime.setText(FormatUtils.formatIsoDateTime(response.getCreatedAt()));

            if (response.getSourceBalanceAfter() != null) {
                binding.tvResultBalanceAfter.setText(FormatUtils.formatVnd(response.getSourceBalanceAfter()));
            }
        }

        binding.btnHome.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_result_to_home)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.walletapp.android.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.walletapp.android.WalletApplication;
import com.walletapp.android.databinding.FragmentHistoryBinding;
import com.walletapp.android.ui.common.ErrorDialog;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private TransactionAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WalletApplication app = (WalletApplication) requireActivity().getApplication();
        viewModel = new HistoryViewModel(app.getWalletRepository());

        initViews();
        observeViewModel();

        viewModel.loadInitial();
    }

    private void initViews() {
        adapter = new TransactionAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvHistory.setLayoutManager(layoutManager);
        binding.rvHistory.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadInitial());

        // Pagination on scroll to end
        binding.rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                    viewModel.loadMore();
                }
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.swipeRefresh.setRefreshing(loading);
        });

        viewModel.getTransactions().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                binding.tvEmptyHistory.setVisibility(View.VISIBLE);
                binding.rvHistory.setVisibility(View.GONE);
            } else {
                binding.tvEmptyHistory.setVisibility(View.GONE);
                binding.rvHistory.setVisibility(View.VISIBLE);
                adapter.setTransactions(list);
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

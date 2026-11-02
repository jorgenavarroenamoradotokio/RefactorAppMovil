package com.redur.electra.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.redur.electra.R;
import com.redur.electra.databinding.FragmentHomeBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Nullable
    private FragmentHomeBinding binding;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        HomeViewModel viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        // Mostramos por pantalla el nombre del usuario que ha iniciado sesion
        String name = viewModel.getUserDisplayName();
        if (binding != null && name != null) {
            binding.textHomeGreeting.setText(getString(R.string.home_greeting, name));
        }
        if (binding != null) {
            addDockButton(binding.getRoot());
        }
    }

    /**
     * Crea por código el acceso a la pantalla de muelle y lo añade al final del layout de Inicio.
     */
    private void addDockButton(@NonNull ViewGroup root) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(R.string.dock_label);
        button.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.nav_piece_dock));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.spacing_l);
        root.addView(button, params);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

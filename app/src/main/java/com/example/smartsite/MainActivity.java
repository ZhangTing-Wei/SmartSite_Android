package com.example.smartsite;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.smartsite.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding; // ✅ 在類別內宣告 binding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater()); // ✅ 正確初始化
        setContentView(binding.getRoot()); // ✅ 使用 binding.getRoot() 而不是 R.layout.activity_main

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectFragment = null;

                if (item.getItemId() == R.id.nav_home) {
                    selectFragment = new HomeFragment();
                } else if (item.getItemId() == R.id.nav_history) {
                    selectFragment = new HistoryFragment();
                } else if (item.getItemId() == R.id.nav_setup) {
                    selectFragment = new SetupFragment();
                }

                if (selectFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectFragment).commit();
                    return true;
                }

                return false;
            }
        });
    }
}

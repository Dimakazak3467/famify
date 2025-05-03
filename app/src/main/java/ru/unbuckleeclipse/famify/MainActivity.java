package ru.unbuckleeclipse.famify;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.android.material.transition.MaterialFade;
import java.util.HashMap;
import java.util.Map;
import ru.unbuckleeclipse.famify.fragments.AddPage;
import ru.unbuckleeclipse.famify.fragments.HomePage;
import ru.unbuckleeclipse.famify.fragments.ProfilePage;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomnav = findViewById(R.id.bottom_navigation);
        bottomnav.setSelectedItemId(R.id.nav_home);
        bottomnav.setOnItemSelectedListener(navListner);

        // Открываем HomePage по умолчанию
        if (savedInstanceState == null) {
            Fragment selectedFragment = new HomePage();
            // Устанавливаем MaterialFadeThrough для анимации
            selectedFragment.setEnterTransition(new MaterialFade());
            selectedFragment.setExitTransition(new MaterialFade());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }

        // Firestore: обновление данных пользователя
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", user.getDisplayName());
            userData.put("email", user.getEmail());
            db.collection("users").document(user.getUid()).set(userData, SetOptions.merge());
        }
    }

    private final NavigationBarView.OnItemSelectedListener navListner = item -> {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomePage();
        } else if (itemId == R.id.nav_add) {
            selectedFragment = new AddPage();
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ProfilePage();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.fade_in,      // enter
                            R.anim.fade_out,     // exit
                            R.anim.fade_in,      // popEnter
                            R.anim.fade_out      // popExit
                    )
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }
        return true;
    };
}
package ru.unbuckleeclipse.famify;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

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

        Fragment selectedFragment = new HomePage();

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", user.getDisplayName());
            userData.put("email", user.getEmail());
            // familyId не трогаем — он появится позже

            db.collection("users").document(user.getUid()).set(userData, SetOptions.merge());
        }
    }

    private NavigationBarView.OnItemSelectedListener navListner = item -> {
        int itemId = item.getItemId();

        Fragment selectedFragment = null;

        if(itemId == R.id.nav_home){
            selectedFragment = new HomePage();
        }else if(itemId == R.id.nav_add){
            selectedFragment = new AddPage();
        }else if(itemId == R.id.nav_profile){
            selectedFragment = new ProfilePage();
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();

        return true;
    };
}
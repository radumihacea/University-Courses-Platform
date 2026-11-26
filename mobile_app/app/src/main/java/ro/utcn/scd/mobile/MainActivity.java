package ro.utcn.scd.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.LiveNotifications;
import ro.utcn.scd.mobile.data.Session;
import ro.utcn.scd.mobile.data.model.AppNotification;
import ro.utcn.scd.mobile.ui.chat.ChatFragment;
import ro.utcn.scd.mobile.ui.login.LoginActivity;
import ro.utcn.scd.mobile.reminders.Reminders;
import ro.utcn.scd.mobile.ui.notifications.NotificationsFragment;
import ro.utcn.scd.mobile.ui.profile.ProfileFragment;
import ro.utcn.scd.mobile.ui.today.TodayFragment;
import ro.utcn.scd.mobile.ui.week.WeekFragment;

/**
 * Ecranul principal: bara de jos cu 4 sectiuni (Azi, Saptamana, Asistent, Notificari).
 * Sectiunile sunt create o singura data si doar ascunse / afisate, ca sa isi pastreze
 * starea (ex. conversatia cu asistentul).
 * Tot aici se porneste conexiunea WebSocket pentru notificari live.
 */
public class MainActivity extends AppCompatActivity implements LiveNotifications.Listener {

    private static final String KEY_TAB = "tab";

    private Session session;
    private LiveNotifications live;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private int unreadCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = Session.get(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }
        live = LiveNotifications.get(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottom_nav);
        setupInsets();

        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        int tab = savedInstanceState != null ? savedInstanceState.getInt(KEY_TAB, R.id.nav_today) : R.id.nav_today;
        bottomNav.setSelectedItemId(tab);
        showTab(tab);
        updateSubtitle();
    }

    /**
     * Bara de sus primeste spatiu pentru bara de stare. Cand apare tastatura (ex. in chat),
     * ascundem bara de jos si lasam loc tastaturii.
     */
    private void setupInsets() {
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean keyboard = insets.isVisible(WindowInsetsCompat.Type.ime());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            bottomNav.setVisibility(keyboard ? View.GONE : View.VISIBLE);
            v.setPadding(bars.left, bars.top, bars.right, keyboard ? ime.bottom : 0);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (live != null) {
            live.addListener(this);
            live.start();
            updateSubtitle();
            refreshUnreadCount();
            Reminders.reschedule(this);
        }
    }

    /** Cate notificari necitite are utilizatorul (pentru bulina de pe bara de jos). */
    public void refreshUnreadCount() {
        ApiClient.get(this).notifications(new ApiClient.Callback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> result) {
                int count = 0;
                for (AppNotification n : result) {
                    if (!n.isRead()) {
                        count++;
                    }
                }
                setUnreadCount(count);
            }

            @Override
            public void onError(String message) {
                // nu e grav: bulina se actualizeaza la urmatoarea incercare
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (live != null) {
            live.removeListener(this);
            // notificarile live doar cat timp aplicatia este deschisa
            if (!isChangingConfigurations()) {
                live.stop();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNav != null) {
            outState.putInt(KEY_TAB, bottomNav.getSelectedItemId());
        }
    }

    // ------------------------------------------------------------------ sectiuni

    private void showTab(int itemId) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        String selectedTag = tagFor(itemId);
        for (int id : new int[]{R.id.nav_today, R.id.nav_week, R.id.nav_chat, R.id.nav_notifications, R.id.nav_profile}) {
            String tag = tagFor(id);
            Fragment fragment = fm.findFragmentByTag(tag);
            if (tag.equals(selectedTag)) {
                if (fragment == null) {
                    tx.add(R.id.fragment_container, createFragment(id), tag);
                } else {
                    tx.show(fragment);
                }
            } else if (fragment != null) {
                tx.hide(fragment);
            }
        }
        tx.commitNow();
        toolbar.setTitle(titleFor(itemId));
    }

    private static String tagFor(int itemId) {
        return "tab_" + itemId;
    }

    private static Fragment createFragment(int itemId) {
        if (itemId == R.id.nav_week) {
            return new WeekFragment();
        }
        if (itemId == R.id.nav_chat) {
            return new ChatFragment();
        }
        if (itemId == R.id.nav_notifications) {
            return new NotificationsFragment();
        }
        if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return new TodayFragment();
    }

    private static String titleFor(int itemId) {
        if (itemId == R.id.nav_week) {
            return "Orarul săptămânii";
        }
        if (itemId == R.id.nav_chat) {
            return "Asistent AI";
        }
        if (itemId == R.id.nav_notifications) {
            return "Notificări";
        }
        if (itemId == R.id.nav_profile) {
            return "Contul meu";
        }
        return "Orarul de azi";
    }

    public void openNotifications() {
        bottomNav.setSelectedItemId(R.id.nav_notifications);
    }

    // ------------------------------------------------------------------ notificari

    /** Numarul de notificari necitite, afisat ca bulina pe bara de jos. */
    public void setUnreadCount(int count) {
        unreadCount = Math.max(0, count);
        if (bottomNav == null) {
            return;
        }
        if (unreadCount == 0) {
            bottomNav.removeBadge(R.id.nav_notifications);
        } else {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_notifications);
            badge.setNumber(unreadCount);
        }
    }

    @Override
    public void onNotification(AppNotification notification) {
        setUnreadCount(unreadCount + 1);
        // orarul s-ar putea fi schimbat (ora mutata, orar publicat): reprogramam reminderele
        Reminders.reschedule(this);
        Snackbar.make(findViewById(R.id.fragment_container), notification.message, Snackbar.LENGTH_LONG)
                .setAnchorView(bottomNav)
                .setAction("Vezi", v -> openNotifications())
                .show();
    }

    @Override
    public void onConnectionChanged(boolean connected) {
        updateSubtitle();
    }

    private void updateSubtitle() {
        if (toolbar == null) {
            return;
        }
        String role = session.isProfessor() ? "Profesor" : "Student";
        boolean connected = live != null && live.isConnected();
        toolbar.setSubtitle(session.fullName() + " · " + role + (connected ? " · ● live" : ""));
    }

    // ------------------------------------------------------------------ deconectare

    public void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Deconectare")
                .setMessage("Sigur vrei să te deconectezi?")
                .setNegativeButton("Anulează", null)
                .setPositiveButton("Deconectare", (d, w) -> {
                    live.stop();
                    Reminders.cancelAll(this);
                    session.clear();
                    goToLogin();
                })
                .show();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}

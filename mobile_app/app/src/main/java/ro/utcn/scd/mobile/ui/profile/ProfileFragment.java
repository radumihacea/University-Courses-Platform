package ro.utcn.scd.mobile.ui.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import ro.utcn.scd.mobile.MainActivity;
import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.Session;
import ro.utcn.scd.mobile.data.Settings;
import ro.utcn.scd.mobile.reminders.Reminders;
import ro.utcn.scd.mobile.ui.availability.AvailabilityActivity;
import ro.utcn.scd.mobile.ui.common.Labels;
import ro.utcn.scd.mobile.ui.optionals.OptionalsActivity;
import ro.utcn.scd.mobile.ui.requests.MyRequestsActivity;

/**
 * Ecranul "Cont": datele utilizatorului, actiunile specifice rolului
 * (profesor: cereri de mutare si disponibilitate; student: optionale) si setarile
 * (remindere, tema, server). De aici se face si deconectarea.
 */
public class ProfileFragment extends Fragment {

    private static final int[] MINUTES = {5, 10, 15, 30};

    private Session session;
    private Settings settings;
    private View remindersRow;
    private View testRow;
    private View themeRow;

    /** Cererea permisiunii de notificari (Android 13+), cand se activeaza reminderele. */
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableReminders();
                } else {
                    Toast.makeText(requireContext(),
                            "Fără permisiunea de notificări, reminderele nu pot fi afișate.", Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = Session.get(requireContext());
        settings = Settings.get(requireContext());
        boolean professor = session.isProfessor();

        ((TextView) view.findViewById(R.id.avatar)).setText(Labels.initials(session.fullName()));
        ((TextView) view.findViewById(R.id.name)).setText(session.fullName());
        ((TextView) view.findViewById(R.id.email)).setText(session.email());
        ((TextView) view.findViewById(R.id.role)).setText(professor ? "Cadru didactic" : "Student");

        // Actiuni dupa rol
        View requests = row(view, R.id.row_requests, R.drawable.ic_swap, "Cererile mele de mutare",
                "Starea cererilor trimise adminului");
        View availability = row(view, R.id.row_availability, R.drawable.ic_event_available, "Disponibilitatea mea",
                "Când nu poți preda sau ce intervale preferi");
        View optionals = row(view, R.id.row_optionals, R.drawable.ic_library, "Discipline opționale",
                "Înscrie-te la opționalele pe care le urmezi");
        requests.setVisibility(professor ? View.VISIBLE : View.GONE);
        availability.setVisibility(professor ? View.VISIBLE : View.GONE);
        optionals.setVisibility(professor ? View.GONE : View.VISIBLE);
        requests.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyRequestsActivity.class)));
        availability.setOnClickListener(v -> startActivity(new Intent(requireContext(), AvailabilityActivity.class)));
        optionals.setOnClickListener(v -> startActivity(new Intent(requireContext(), OptionalsActivity.class)));

        // Setari
        remindersRow = row(view, R.id.row_reminders, R.drawable.ic_alarm, "Remindere înainte de ore", "");
        remindersRow.findViewById(R.id.row_switch).setVisibility(View.VISIBLE);
        remindersRow.setOnClickListener(v -> {
            if (settings.remindersEnabled()) {
                settings.setRemindersEnabled(false);
                Reminders.cancelAll(requireContext());
                updateReminders();
            } else if (needsPermission()) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                enableReminders();
            }
        });

        testRow = row(view, R.id.row_reminder_test, R.drawable.ic_notifications, "Trimite un reminder de probă",
                "Apare în bara de notificări după 5 secunde");
        testRow.setOnClickListener(v -> {
            if (needsPermission()) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
            Reminders.sendTest(requireContext());
            Toast.makeText(requireContext(), "Reminderul apare în 5 secunde.", Toast.LENGTH_SHORT).show();
        });

        themeRow = row(view, R.id.row_theme, R.drawable.ic_palette, "Temă", "");
        themeRow.setOnClickListener(v -> chooseTheme());

        row(view, R.id.row_server, R.drawable.ic_dns, "Server", session.serverUrl()).setClickable(false);

        view.findViewById(R.id.logout).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).confirmLogout();
            }
        });

        updateReminders();
        updateTheme();
    }

    private View row(View root, int id, int icon, String title, String subtitle) {
        View row = root.findViewById(id);
        ((ImageView) row.findViewById(R.id.row_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.row_title)).setText(title);
        TextView sub = row.findViewById(R.id.row_subtitle);
        sub.setText(subtitle);
        sub.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        return row;
    }

    private boolean needsPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    /** La activare se alege si cu cate minute inainte de ora vine reminderul. */
    private void enableReminders() {
        String[] labels = new String[MINUTES.length];
        int checked = 2;
        for (int i = 0; i < MINUTES.length; i++) {
            labels[i] = "Cu " + MINUTES[i] + " minute înainte";
            if (MINUTES[i] == settings.reminderMinutes()) {
                checked = i;
            }
        }
        final int[] choice = {checked};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Când vrei să fii anunțat?")
                .setSingleChoiceItems(labels, checked, (d, which) -> choice[0] = which)
                .setNegativeButton("Renunță", null)
                .setPositiveButton("Activează", (d, w) -> {
                    settings.setReminderMinutes(MINUTES[choice[0]]);
                    settings.setRemindersEnabled(true);
                    Reminders.reschedule(requireContext());
                    updateReminders();
                    Toast.makeText(requireContext(), "Reminderele sunt active.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void updateReminders() {
        boolean on = settings.remindersEnabled();
        ((MaterialSwitch) remindersRow.findViewById(R.id.row_switch)).setChecked(on);
        TextView sub = remindersRow.findViewById(R.id.row_subtitle);
        sub.setVisibility(View.VISIBLE);
        sub.setText(on ? "Cu " + settings.reminderMinutes() + " minute înainte de fiecare oră"
                : "Dezactivate");
    }

    private void chooseTheme() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Temă")
                .setSingleChoiceItems(Settings.THEME_NAMES, settings.theme(), (d, which) -> {
                    d.dismiss();
                    settings.setTheme(which); // ecranul se redeseneaza automat cu noua tema
                })
                .show();
    }

    private void updateTheme() {
        TextView sub = themeRow.findViewById(R.id.row_subtitle);
        sub.setVisibility(View.VISIBLE);
        sub.setText(Settings.THEME_NAMES[settings.theme()]);
    }
}

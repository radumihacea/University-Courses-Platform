package ro.utcn.scd.mobile.ui.today;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.LiveNotifications;
import ro.utcn.scd.mobile.data.Session;
import ro.utcn.scd.mobile.data.model.AppNotification;
import ro.utcn.scd.mobile.data.model.DaySchedule;
import ro.utcn.scd.mobile.data.model.ScheduleEntry;
import ro.utcn.scd.mobile.ui.common.EntryDetails;
import ro.utcn.scd.mobile.ui.common.Labels;
import ro.utcn.scd.mobile.ui.common.ScheduleAdapter;

/**
 * Orarul unei zile (implicit azi), cu sageti pentru ziua anterioara / urmatoare.
 * Tine cont de saptamana para / impara si de vacante (calculate de backend).
 * Ora in desfasurare este marcata cu "Acum", iar urmatoarea cu "Urmează".
 */
public class TodayFragment extends Fragment implements LiveNotifications.Listener {

    private LocalDate date = LocalDate.now();
    private ScheduleAdapter adapter;
    private SwipeRefreshLayout refresh;
    private RecyclerView list;
    private TextView dateTitle;
    private TextView weekInfo;
    private View backToToday;
    private View empty;
    private TextView emptyText;
    private View emptyRetry;
    /** ultima cerere trimisa - raspunsurile vechi (daca utilizatorul schimba rapid ziua) se ignora */
    private int requestId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getString("date") != null) {
            date = LocalDate.parse(savedInstanceState.getString("date"));
        }
        refresh = view.findViewById(R.id.refresh);
        list = view.findViewById(R.id.list);
        dateTitle = view.findViewById(R.id.date_title);
        weekInfo = view.findViewById(R.id.week_info);
        backToToday = view.findViewById(R.id.back_to_today);
        empty = view.findViewById(R.id.empty);
        emptyText = view.findViewById(R.id.empty_text);
        emptyRetry = view.findViewById(R.id.empty_retry);

        adapter = new ScheduleAdapter(Session.get(requireContext()).isProfessor());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        adapter.setOnEntryClick(entry -> EntryDetails.show(requireContext(), entry));

        refresh.setOnRefreshListener(this::load);
        refresh.setOnChildScrollUpCallback((parent, child) -> list.canScrollVertically(-1));
        view.findViewById(R.id.prev_day).setOnClickListener(v -> changeDate(date.minusDays(1)));
        view.findViewById(R.id.next_day).setOnClickListener(v -> changeDate(date.plusDays(1)));
        backToToday.setOnClickListener(v -> changeDate(LocalDate.now()));
        emptyRetry.setOnClickListener(v -> load());

        load();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("date", date.toString());
    }

    /** La intoarcerea din alt ecran (ex. dupa inscrierea la o optionala) orarul se reincarca. */
    private boolean firstResume = true;

    @Override
    public void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
        } else {
            load();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LiveNotifications.get(requireContext()).addListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        LiveNotifications.get(requireContext()).removeListener(this);
    }

    /** O notificare noua (ex. orar publicat, ora mutata) poate schimba orarul: il reincarcam. */
    @Override
    public void onNotification(AppNotification notification) {
        if (isAdded()) {
            load();
        }
    }

    private void changeDate(LocalDate newDate) {
        date = newDate;
        load();
    }

    private void load() {
        dateTitle.setText(Labels.dayTitle(date));
        weekInfo.setText(relativeDay());
        backToToday.setVisibility(date.equals(LocalDate.now()) ? View.GONE : View.VISIBLE);
        refresh.setRefreshing(true);

        int current = ++requestId;
        ApiClient.get(requireContext()).day(date, new ApiClient.Callback<DaySchedule>() {
            @Override
            public void onSuccess(DaySchedule result) {
                if (!isAdded() || current != requestId) {
                    return;
                }
                refresh.setRefreshing(false);
                show(result);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || current != requestId) {
                    return;
                }
                refresh.setRefreshing(false);
                adapter.submit(new ArrayList<>());
                showEmpty(message, true);
            }
        });
    }

    private void show(DaySchedule day) {
        // ex: "Azi · Săptămâna 9 (impară)"
        String info = relativeDay();
        if (day.weekNumber != null) {
            info += (info.isEmpty() ? "" : " · ") + "Săptămâna " + day.weekNumber
                    + ("ODD".equals(day.parity) ? " (impară)" : " (pară)");
        } else if (day.holiday) {
            info += (info.isEmpty() ? "" : " · ") + "Vacanță";
        }
        weekInfo.setText(info);

        List<ScheduleEntry> entries = day.entries != null ? day.entries : new ArrayList<>();
        List<ScheduleAdapter.Row> rows = new ArrayList<>();
        boolean isToday = date.equals(LocalDate.now());
        LocalTime now = LocalTime.now();
        boolean nextMarked = false;
        for (ScheduleEntry e : entries) {
            String badge = null;
            if (isToday) {
                LocalTime start = LocalTime.parse(e.start());
                LocalTime end = LocalTime.parse(e.end());
                if (!now.isBefore(start) && now.isBefore(end)) {
                    badge = "Acum";
                } else if (!nextMarked && now.isBefore(start)) {
                    badge = "Urmează";
                    nextMarked = true;
                }
            }
            rows.add(ScheduleAdapter.Row.entry(e, badge));
        }
        adapter.submit(rows);

        if (entries.isEmpty()) {
            showEmpty(emptyMessage(day), false);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    private String emptyMessage(DaySchedule day) {
        switch (date.getDayOfWeek()) {
            case SATURDAY:
            case SUNDAY:
                return "Este weekend – nu ai ore.";
            default:
                break;
        }
        if (day.holiday) {
            return "Este vacanță – nu ai ore.";
        }
        if (day.weekNumber == null) {
            return "Data nu face parte din semestru.";
        }
        if (day.message != null && day.message.toLowerCase(Labels.RO).contains("publicat")) {
            return "Orarul nu a fost publicat încă.";
        }
        return "Nu ai ore în această zi.";
    }

    private void showEmpty(String message, boolean canRetry) {
        empty.setVisibility(View.VISIBLE);
        emptyText.setText(message);
        emptyRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }

    private String relativeDay() {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "Azi";
        }
        if (date.equals(today.plusDays(1))) {
            return "Mâine";
        }
        if (date.equals(today.minusDays(1))) {
            return "Ieri";
        }
        return "";
    }
}

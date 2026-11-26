package ro.utcn.scd.mobile.ui.week;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.LiveNotifications;
import ro.utcn.scd.mobile.data.Session;
import ro.utcn.scd.mobile.data.model.AppNotification;
import ro.utcn.scd.mobile.data.model.ScheduleEntry;
import ro.utcn.scd.mobile.ui.common.EntryDetails;
import ro.utcn.scd.mobile.ui.common.Labels;
import ro.utcn.scd.mobile.ui.common.ScheduleAdapter;

/**
 * Orarul saptamanal complet, grupat pe zile. Orele care au loc doar in saptamanile
 * pare sau impare sunt marcate cu "Săpt. pare" / "Săpt. impare".
 */
public class WeekFragment extends Fragment implements LiveNotifications.Listener {

    private static final List<String> DAY_ORDER = java.util.Arrays.asList(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

    private ScheduleAdapter adapter;
    private SwipeRefreshLayout refresh;
    private RecyclerView list;
    private View empty;
    private TextView emptyText;
    private View emptyRetry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_week, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        refresh = view.findViewById(R.id.refresh);
        list = view.findViewById(R.id.list);
        empty = view.findViewById(R.id.empty);
        emptyText = view.findViewById(R.id.empty_text);
        emptyRetry = view.findViewById(R.id.empty_retry);

        adapter = new ScheduleAdapter(Session.get(requireContext()).isProfessor());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        adapter.setOnEntryClick(entry -> EntryDetails.show(requireContext(), entry));

        refresh.setOnRefreshListener(this::load);
        refresh.setOnChildScrollUpCallback((parent, child) -> list.canScrollVertically(-1));
        emptyRetry.setOnClickListener(v -> load());
        load();
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

    @Override
    public void onNotification(AppNotification notification) {
        if (isAdded()) {
            load();
        }
    }

    private void load() {
        refresh.setRefreshing(true);
        ApiClient.get(requireContext()).week(new ApiClient.Callback<List<ScheduleEntry>>() {
            @Override
            public void onSuccess(List<ScheduleEntry> result) {
                if (!isAdded()) {
                    return;
                }
                refresh.setRefreshing(false);
                show(result);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                refresh.setRefreshing(false);
                adapter.submit(new ArrayList<>());
                showEmpty(message, true);
            }
        });
    }

    private void show(List<ScheduleEntry> entries) {
        List<ScheduleEntry> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, Comparator
                .comparingInt((ScheduleEntry e) -> DAY_ORDER.indexOf(e.dayOfWeek))
                .thenComparing(ScheduleEntry::start));

        List<ScheduleAdapter.Row> rows = new ArrayList<>();
        String currentDay = null;
        for (ScheduleEntry e : sorted) {
            if (!e.dayOfWeek.equals(currentDay)) {
                currentDay = e.dayOfWeek;
                rows.add(ScheduleAdapter.Row.header(Labels.dayName(currentDay)));
            }
            rows.add(ScheduleAdapter.Row.entry(e, null));
        }
        adapter.submit(rows);

        if (sorted.isEmpty()) {
            showEmpty("Orarul nu a fost publicat încă.", false);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    private void showEmpty(String message, boolean canRetry) {
        empty.setVisibility(View.VISIBLE);
        emptyText.setText(message);
        emptyRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }
}

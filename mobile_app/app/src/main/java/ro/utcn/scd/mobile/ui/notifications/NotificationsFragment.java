package ro.utcn.scd.mobile.ui.notifications;

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

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import ro.utcn.scd.mobile.MainActivity;
import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.LiveNotifications;
import ro.utcn.scd.mobile.data.model.AppNotification;

/**
 * Notificarile utilizatorului (cele mai noi primele). Notificarile noi apar instantaneu,
 * prin WebSocket, fara reincarcare. Click pe o notificare = marcata ca citita.
 */
public class NotificationsFragment extends Fragment implements LiveNotifications.Listener {

    private final List<AppNotification> items = new ArrayList<>();
    private NotificationAdapter adapter;
    private SwipeRefreshLayout refresh;
    private RecyclerView list;
    private TextView summary;
    private View markAll;
    private View empty;
    private TextView emptyText;
    private View emptyRetry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        refresh = view.findViewById(R.id.refresh);
        list = view.findViewById(R.id.list);
        summary = view.findViewById(R.id.summary);
        markAll = view.findViewById(R.id.mark_all);
        empty = view.findViewById(R.id.empty);
        emptyText = view.findViewById(R.id.empty_text);
        emptyRetry = view.findViewById(R.id.empty_retry);
        ((android.widget.ImageView) view.findViewById(R.id.empty_icon)).setImageResource(R.drawable.ic_notifications);

        adapter = new NotificationAdapter(items, (n, position) -> markRead(n, position));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        refresh.setOnRefreshListener(this::load);
        refresh.setOnChildScrollUpCallback((parent, child) -> list.canScrollVertically(-1));
        markAll.setOnClickListener(v -> markAllRead());
        emptyRetry.setOnClickListener(v -> load());
        load();
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

    /** Notificare noua, primita live: apare imediat in capul listei. */
    @Override
    public void onNotification(AppNotification notification) {
        if (!isAdded()) {
            return;
        }
        items.add(0, notification);
        adapter.notifyItemInserted(0);
        list.scrollToPosition(0);
        updateSummary(false);
    }

    private void load() {
        refresh.setRefreshing(true);
        ApiClient.get(requireContext()).notifications(new ApiClient.Callback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> result) {
                if (!isAdded()) {
                    return;
                }
                refresh.setRefreshing(false);
                items.clear();
                items.addAll(result);
                adapter.notifyDataSetChanged();
                updateSummary(true);
                if (items.isEmpty()) {
                    showEmpty("Nu ai nicio notificare.", false);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                refresh.setRefreshing(false);
                if (items.isEmpty()) {
                    showEmpty(message, true);
                } else {
                    Snackbar.make(list, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void markRead(AppNotification n, int position) {
        if (n.isRead() || n.id == null || position == RecyclerView.NO_POSITION) {
            return;
        }
        n.read = true; // actualizam imediat, apoi confirmam pe server
        adapter.notifyItemChanged(position);
        updateSummary(true);
        ApiClient.get(requireContext()).markRead(n.id, new ApiClient.Callback<AppNotification>() {
            @Override
            public void onSuccess(AppNotification result) {
            }

            @Override
            public void onError(String message) {
                n.read = false;
                if (isAdded()) {
                    adapter.notifyDataSetChanged();
                    updateSummary(true);
                    Snackbar.make(list, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void markAllRead() {
        for (int i = 0; i < items.size(); i++) {
            markRead(items.get(i), i);
        }
    }

    private int unread() {
        int count = 0;
        for (AppNotification n : items) {
            if (!n.isRead()) {
                count++;
            }
        }
        return count;
    }

    /** syncBadge = actualizeaza si bulina de pe bara de jos (la primirea live, MainActivity o creste deja). */
    private void updateSummary(boolean syncBadge) {
        int unread = unread();
        summary.setText(items.isEmpty() ? "" : unread == 0 ? "Toate notificările sunt citite"
                : unread == 1 ? "1 notificare necitită" : unread + " notificări necitite");
        markAll.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        if (syncBadge && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setUnreadCount(unread);
        }
    }

    private void showEmpty(String message, boolean canRetry) {
        empty.setVisibility(View.VISIBLE);
        emptyText.setText(message);
        emptyRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }
}

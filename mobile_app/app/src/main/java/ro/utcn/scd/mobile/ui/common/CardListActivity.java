package ro.utcn.scd.mobile.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import ro.utcn.scd.mobile.R;

/**
 * Baza pentru ecranele de tip lista de carduri (cererile mele, disponibilitate, optionale):
 * bara de sus cu buton inapoi, lista care se reincarca trasa in jos, mesaj cand e goala,
 * buton optional de adaugare.
 */
public abstract class CardListActivity extends AppCompatActivity {

    /** Un card: titlu, eticheta, subtitlu, text si un buton optional. */
    public static final class Card {
        public String title;
        public String badge;
        public String subtitle;
        public String body;
        public String actionText;
        public Runnable action;
    }

    protected SwipeRefreshLayout refresh;
    protected ExtendedFloatingActionButton fab;
    private RecyclerView list;
    private TextView intro;
    private View empty;
    private TextView emptyText;
    private View emptyRetry;
    private final List<Card> cards = new ArrayList<>();
    private final CardAdapter adapter = new CardAdapter();

    protected abstract String screenTitle();

    /** Incarca datele si apeleaza showCards(...) sau showError(...). */
    protected abstract void load();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list);
        InsetsHelper.padForSystemBarsAndKeyboard(findViewById(R.id.root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(screenTitle());
        toolbar.setNavigationOnClickListener(v -> finish());

        refresh = findViewById(R.id.refresh);
        list = findViewById(R.id.list);
        intro = findViewById(R.id.intro);
        fab = findViewById(R.id.fab);
        empty = findViewById(R.id.empty);
        emptyText = findViewById(R.id.empty_text);
        emptyRetry = findViewById(R.id.empty_retry);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        refresh.setOnRefreshListener(this::reload);
        refresh.setOnChildScrollUpCallback((parent, child) -> list.canScrollVertically(-1));
        emptyRetry.setOnClickListener(v -> reload());
        reload();
    }

    protected void reload() {
        refresh.setRefreshing(true);
        load();
    }

    protected void setIntro(String text) {
        intro.setText(text);
        intro.setVisibility(View.VISIBLE);
    }

    protected void showCards(List<Card> newCards, String emptyMessage) {
        refresh.setRefreshing(false);
        cards.clear();
        cards.addAll(newCards);
        adapter.notifyDataSetChanged();
        empty.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
        emptyText.setText(emptyMessage);
        emptyRetry.setVisibility(View.GONE);
    }

    protected void showError(String message) {
        refresh.setRefreshing(false);
        if (cards.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            emptyText.setText(message);
            emptyRetry.setVisibility(View.VISIBLE);
        } else {
            com.google.android.material.snackbar.Snackbar.make(list, message,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
        }
    }

    private class CardAdapter extends RecyclerView.Adapter<CardAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_generic, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Card c = cards.get(position);
            h.title.setText(c.title);
            setOptional(h.badge, c.badge);
            setOptional(h.subtitle, c.subtitle);
            setOptional(h.body, c.body);
            if (c.actionText != null && c.action != null) {
                h.action.setVisibility(View.VISIBLE);
                h.action.setText(c.actionText);
                h.action.setOnClickListener(v -> c.action.run());
            } else {
                h.action.setVisibility(View.GONE);
            }
        }

        private void setOptional(TextView view, String text) {
            view.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
            view.setText(text);
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView badge;
            final TextView subtitle;
            final TextView body;
            final MaterialButton action;

            Holder(View view) {
                super(view);
                title = view.findViewById(R.id.title);
                badge = view.findViewById(R.id.badge);
                subtitle = view.findViewById(R.id.subtitle);
                body = view.findViewById(R.id.body);
                action = view.findViewById(R.id.action);
            }
        }
    }
}

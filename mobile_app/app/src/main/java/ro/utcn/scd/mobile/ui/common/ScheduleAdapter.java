package ro.utcn.scd.mobile.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.model.ScheduleEntry;

/**
 * Lista de ore, folosita si pe ecranul "Azi", si pe "Saptamana".
 * Poate contine si titluri (ex. "Luni") intre ore.
 */
public class ScheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ENTRY = 1;

    /** Un rand din lista: fie titlu, fie o ora (cu o eticheta optionala, ex. "Acum"). */
    public static final class Row {
        final String header;
        final ScheduleEntry entry;
        final String badge;

        private Row(String header, ScheduleEntry entry, String badge) {
            this.header = header;
            this.entry = entry;
            this.badge = badge;
        }

        public static Row header(String text) {
            return new Row(text, null, null);
        }

        public static Row entry(ScheduleEntry entry, String badge) {
            return new Row(null, entry, badge);
        }
    }

    public interface OnEntryClick {
        void onClick(ScheduleEntry entry);
    }

    /** Profesorul vede cui preda; studentul vede cine preda. */
    private final boolean professorView;
    private final List<Row> rows = new ArrayList<>();
    private OnEntryClick onEntryClick;

    public ScheduleAdapter(boolean professorView) {
        this.professorView = professorView;
    }

    public void setOnEntryClick(OnEntryClick listener) {
        this.onEntryClick = listener;
    }

    public void submit(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header != null ? TYPE_HEADER : TYPE_ENTRY;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_day_header, parent, false));
        }
        return new EntryHolder(inflater.inflate(R.layout.item_schedule_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).text.setText(row.header);
        } else {
            ((EntryHolder) holder).bind(row.entry, row.badge, professorView);
            holder.itemView.setOnClickListener(v -> {
                if (onEntryClick != null) {
                    onEntryClick.onClick(row.entry);
                }
            });
        }
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView text;

        HeaderHolder(View view) {
            super(view);
            text = view.findViewById(R.id.header);
        }
    }

    static final class EntryHolder extends RecyclerView.ViewHolder {
        final View stripe;
        final TextView time;
        final TextView badge;
        final TextView title;
        final TextView discipline;
        final TextView room;
        final TextView person;

        EntryHolder(View view) {
            super(view);
            stripe = view.findViewById(R.id.stripe);
            time = view.findViewById(R.id.time);
            badge = view.findViewById(R.id.badge);
            title = view.findViewById(R.id.title);
            discipline = view.findViewById(R.id.discipline);
            room = view.findViewById(R.id.room);
            person = view.findViewById(R.id.person);
        }

        void bind(ScheduleEntry e, String badgeText, boolean professorView) {
            stripe.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), Labels.activityColor(e.type)));
            time.setText(e.start() + " – " + e.end());
            title.setText(e.disciplineCode + " · " + Labels.activityType(e.type));
            discipline.setText(e.disciplineName);
            room.setText("Sala " + e.roomCode);
            person.setText(professorView ? (e.targetName != null ? e.targetName : "") : e.professorName);

            // eticheta: "Acum" / "Urmează" (pe ecranul Azi) sau saptamanile pare / impare
            String text = badgeText != null ? badgeText : Labels.parity(e.parity);
            badge.setVisibility(text != null ? View.VISIBLE : View.GONE);
            badge.setText(text);
        }
    }
}

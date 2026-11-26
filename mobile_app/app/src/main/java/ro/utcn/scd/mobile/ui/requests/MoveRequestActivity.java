package ro.utcn.scd.mobile.ui.requests;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.model.ChangeRequest;
import ro.utcn.scd.mobile.data.model.MoveOption;
import ro.utcn.scd.mobile.data.model.Room;
import ro.utcn.scd.mobile.ui.common.InsetsHelper;
import ro.utcn.scd.mobile.ui.common.Labels;

/**
 * Profesorul cere mutarea unei ore:
 *   1. alege un interval liber (serverul le calculeaza: profesorul si studentii liberi, sali disponibile);
 *   2. alege sala;
 *   3. scrie motivul si trimite.
 * Adminul primeste cererea live in panoul web; la aprobare, studentii si profesorul sunt notificati.
 */
public class MoveRequestActivity extends AppCompatActivity {

    public static final String EXTRA_ENTRY_ID = "entryId";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CURRENT = "current";

    private long entryId;
    private final List<MoveOption> options = new ArrayList<>();
    private int selected = -1;
    private SlotAdapter adapter;
    private View progress;
    private TextView message;
    private View details;
    private ChipGroup rooms;
    private TextInputEditText reason;
    private MaterialButton submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_move_request);
        InsetsHelper.padForSystemBarsAndKeyboard(findViewById(R.id.root));

        entryId = getIntent().getLongExtra(EXTRA_ENTRY_ID, -1);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.entry_title)).setText(getIntent().getStringExtra(EXTRA_TITLE));
        ((TextView) findViewById(R.id.entry_current)).setText("Acum: " + getIntent().getStringExtra(EXTRA_CURRENT));

        progress = findViewById(R.id.progress);
        message = findViewById(R.id.message);
        details = findViewById(R.id.details);
        rooms = findViewById(R.id.rooms);
        reason = findViewById(R.id.reason);
        submit = findViewById(R.id.submit);

        RecyclerView slots = findViewById(R.id.slots);
        slots.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlotAdapter();
        slots.setAdapter(adapter);
        submit.setOnClickListener(v -> send());

        loadOptions();
    }

    private void loadOptions() {
        ApiClient.get(this).moveOptions(entryId, new ApiClient.Callback<List<MoveOption>>() {
            @Override
            public void onSuccess(List<MoveOption> result) {
                progress.setVisibility(View.GONE);
                options.clear();
                options.addAll(result);
                adapter.notifyDataSetChanged();
                if (options.isEmpty()) {
                    showMessage("Nu există niciun interval în care tu și studenții să fiți liberi și să existe o sală potrivită.");
                }
            }

            @Override
            public void onError(String error) {
                progress.setVisibility(View.GONE);
                showMessage(error);
            }
        });
    }

    private void select(int position) {
        int previous = selected;
        selected = position;
        adapter.notifyItemChanged(position);
        if (previous >= 0) {
            adapter.notifyItemChanged(previous);
        }
        rooms.removeAllViews();
        boolean first = true;
        for (Room room : options.get(position).rooms) {
            Chip chip = new Chip(this);
            chip.setText(room.code + " · " + room.capacity + " locuri");
            chip.setCheckable(true);
            chip.setTag(room.id);
            chip.setId(View.generateViewId());
            rooms.addView(chip);
            if (first) {
                chip.setChecked(true);
                first = false;
            }
        }
        details.setVisibility(View.VISIBLE);
    }

    private void send() {
        if (selected < 0) {
            return;
        }
        String text = reason.getText() == null ? "" : reason.getText().toString().trim();
        if (text.isEmpty()) {
            reason.setError("Scrie motivul cererii");
            return;
        }
        Long roomId = null;
        int checked = rooms.getCheckedChipId();
        if (checked != View.NO_ID) {
            roomId = (Long) rooms.findViewById(checked).getTag();
        }
        MoveOption option = options.get(selected);
        submit.setEnabled(false);
        submit.setText("Se trimite...");
        ApiClient.get(this).createChangeRequest(entryId, option.timeSlotId, roomId, text,
                new ApiClient.Callback<ChangeRequest>() {
                    @Override
                    public void onSuccess(ChangeRequest result) {
                        Toast.makeText(MoveRequestActivity.this,
                                "Cererea a fost trimisă. Vei primi o notificare când adminul răspunde.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        submit.setEnabled(true);
                        submit.setText("Trimite cererea");
                        Toast.makeText(MoveRequestActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showMessage(String text) {
        message.setText(text);
        message.setVisibility(View.VISIBLE);
    }

    /** Lista intervalelor posibile; cel ales este evidentiat. */
    private class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot_option, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            MoveOption o = options.get(position);
            holder.slot.setText(Labels.slot(o.dayOfWeek, o.startTime, o.endTime));
            int count = o.rooms == null ? 0 : o.rooms.size();
            holder.roomsInfo.setText(count == 1 ? "1 sală liberă" : count + " săli libere");
            holder.preferred.setVisibility(o.preferred ? View.VISIBLE : View.GONE);
            holder.card.setChecked(position == selected);
            holder.card.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    select(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final TextView slot;
            final TextView roomsInfo;
            final TextView preferred;

            Holder(View view) {
                super(view);
                card = (MaterialCardView) view;
                slot = view.findViewById(R.id.slot);
                roomsInfo = view.findViewById(R.id.rooms_info);
                preferred = view.findViewById(R.id.preferred);
            }
        }
    }
}

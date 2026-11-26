package ro.utcn.scd.mobile.ui.availability;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.model.ProfessorConstraint;
import ro.utcn.scd.mobile.data.model.TimeSlot;
import ro.utcn.scd.mobile.ui.common.CardListActivity;
import ro.utcn.scd.mobile.ui.common.Labels;

/**
 * Disponibilitatea profesorului: intervalele in care nu poate preda, pe care ar vrea sa le evite
 * sau pe care le prefera. Algoritmul de generare a orarului le respecta la urmatoarea generare.
 */
public class AvailabilityActivity extends CardListActivity {

    private static final String[] KINDS = {
            "Nu pot preda (obligatoriu)",
            "Aș prefera să evit",
            "Prefer acest interval"};

    @Override
    protected String screenTitle() {
        return "Disponibilitatea mea";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setIntro("Intervalele marcate aici sunt folosite de generatorul de orar: nu îți va pune ore când nu poți "
                + "și va încerca să folosească intervalele preferate.");
        fab.setText("Adaugă");
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(v -> pickSlot());
    }

    @Override
    protected void load() {
        ApiClient.get(this).constraints(new ApiClient.Callback<List<ProfessorConstraint>>() {
            @Override
            public void onSuccess(List<ProfessorConstraint> result) {
                List<ProfessorConstraint> sorted = new ArrayList<>(result);
                sorted.sort(Comparator.comparing((ProfessorConstraint c) -> c.timeSlotId));
                List<Card> cards = new ArrayList<>();
                for (ProfessorConstraint c : sorted) {
                    Card card = new Card();
                    card.title = Labels.pretty(c.timeSlotLabel);
                    card.badge = kindLabel(c);
                    card.body = c.originalText;
                    card.actionText = "Șterge";
                    card.action = () -> delete(c);
                    cards.add(card);
                }
                showCards(cards, "Nu ai marcat niciun interval. Apasă „Adaugă” ca să spui când nu poți preda.");
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private static String kindLabel(ProfessorConstraint c) {
        if ("PREFERRED".equals(c.type)) {
            return "Preferat";
        }
        return Boolean.TRUE.equals(c.mandatory) ? "Indisponibil" : "De evitat";
    }

    /** Pasul 1: alege intervalul. */
    private void pickSlot() {
        ApiClient.get(this).timeSlots(new ApiClient.Callback<List<TimeSlot>>() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                String[] labels = new String[slots.size()];
                for (int i = 0; i < slots.size(); i++) {
                    TimeSlot s = slots.get(i);
                    labels[i] = Labels.slot(s.dayOfWeek, s.startTime, s.endTime);
                }
                new MaterialAlertDialogBuilder(AvailabilityActivity.this)
                        .setTitle("Alege intervalul")
                        .setItems(labels, (d, which) -> pickKind(slots.get(which), labels[which]))
                        .setNegativeButton("Renunță", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AvailabilityActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Pasul 2: ce fel de restrictie. */
    private void pickKind(TimeSlot slot, String label) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(label)
                .setItems(KINDS, (d, which) -> {
                    String type = which == 2 ? "PREFERRED" : "UNAVAILABLE";
                    boolean mandatory = which == 0;
                    ApiClient.get(this).addConstraint(slot.id, type, mandatory, null,
                            new ApiClient.Callback<ProfessorConstraint>() {
                                @Override
                                public void onSuccess(ProfessorConstraint result) {
                                    Toast.makeText(AvailabilityActivity.this, "Salvat.", Toast.LENGTH_SHORT).show();
                                    reload();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(AvailabilityActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Renunță", null)
                .show();
    }

    private void delete(ProfessorConstraint c) {
        ApiClient.get(this).deleteConstraint(c.id, new ApiClient.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                reload();
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }
}

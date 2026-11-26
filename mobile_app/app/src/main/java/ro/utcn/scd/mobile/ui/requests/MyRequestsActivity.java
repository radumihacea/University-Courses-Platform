package ro.utcn.scd.mobile.ui.requests;

import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.model.ChangeRequest;
import ro.utcn.scd.mobile.ui.common.CardListActivity;
import ro.utcn.scd.mobile.ui.common.Labels;

/** Cererile de mutare trimise de profesor, cu starea lor. Cele in asteptare pot fi anulate. */
public class MyRequestsActivity extends CardListActivity {

    @Override
    protected String screenTitle() {
        return "Cererile mele de mutare";
    }

    @Override
    protected void load() {
        setIntro("Pentru o cerere nouă, apasă pe o oră din orar și alege „Cere mutarea”.");
        ApiClient.get(this).myChangeRequests(new ApiClient.Callback<List<ChangeRequest>>() {
            @Override
            public void onSuccess(List<ChangeRequest> result) {
                List<Card> cards = new ArrayList<>();
                for (ChangeRequest r : result) {
                    Card c = new Card();
                    c.title = r.disciplineCode + " · " + Labels.activityType(r.activityType)
                            + (r.targetName != null ? " · " + r.targetName : "");
                    c.badge = Labels.requestStatus(r.status);
                    c.subtitle = Labels.relativeTime(r.createdAt);
                    StringBuilder body = new StringBuilder()
                            .append("Din: ").append(Labels.pretty(r.currentTimeSlotLabel)).append(", sala ").append(r.currentRoomCode)
                            .append("\nÎn: ").append(Labels.pretty(r.proposedTimeSlotLabel))
                            .append(r.proposedRoomCode != null ? ", sala " + r.proposedRoomCode : "")
                            .append("\nMotiv: ").append(r.reason);
                    if (r.adminComment != null && !r.adminComment.isEmpty()) {
                        body.append("\nRăspuns admin: ").append(r.adminComment);
                    }
                    c.body = body.toString();
                    if ("PENDING".equals(r.status)) {
                        c.actionText = "Anulează";
                        c.action = () -> confirmCancel(r);
                    }
                    cards.add(c);
                }
                showCards(cards, "Nu ai trimis nicio cerere de mutare.");
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void confirmCancel(ChangeRequest r) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Anulezi cererea?")
                .setMessage(r.disciplineCode + " – mutarea în " + Labels.pretty(r.proposedTimeSlotLabel))
                .setNegativeButton("Nu", null)
                .setPositiveButton("Anulează cererea", (d, w) ->
                        ApiClient.get(this).cancelChangeRequest(r.id, new ApiClient.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(MyRequestsActivity.this, "Cererea a fost anulată.", Toast.LENGTH_SHORT).show();
                                reload();
                            }

                            @Override
                            public void onError(String message) {
                                showError(message);
                            }
                        }))
                .show();
    }
}

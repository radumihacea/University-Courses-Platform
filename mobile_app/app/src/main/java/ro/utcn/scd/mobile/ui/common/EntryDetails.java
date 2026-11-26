package ro.utcn.scd.mobile.ui.common;

import android.content.Context;
import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ro.utcn.scd.mobile.data.Session;
import ro.utcn.scd.mobile.data.model.ScheduleEntry;
import ro.utcn.scd.mobile.ui.requests.MoveRequestActivity;

/** Detaliile unei ore (la apasare). Profesorul poate cere de aici mutarea orei. */
public final class EntryDetails {

    private EntryDetails() {
    }

    public static void show(Context context, ScheduleEntry e) {
        boolean professor = Session.get(context).isProfessor();
        String parity = Labels.parity(e.parity);
        StringBuilder text = new StringBuilder()
                .append(e.disciplineName).append("\n\n")
                .append("Când: ").append(Labels.slot(e.dayOfWeek, e.startTime, e.endTime))
                .append(parity != null ? " (" + parity.toLowerCase(Labels.RO) + ")" : " (în fiecare săptămână)").append("\n")
                .append("Sala: ").append(e.roomCode).append("\n")
                .append("Cadru didactic: ").append(e.professorName);
        if (e.targetName != null) {
            text.append("\n").append(professor ? "Cu: " : "Formație: ").append(e.targetName);
        }

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(e.disciplineCode + " · " + Labels.activityType(e.type))
                .setMessage(text.toString())
                .setNegativeButton("Închide", null);
        if (professor) {
            dialog.setPositiveButton("Cere mutarea", (d, w) -> context.startActivity(
                    new Intent(context, MoveRequestActivity.class)
                            .putExtra(MoveRequestActivity.EXTRA_ENTRY_ID, (long) e.id)
                            .putExtra(MoveRequestActivity.EXTRA_TITLE, e.disciplineCode + " · " + Labels.activityType(e.type)
                                    + (e.targetName != null ? " · " + e.targetName : ""))
                            .putExtra(MoveRequestActivity.EXTRA_CURRENT, Labels.slot(e.dayOfWeek, e.startTime, e.endTime)
                                    + ", sala " + e.roomCode
                                    + (parity != null ? " (" + parity.toLowerCase(Labels.RO) + ")" : ""))));
        }
        dialog.show();
    }
}

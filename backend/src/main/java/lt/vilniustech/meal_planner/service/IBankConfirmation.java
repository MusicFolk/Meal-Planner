package lt.vilniustech.meal_planner.service;

import java.util.UUID;
import lt.vilniustech.meal_planner.entity.Payment;

/** Sąsaja, per kurią bankas praneša mokėjimo rezultatą (MOK-2). */
public interface IBankConfirmation {

    Payment confirm(UUID reference, boolean successful);
}

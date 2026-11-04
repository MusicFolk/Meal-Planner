package lt.vilniustech.meal_planner.service;

/** Sąsaja, per kurią panaikinamos pasirinkto naudotojo sesijos (AUT-4). */
public interface ISessions {

    void invalidateAll(Long userId);
}

package lt.vilniustech.meal_planner.service;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Seka aktyvias naudotojų sesijas, kad užblokavus ar ištrynus paskyrą jas būtų galima iš karto panaikinti (NFR-50). */
@Component
public class SessionService implements ISessions, HttpSessionListener {

    static final String USER_ID_ATTRIBUTE = "MEAL_PLANNER_USER_ID";

    private final Map<Long, Set<HttpSession>> sessionsByUser = new ConcurrentHashMap<>();

    public void register(Long userId, HttpSession session) {
        session.setAttribute(USER_ID_ATTRIBUTE, userId);
        sessionsByUser.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void invalidateAll(Long userId) {
        Set<HttpSession> sessions = sessionsByUser.remove(userId);
        if (sessions == null) {
            return;
        }
        for (HttpSession session : List.copyOf(sessions)) {
            try {
                session.invalidate();
            } catch (IllegalStateException alreadyInvalidated) {
                // sesija jau panaikinta
            }
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        Object userId = event.getSession().getAttribute(USER_ID_ATTRIBUTE);
        if (userId instanceof Long id) {
            Set<HttpSession> sessions = sessionsByUser.get(id);
            if (sessions != null) {
                sessions.remove(event.getSession());
            }
        }
    }
}

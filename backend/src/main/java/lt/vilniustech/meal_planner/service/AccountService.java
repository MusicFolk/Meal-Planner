package lt.vilniustech.meal_planner.service;

import java.time.Clock;
import java.time.DayOfWeek;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PaymentDto;
import lt.vilniustech.meal_planner.dto.UserDtos.ClientDataExport;
import lt.vilniustech.meal_planner.dto.UserDtos.ProfileExport;
import lt.vilniustech.meal_planner.dto.UserDtos.SettingsDto;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.PaymentRepository;
import lt.vilniustech.meal_planner.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private final UserRepository users;
    private final PaymentRepository payments;
    private final QuestionnaireService questionnaires;
    private final IMealPlans plans;
    private final ReviewService reviews;
    private final SubscriptionService subscriptions;
    private final ISessions sessions;
    private final Clock clock;

    public AccountService(UserRepository users, PaymentRepository payments, QuestionnaireService questionnaires,
                          IMealPlans plans, ReviewService reviews, SubscriptionService subscriptions,
                          ISessions sessions, Clock clock) {
        this.users = users;
        this.payments = payments;
        this.questionnaires = questionnaires;
        this.plans = plans;
        this.reviews = reviews;
        this.subscriptions = subscriptions;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SettingsDto settings(Long clientId) {
        Client client = client(clientId);
        return new SettingsDto(client.getTheme(), client.getFirstDayOfWeek());
    }

    /** Išsaugo sąsajos nustatymus paskyroje, kad jie būtų taikomi visuose įrenginiuose (FR-47). */
    public SettingsDto updateSettings(Long clientId, SettingsDto settings) {
        if (settings.firstDayOfWeek() != DayOfWeek.MONDAY
                && settings.firstDayOfWeek() != DayOfWeek.SUNDAY) {
            throw ApiException.badRequest("Savaitės pirmoji diena gali būti pirmadienis arba sekmadienis.");
        }
        Client client = client(clientId);
        client.setTheme(settings.theme());
        client.setFirstDayOfWeek(settings.firstDayOfWeek());
        return settings(clientId);
    }

    /** Visi kliento duomenys vienu JSON failu (FR-45). */
    @Transactional(readOnly = true)
    public ClientDataExport export(Long clientId) {
        Client client = client(clientId);
        return new ClientDataExport(clock.instant(),
                new ProfileExport(client.getFirstName(), client.getEmail(), client.getCreatedAt(),
                        client.isHealthDataConsent(), client.getTheme(), client.getFirstDayOfWeek()),
                questionnaires.get(clientId).orElse(null),
                plans.exportPlans(clientId),
                reviews.clientReviews(clientId),
                subscriptions.details(clientId),
                payments.findByClientIdOrderByCreatedAtDesc(clientId).stream().map(PaymentDto::of).toList());
    }

    /**
     * Ištrina paskyrą ir visus kliento asmens duomenis; mokėjimai nuasmeninami (lieka suma ir data),
     * sesijos panaikinamos (FR-46).
     */
    public void delete(Long clientId) {
        client(clientId);
        payments.anonymizeByClientId(clientId);
        users.hardDelete(clientId);
        sessions.invalidateAll(clientId);
    }

    private Client client(Long clientId) {
        return users.findById(clientId).filter(Client.class::isInstance).map(Client.class::cast)
                .orElseThrow(() -> ApiException.notFound("Klientas nerastas."));
    }
}

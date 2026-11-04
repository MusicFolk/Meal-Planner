# Meal Planner

Asmeninio mitybos planavimo sistema „Meal Planner“ – kompleksinis projektas (PRIfs-23).
Sistema pagal kliento anketą apskaičiuoja KMI ir paros kalorijų poreikį (Mifflin–St Jeor),
pateikia savaitės mitybos planą su receptais ir produktų sąrašu, leidžia užsakyti dietologo peržiūrą.

Šioje versijoje įgyvendintas **1 sprintas** (14 lentelė): visas sistemos karkasas be DI.
Planai sudaromi iš demonstracinio plano šablono DB, porcijas proporcingai pritaikant kliento paros poreikiui.

## Struktūra (NFR-35)

```
backend/            Spring Boot 4.1 programų serveris (Java 21, Maven)
  src/main/resources/db/migration/   Flyway migracijos: schema, produktai (USDA), demonstraciniai duomenys
  tools/usda/       Vienkartinio USDA FoodData Central importo skriptas (NFR-08)
frontend/           React 19 + TypeScript + Vite 8 + MUI PWA kliento sąsaja
nginx/              Žiniatinklio serverio konfigūracija
docker-compose.yml  Visos sistemos paleidimas
.env.example        Konfigūracijos pavyzdys
```

Backend klasės (`lt.vilniustech.meal_planner`) sugrupuotos pagal sluoksnius:

| Paketas | Turinys |
|---|---|
| `controller` | REST valdikliai (`/api/...`), įskaitant banko imitacijos valdiklį |
| `service` | Verslo logika ir komponentų sąsajos (`I*`), banko sąsajos imitacija `BankSimulator` (NFR-09) |
| `repository` | Spring Data JPA saugyklos |
| `entity` | JPA esybės |
| `enums` | Esybių ir DTO išvardijimai |
| `dto` | Užklausų ir atsakymų objektai |
| `mapper` | Esybių konvertavimas į DTO |
| `event` | Programos įvykiai |
| `exception` | Klaidų tipai ir bendras klaidų apdorojimas |
| `config` | Spring konfigūracija (saugumas, kalorijų modulis) |
| `security` | Prisijungusio naudotojo duomenys |
| `calories` | Kalorijų skaičiavimo (KAL) modulis – atskiras, be priklausomybių (NFR-48) |

Architektūros komponentus (3.5 poskyris) atitinka servisai: AUT – `AuthService`, `SessionService`;
NAU – `AccountService`, `AdminUserService`; ANK – `QuestionnaireService`; PRD – `ProductCatalogService`;
PLN – `MealPlanService`; PER – `ReviewService`; MOK – `PaymentService`, `SubscriptionService`;
pranešimai klientui – `NotificationService`.

## Paleidimas su Docker (NFR-37)

Reikia tik Docker.

```bash
cp .env.example .env      # pakeiskite DB_PASSWORD
docker compose up --build
```

Sistema pasiekiama adresu http://localhost (prievadą galima keisti `HTTP_PORT`).
Pirmą kartą paleidus Flyway sukuria schemą ir įrašo produktus bei demonstracinius duomenis.

## Kūrimo aplinka

```bash
# PostgreSQL 18
docker run -d --name mp-dev-db -e POSTGRES_DB=mealplanner -e POSTGRES_USER=mealplanner \
  -e POSTGRES_PASSWORD=mealplanner -p 5432:5432 postgres:18

# Programų serveris (http://localhost:8080)
cd backend && COOKIE_SECURE=false ./mvnw spring-boot:run

# Kliento sąsaja (http://localhost:5173, /api/ perduodama į 8080)
cd frontend && npm install && npm run dev
```

Testai: `cd backend && ./mvnw test` (vienetų testai ir integraciniai testai su Testcontainers – reikia Docker).
Kliento sąsajos tikrinimas: `cd frontend && npm run lint && npm run build`.

## Demonstracinės paskyros (NFR-41)

Visų slaptažodis: `Demo1234`

| El. paštas | Vaidmuo | Būsena |
|---|---|---|
| `klientas@mealplanner.lt` | Klientas | Aktyvi prenumerata, planas, atlikta dietologo peržiūra |
| `klientas2@mealplanner.lt` | Klientas | Bandomasis laikotarpis, anketa užpildyta, plano dar nėra |
| `klientas3@mealplanner.lt` | Klientas | Prenumerata pasibaigusi, laukianti peržiūros užklausa |
| `dietologas@mealplanner.lt` | Dietologas | |
| `dietologas2@mealplanner.lt` | Dietologas | |
| `admin@mealplanner.lt` | Administratorius | |

Mokėjimai vykdomi per banko sąsajos imitaciją: atsidariusiame lange pasirinkite „Apmokėti“ arba „Atmesti“.

## 1 sprinto apimtis

Įgyvendintos naudotojų istorijos NI-01–NI-15 (užduotys 1, 3–8 ir antrinės 3.1–3.3, 4.1, 5.1):

- registracija, prisijungimas, atsijungimas; užrakinimas 15 min. po 5 nesėkmingų bandymų; prieiga pagal vaidmenis, CSRF;
- anketa su laukų ribomis (tikrinama ir sąsajoje, ir serveryje), alergenai, nemėgstami produktai, lėtinės ligos įspėjimas;
- KMI ir paros kalorijų poreikis su skaičiavimo paaiškinimu ir saugia riba;
- savaitės plano, recepto, produktų sąrašo (su serveryje saugomomis varnelėmis) ir dietologo komentarų peržiūra;
- dietologo peržiūros užsakymas su apmokėjimu, užklausos būsenos;
- dietologo darbo vieta (užklausos, anketa be kontaktinių duomenų, komentarai, pateikimas, pranešimas klientui);
- administratoriaus darbo vieta (dietologo paskyra su laikinu slaptažodžiu, blokavimas, filtrai);
- prenumerata (7 d. bandomasis laikotarpis, 30 d. apmokėjimas, nepratęsiama automatiškai);
- duomenų atsisiuntimas JSON formatu, paskyros ištrynimas (mokėjimai nuasmeninami), tema ir savaitės pirmoji diena.

Kituose sprintuose: DI generavimas su Gemini, plano patikra, `AiGenerationRequest`, `PlanValidation` (2 sprintas);
planų istorija, DI parametrai, `AiConfiguration` (3 sprintas). HTTPS su Let's Encrypt (NFR-11) konfigūruojamas
diegiant į Azure prieš galutinę demonstraciją.

## Trumpa instrukcija dietologui (NFR-43)

1. Prisijunkite gautu el. paštu ir laikinu slaptažodžiu; pirmą kartą sistema paprašys susikurti naują slaptažodį.
2. Lange „Peržiūros užklausos“ matomos apmokėtos užklausos (seniausios viršuje); jas galima filtruoti pagal būseną.
3. Paspaudus „Pradėti peržiūrą“, užklausa priskiriama jums ir tampa „Peržiūrima“.
4. Kairėje matote kliento anketą ir klausimą, dešinėje – savaitės planą. Paspaudę patiekalą, matote jo receptą,
   o „Komentuoti patiekalą“ – parašote komentarą patiekalui.
5. Įrašykite bendrą komentarą planui (1–2000 simbolių) ir paspauskite „Pateikti peržiūrą“. Po to komentarų
   redaguoti nebegalima, o klientas gauna pranešimą.

## Trumpa instrukcija administratoriui (NFR-43)

1. Lange „Naudotojai“ matomi visi naudotojai; juos galima filtruoti pagal vaidmenį ir būseną.
2. „Blokuoti“ – naudotojas nebegali prisijungti, jo sesijos iš karto panaikinamos, duomenys lieka.
   Savo paskyros užblokuoti negalima.
3. „Sukurti dietologo paskyrą“ – įveskite vardą, pavardę ir el. paštą. Sugeneruotas laikinas slaptažodis
   parodomas tik vieną kartą – perduokite jį dietologui.

## Produktų katalogas

`V2__products.sql` sugeneruotas skriptu `backend/tools/usda/generate_products.py` iš USDA FoodData Central
SR Legacy (2018-04) duomenų rinkinio: 157 produktai su lietuviškais pavadinimais, FDC ID ir maistine verte 100 g.

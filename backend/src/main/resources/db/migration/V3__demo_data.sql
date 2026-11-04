-- Demonstraciniai duomenys (NFR-41). Realūs asmens duomenys nenaudojami.
-- Visų demonstracinių naudotojų slaptažodis: Demo1234

-- Pagalbinė funkcija: prideda vieno valgymo patiekalą su receptu prie plano.
-- p_ingredients formatas: 'FDC_ID:gramai,FDC_ID:gramai,...'
CREATE FUNCTION pg_temp.add_dish(p_plan BIGINT, p_day INT, p_type TEXT, p_name TEXT,
                                 p_ingredients TEXT, p_steps TEXT[]) RETURNS VOID AS $$
DECLARE
    v_day    BIGINT;
    v_meal   BIGINT;
    v_dish   BIGINT;
    v_recipe BIGINT;
    v_pair   TEXT;
    i        INT;
BEGIN
    SELECT id INTO v_day FROM plan_day WHERE meal_plan_id = p_plan AND day_number = p_day;
    IF v_day IS NULL THEN
        INSERT INTO plan_day (meal_plan_id, day_number) VALUES (p_plan, p_day) RETURNING id INTO v_day;
    END IF;
    INSERT INTO meal (plan_day_id, type) VALUES (v_day, p_type) RETURNING id INTO v_meal;
    INSERT INTO dish (meal_id, name) VALUES (v_meal, p_name) RETURNING id INTO v_dish;
    INSERT INTO recipe (dish_id) VALUES (v_dish) RETURNING id INTO v_recipe;
    FOREACH v_pair IN ARRAY string_to_array(p_ingredients, ',') LOOP
        INSERT INTO ingredient (recipe_id, product_id, grams)
        SELECT v_recipe, p.id, split_part(v_pair, ':', 2)::INT
        FROM product p WHERE p.fdc_id = split_part(v_pair, ':', 1)::INT;
    END LOOP;
    FOR i IN 1 .. array_length(p_steps, 1) LOOP
        INSERT INTO recipe_step (recipe_id, step_number, description) VALUES (v_recipe, i, p_steps[i]);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Perskaičiuoja plano patiekalų ir dienų maistinę vertę pagal produktų DB bei sudaro produktų sąrašą
CREATE FUNCTION pg_temp.finalize_plan(p_plan BIGINT) RETURNS VOID AS $$
DECLARE
    v_list BIGINT;
BEGIN
    UPDATE dish d SET
        portion_grams = s.grams,
        kcal = round(s.kcal),
        protein_g = round(s.protein, 1),
        fat_g = round(s.fat, 1),
        carbs_g = round(s.carbs, 1)
    FROM (
        SELECT r.dish_id,
               sum(i.grams) AS grams,
               sum(i.grams * p.kcal / 100.0) AS kcal,
               sum(i.grams * p.protein_g / 100.0) AS protein,
               sum(i.grams * p.fat_g / 100.0) AS fat,
               sum(i.grams * p.carbs_g / 100.0) AS carbs
        FROM recipe r
        JOIN ingredient i ON i.recipe_id = r.id
        JOIN product p ON p.id = i.product_id
        GROUP BY r.dish_id
    ) s
    WHERE d.id = s.dish_id
      AND d.meal_id IN (SELECT m.id FROM meal m JOIN plan_day pd ON pd.id = m.plan_day_id WHERE pd.meal_plan_id = p_plan);

    UPDATE plan_day pd SET total_calories = s.kcal
    FROM (
        SELECT m.plan_day_id, sum(d.kcal) AS kcal
        FROM meal m JOIN dish d ON d.meal_id = m.id
        GROUP BY m.plan_day_id
    ) s
    WHERE pd.id = s.plan_day_id AND pd.meal_plan_id = p_plan;

    INSERT INTO shopping_list (meal_plan_id) VALUES (p_plan) RETURNING id INTO v_list;
    INSERT INTO shopping_list_item (shopping_list_id, product_id, total_grams)
    SELECT v_list, i.product_id, sum(i.grams)
    FROM plan_day pd
    JOIN meal m ON m.plan_day_id = pd.id
    JOIN dish d ON d.meal_id = m.id
    JOIN recipe r ON r.dish_id = d.id
    JOIN ingredient i ON i.recipe_id = r.id
    WHERE pd.meal_plan_id = p_plan
    GROUP BY i.product_id;
END;
$$ LANGUAGE plpgsql;

-- Nukopijuoja plano šabloną klientui, proporcingai pakoreguodama porcijas pagal paros poreikį
-- (pusryčiai 25 %, pietūs 40 %, vakarienė 35 %)
CREATE FUNCTION pg_temp.copy_plan(p_template BIGINT, p_client BIGINT, p_daily INT, p_active BOOLEAN,
                                  p_created TIMESTAMPTZ) RETURNS BIGINT AS $$
DECLARE
    v_plan   BIGINT;
    v_day    BIGINT;
    v_meal   BIGINT;
    v_dish   BIGINT;
    v_recipe BIGINT;
    v_target INT;
    v_factor NUMERIC;
    sd       RECORD;
    sm       RECORD;
BEGIN
    INSERT INTO meal_plan (client_id, created_at, active, ai_generated, daily_calories)
    VALUES (p_client, p_created, p_active, FALSE, p_daily) RETURNING id INTO v_plan;
    FOR sd IN SELECT * FROM plan_day WHERE meal_plan_id = p_template ORDER BY day_number LOOP
        INSERT INTO plan_day (meal_plan_id, day_number) VALUES (v_plan, sd.day_number) RETURNING id INTO v_day;
        FOR sm IN SELECT m.type, d.id AS dish_id, d.name, d.kcal, r.id AS recipe_id
                  FROM meal m JOIN dish d ON d.meal_id = m.id JOIN recipe r ON r.dish_id = d.id
                  WHERE m.plan_day_id = sd.id LOOP
            v_target := round(p_daily * CASE sm.type WHEN 'BREAKFAST' THEN 0.25 WHEN 'LUNCH' THEN 0.40 ELSE 0.35 END);
            v_factor := v_target::NUMERIC / sm.kcal;
            INSERT INTO meal (plan_day_id, type, target_calories) VALUES (v_day, sm.type, v_target) RETURNING id INTO v_meal;
            INSERT INTO dish (meal_id, name) VALUES (v_meal, sm.name) RETURNING id INTO v_dish;
            INSERT INTO recipe (dish_id) VALUES (v_dish) RETURNING id INTO v_recipe;
            INSERT INTO ingredient (recipe_id, product_id, grams)
            SELECT v_recipe, product_id, greatest(1, round(grams * v_factor)) FROM ingredient WHERE recipe_id = sm.recipe_id;
            INSERT INTO recipe_step (recipe_id, step_number, description)
            SELECT v_recipe, step_number, description FROM recipe_step WHERE recipe_id = sm.recipe_id;
        END LOOP;
    END LOOP;
    PERFORM pg_temp.finalize_plan(v_plan);
    RETURN v_plan;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    v_template BIGINT;
BEGIN
    -- Demonstracinis plano šablonas (client_id IS NULL), iš kurio 1 sprinte sudaromi klientų planai
    INSERT INTO meal_plan (client_id, active, ai_generated) VALUES (NULL, FALSE, FALSE) RETURNING id INTO v_template;

    -- 1 diena
    PERFORM pg_temp.add_dish(v_template, 1, 'BREAKFAST', 'Avižinė košė su mėlynėmis ir graikišku jogurtu',
        '173904:70,171267:200,171711:100,170894:100,169640:10',
        ARRAY['Užvirinkite pieną ir suberkite avižinius dribsnius.',
              'Virkite 5 min. nuolat maišydami, kol košė sutirštės.',
              'Patiekite su graikišku jogurtu, mėlynėmis ir medumi.']);
    PERFORM pg_temp.add_dish(v_template, 1, 'LUNCH', 'Vištienos krūtinėlė su grikiais ir daržovėmis',
        '171077:180,170685:90,170379:150,170393:80,171413:10,173468:2',
        ARRAY['Grikius išvirkite pasūdytame vandenyje (apie 15 min.).',
              'Vištienos krūtinėlę pasūdykite ir apkepkite aliejuje po 6–7 min. iš abiejų pusių.',
              'Brokolius ir morkas troškinkite garuose 7 min.',
              'Patiekite vištieną su grikiais ir daržovėmis.']);
    PERFORM pg_temp.add_dish(v_template, 1, 'DINNER', 'Lęšių troškinys su pomidorais ir ryžiais',
        '172420:80,170501:200,170000:60,170393:60,168877:60,171413:10,169230:5,170923:2',
        ARRAY['Svogūną, česnaką ir morką pakepinkite aliejuje.',
              'Suberkite lęšius, kmynus, supilkite trintus pomidorus ir 300 ml vandens.',
              'Troškinkite 25 min., kol lęšiai suminkštės.',
              'Atskirai išvirkite ryžius ir patiekite su troškiniu.']);
    -- 2 diena
    PERFORM pg_temp.add_dish(v_template, 2, 'BREAKFAST', 'Omletas su špinatais ir viso grūdo duona',
        '171287:150,168462:60,170457:80,172688:70,173410:5',
        ARRAY['Kiaušinius išplakite su žiupsneliu druskos.',
              'Ištirpinkite sviestą keptuvėje, suberkite špinatus ir supilkite kiaušinius.',
              'Kepkite 4 min. ant vidutinės ugnies.',
              'Patiekite su duona ir pomidorų griežinėliais.']);
    PERFORM pg_temp.add_dish(v_template, 2, 'LUNCH', 'Lašiša su bulvėmis ir agurkų salotomis',
        '175167:150,170026:250,168409:120,171257:30,172233:5',
        ARRAY['Bulves nulupkite ir išvirkite (20 min.).',
              'Lašišą kepkite orkaitėje 200 °C temperatūroje 15 min.',
              'Agurkus supjaustykite ir sumaišykite su grietine ir krapais.']);
    PERFORM pg_temp.add_dish(v_template, 2, 'DINNER', 'Kalakutienos ir daržovių troškinys su bulguru',
        '171505:150,170688:70,169291:150,170108:100,170000:50,171413:10',
        ARRAY['Bulgurą užpilkite verdančiu vandeniu ir palikite 15 min.',
              'Kalakutieną apkepkite aliejuje su svogūnu.',
              'Suberkite cukinijas ir paprikas, troškinkite 10 min.',
              'Patiekite su bulguru.']);
    -- 3 diena
    PERFORM pg_temp.add_dish(v_template, 3, 'BREAKFAST', 'Varškė su braškėmis, riešutais ir rugine duona',
        '173417:200,167762:150,169640:15,170187:15,172684:50',
        ARRAY['Braškes supjaustykite.',
              'Varškę sumaišykite su medumi ir braškėmis, apibarstykite riešutais.',
              'Patiekite su rugine duona.']);
    PERFORM pg_temp.add_dish(v_template, 3, 'LUNCH', 'Jautienos troškinys su perlinėmis kruopomis',
        '174055:150,170284:80,170393:80,170000:60,169988:40,172336:10',
        ARRAY['Jautieną supjaustykite kubeliais ir apkepkite aliejuje.',
              'Suberkite svogūną, morkas ir salierą, supilkite 500 ml vandens.',
              'Suberkite perlines kruopas ir troškinkite 45 min.']);
    PERFORM pg_temp.add_dish(v_template, 3, 'DINNER', 'Menkė su rudaisiais ryžiais ir šparaginėmis pupelėmis',
        '171955:180,169703:80,169961:150,167746:20,171413:10',
        ARRAY['Ryžius išvirkite (30 min.).',
              'Menkę apšlakstykite citrinos sultimis ir kepkite orkaitėje 180 °C 15 min.',
              'Šparagines pupeles išvirkite per 5 min. ir apšlakstykite aliejumi.']);
    -- 4 diena
    PERFORM pg_temp.add_dish(v_template, 4, 'BREAKFAST', 'Kiaušinienė su avokadu ir rugine duona',
        '171287:120,171705:70,172684:70,170457:80',
        ARRAY['Kiaušinius iškepkite keptuvėje.',
              'Avokadą sutrinkite šakute ir užtepkite ant duonos.',
              'Patiekite su kiaušiniais ir pomidorais.']);
    PERFORM pg_temp.add_dish(v_template, 4, 'LUNCH', 'Kiaulienos išpjova su batatais ir brokoliais',
        '168249:170,168482:250,170379:150,172336:10',
        ARRAY['Batatus supjaustykite skiltelėmis ir kepkite orkaitėje 200 °C 25 min.',
              'Kiaulienos išpjovą apkepkite aliejuje ir kepkite orkaitėje dar 15 min.',
              'Brokolius troškinkite garuose 6 min.']);
    PERFORM pg_temp.add_dish(v_template, 4, 'DINNER', 'Viso grūdo makaronai su vištiena ir pomidorų padažu',
        '169738:90,171077:120,170501:150,169230:5,170848:15,171317:1',
        ARRAY['Makaronus išvirkite pagal nurodymus ant pakuotės.',
              'Vištieną supjaustykite ir apkepkite su česnaku.',
              'Supilkite trintus pomidorus, įberkite baziliko ir troškinkite 10 min.',
              'Sumaišykite su makaronais ir apibarstykite parmezanu.']);
    -- 5 diena
    PERFORM pg_temp.add_dish(v_template, 5, 'BREAKFAST', 'Bananų ir kefyro kokteilis su avižomis',
        '170904:300,173944:120,173904:40,170554:10',
        ARRAY['Visus produktus sudėkite į trintuvą.',
              'Trinkite 1 min., kol masė taps vientisa.']);
    PERFORM pg_temp.add_dish(v_template, 5, 'LUNCH', 'Avinžirnių ir daržovių karis su ryžiais',
        '173756:80,170173:100,168877:70,168462:80,170000:50,172231:2,170457:100',
        ARRAY['Avinžirnius išmirkykite per naktį ir išvirkite (60 min.).',
              'Svogūną pakepinkite, įberkite ciberžolės ir pomidorų.',
              'Supilkite kokosų pieną, suberkite avinžirnius ir špinatus, troškinkite 10 min.',
              'Patiekite su virtais ryžiais.']);
    PERFORM pg_temp.add_dish(v_template, 5, 'DINNER', 'Kepta skumbrė su bulvių koše ir raugintais kopūstais',
        '175119:150,170026:250,171267:50,173410:10,169279:100',
        ARRAY['Bulves išvirkite ir sutrinkite su pienu ir sviestu.',
              'Skumbrę kepkite orkaitėje 200 °C 18 min.',
              'Patiekite su raugintais kopūstais.']);
    -- 6 diena
    PERFORM pg_temp.add_dish(v_template, 6, 'BREAKFAST', 'Jogurtas su granola ir avietėmis',
        '170886:200,171646:50,167755:100',
        ARRAY['Į dubenėlį įdėkite jogurtą.',
              'Apibarstykite granola ir avietėmis.']);
    PERFORM pg_temp.add_dish(v_template, 6, 'LUNCH', 'Vištienos šlaunelės su kuskusu ir cukinijomis',
        '173627:180,169699:80,169291:150,170108:80,171413:10',
        ARRAY['Kuskusą užpilkite verdančiu vandeniu ir palikite 5 min.',
              'Vištienos šlaunelių mėsą kepkite keptuvėje 12 min.',
              'Cukinijas ir paprikas apkepkite aliejuje.',
              'Patiekite viską kartu.']);
    PERFORM pg_temp.add_dish(v_template, 6, 'DINNER', 'Tofu ir daržovių keptuvė su ryžių makaronais',
        '172475:180,169742:70,170379:100,170393:70,174277:15,170150:5,172336:10',
        ARRAY['Ryžių makaronus užpilkite karštu vandeniu 10 min.',
              'Tofu supjaustykite kubeliais ir apkepkite aliejuje.',
              'Suberkite brokolius ir morkas, kepkite 5 min.',
              'Sumaišykite su makaronais, sojų padažu ir sezamo sėklomis.']);
    -- 7 diena
    PERFORM pg_temp.add_dish(v_template, 7, 'BREAKFAST', 'Sumuštiniai su kumpiu, sūriu ir daržovėmis',
        '172688:90,173864:60,171251:30,168409:80,169249:30',
        ARRAY['Ant duonos riekių dėkite salotų lapus, kumpį ir sūrį.',
              'Papuoškite agurkų griežinėliais.']);
    PERFORM pg_temp.add_dish(v_template, 7, 'LUNCH', 'Upėtakis su kinoja ir smidrais',
        '173717:170,168874:80,168389:150,171413:10,167746:20',
        ARRAY['Kinoją išvirkite (15 min.).',
              'Upėtakį kepkite keptuvėje po 4 min. iš abiejų pusių.',
              'Smidrus apkepkite aliejuje ir apšlakstykite citrinos sultimis.']);
    PERFORM pg_temp.add_dish(v_template, 7, 'DINNER', 'Grybų ir grikių troškinys su grietine',
        '170685:80,169251:200,170000:60,171257:40,170393:60,172336:10',
        ARRAY['Grikius išvirkite (15 min.).',
              'Svogūną ir morkas pakepinkite aliejuje, suberkite pievagrybius ir kepkite 10 min.',
              'Sumaišykite su grikiais ir grietine.']);

    PERFORM pg_temp.finalize_plan(v_template);
END;
$$;

-- Naudotojai
INSERT INTO app_user (role, email, password_hash, first_name, last_name, created_at,
                      health_data_consent, theme, first_day_of_week, must_change_password) VALUES
    ('ADMIN', 'admin@mealplanner.lt', '$2a$10$rzxYapPBGRw43U83r9X2HOzZ61AtI.uVqkJfLHmNH3fO.yWQ8SBIa',
     'Administratorius', 'Sistemos', now() - INTERVAL '30 days', NULL, NULL, NULL, NULL),
    ('DIETITIAN', 'dietologas@mealplanner.lt', '$2a$10$rzxYapPBGRw43U83r9X2HOzZ61AtI.uVqkJfLHmNH3fO.yWQ8SBIa',
     'Rūta', 'Dietologė', now() - INTERVAL '30 days', NULL, NULL, NULL, FALSE),
    ('DIETITIAN', 'dietologas2@mealplanner.lt', '$2a$10$rzxYapPBGRw43U83r9X2HOzZ61AtI.uVqkJfLHmNH3fO.yWQ8SBIa',
     'Tomas', 'Dietologas', now() - INTERVAL '20 days', NULL, NULL, NULL, FALSE),
    ('CLIENT', 'klientas@mealplanner.lt', '$2a$10$rzxYapPBGRw43U83r9X2HOzZ61AtI.uVqkJfLHmNH3fO.yWQ8SBIa',
     'Jonas', NULL, now() - INTERVAL '10 days', TRUE, 'SYSTEM', 'MONDAY', NULL),
    ('CLIENT', 'klientas2@mealplanner.lt', '$2a$10$rzxYapPBGRw43U83r9X2HOzZ61AtI.uVqkJfLHmNH3fO.yWQ8SBIa',
     'Ona', NULL, now() - INTERVAL '2 days', TRUE, 'SYSTEM', 'MONDAY', NULL),
    ('CLIENT', 'klientas3@mealplanner.lt', '$2a$10$rzxYapPBGRw43U83r9X2HOzZ61AtI.uVqkJfLHmNH3fO.yWQ8SBIa',
     'Petras', NULL, now() - INTERVAL '40 days', TRUE, 'DARK', 'SUNDAY', NULL);

-- Prenumeratos: klientas – aktyvi, klientas2 – bandomasis laikotarpis, klientas3 – pasibaigusi
INSERT INTO subscription (client_id, trial_ends_at, paid_until)
SELECT id, created_at + INTERVAL '7 days',
       CASE email WHEN 'klientas@mealplanner.lt' THEN now() + INTERVAL '27 days' END
FROM app_user WHERE role = 'CLIENT';

-- Anketos (KMI ir paros poreikis apskaičiuoti pagal Mifflin–St Jeor formulę)
-- klientas: 180 cm, 80 kg, 30 m., vyras, vidutinis aktyvumas, sulieknėti:
--   BMR = 10*80 + 6,25*180 - 5*30 + 5 = 1780; 1780 * 1,55 * 0,85 = 2345,15 -> 2345 kcal; KMI = 24,7
-- klientas2: 165 cm, 62,5 kg, 27 m., moteris, lengvas aktyvumas, palaikyti svorį:
--   BMR = 625 + 1031,25 - 135 - 161 = 1360,25; *1,375 = 1870,34 -> 1870 kcal; KMI = 23,0
-- klientas3: 175 cm, 95 kg, 45 m., vyras, sėslus, sulieknėti:
--   BMR = 950 + 1093,75 - 225 + 5 = 1823,75; *1,2*0,85 = 1860,23 -> 1860 kcal; KMI = 31,0
INSERT INTO questionnaire (client_id, height_cm, weight_kg, age, gender, activity_level, goal, chronic_disease,
                           bmi, daily_calories, updated_at)
SELECT u.id, v.height, v.weight, v.age, v.gender, v.activity, v.goal, v.chronic, v.bmi, v.calories, now() - v.ago
FROM (VALUES
    ('klientas@mealplanner.lt', 180, 80.0, 30, 'MALE', 'MODERATE', 'LOSE_WEIGHT', FALSE, 24.7, 2345, INTERVAL '8 days'),
    ('klientas2@mealplanner.lt', 165, 62.5, 27, 'FEMALE', 'LIGHT', 'MAINTAIN', FALSE, 23.0, 1870, INTERVAL '1 day'),
    ('klientas3@mealplanner.lt', 175, 95.0, 45, 'MALE', 'SEDENTARY', 'LOSE_WEIGHT', TRUE, 31.0, 1860, INTERVAL '35 days')
) AS v(email, height, weight, age, gender, activity, goal, chronic, bmi, calories, ago)
JOIN app_user u ON u.email = v.email;

INSERT INTO questionnaire_allergen (questionnaire_id, allergen_id)
SELECT q.id, a.id FROM questionnaire q JOIN app_user u ON u.id = q.client_id
JOIN allergen a ON a.code IN ('RIESUTAI')
WHERE u.email = 'klientas2@mealplanner.lt';

INSERT INTO questionnaire_disliked_product (questionnaire_id, product_id)
SELECT q.id, p.id FROM questionnaire q JOIN app_user u ON u.id = q.client_id
JOIN product p ON p.fdc_id IN (169251, 175119)
WHERE u.email = 'klientas@mealplanner.lt';

-- Klientų planai, sudaryti iš demonstracinio šablono
DO $$
DECLARE
    v_template BIGINT;
    v_client   BIGINT;
    v_client3  BIGINT;
    v_old      BIGINT;
    v_plan     BIGINT;
    v_plan3    BIGINT;
    v_payment  BIGINT;
    v_review   BIGINT;
    v_dietitian BIGINT;
BEGIN
    SELECT id INTO v_template FROM meal_plan WHERE client_id IS NULL ORDER BY id LIMIT 1;
    SELECT id INTO v_client FROM app_user WHERE email = 'klientas@mealplanner.lt';
    SELECT id INTO v_client3 FROM app_user WHERE email = 'klientas3@mealplanner.lt';
    SELECT id INTO v_dietitian FROM app_user WHERE email = 'dietologas@mealplanner.lt';

    v_old := pg_temp.copy_plan(v_template, v_client, 2400, FALSE, now() - INTERVAL '9 days');
    v_plan := pg_temp.copy_plan(v_template, v_client, 2345, TRUE, now() - INTERVAL '8 days');
    v_plan3 := pg_temp.copy_plan(v_template, v_client3, 1860, TRUE, now() - INTERVAL '35 days');

    -- Kelios produktų sąrašo varnelės
    UPDATE shopping_list_item SET purchased = TRUE
    WHERE id IN (SELECT sli.id FROM shopping_list_item sli JOIN shopping_list sl ON sl.id = sli.shopping_list_id
                 WHERE sl.meal_plan_id = v_plan ORDER BY sli.id LIMIT 4);

    -- Mokėjimai
    INSERT INTO payment (reference, client_id, amount, purpose, method, status, created_at)
    VALUES (gen_random_uuid(), v_client, 14.99, 'SUBSCRIPTION', 'CARD', 'PAID', now() - INTERVAL '3 days'),
           (gen_random_uuid(), v_client3, 14.99, 'SUBSCRIPTION', 'BANK_LINK', 'FAILED', now() - INTERVAL '30 days');

    INSERT INTO payment (reference, client_id, amount, purpose, method, status, created_at, review_meal_plan_id, review_question)
    VALUES (gen_random_uuid(), v_client, 19.99, 'REVIEW', 'BANK_LINK', 'PAID', now() - INTERVAL '7 days', v_plan,
            'Ar šis planas tinka, jei sportuoju 4 kartus per savaitę?')
    RETURNING id INTO v_payment;

    -- Atlikta dietologo peržiūra su komentarais
    INSERT INTO review_request (client_id, meal_plan_id, payment_id, dietitian_id, question, status, submitted_at, reviewed_at)
    VALUES (v_client, v_plan, v_payment, v_dietitian, 'Ar šis planas tinka, jei sportuoju 4 kartus per savaitę?',
            'COMPLETED', now() - INTERVAL '7 days', now() - INTERVAL '6 days')
    RETURNING id INTO v_review;

    INSERT INTO comment (review_request_id, dish_id, text, created_at)
    VALUES (v_review, NULL, 'Planas subalansuotas ir tinka jūsų aktyvumui. Treniruočių dienomis rekomenduoju po treniruotės suvalgyti papildomą baltymų šaltinį, pvz., graikiško jogurto.', now() - INTERVAL '6 days');

    INSERT INTO comment (review_request_id, dish_id, text, created_at)
    SELECT v_review, d.id, 'Puikus baltymų ir angliavandenių derinys po treniruotės.', now() - INTERVAL '6 days'
    FROM dish d JOIN meal m ON m.id = d.meal_id JOIN plan_day pd ON pd.id = m.plan_day_id
    WHERE pd.meal_plan_id = v_plan AND pd.day_number = 1 AND m.type = 'LUNCH';

    INSERT INTO comment (review_request_id, dish_id, text, created_at)
    SELECT v_review, d.id, 'Skumbrėje daug naudingų omega-3 riebalų rūgščių – palikite šį patiekalą.', now() - INTERVAL '6 days'
    FROM dish d JOIN meal m ON m.id = d.meal_id JOIN plan_day pd ON pd.id = m.plan_day_id
    WHERE pd.meal_plan_id = v_plan AND pd.day_number = 5 AND m.type = 'DINNER';

    -- Laukianti peržiūros užklausa (kitam klientui)
    INSERT INTO payment (reference, client_id, amount, purpose, method, status, created_at, review_meal_plan_id, review_question)
    VALUES (gen_random_uuid(), v_client3, 19.99, 'REVIEW', 'CARD', 'PAID', now() - INTERVAL '1 day', v_plan3,
            'Turiu II tipo diabetą. Ar galiu laikytis šio plano?')
    RETURNING id INTO v_payment;

    INSERT INTO review_request (client_id, meal_plan_id, payment_id, question, status, submitted_at)
    VALUES (v_client3, v_plan3, v_payment, 'Turiu II tipo diabetą. Ar galiu laikytis šio plano?', 'PENDING',
            now() - INTERVAL '1 day');

    -- Pranešimai
    INSERT INTO notification (client_id, message, created_at, read)
    VALUES (v_client, 'Dietologė Rūta Dietologė peržiūrėjo jūsų mitybos planą. Komentarai pateikti plano lange.', now() - INTERVAL '6 days', FALSE),
           (v_client, 'Prenumerata aktyvuota 30 dienų.', now() - INTERVAL '3 days', TRUE),
           (v_client3, 'Peržiūros užklausa sukurta. Būsena – „Laukia“.', now() - INTERVAL '1 day', FALSE);
END;
$$;

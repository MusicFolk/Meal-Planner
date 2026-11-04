-- Sugeneruota skriptu backend/tools/usda/generate_products.py
-- Šaltinis: USDA FoodData Central, SR Legacy (2018-04). Maistinė vertė 100 g.

INSERT INTO product (name, fdc_id, category, kcal, protein_g, fat_g, carbs_g) VALUES
  ('Vištienos krūtinėlė', 171077, 'MEAT_FISH', 120, 22.50, 2.62, 0.00), -- Chicken, broiler or fryers, breast, skinless, boneless, meat only, raw
  ('Vištienos šlaunelės mėsa', 173627, 'MEAT_FISH', 121, 19.66, 4.12, 0.00), -- Chicken, broilers or fryers, dark meat, thigh, meat only, raw
  ('Malta vištiena', 171116, 'MEAT_FISH', 143, 17.44, 8.10, 0.04), -- Chicken, ground, raw
  ('Kalakutienos krūtinėlė', 171098, 'MEAT_FISH', 114, 23.66, 1.48, 0.14), -- Turkey, whole, breast, meat only, raw
  ('Malta kalakutiena', 171505, 'MEAT_FISH', 148, 19.66, 7.66, 0.00), -- Turkey, Ground, raw
  ('Malta jautiena (10 % riebumo)', 174030, 'MEAT_FISH', 176, 20.00, 10.00, 0.00), -- Beef, ground, 90% lean meat / 10% fat, raw
  ('Jautienos nugarinė', 174055, 'MEAT_FISH', 131, 22.09, 4.08, 0.00), -- Beef, top sirloin, steak, separable lean only, trimmed to 1/8" fat, all grades, 
  ('Kiaulienos išpjova', 168249, 'MEAT_FISH', 109, 20.95, 2.17, 0.00), -- Pork, fresh, loin, tenderloin, separable lean only, raw
  ('Kiaulienos karbonadas', 167829, 'MEAT_FISH', 127, 21.99, 3.71, 0.00), -- Pork, fresh, loin, center loin (chops), bone-in, separable lean only, raw
  ('Kumpis, griežinėliais', 173864, 'MEAT_FISH', 164, 16.60, 8.80, 3.63), -- Ham, sliced, regular (approximately 11% fat)
  ('Lašiša', 175167, 'MEAT_FISH', 208, 20.42, 13.42, 0.00), -- Fish, salmon, Atlantic, farmed, raw
  ('Menkė', 171955, 'MEAT_FISH', 82, 17.81, 0.67, 0.00), -- Fish, cod, Atlantic, raw
  ('Tunas savo sultyse', 173709, 'MEAT_FISH', 86, 19.44, 0.96, 0.00), -- Fish, tuna, light, canned in water, drained solids (Includes foods for USDA's Fo
  ('Skumbrė', 175119, 'MEAT_FISH', 205, 18.60, 13.89, 0.00), -- Fish, mackerel, Atlantic, raw
  ('Silkė', 175116, 'MEAT_FISH', 158, 17.96, 9.04, 0.00), -- Fish, herring, Atlantic, raw
  ('Upėtakis', 173717, 'MEAT_FISH', 141, 19.94, 6.18, 0.00), -- Fish, trout, rainbow, farmed, raw
  ('Tilapija', 175176, 'MEAT_FISH', 96, 20.08, 1.70, 0.00), -- Fish, tilapia, raw
  ('Polakas', 173725, 'MEAT_FISH', 76, 17.17, 0.82, 0.00), -- Fish, pollock, Alaska, raw
  ('Krevetės', 175179, 'MEAT_FISH', 85, 20.10, 0.51, 0.00), -- Crustaceans, shrimp, raw
  ('Kiaušiniai', 171287, 'DAIRY_EGGS', 143, 12.56, 9.51, 0.72), -- Egg, whole, raw, fresh
  ('Pienas 2 %', 171267, 'DAIRY_EGGS', 50, 3.30, 1.98, 4.80), -- Milk, reduced fat, fluid, 2% milkfat, with added vitamin A and vitamin D
  ('Pienas 1 %', 170872, 'DAIRY_EGGS', 42, 3.37, 0.97, 4.99), -- Milk, lowfat, fluid, 1% milkfat, with added vitamin A and vitamin D
  ('Graikiškas jogurtas, neriebus', 170894, 'DAIRY_EGGS', 59, 10.19, 0.39, 3.60), -- Yogurt, Greek, plain, nonfat (Includes foods for USDA's Food Distribution Progra
  ('Natūralus jogurtas', 170886, 'DAIRY_EGGS', 63, 5.25, 1.55, 7.04), -- Yogurt, plain, low fat
  ('Varškė 1 %', 173417, 'DAIRY_EGGS', 72, 12.39, 1.02, 2.72), -- Cheese, cottage, lowfat, 1% milkfat
  ('Grūdėta varškė', 172179, 'DAIRY_EGGS', 98, 11.12, 4.30, 3.38), -- Cheese, cottage, creamed, large or small curd
  ('Mocarela', 170847, 'DAIRY_EGGS', 254, 24.26, 15.92, 2.77), -- Cheese, mozzarella, part skim milk
  ('Parmezanas', 170848, 'DAIRY_EGGS', 392, 35.75, 25.00, 3.22), -- Cheese, parmesan, hard
  ('Čederio sūris', 173414, 'DAIRY_EGGS', 403, 22.87, 33.31, 3.37), -- Cheese, cheddar (Includes foods for USDA's Food Distribution Program)
  ('Šveicariškas sūris', 171251, 'DAIRY_EGGS', 393, 26.96, 30.99, 1.44), -- Cheese, swiss
  ('Fetos sūris', 173420, 'DAIRY_EGGS', 265, 14.21, 21.49, 3.88), -- Cheese, feta
  ('Rikota', 171248, 'DAIRY_EGGS', 138, 11.39, 7.91, 5.14), -- Cheese, ricotta, part skim milk
  ('Kreminis sūris', 173418, 'DAIRY_EGGS', 350, 6.15, 34.44, 5.52), -- Cheese, cream
  ('Sviestas', 173410, 'DAIRY_EGGS', 717, 0.85, 81.11, 0.06), -- Butter, salted
  ('Grietinė', 171257, 'DAIRY_EGGS', 198, 2.44, 19.35, 4.63), -- Cream, sour, cultured
  ('Grietinėlė', 170857, 'DAIRY_EGGS', 195, 2.96, 19.10, 3.66), -- Cream, fluid, light (coffee cream or table cream)
  ('Kefyras', 170904, 'DAIRY_EGGS', 43, 3.79, 1.02, 4.77), -- Kefir, lowfat, plain, LIFEWAY
  ('Avižiniai dribsniai', 173904, 'GRAINS', 379, 13.15, 6.52, 67.70), -- Cereals, oats, regular and quick, not fortified, dry
  ('Grikiai', 170685, 'GRAINS', 346, 11.73, 2.71, 74.95), -- Buckwheat groats, roasted, dry
  ('Baltieji ryžiai', 168877, 'GRAINS', 365, 7.13, 0.66, 79.95), -- Rice, white, long-grain, regular, raw, enriched
  ('Rudieji ryžiai', 169703, 'GRAINS', 367, 7.54, 3.20, 76.25), -- Rice, brown, long-grain, raw (Includes foods for USDA's Food Distribution Progra
  ('Makaronai', 169736, 'GRAINS', 371, 13.04, 1.51, 74.67), -- Pasta, dry, enriched
  ('Viso grūdo makaronai', 169738, 'GRAINS', 352, 13.87, 2.93, 73.37), -- Pasta, whole-wheat, dry (Includes foods for USDA's Food Distribution Program)
  ('Kiaušininiai makaronai', 169731, 'GRAINS', 384, 14.16, 4.44, 71.27), -- Noodles, egg, dry, enriched
  ('Ryžių makaronai', 169742, 'GRAINS', 364, 5.95, 0.56, 80.18), -- Rice noodles, dry
  ('Bolivinė balanda (kinoja)', 168874, 'GRAINS', 368, 14.12, 6.07, 64.16), -- Quinoa, uncooked
  ('Perlinės kruopos', 170284, 'GRAINS', 352, 9.91, 1.16, 77.72), -- Barley, pearled, raw
  ('Bulguras', 170688, 'GRAINS', 342, 12.29, 1.33, 75.87), -- Bulgur, dry
  ('Kuskusas', 169699, 'GRAINS', 376, 12.76, 0.64, 77.43), -- Couscous, dry
  ('Soros', 169702, 'GRAINS', 378, 11.02, 4.22, 72.85), -- Millet, raw
  ('Manų kruopos', 169715, 'GRAINS', 360, 12.68, 1.05, 72.83), -- Semolina, enriched
  ('Kukurūzų kruopos', 171670, 'GRAINS', 371, 8.80, 1.20, 79.60), -- Cereals, corn grits, yellow, regular and quick, unenriched, dry
  ('Kvietiniai miltai', 168894, 'GRAINS', 364, 10.33, 0.98, 76.31), -- Wheat flour, white, all-purpose, enriched, bleached
  ('Viso grūdo duona', 172688, 'GRAINS', 252, 12.45, 3.50, 42.71), -- Bread, whole-wheat, commercially prepared
  ('Ruginė duona', 172684, 'GRAINS', 259, 8.50, 3.30, 48.30), -- Bread, rye
  ('Balta duona', 174924, 'GRAINS', 266, 8.85, 3.33, 49.42), -- Bread, white, commercially prepared (includes soft bread crumbs)
  ('Kvietinė tortilija', 173242, 'GRAINS', 325, 8.70, 7.10, 55.60), -- Tortillas, ready-to-bake or -fry, flour, without added calcium
  ('Viso grūdo traškučiai', 172749, 'GRAINS', 427, 10.58, 14.13, 69.55), -- Crackers, whole-wheat
  ('Granola', 171646, 'GRAINS', 489, 13.67, 24.31, 53.88), -- Cereals ready-to-eat, granola, homemade
  ('Lęšiai', 172420, 'GRAINS', 352, 24.63, 1.06, 63.35), -- Lentils, raw
  ('Avinžirniai', 173756, 'GRAINS', 378, 20.47, 6.04, 62.95), -- Chickpeas (garbanzo beans, bengal gram), mature seeds, raw
  ('Raudonosios pupelės (konservuotos)', 173741, 'GRAINS', 84, 5.22, 0.60, 14.50), -- Beans, kidney, all types, mature seeds, canned
  ('Juodosios pupelės', 173734, 'GRAINS', 341, 21.60, 1.42, 62.36), -- Beans, black, mature seeds, raw
  ('Baltosios pupelės (konservuotos)', 175204, 'GRAINS', 114, 7.26, 0.29, 21.20), -- Beans, white, mature seeds, canned
  ('Žalieji žirneliai (šaldyti)', 170016, 'GRAINS', 77, 5.22, 0.40, 13.62), -- Peas, green, frozen, unprepared (Includes foods for USDA's Food Distribution Pro
  ('Edamamė pupelės', 168410, 'GRAINS', 109, 11.22, 4.73, 7.61), -- Edamame, frozen, unprepared
  ('Tofu', 172475, 'GRAINS', 144, 17.27, 8.72, 2.78), -- Tofu, raw, firm, prepared with calcium sulfate
  ('Bulvės', 170026, 'VEGETABLES', 77, 2.05, 0.09, 17.49), -- Potatoes, flesh and skin, raw
  ('Batatai', 168482, 'VEGETABLES', 86, 1.57, 0.05, 20.12), -- Sweet potato, raw, unprepared (Includes foods for USDA's Food Distribution Progr
  ('Morkos', 170393, 'VEGETABLES', 41, 0.93, 0.24, 9.58), -- Carrots, raw
  ('Svogūnai', 170000, 'VEGETABLES', 40, 1.10, 0.10, 9.34), -- Onions, raw
  ('Svogūnų laiškai', 170005, 'VEGETABLES', 32, 1.83, 0.19, 7.34), -- Onions, spring or scallions (includes tops and bulb), raw
  ('Česnakai', 169230, 'VEGETABLES', 149, 6.36, 0.50, 33.06), -- Garlic, raw
  ('Pomidorai', 170457, 'VEGETABLES', 18, 0.88, 0.20, 3.89), -- Tomatoes, red, ripe, raw, year round average
  ('Trinti pomidorai (konservuoti)', 170501, 'VEGETABLES', 32, 1.64, 0.28, 7.29), -- Tomatoes, crushed, canned
  ('Pomidorų pasta', 170459, 'VEGETABLES', 82, 4.32, 0.47, 18.91), -- Tomato products, canned, paste, without salt added (Includes foods for USDA's Fo
  ('Agurkai', 168409, 'VEGETABLES', 15, 0.65, 0.11, 3.63), -- Cucumber, with peel, raw
  ('Raudonosios paprikos', 170108, 'VEGETABLES', 26, 0.99, 0.30, 6.03), -- Peppers, sweet, red, raw
  ('Brokoliai', 170379, 'VEGETABLES', 34, 2.82, 0.37, 6.64), -- Broccoli, raw
  ('Žiediniai kopūstai', 169986, 'VEGETABLES', 25, 1.92, 0.28, 4.97), -- Cauliflower, raw
  ('Briuselio kopūstai', 170383, 'VEGETABLES', 43, 3.38, 0.30, 8.95), -- Brussels sprouts, raw
  ('Baltagūžiai kopūstai', 169975, 'VEGETABLES', 25, 1.28, 0.10, 5.80), -- Cabbage, raw
  ('Rauginti kopūstai', 169279, 'VEGETABLES', 19, 0.91, 0.14, 4.28), -- Sauerkraut, canned, solids and liquids
  ('Špinatai', 168462, 'VEGETABLES', 23, 2.86, 0.39, 3.63), -- Spinach, raw
  ('Lapiniai kopūstai (kale)', 168421, 'VEGETABLES', 35, 2.92, 1.49, 4.42), -- Kale, raw
  ('Salotos', 169249, 'VEGETABLES', 15, 1.36, 0.15, 2.87), -- Lettuce, green leaf, raw
  ('Cukinijos', 169291, 'VEGETABLES', 17, 1.21, 0.32, 3.11), -- Squash, summer, zucchini, includes skin, raw
  ('Baklažanai', 169228, 'VEGETABLES', 25, 0.98, 0.18, 5.88), -- Eggplant, raw
  ('Pievagrybiai', 169251, 'VEGETABLES', 22, 3.09, 0.34, 3.26), -- Mushrooms, white, raw
  ('Gluosniabudės', 168580, 'VEGETABLES', 33, 3.31, 0.41, 6.09), -- Mushrooms, oyster, raw
  ('Burokėliai', 169145, 'VEGETABLES', 43, 1.61, 0.17, 9.56), -- Beets, raw
  ('Salierų stiebai', 169988, 'VEGETABLES', 14, 0.69, 0.17, 2.97), -- Celery, raw
  ('Porai', 169246, 'VEGETABLES', 61, 1.50, 0.30, 14.15), -- Leeks, (bulb and lower leaf-portion), raw
  ('Šparaginės pupelės', 169961, 'VEGETABLES', 31, 1.83, 0.22, 6.97), -- Beans, snap, green, raw
  ('Moliūgas', 168448, 'VEGETABLES', 26, 1.00, 0.10, 6.50), -- Pumpkin, raw
  ('Smidrai', 168389, 'VEGETABLES', 20, 2.20, 0.12, 3.88), -- Asparagus, raw
  ('Ridikėliai', 169276, 'VEGETABLES', 16, 0.68, 0.10, 3.40), -- Radishes, raw
  ('Kukurūzai (šaldyti)', 168398, 'VEGETABLES', 88, 3.02, 0.78, 20.71), -- Corn, sweet, yellow, frozen, kernels cut off cob, unprepared (Includes foods for
  ('Krapai', 172233, 'VEGETABLES', 43, 3.46, 1.12, 7.02), -- Dill weed, fresh
  ('Petražolės', 170416, 'VEGETABLES', 36, 2.97, 0.79, 6.33), -- Parsley, fresh
  ('Imbieras', 169231, 'VEGETABLES', 80, 1.82, 0.75, 17.77), -- Ginger root, raw
  ('Avokadai', 171705, 'VEGETABLES', 160, 2.00, 14.66, 8.53), -- Avocados, raw, all commercial varieties
  ('Obuoliai', 171688, 'FRUITS', 52, 0.26, 0.17, 13.81), -- Apples, raw, with skin (Includes foods for USDA's Food Distribution Program)
  ('Bananai', 173944, 'FRUITS', 89, 1.09, 0.33, 22.84), -- Bananas, raw
  ('Mėlynės', 171711, 'FRUITS', 57, 0.74, 0.33, 14.49), -- Blueberries, raw
  ('Braškės', 167762, 'FRUITS', 32, 0.67, 0.30, 7.68), -- Strawberries, raw
  ('Avietės', 167755, 'FRUITS', 52, 1.20, 0.65, 11.94), -- Raspberries, raw
  ('Spanguolės', 171722, 'FRUITS', 46, 0.46, 0.13, 11.97), -- Cranberries, raw
  ('Apelsinai', 169097, 'FRUITS', 47, 0.94, 0.12, 11.75), -- Oranges, raw, all commercial varieties
  ('Citrinos', 167746, 'FRUITS', 29, 1.10, 0.30, 9.32), -- Lemons, raw, without peel
  ('Kriaušės', 169118, 'FRUITS', 57, 0.36, 0.14, 15.23), -- Pears, raw
  ('Vynuogės', 174683, 'FRUITS', 69, 0.72, 0.16, 18.10), -- Grapes, red or green (European type, such as Thompson seedless), raw
  ('Kiviai', 168153, 'FRUITS', 61, 1.14, 0.52, 14.66), -- Kiwifruit, green, raw
  ('Slyvos', 169949, 'FRUITS', 46, 0.70, 0.28, 11.42), -- Plums, raw
  ('Persikai', 169928, 'FRUITS', 39, 0.91, 0.25, 9.54), -- Peaches, yellow, raw
  ('Abrikosai', 171697, 'FRUITS', 48, 1.40, 0.39, 11.12), -- Apricots, raw
  ('Trešnės', 171719, 'FRUITS', 63, 1.06, 0.20, 16.01), -- Cherries, sweet, raw
  ('Mangai', 169910, 'FRUITS', 60, 0.82, 0.38, 14.98), -- Mangos, raw
  ('Ananasai', 169124, 'FRUITS', 50, 0.54, 0.12, 13.12), -- Pineapple, raw, all varieties
  ('Razinos', 168165, 'FRUITS', 299, 3.30, 0.25, 79.32), -- Raisins, dark, seedless (Includes foods for USDA's Food Distribution Program)
  ('Migdolai', 170567, 'NUTS_SEEDS', 579, 21.15, 49.93, 21.55), -- Nuts, almonds
  ('Graikiniai riešutai', 170187, 'NUTS_SEEDS', 654, 15.23, 65.21, 13.71), -- Nuts, walnuts, english
  ('Lazdyno riešutai', 170581, 'NUTS_SEEDS', 628, 14.95, 60.75, 16.70), -- Nuts, hazelnuts or filberts
  ('Anakardžiai', 170162, 'NUTS_SEEDS', 553, 18.22, 43.85, 30.19), -- Nuts, cashew nuts, raw
  ('Pistacijos', 170184, 'NUTS_SEEDS', 560, 20.16, 45.32, 27.17), -- Nuts, pistachio nuts, raw
  ('Žemės riešutų sviestas', 172470, 'NUTS_SEEDS', 598, 22.21, 51.36, 22.31), -- Peanut butter, smooth style, without salt
  ('Saulėgrąžos', 170562, 'NUTS_SEEDS', 584, 20.78, 51.46, 20.00), -- Seeds, sunflower seed kernels, dried
  ('Moliūgų sėklos', 170556, 'NUTS_SEEDS', 559, 30.23, 49.05, 10.71), -- Seeds, pumpkin and squash seed kernels, dried
  ('Ispaninio šalavijo (chia) sėklos', 170554, 'NUTS_SEEDS', 486, 16.54, 30.74, 42.12), -- Seeds, chia seeds, dried
  ('Linų sėmenys', 169414, 'NUTS_SEEDS', 534, 18.29, 42.16, 28.88), -- Seeds, flaxseed
  ('Sezamo sėklos', 170150, 'NUTS_SEEDS', 573, 17.73, 49.67, 23.45), -- Seeds, sesame seeds, whole, dried
  ('Tahini (sezamų pasta)', 170189, 'NUTS_SEEDS', 595, 17.00, 53.76, 21.19), -- Seeds, sesame butter, tahini, from roasted and toasted kernels (most common type
  ('Alyvuogių aliejus', 171413, 'FATS_SPICES', 884, 0.00, 100.00, 0.00), -- Oil, olive, salad or cooking
  ('Rapsų aliejus', 172336, 'FATS_SPICES', 884, 0.00, 100.00, 0.00), -- Oil, canola
  ('Saulėgrąžų aliejus', 171025, 'FATS_SPICES', 884, 0.00, 100.00, 0.00), -- Oil, sunflower, linoleic, (approx. 65%)
  ('Juodieji pipirai', 170931, 'FATS_SPICES', 251, 10.39, 3.26, 63.95), -- Spices, pepper, black
  ('Druska', 173468, 'FATS_SPICES', 0, 0.00, 0.00, 0.00), -- Salt, table
  ('Paprika (prieskonis)', 171329, 'FATS_SPICES', 282, 14.14, 12.89, 53.99), -- Spices, paprika
  ('Malustas cinamonas', 171320, 'FATS_SPICES', 247, 3.99, 1.24, 80.59), -- Spices, cinnamon, ground
  ('Džiovintas bazilikas', 171317, 'FATS_SPICES', 233, 22.98, 4.07, 47.75), -- Spices, basil, dried
  ('Džiovintas raudonėlis', 171328, 'FATS_SPICES', 265, 9.00, 4.28, 68.92), -- Spices, oregano, dried
  ('Ciberžolė', 172231, 'FATS_SPICES', 312, 9.68, 3.25, 67.14), -- Spices, turmeric, ground
  ('Kmynai (kuminas)', 170923, 'FATS_SPICES', 375, 17.81, 22.27, 44.24), -- Spices, cumin seed
  ('Sojų padažas', 174277, 'FATS_SPICES', 53, 8.14, 0.57, 4.93), -- Soy sauce made from soy and wheat (shoyu)
  ('Garstyčios', 172234, 'FATS_SPICES', 60, 3.74, 3.34, 5.83), -- Mustard, prepared, yellow
  ('Obuolių actas', 173469, 'FATS_SPICES', 21, 0.00, 0.00, 0.93), -- Vinegar, cider
  ('Medus', 169640, 'OTHER', 304, 0.30, 0.00, 82.40), -- Honey
  ('Cukrus', 169655, 'OTHER', 387, 0.00, 0.00, 99.98), -- Sugars, granulated
  ('Uogienė', 169641, 'OTHER', 278, 0.37, 0.07, 68.86), -- Jams and preserves
  ('Kakavos milteliai', 169593, 'OTHER', 228, 19.60, 13.70, 57.90), -- Cocoa, dry powder, unsweetened
  ('Humusas', 174289, 'OTHER', 237, 7.78, 17.82, 15.00), -- Hummus, commercial
  ('Majonezas, neriebus', 171443, 'OTHER', 361, 0.37, 40.00, 0.00), -- Mayonnaise, reduced fat, with olive oil
  ('Sojų gėrimas', 172446, 'OTHER', 54, 3.27, 1.75, 6.28), -- Soymilk, original and vanilla, unfortified
  ('Migdolų gėrimas', 174832, 'OTHER', 15, 0.40, 0.96, 1.31), -- Beverages, almond milk, unsweetened, shelf stable
  ('Kokosų pienas', 170173, 'OTHER', 197, 2.02, 21.33, 2.81), -- Nuts, coconut milk, canned (liquid expressed from grated meat and water)
  ('Juodosios alyvuogės', 169094, 'OTHER', 116, 0.84, 10.90, 6.04), -- Olives, ripe, canned (small-extra large)
  ('Daržovių sultinys', 171583, 'OTHER', 5, 0.24, 0.07, 0.93); -- Soup, vegetable broth, ready to serve

INSERT INTO product_allergen (product_id, allergen_id)
SELECT p.id, a.id FROM (VALUES
  (175167, 'ZUVIS'),
  (171955, 'ZUVIS'),
  (173709, 'ZUVIS'),
  (175119, 'ZUVIS'),
  (175116, 'ZUVIS'),
  (173717, 'ZUVIS'),
  (175176, 'ZUVIS'),
  (173725, 'ZUVIS'),
  (175179, 'VEZIAGYVIAI'),
  (171287, 'KIAUSINIAI'),
  (171267, 'LAKTOZE'),
  (170872, 'LAKTOZE'),
  (170894, 'LAKTOZE'),
  (170886, 'LAKTOZE'),
  (173417, 'LAKTOZE'),
  (172179, 'LAKTOZE'),
  (170847, 'LAKTOZE'),
  (170848, 'LAKTOZE'),
  (173414, 'LAKTOZE'),
  (171251, 'LAKTOZE'),
  (173420, 'LAKTOZE'),
  (171248, 'LAKTOZE'),
  (173418, 'LAKTOZE'),
  (173410, 'LAKTOZE'),
  (171257, 'LAKTOZE'),
  (170857, 'LAKTOZE'),
  (170904, 'LAKTOZE'),
  (173904, 'GLITIMAS'),
  (169736, 'GLITIMAS'),
  (169738, 'GLITIMAS'),
  (169731, 'GLITIMAS'),
  (169731, 'KIAUSINIAI'),
  (170284, 'GLITIMAS'),
  (170688, 'GLITIMAS'),
  (169699, 'GLITIMAS'),
  (169715, 'GLITIMAS'),
  (168894, 'GLITIMAS'),
  (172688, 'GLITIMAS'),
  (172684, 'GLITIMAS'),
  (174924, 'GLITIMAS'),
  (173242, 'GLITIMAS'),
  (172749, 'GLITIMAS'),
  (171646, 'GLITIMAS'),
  (171646, 'RIESUTAI'),
  (168410, 'SOJOS'),
  (172475, 'SOJOS'),
  (169988, 'SALIERAI'),
  (170567, 'RIESUTAI'),
  (170187, 'RIESUTAI'),
  (170581, 'RIESUTAI'),
  (170162, 'RIESUTAI'),
  (170184, 'RIESUTAI'),
  (172470, 'RIESUTAI'),
  (170150, 'SEZAMAS'),
  (170189, 'SEZAMAS'),
  (174277, 'SOJOS'),
  (174277, 'GLITIMAS'),
  (172234, 'GARSTYCIOS'),
  (174289, 'SEZAMAS'),
  (171443, 'KIAUSINIAI'),
  (172446, 'SOJOS'),
  (174832, 'RIESUTAI'),
  (171583, 'SALIERAI')
) AS v(fdc_id, code)
JOIN product p ON p.fdc_id = v.fdc_id
JOIN allergen a ON a.code = v.code;

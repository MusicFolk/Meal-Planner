#!/usr/bin/env python3
"""
Vienkartinis USDA FoodData Central produktų importas (NFR-08, NFR-40).

Skriptas nuskaito USDA FoodData Central SR Legacy CSV rinkinį ir sugeneruoja
Flyway migraciją su produktų katalogu (lietuviški pavadinimai, kategorija,
FDC ID ir maistinė vertė 100 g) bei produktų ir alergenų sąsajomis.
Sistemai veikiant USDA API nekviečiamas.

Naudojimas:
  1. Atsisiųsti https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip
     ir išskleisti.
  2. python3 generate_products.py <išskleisto_aplanko_kelias> \
       > ../../src/main/resources/db/migration/V2__products.sql
"""
import csv
import sys

# Maistinių medžiagų ID USDA FoodData Central sistemoje
ENERGY_KCAL = "1008"
PROTEIN = "1003"
FAT = "1004"
CARBS = "1005"

# (FDC ID, lietuviškas pavadinimas, kategorija, alergenai)
PRODUCTS = [
    # Mėsa ir žuvis
    (171077, "Vištienos krūtinėlė", "MEAT_FISH", []),
    (173627, "Vištienos šlaunelės mėsa", "MEAT_FISH", []),
    (171116, "Malta vištiena", "MEAT_FISH", []),
    (171098, "Kalakutienos krūtinėlė", "MEAT_FISH", []),
    (171505, "Malta kalakutiena", "MEAT_FISH", []),
    (174030, "Malta jautiena (10 % riebumo)", "MEAT_FISH", []),
    (174055, "Jautienos nugarinė", "MEAT_FISH", []),
    (168249, "Kiaulienos išpjova", "MEAT_FISH", []),
    (167829, "Kiaulienos karbonadas", "MEAT_FISH", []),
    (173864, "Kumpis, griežinėliais", "MEAT_FISH", []),
    (175167, "Lašiša", "MEAT_FISH", ["ZUVIS"]),
    (171955, "Menkė", "MEAT_FISH", ["ZUVIS"]),
    (173709, "Tunas savo sultyse", "MEAT_FISH", ["ZUVIS"]),
    (175119, "Skumbrė", "MEAT_FISH", ["ZUVIS"]),
    (175116, "Silkė", "MEAT_FISH", ["ZUVIS"]),
    (173717, "Upėtakis", "MEAT_FISH", ["ZUVIS"]),
    (175176, "Tilapija", "MEAT_FISH", ["ZUVIS"]),
    (173725, "Polakas", "MEAT_FISH", ["ZUVIS"]),
    (175179, "Krevetės", "MEAT_FISH", ["VEZIAGYVIAI"]),
    # Pieno produktai ir kiaušiniai
    (171287, "Kiaušiniai", "DAIRY_EGGS", ["KIAUSINIAI"]),
    (171267, "Pienas 2 %", "DAIRY_EGGS", ["LAKTOZE"]),
    (170872, "Pienas 1 %", "DAIRY_EGGS", ["LAKTOZE"]),
    (170894, "Graikiškas jogurtas, neriebus", "DAIRY_EGGS", ["LAKTOZE"]),
    (170886, "Natūralus jogurtas", "DAIRY_EGGS", ["LAKTOZE"]),
    (173417, "Varškė 1 %", "DAIRY_EGGS", ["LAKTOZE"]),
    (172179, "Grūdėta varškė", "DAIRY_EGGS", ["LAKTOZE"]),
    (170847, "Mocarela", "DAIRY_EGGS", ["LAKTOZE"]),
    (170848, "Parmezanas", "DAIRY_EGGS", ["LAKTOZE"]),
    (173414, "Čederio sūris", "DAIRY_EGGS", ["LAKTOZE"]),
    (171251, "Šveicariškas sūris", "DAIRY_EGGS", ["LAKTOZE"]),
    (173420, "Fetos sūris", "DAIRY_EGGS", ["LAKTOZE"]),
    (171248, "Rikota", "DAIRY_EGGS", ["LAKTOZE"]),
    (173418, "Kreminis sūris", "DAIRY_EGGS", ["LAKTOZE"]),
    (173410, "Sviestas", "DAIRY_EGGS", ["LAKTOZE"]),
    (171257, "Grietinė", "DAIRY_EGGS", ["LAKTOZE"]),
    (170857, "Grietinėlė", "DAIRY_EGGS", ["LAKTOZE"]),
    (170904, "Kefyras", "DAIRY_EGGS", ["LAKTOZE"]),
    # Grūdai ir ankštiniai
    (173904, "Avižiniai dribsniai", "GRAINS", ["GLITIMAS"]),
    (170685, "Grikiai", "GRAINS", []),
    (168877, "Baltieji ryžiai", "GRAINS", []),
    (169703, "Rudieji ryžiai", "GRAINS", []),
    (169736, "Makaronai", "GRAINS", ["GLITIMAS"]),
    (169738, "Viso grūdo makaronai", "GRAINS", ["GLITIMAS"]),
    (169731, "Kiaušininiai makaronai", "GRAINS", ["GLITIMAS", "KIAUSINIAI"]),
    (169742, "Ryžių makaronai", "GRAINS", []),
    (168874, "Bolivinė balanda (kinoja)", "GRAINS", []),
    (170284, "Perlinės kruopos", "GRAINS", ["GLITIMAS"]),
    (170688, "Bulguras", "GRAINS", ["GLITIMAS"]),
    (169699, "Kuskusas", "GRAINS", ["GLITIMAS"]),
    (169702, "Soros", "GRAINS", []),
    (169715, "Manų kruopos", "GRAINS", ["GLITIMAS"]),
    (171670, "Kukurūzų kruopos", "GRAINS", []),
    (168894, "Kvietiniai miltai", "GRAINS", ["GLITIMAS"]),
    (172688, "Viso grūdo duona", "GRAINS", ["GLITIMAS"]),
    (172684, "Ruginė duona", "GRAINS", ["GLITIMAS"]),
    (174924, "Balta duona", "GRAINS", ["GLITIMAS"]),
    (173242, "Kvietinė tortilija", "GRAINS", ["GLITIMAS"]),
    (172749, "Viso grūdo traškučiai", "GRAINS", ["GLITIMAS"]),
    (171646, "Granola", "GRAINS", ["GLITIMAS", "RIESUTAI"]),
    (172420, "Lęšiai", "GRAINS", []),
    (173756, "Avinžirniai", "GRAINS", []),
    (173741, "Raudonosios pupelės (konservuotos)", "GRAINS", []),
    (173734, "Juodosios pupelės", "GRAINS", []),
    (175204, "Baltosios pupelės (konservuotos)", "GRAINS", []),
    (170016, "Žalieji žirneliai (šaldyti)", "GRAINS", []),
    (168410, "Edamamė pupelės", "GRAINS", ["SOJOS"]),
    (172475, "Tofu", "GRAINS", ["SOJOS"]),
    # Daržovės
    (170026, "Bulvės", "VEGETABLES", []),
    (168482, "Batatai", "VEGETABLES", []),
    (170393, "Morkos", "VEGETABLES", []),
    (170000, "Svogūnai", "VEGETABLES", []),
    (170005, "Svogūnų laiškai", "VEGETABLES", []),
    (169230, "Česnakai", "VEGETABLES", []),
    (170457, "Pomidorai", "VEGETABLES", []),
    (170501, "Trinti pomidorai (konservuoti)", "VEGETABLES", []),
    (170459, "Pomidorų pasta", "VEGETABLES", []),
    (168409, "Agurkai", "VEGETABLES", []),
    (170108, "Raudonosios paprikos", "VEGETABLES", []),
    (170379, "Brokoliai", "VEGETABLES", []),
    (169986, "Žiediniai kopūstai", "VEGETABLES", []),
    (170383, "Briuselio kopūstai", "VEGETABLES", []),
    (169975, "Baltagūžiai kopūstai", "VEGETABLES", []),
    (169279, "Rauginti kopūstai", "VEGETABLES", []),
    (168462, "Špinatai", "VEGETABLES", []),
    (168421, "Lapiniai kopūstai (kale)", "VEGETABLES", []),
    (169249, "Salotos", "VEGETABLES", []),
    (169291, "Cukinijos", "VEGETABLES", []),
    (169228, "Baklažanai", "VEGETABLES", []),
    (169251, "Pievagrybiai", "VEGETABLES", []),
    (168580, "Gluosniabudės", "VEGETABLES", []),
    (169145, "Burokėliai", "VEGETABLES", []),
    (169988, "Salierų stiebai", "VEGETABLES", ["SALIERAI"]),
    (169246, "Porai", "VEGETABLES", []),
    (169961, "Šparaginės pupelės", "VEGETABLES", []),
    (168448, "Moliūgas", "VEGETABLES", []),
    (168389, "Smidrai", "VEGETABLES", []),
    (169276, "Ridikėliai", "VEGETABLES", []),
    (168398, "Kukurūzai (šaldyti)", "VEGETABLES", []),
    (172233, "Krapai", "VEGETABLES", []),
    (170416, "Petražolės", "VEGETABLES", []),
    (169231, "Imbieras", "VEGETABLES", []),
    (171705, "Avokadai", "VEGETABLES", []),
    # Vaisiai ir uogos
    (171688, "Obuoliai", "FRUITS", []),
    (173944, "Bananai", "FRUITS", []),
    (171711, "Mėlynės", "FRUITS", []),
    (167762, "Braškės", "FRUITS", []),
    (167755, "Avietės", "FRUITS", []),
    (171722, "Spanguolės", "FRUITS", []),
    (169097, "Apelsinai", "FRUITS", []),
    (167746, "Citrinos", "FRUITS", []),
    (169118, "Kriaušės", "FRUITS", []),
    (174683, "Vynuogės", "FRUITS", []),
    (168153, "Kiviai", "FRUITS", []),
    (169949, "Slyvos", "FRUITS", []),
    (169928, "Persikai", "FRUITS", []),
    (171697, "Abrikosai", "FRUITS", []),
    (171719, "Trešnės", "FRUITS", []),
    (169910, "Mangai", "FRUITS", []),
    (169124, "Ananasai", "FRUITS", []),
    (168165, "Razinos", "FRUITS", []),
    # Riešutai ir sėklos
    (170567, "Migdolai", "NUTS_SEEDS", ["RIESUTAI"]),
    (170187, "Graikiniai riešutai", "NUTS_SEEDS", ["RIESUTAI"]),
    (170581, "Lazdyno riešutai", "NUTS_SEEDS", ["RIESUTAI"]),
    (170162, "Anakardžiai", "NUTS_SEEDS", ["RIESUTAI"]),
    (170184, "Pistacijos", "NUTS_SEEDS", ["RIESUTAI"]),
    (172470, "Žemės riešutų sviestas", "NUTS_SEEDS", ["RIESUTAI"]),
    (170562, "Saulėgrąžos", "NUTS_SEEDS", []),
    (170556, "Moliūgų sėklos", "NUTS_SEEDS", []),
    (170554, "Ispaninio šalavijo (chia) sėklos", "NUTS_SEEDS", []),
    (169414, "Linų sėmenys", "NUTS_SEEDS", []),
    (170150, "Sezamo sėklos", "NUTS_SEEDS", ["SEZAMAS"]),
    (170189, "Tahini (sezamų pasta)", "NUTS_SEEDS", ["SEZAMAS"]),
    # Riebalai ir prieskoniai
    (171413, "Alyvuogių aliejus", "FATS_SPICES", []),
    (172336, "Rapsų aliejus", "FATS_SPICES", []),
    (171025, "Saulėgrąžų aliejus", "FATS_SPICES", []),
    (170931, "Juodieji pipirai", "FATS_SPICES", []),
    (173468, "Druska", "FATS_SPICES", []),
    (171329, "Paprika (prieskonis)", "FATS_SPICES", []),
    (171320, "Malustas cinamonas", "FATS_SPICES", []),
    (171317, "Džiovintas bazilikas", "FATS_SPICES", []),
    (171328, "Džiovintas raudonėlis", "FATS_SPICES", []),
    (172231, "Ciberžolė", "FATS_SPICES", []),
    (170923, "Kmynai (kuminas)", "FATS_SPICES", []),
    (174277, "Sojų padažas", "FATS_SPICES", ["SOJOS", "GLITIMAS"]),
    (172234, "Garstyčios", "FATS_SPICES", ["GARSTYCIOS"]),
    (173469, "Obuolių actas", "FATS_SPICES", []),
    # Kita
    (169640, "Medus", "OTHER", []),
    (169655, "Cukrus", "OTHER", []),
    (169641, "Uogienė", "OTHER", []),
    (169593, "Kakavos milteliai", "OTHER", []),
    (174289, "Humusas", "OTHER", ["SEZAMAS"]),
    (171443, "Majonezas, neriebus", "OTHER", ["KIAUSINIAI"]),
    (172446, "Sojų gėrimas", "OTHER", ["SOJOS"]),
    (174832, "Migdolų gėrimas", "OTHER", ["RIESUTAI"]),
    (170173, "Kokosų pienas", "OTHER", []),
    (169094, "Juodosios alyvuogės", "OTHER", []),
    (171583, "Daržovių sultinys", "OTHER", ["SALIERAI"]),
]


def sql_str(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def main(folder: str) -> None:
    ids = {str(p[0]) for p in PRODUCTS}
    descriptions = {}
    with open(f"{folder}/food.csv", newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if row["fdc_id"] in ids:
                descriptions[row["fdc_id"]] = row["description"]

    nutrients = {i: {} for i in ids}
    wanted = {ENERGY_KCAL, PROTEIN, FAT, CARBS}
    with open(f"{folder}/food_nutrient.csv", newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if row["fdc_id"] in ids and row["nutrient_id"] in wanted:
                nutrients[row["fdc_id"]][row["nutrient_id"]] = float(row["amount"])

    missing = [i for i in ids if i not in descriptions or len(nutrients[i]) < 4]
    if missing:
        sys.exit(f"Trūksta duomenų FDC ID: {missing}")

    out = sys.stdout
    out.write("-- Sugeneruota skriptu backend/tools/usda/generate_products.py\n")
    out.write("-- Šaltinis: USDA FoodData Central, SR Legacy (2018-04). Maistinė vertė 100 g.\n\n")
    out.write("INSERT INTO product (name, fdc_id, category, kcal, protein_g, fat_g, carbs_g) VALUES\n")
    rows = []
    for fdc_id, name, category, _ in PRODUCTS:
        n = nutrients[str(fdc_id)]
        rows.append(
            f"  ({sql_str(name)}, {fdc_id}, '{category}', {round(n[ENERGY_KCAL])}, "
            f"{n[PROTEIN]:.2f}, {n[FAT]:.2f}, {n[CARBS]:.2f}) -- {descriptions[str(fdc_id)][:80]}"
        )
    # Komentaras eilutės gale, todėl kablelį dedame prieš jį
    fixed = []
    for i, r in enumerate(rows):
        code, comment = r.split(" -- ", 1)
        sep = "," if i < len(rows) - 1 else ";"
        fixed.append(f"{code}{sep} -- {comment}")
    out.write("\n".join(fixed) + "\n\n")

    out.write("INSERT INTO product_allergen (product_id, allergen_id)\n")
    pairs = [(fdc_id, a) for fdc_id, _, _, allergens in PRODUCTS for a in allergens]
    out.write("SELECT p.id, a.id FROM (VALUES\n")
    out.write(",\n".join(f"  ({fdc_id}, '{code}')" for fdc_id, code in pairs))
    out.write("\n) AS v(fdc_id, code)\n")
    out.write("JOIN product p ON p.fdc_id = v.fdc_id\n")
    out.write("JOIN allergen a ON a.code = v.code;\n")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])

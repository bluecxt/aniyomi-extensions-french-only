import os
import re
import requests
from bs4 import BeautifulSoup

def get_config_from_kotlin():
    """Lit le fichier AnimeSama.kt pour extraire la config actuelle."""
    # Le fichier .kt est dans le même dossier parent
    kt_path = os.path.join(os.path.dirname(__file__), "src/eu/kanade/tachiyomi/animeextension/fr/animesama/AnimeSama.kt")
    
    config = {"url": "https://anime-sama.to", "selectors": {}}
    
    try:
        with open(kt_path, "r", encoding="utf-8") as f:
            code = f.read()
            
            # Extraction de l'URL par défaut
            url_match = re.search(r'PREF_URL_DEFAULT\s*=\s*"([^"]+)"', code)
            if url_match:
                config["url"] = url_match.group(1)
            
            # Extraction des sélecteurs
            pop_match = re.search(r'popularAnimeParse.*?select\("([^"]+)"\)', code, re.DOTALL)
            if pop_match: config["selectors"]["popular"] = pop_match.group(1)
            
            lat_match = re.search(r'latestUpdatesParse.*?select\("([^"]+)"\)', code, re.DOTALL)
            if lat_match: config["selectors"]["latest"] = lat_match.group(1)
            
    except Exception as e:
        print(f"⚠️ Erreur lecture Kotlin : {e}. Utilisation des valeurs par défaut.")
    
    return config

def simulate():
    config = get_config_from_kotlin()
    print(f"🎬 [SIMULATEUR DYNAMIQUE ANIME-SAMA]")
    print(f"🌐 URL extraite du code : {config['url']}\n")

    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36", "Referer": f"{config['url']}/"}
    
    try:
        resp = requests.get(config['url'], headers=headers, timeout=10)
        soup = BeautifulSoup(resp.text, 'html.parser')
        
        # Test Populaire
        sel_pop = config["selectors"].get("popular", "#containerPepites > div a")
        items_pop = soup.select(sel_pop)
        print(f"🔥 Populaire ('{sel_pop}') : {'✅' if items_pop else '❌'} {len(items_pop)} items")

        # Test Derniers Ajouts
        sel_lat = config["selectors"].get("latest", "#containerAjoutsAnimes > div")
        items_lat = soup.select(sel_lat)
        print(f"🆕 Derniers ajouts ('{sel_lat}') : {'✅' if items_lat else '❌'} {len(items_lat)} items")

    except Exception as e:
        print(f"❌ Erreur : {e}")

if __name__ == "__main__":
    simulate()

import os
import re
import requests
from bs4 import BeautifulSoup

def get_config():
    current_dir = os.path.dirname(__file__)
    kt_file = None
    for root, _, files in os.walk(current_dir):
        if "src/test" in root: continue
        for f in files:
            if f.endswith(".kt") and not any(x in f for x in ["Filters", "Activity", "Helper", "Dto"]):
                kt_file = os.path.join(root, f)
                break
        if kt_file: break
    
    config = {"url": None, "selectors": []}
    if not kt_file: return config

    try:
        with open(kt_file, "r", encoding="utf-8") as f:
            code = f.read()
            # Try patterns for URL
            m = re.search(r'PREF_URL_DEFAULT\s*=\s*"([^"]+)"', code)
            if not m: m = re.search(r'baseUrl\s*=\s*"([^"]+)"', code)
            if not m: m = re.search(r'val\s+domain\s*=\s*"([^"]+)"', code)
            
            if m:
                val = m.group(1)
                config["url"] = val if val.startswith("http") else f"https://{val}"
            
            # Extract common selectors
            config["selectors"] = list(set(re.findall(r'select\("([^"]+)"\)', code)))
    except: pass
    return config

def run():
    conf = get_config()
    if not conf["url"]:
        print("❌ URL introuvable dans le code Kotlin.")
        return
    
    print(f"🔍 Test de {conf['url']}...")
    headers = {"User-Agent": "Mozilla/5.0 (Linux; Android 13)"}
    try:
        r = requests.get(conf["url"], headers=headers, timeout=12)
        if r.status_code == 200:
            print("✅ Connexion réussie.")
            soup = BeautifulSoup(r.text, 'html.parser')
            for s in conf["selectors"]:
                # Clean selector (remove :contains)
                clean_s = re.sub(r':contains\([^)]+\)', '', s)
                if not clean_s or len(clean_s) < 3: continue
                try:
                    items = soup.select(clean_s)
                    if items and len(items) > 1:
                        print(f"✨ Succès : {len(items)} éléments trouvés avec '{s}'")
                        return
                except: continue
            print("⚠️ Aucun élément trouvé avec les sélecteurs du code.")
        else:
            print(f"❌ Erreur HTTP {r.status_code}")
    except Exception as e:
        print(f"❌ Erreur : {e}")

if __name__ == "__main__":
    run()

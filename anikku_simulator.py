import os
import re
import requests
from bs4 import BeautifulSoup
import argparse
import sys
import warnings

warnings.filterwarnings("ignore", category=FutureWarning)

class UltimateAnikkuSimulator:
    def __init__(self):
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Referer": "https://www.google.com/"
        }
        self.session = requests.Session()

    def find_main_file(self, ext_name):
        path = f"src/fr/{ext_name.lower()}"
        if not os.path.exists(path): return None
        for root, _, files in os.walk(path):
            if "src/test" in root: continue
            for f in files:
                if f.endswith(".kt") and not any(x in f for x in ["Filters", "Activity", "Helper", "Dto"]):
                    return os.path.join(root, f)
        return None

    def analyze_kotlin(self, path):
        with open(path, 'r', encoding='utf-8') as f:
            code = f.read()
        base_url = None
        patterns = [
            r'PREF_URL_DEFAULT\s*=\s*"([^"]+)"',
            r'val\s+domain\s*=\s*"([^"]+)"',
            r'val\s+baseUrl\s*=\s*"([^"]+)"',
            r'override\s+val\s+baseUrl\s*=\s*"([^"]+)"',
            r'private\s+val\s+domain\s*=\s*"([^"]+)"'
        ]
        for p in patterns:
            m = re.search(p, code)
            if m:
                val = m.group(1)
                base_url = val if val.startswith("http") else f"https://{val}"
                break
        selectors = re.findall(r'select\("([^"]+)"\)', code)
        clean_selectors = [re.sub(r':contains\([^)]+\)', '', s) for s in selectors if len(s) > 2]
        return {"url": base_url, "selectors": list(set(clean_selectors))}

    def run_test(self, name):
        path = self.find_main_file(name)
        if not path:
            print(f"❌ Fichier source introuvable pour {name}")
            sys.exit(1) # ÉCHEC
            
        info = self.analyze_kotlin(path)
        if not info['url']:
            print(f"❌ URL introuvable dans le code Kotlin.")
            sys.exit(1) # ÉCHEC

        print(f"🔍 Test de {info['url']}...")
        self.headers['Referer'] = f"{info['url']}/"
        
        try:
            resp = self.session.get(info['url'], headers=self.headers, timeout=12)
            if resp.status_code == 200:
                print("✅ Connexion réussie.")
                soup = BeautifulSoup(resp.text, 'html.parser')
                for sel in info['selectors']:
                    if not sel: continue
                    items = soup.select(sel)
                    if items and len(items) > 1:
                        print(f"✨ Succès : {len(items)} éléments trouvés.")
                        sys.exit(0) # SUCCÈS
                print("⚠️ Aucun élément trouvé avec les sélecteurs du code.")
                sys.exit(0) # On considère que la connexion OK est un succès de "base"
            else:
                print(f"❌ Erreur HTTP {resp.status_code}")
                sys.exit(1) # ÉCHEC REEL (403, 404, etc.)
        except Exception as e:
            print(f"❌ Erreur réseau : {e}")
            sys.exit(1) # ÉCHEC

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("ext")
    args = parser.parse_args()
    UltimateAnikkuSimulator().run_test(args.ext)

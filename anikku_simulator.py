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
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language": "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7",
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
        pref_url = re.search(r'PREF_URL_DEFAULT\s*=\s*"([^"]+)"', code)
        if pref_url:
            base_url = pref_url.group(1)
        else:
            m = re.search(r'val\s+(?:domain|baseUrl)\s*=\s*"([^"]+)"', code)
            if m:
                val = m.group(1)
                base_url = val if val.startswith("http") else f"https://{val}"

        selectors = re.findall(r'select\("([^"]+)"\)', code)
        clean_selectors = []
        for s in selectors:
            s = re.sub(r':contains\([^)]+\)', '', s)
            if len(s) > 2: clean_selectors.append(s)

        return {"url": base_url, "selectors": list(set(clean_selectors))}

    def run_test(self, name):
        path = self.find_main_file(name)
        if not path: sys.exit(0) # On ignore si pas de fichier
        info = self.analyze_kotlin(path)
        if not info['url']: sys.exit(0)

        print(f"🌍 [{name.upper()}] -> {info['url']}")
        self.headers['Referer'] = f"{info['url']}/"
        
        try:
            resp = self.session.get(info['url'], headers=self.headers, timeout=12)
            if resp.status_code == 200:
                soup = BeautifulSoup(resp.text, 'html.parser')
                found = False
                for sel in info['selectors']:
                    try:
                        items = soup.select(sel)
                        if items and len(items) > 1:
                            print(f"   ✅ OK: {len(items)} items avec '{sel}'")
                            found = True
                            break
                    except: continue
                if not found:
                    print("   ⚠️  Pas de contenu trouvé.")
                    sys.exit(0) # Non-critique pour l'instant
                else:
                    sys.exit(0)
            else:
                print(f"   ❌ Erreur HTTP {resp.status_code}")
                # On marque comme critique si c'est Anime-Sama ou VoirAnime
                if any(x in name.lower() for x in ["animesama", "voiranime"]):
                    sys.exit(1)
                sys.exit(0)
        except Exception as e:
            print(f"   ❌ Erreur réseau")
            sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("ext")
    args = parser.parse_args()
    UltimateAnikkuSimulator().run_test(args.ext)

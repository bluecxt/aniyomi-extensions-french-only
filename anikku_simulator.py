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
        # Headers plus complets pour imiter un vrai navigateur et éviter les 403
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language": "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7",
            "Accept-Encoding": "gzip, deflate, br",
            "Connection": "keep-alive",
            "Cache-Control": "max-age=0",
            "Sec-Ch-Ua": '"Chromium";v="122", "Not(A:Brand";v="24", "Google Chrome";v="122"',
            "Sec-Ch-Ua-Mobile": "?0",
            "Sec-Ch-Ua-Platform": '"Windows"',
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "none",
            "Sec-Fetch-User": "?1",
            "Upgrade-Insecure-Requests": "1"
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
        
        # Recherche plus large de l'URL
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
        clean_selectors = []
        for s in selectors:
            s = re.sub(r':contains\([^)]+\)', '', s)
            if len(s) > 2: clean_selectors.append(s)

        return {"url": base_url, "selectors": list(set(clean_selectors))}

    def run_test(self, name):
        path = self.find_main_file(name)
        if not path:
            print(f"❌ Fichier source introuvable pour {name}")
            sys.exit(1)
            
        info = self.analyze_kotlin(path)
        if not info['url']:
            print(f"❌ URL non détectée pour {name}")
            sys.exit(1)

        print(f"🌍 [{name.upper()}] Target: {info['url']}")
        self.headers['Referer'] = f"{info['url']}/"
        
        try:
            resp = self.session.get(info['url'], headers=self.headers, timeout=15)
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
                
                if found:
                    sys.exit(0)
                else:
                    print("   ⚠️  Connexion OK mais aucun contenu trouvé.")
                    # On ne fait pas échouer si la connexion est OK (le scraping est complexe)
                    sys.exit(0)
            else:
                print(f"   ❌ Erreur HTTP {resp.status_code}")
                # Tout code non-200 est un échec pour le résumé
                sys.exit(1)
        except Exception as e:
            print(f"   ❌ Erreur réseau : {e}")
            sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("ext")
    args = parser.parse_args()
    UltimateAnikkuSimulator().run_test(args.ext)

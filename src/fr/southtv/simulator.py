import requests
import sys

def run():
    base_url = "https://southtv.fr"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer": f"{base_url}/"
    }
    
    print(f"📺 [SOUTHTV] Focus sur VF et FILMS")
    tests = [
        {"name": "South Park (VF)", "url": f"{base_url}/southpark/s1e1.mp4", "critical": True},
        {"name": "South Park le Film", "url": f"{base_url}/films/Filmsouthpark.mp4", "critical": True}
    ]
    
    success = True
    for t in tests:
        try:
            r = requests.head(t['url'], headers=headers, timeout=15, allow_redirects=True)
            if r.status_code == 200:
                print(f"   ✅ {t['name']} : OK")
            else:
                print(f"   ❌ {t['name']} : Erreur HTTP {r.status_code}")
                if t['critical']: success = False
        except Exception as e:
            print(f"   ❌ Erreur réseau pour {t['name']} : {e}")
            if t['critical']: success = False
    
    if success:
        print("\n🏆 VERDICT : FONCTIONNELLE.")
        sys.exit(0)
    else:
        print("\n❌ VERDICT : PANNE CRITIQUE.")
        sys.exit(1)

if __name__ == "__main__":
    run()

import requests

def run():
    base_url = "https://southtv.fr"
    headers = {"User-Agent": "Mozilla/5.0"}
    
    print(f"📺 [SIMULATEUR SOUTHTV] Focus sur VF et FILMS")
    
    tests = [
        {"name": "South Park (VF)", "url": f"{base_url}/southpark/s1e1.mp4", "critical": True},
        {"name": "South Park le Film", "url": f"{base_url}/films/Filmsouthpark.mp4", "critical": True},
        {"name": "American Dad (Extra)", "url": f"{base_url}/americandad/s1e1.mp4", "critical": False}
    ]
    
    success_count = 0
    for t in tests:
        try:
            r = requests.head(t['url'], headers=headers, timeout=10, allow_redirects=True)
            if r.status_code == 200:
                print(f"   ✅ {t['name']} : OK")
                success_count += 1
            else:
                status = "❌ ÉCHEC CRITIQUE" if t['critical'] else "⚠️ INFO (Non-critique)"
                print(f"   {status} : {t['name']} (HTTP {r.status_code})")
        except:
            print(f"   ❌ Erreur réseau pour {t['name']}")
    
    # Verdict : Si VF et Films sont OK, c'est réussi
    if success_count >= 2:
        print("\n🏆 VERDICT : L'extension est considérée comme FONCTIONNELLE.")
    else:
        print("\n❌ VERDICT : Panne critique détectée sur les sources principales.")

if __name__ == "__main__":
    run()

import requests
import sys

def run():
    url = "https://api.franime.fr/api/animes/"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer": "https://franime.fr/",
        "Accept": "application/json"
    }
    
    print(f"📡 [FRANIME] Test de l'API...")
    try:
        r = requests.get(url, headers=headers, timeout=15)
        if r.status_code == 200:
            data = r.json()
            if len(data) > 0:
                print(f"✅ SUCCÈS : {len(data)} animés trouvés.")
                sys.exit(0)
        print(f"❌ ÉCHEC : Erreur HTTP {r.status_code}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ ERREUR : {e}")
        sys.exit(1)

if __name__ == "__main__":
    run()

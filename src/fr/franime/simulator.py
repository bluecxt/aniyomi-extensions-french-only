import requests

def run():
    url = "https://api.franime.fr/api/animes/"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36",
        "Accept": "application/json"
    }
    
    print(f"📡 [SIMULATEUR FRANIME] Test de l'API : {url}")
    try:
        r = requests.get(url, headers=headers, timeout=15)
        if r.status_code == 200:
            data = r.json()
            if len(data) > 0:
                print(f"✅ SUCCÈS : {len(data)} animés listés dans la base JSON.")
                print(f"👉 Premier animé : {data[0].get('title', 'N/A')}")
                return
        print(f"❌ ÉCHEC : Erreur HTTP {r.status_code} ou base vide.")
    except Exception as e:
        print(f"❌ ERREUR : {e}")

if __name__ == "__main__":
    run()

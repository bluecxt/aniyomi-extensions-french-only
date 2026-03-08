import os
import subprocess
import sys
import json

def run_all():
    print("🚀 [CHEF D'ORCHESTRE] Démarrage de l'audit complet...\n")
    
    extensions_path = "src/fr"
    results = []
    
    # Chargement des extensions à ignorer
    ignored = []
    if os.path.exists("ignored_extensions.json"):
        try:
            with open("ignored_extensions.json", "r") as f:
                ignored = json.load(f)
        except Exception as e:
            print(f"⚠️ Erreur chargement ignored_extensions.json: {e}")

    # On parcourt chaque dossier d'extension
    for ext_name in sorted(os.listdir(extensions_path)):
        if ext_name in ignored:
            print(f"--------------------------------------------------")
            print(f"⏭️  Ignoré : {ext_name.upper()}")
            results.append((ext_name, "⏭️  SKIPPED"))
            continue

        ext_dir = os.path.join(extensions_path, ext_name)
        sim_path = os.path.join(ext_dir, "simulator.py")
        
        if os.path.isfile(sim_path):
            print(f"--------------------------------------------------")
            print(f"🔎 Audit de : {ext_name.upper()}")
            
            # On lance le simulateur local
            process = subprocess.run([sys.executable, sim_path])
            
            status = "✅ PASS" if process.returncode == 0 else "❌ FAIL"
            results.append((ext_name, status))
        else:
            results.append((ext_name, "⚠️ NO SIM"))

    # Affichage du Résumé Final
    print("\n\n" + "="*50)
    print("📊 RÉSUMÉ FINAL DE L'AUDIT")
    print("="*50)
    print(f"{'Extension':<15} | {'Statut':<10}")
    print("-" * 30)
    for name, status in results:
        print(f"{name:<15} | {status:<10}")
    print("="*50)

    if any(status == "❌ FAIL" for _, status in results):
        sys.exit(1)
    sys.exit(0)

if __name__ == "__main__":
    run_all()

import os
import subprocess
import sys

def run_all():
    print("🚀 [CHEF D'ORCHESTRE] Démarrage de l'audit complet...\n")
    
    extensions_path = "src/fr"
    results = []
    
    # On parcourt chaque dossier d'extension
    for ext_name in sorted(os.listdir(extensions_path)):
        ext_dir = os.path.join(extensions_path, ext_name)
        sim_path = os.path.join(ext_dir, "simulator.py")
        
        if os.path.isfile(sim_path):
            print(f"--------------------------------------------------")
            print(f"🔎 Audit de : {ext_name.upper()}")
            
            # On lance le simulateur local de l'extension
            # On utilise le même interpréteur Python (sys.executable)
            process = subprocess.run([sys.executable, sim_path], capture_output=False)
            
            status = "✅ PASS" if process.returncode == 0 else "❌ FAIL"
            results.append((ext_name, status))
        else:
            # Si pas de simulateur, on le note mais on ne fait pas échouer l'audit global
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

    # Si au moins un test critique a échoué, on renvoie une erreur globale
    if any(status == "❌ FAIL" for _, status in results):
        sys.exit(1)
    sys.exit(0)

if __name__ == "__main__":
    run_all()

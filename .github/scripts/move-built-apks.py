from pathlib import Path
import shutil
import os

REPO_APK_DIR = Path("repo/apk")

# Ensure the directory exists
REPO_APK_DIR.mkdir(parents=True, exist_ok=True)

# Path where GitHub Actions downloads the artifacts
ARTIFACTS_DIR = Path.home().joinpath("apk-artifacts")

if ARTIFACTS_DIR.exists():
    for apk in ARTIFACTS_DIR.glob("**/*.apk"):
        apk_name = apk.name.replace("-release.apk", ".apk")
        
        # In our case, they might be named -debug.apk if built with assembleDebug 
        # but the CI uses assembleRelease usually (checked in generate-build-matrices.py)
        # We normalize to .apk
        target_name = apk_name
        if target_name.endswith("-debug.apk"):
            target_name = target_name.replace("-debug.apk", ".apk")
        elif target_name.endswith("-release.apk"):
             target_name = target_name.replace("-release.apk", ".apk")

        dest_path = REPO_APK_DIR.joinpath(target_name)
        
        # Remove existing one if it exists to replace it with the new build
        if dest_path.exists():
            dest_path.unlink()
            
        shutil.move(apk, dest_path)
        print(f"Moved {apk.name} to {dest_path}")
else:
    print("No artifacts directory found.")

# crDroid 13 (Android 17) device tree for Samsung Galaxy A14 4G (SM-A145F)

**Status**
- HALs are the stock A145FXXSEDZF2 blobs.

# Building

1. Sync crDroid:
```
repo init -u https://github.com/crdroidandroid/android.git -b 17.0 --git-lfs
```
2. Copy `local_manifests/s5e3830.xml` from [s5e3830-common](https://github.com/clangsdorff/android_device_samsung_s5e3830-common) to `.repo/local_manifests/` and sync:
```
repo sync -c -j$(nproc --all) --force-sync --no-clone-bundle --no-tags
```
3. Apply the tree patches:
```
./device/samsung/s5e3830-common/patches/apply.sh
```
4. Build:
```
source build/envsetup.sh
breakfast a14
m bacon
```

# Blobs

`proprietary-files.txt` is extracted from A145FXXSEDZF2 with `extract-files.py`. To regenerate the vendor tree from an extracted firmware dump:
```
./extract-files.py /path/to/dump
```

**Join / Follow**
- Telegram: https://t.me/a14stuffs

**Credits**
- LineageOS for `android_device_samsung_s5e9925-common`, used as the base for the GKI layout, and `extract-utils`
- samsungexynos850 for `android_device_samsung_a21s-common`, the Exynos 850 reference tree

**Device & Notes**
- Device: Samsung Galaxy A14 4G (SM-A145F), Exynos 850 (s5e3830)
- Kernel: [langsdorffkernel](https://github.com/clangsdorff/langsdorffkernel)

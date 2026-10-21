# LineageOS 24 device tree for Samsung Galaxy A14 4G (SM-A145F)

**Status**
- Bring-up, not booting yet.
- SELinux is permissive (`a14noavbp_defconfig`) until the vendor sepolicy is written.
- HALs are the stock A145FXXSEDZF2 blobs.

# Building

1. Sync LineageOS:
```
repo init -u https://github.com/LineageOS/android.git -b lineage-24.0 --git-lfs
```
2. Copy `local_manifests/a14.xml` to `.repo/local_manifests/` and sync:
```
repo sync -c -j$(nproc --all) --force-sync --no-clone-bundle --no-tags
```
3. Build:
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

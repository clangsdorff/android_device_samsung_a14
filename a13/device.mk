#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

TARGET_IS_A13 := true

DEVICE_PACKAGE_OVERLAYS += device/samsung/a14/a13/overlay

$(call inherit-product, device/samsung/a14/device.mk)

PRODUCT_COPY_FILES += \
    device/samsung/a14/a13/fstab.s5e3830:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.s5e3830 \
    device/samsung/a14/a13/fstab.s5e3830:$(TARGET_COPY_OUT_RAMDISK)/fstab.s5e3830 \
    device/samsung/a14/a13/fstab.s5e3830:$(TARGET_COPY_OUT_RAMDISK)/first_stage_ramdisk/fstab.s5e3830

# Touch, grip and sensorhub firmware, requested before /vendor is mounted
PRODUCT_COPY_FILES += \
    $(foreach f,abov/a96t396_a13_main.bin sensorhub/shub_nacho_a13.bin tsp_himax/hx83112f_a13.bin tsp_novatek/nt36672_a13_csot.bin tsp_novatek/nt36672_a13_csot_mp.bin tsp_novatek/nt36672_a13_tianma.bin tsp_novatek/nt36672_a13_tianma_mp.bin tsp_ovt/td4375_a13_boe.bin,\
        vendor/samsung/a13/proprietary/vendor/firmware/$(f):$(TARGET_COPY_OUT_RAMDISK)/lib/firmware/$(f))

# NXP NFC and HIDL fingerprint HALs from the A13 stock vendor
PRODUCT_PACKAGES += \
    android.hardware.biometrics.fingerprint@2.1.vendor \
    android.hardware.nfc@1.0.vendor \
    android.hardware.nfc@1.1.vendor \
    android.hardware.nfc@1.2.vendor \
    android.hardware.nfc-V1-ndk.vendor \
    libhardware_legacy.vendor

$(call inherit-product, vendor/samsung/a13/a13-vendor.mk)

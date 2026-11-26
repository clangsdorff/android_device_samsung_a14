#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

LOCAL_PATH := device/samsung/a14/a13

# Inherit from the common tree
$(call inherit-product, device/samsung/s5e3830-common/common.mk)

# Audio - Effects - SoundBooster
$(call soong_config_set,samsungAudioVars,soundbooster_dsp_library,//vendor/samsung/a13:lib_SoundBooster_ver1100)

# Fingerprint
PRODUCT_PACKAGES += \
    android.hardware.biometrics.fingerprint-service.samsung32

# Overlays
PRODUCT_PACKAGES += \
    FrameworkResOverlayA13

PRODUCT_SOONG_NAMESPACES += \
    device/samsung/a14

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/fstab.s5e3830:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.s5e3830 \
    $(LOCAL_PATH)/fstab.s5e3830:$(TARGET_COPY_OUT_RAMDISK)/fstab.s5e3830 \
    $(LOCAL_PATH)/fstab.s5e3830:$(TARGET_COPY_OUT_RAMDISK)/first_stage_ramdisk/fstab.s5e3830

# Touch, grip and sensorhub firmware, requested before /vendor is mounted
PRODUCT_COPY_FILES += \
    $(foreach f,abov/a96t396_a13_main.bin sensorhub/shub_nacho_a13.bin tsp_himax/hx83112f_a13.bin tsp_novatek/nt36672_a13_csot.bin tsp_novatek/nt36672_a13_csot_mp.bin tsp_novatek/nt36672_a13_tianma.bin tsp_novatek/nt36672_a13_tianma_mp.bin tsp_ovt/td4375_a13_boe.bin,\
        vendor/samsung/a13/proprietary/vendor/firmware/$(f):$(TARGET_COPY_OUT_RAMDISK)/lib/firmware/$(f) \
        vendor/samsung/a13/proprietary/vendor/firmware/$(f):$(TARGET_COPY_OUT_RECOVERY)/root/lib/firmware/$(f))

# NXP NFC from the A13 stock vendor
PRODUCT_PACKAGES += \
    android.hardware.nfc-service.nxp-samsung \
    android.hardware.nfc@1.0.vendor \
    android.hardware.nfc@1.1.vendor \
    android.hardware.nfc@1.2.vendor \
    android.hardware.nfc-V1-ndk.vendor \
    libhardware_legacy.vendor

PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)/nfc/nxp

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/nfc/com.nxp.mifare.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/com.nxp.mifare.xml

# keep the thermal hal alive if init fails, the failing step shows up in logcat
PRODUCT_VENDOR_PROPERTIES += \
    vendor.disable.thermalhal.control=1

$(call inherit-product, vendor/samsung/a13/a13-vendor.mk)

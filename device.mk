#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

LOCAL_PATH := device/samsung/a14

# Inherit from the common tree
$(call inherit-product, device/samsung/s5e3830-common/common.mk)

# Audio - Effects - SoundBooster
$(call soong_config_set,samsungAudioVars,soundbooster_dsp_library,//vendor/samsung/a14:lib_SoundBooster_ver1100)

# Fstab
PRODUCT_PACKAGES += \
    fstab.s5e3830 \
    fstab.s5e3830.vendor_ramdisk

PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)/configs/init/fstab

# Fingerprint
PRODUCT_PACKAGES += \
    android.hardware.biometrics.fingerprint-service.samsung

# NFC
PRODUCT_VENDOR_PROPERTIES += \
    ro.vendor.nfc.feature.chipname=SLSI

PRODUCT_PACKAGES += \
    sec.android.hardware.nfc-service

PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)/nfc/sec

# Permissions
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.sensor.light.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.light.xml

# Touchscreen firmware, requested before /vendor is mounted
PRODUCT_COPY_FILES += \
    $(foreach f,ft8720_a14.bin ft8720_a14_ramtest.bin ili7807_a14.hex nt36672_a14_csot.bin nt36672_a14_csot_mp.bin nt36672_a14_tianma.bin nt36672_a14_tianma_mp.bin,\
        vendor/samsung/a14/proprietary/vendor/firmware/$(f):$(TARGET_COPY_OUT_VENDOR_RAMDISK)/vendor/firmware/$(f) \
        vendor/samsung/a14/proprietary/vendor/firmware/$(f):$(TARGET_COPY_OUT_RECOVERY)/root/vendor/firmware/$(f))

# Call the proprietary setup
$(call inherit-product, vendor/samsung/a14/a14-vendor.mk)

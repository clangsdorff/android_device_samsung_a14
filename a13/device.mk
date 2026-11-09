#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

TARGET_IS_A13 := true

$(call inherit-product, device/samsung/a14/device.mk)

PRODUCT_COPY_FILES += \
    device/samsung/a14/a13/fstab.s5e3830:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.s5e3830

# NXP NFC and HIDL fingerprint HALs from the A13 stock vendor
PRODUCT_PACKAGES += \
    android.hardware.biometrics.fingerprint@2.1.vendor \
    android.hardware.nfc@1.0.vendor \
    android.hardware.nfc@1.1.vendor \
    android.hardware.nfc@1.2.vendor \
    android.hardware.nfc-V1-ndk.vendor \
    libhardware_legacy.vendor

$(call inherit-product, vendor/samsung/a13/a13-vendor.mk)

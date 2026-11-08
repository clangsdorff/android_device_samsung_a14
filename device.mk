#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

LOCAL_PATH := device/samsung/a14

# All components inherited here go to system image
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/generic_system.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/non_ab_device.mk)

# All components inherited here go to system_ext image
$(call inherit-product, $(SRC_TARGET_DIR)/product/handheld_system_ext.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/telephony_system_ext.mk)

# All components inherited here go to product image
$(call inherit-product, $(SRC_TARGET_DIR)/product/aosp_product.mk)

# All components inherited here go to vendor image
$(call inherit-product, $(SRC_TARGET_DIR)/product/media_vendor.mk)

# Setup dalvik vm configs
$(call inherit-product, frameworks/native/build/phone-xhdpi-4096-dalvik-heap.mk)

# Dexpreopt
PRODUCT_DEX_PREOPT_DEFAULT_COMPILER_FILTER := speed

# Dexopt & JIT
PRODUCT_PRODUCT_PROPERTIES += \
    dalvik.vm.usejit=false \
    dalvik.vm.usejitprofiles=false \
    dalvik.vm.dex2oat-filter=speed \
    pm.dexopt.install=speed \
    pm.dexopt.install-bulk=speed \
    pm.dexopt.install-bulk-downgraded=speed \
    pm.dexopt.install-bulk-secondary=speed \
    pm.dexopt.install-bulk-secondary-downgraded=speed \
    pm.dexopt.install-fast=speed \
    pm.dexopt.ab-ota=speed \
    pm.dexopt.first-boot=verify \
    pm.dexopt.boot-after-ota=verify \
    pm.dexopt.boot-after-mainline-update=verify \
    pm.dexopt.bg-dexopt=speed \
    pm.dexopt.inactive=speed \
    pm.dexopt.cmdline=speed

# Enable project quotas and casefolding for emulated storage without sdcardfs
$(call inherit-product, $(SRC_TARGET_DIR)/product/emulated_storage.mk)

# IMS
$(call inherit-product, hardware/lineage/generic-ims/ims.mk)

# CAS
TARGET_REQUIRES_HIDL_CAS_HAL := false

# Display
TARGET_SCREEN_HEIGHT := 2408
TARGET_SCREEN_WIDTH := 1080

# Attention
PRODUCT_PACKAGES += \
    ScreenAttention

# Init
PRODUCT_PACKAGES += \
    fstab.s5e3830 \
    fstab.s5e3830.vendor_ramdisk \
    init.a14.rc

# Stock HAL dependencies
PRODUCT_PACKAGES += \
    android.frameworks.cameraservice.common-V1-ndk.vendor \
    android.frameworks.cameraservice.device-V2-ndk.vendor \
    android.frameworks.cameraservice.service-V2-ndk.vendor \
    android.frameworks.sensorservice-V1-ndk.vendor \
    android.hardware.security.keymint-V2-ndk.vendor \
    android.hardware.security.rkp-V2-ndk.vendor \
    android.hardware.security.secureclock-V1-ndk.vendor \
    android.hardware.security.sharedsecret-V1-ndk.vendor \
    android.hardware.sensors-V2-ndk.vendor \
    android.hardware.sensors@2.0.vendor \
    android.hardware.sensors@2.0-ScopedWakelock.vendor \
    android.hardware.sensors@2.1.vendor \
    android.hardware.wifi-V2-ndk.vendor \
    android.hardware.wifi.supplicant-V3-ndk.vendor \
    android.system.keystore2-V1-ndk.vendor \
    libaudioutils.vendor \
    libcamera_metadata.vendor \
    libcppbor.vendor \
    libfmq.vendor \
    libhardware.vendor \
    libkeymaster_portable.vendor \
    libkeystore-engine-wifi-hidl \
    libmediandk.vendor \
    libnativewindow.vendor \
    libnl.vendor \
    libpower.vendor \
    libpuresoftkeymasterdevice.vendor \
    libstagefright_foundation.vendor \
    libwifi-system-iface.vendor \
    libxml2.vendor

# vndbinder context manager, only pulled in below shipping api level 30
PRODUCT_PACKAGES += \
    vndservice \
    vndservicemanager

# USB
PRODUCT_PACKAGES += \
    android.hardware.usb-service.samsung \
    android.hardware.usb.gadget-service.samsung \
    init.s5e3830.usb.rc

$(call soong_config_set,samsungUsbGadgetVars,gadget_name,13600000.dwc3)

# Audio
PRODUCT_PACKAGES += \
    android.hardware.audio.service \
    android.hardware.bluetooth.audio-impl \
    audio.bluetooth.default

# NFC
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/nfc/libnfc-nci.conf:$(TARGET_COPY_OUT_PRODUCT)/etc/libnfc-nci.conf \
    $(LOCAL_PATH)/sku/permissions/unavail.android.hardware.nfc.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/sku_disabled/unavail.hardware.samsung.nfc.xml \
    $(foreach f,android.hardware.nfc.xml android.hardware.nfc.hce.xml android.hardware.nfc.hcef.xml android.hardware.nfc.uicc.xml com.nxp.mifare.xml,\
        $(LOCAL_PATH)/configs/nfc/$(f):$(TARGET_COPY_OUT_ODM)/etc/permissions/$(f))

# Overlays
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay
PRODUCT_ENFORCE_RRO_TARGETS += *

PRODUCT_PACKAGES += \
    WifiOverlay

# Permissions
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.fingerprint.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.fingerprint.xml

# Task profiles
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/processgroup/task_profiles.json:$(TARGET_COPY_OUT_VENDOR)/etc/task_profiles.json

# Kernel
PRODUCT_ENABLE_UFFD_GC := true
PRODUCT_SET_DEBUGFS_RESTRICTIONS := true

# Touchscreen firmware, requested before /vendor is mounted
PRODUCT_COPY_FILES += \
    $(foreach f,ft8720_a14.bin ft8720_a14_ramtest.bin ili7807_a14.hex nt36672_a14_csot.bin nt36672_a14_csot_mp.bin nt36672_a14_tianma.bin nt36672_a14_tianma_mp.bin,\
        vendor/samsung/a14/proprietary/vendor/firmware/$(f):$(TARGET_COPY_OUT_VENDOR_RAMDISK)/vendor/firmware/$(f))

# OTA
AB_OTA_UPDATER := false

# Partitions
PRODUCT_USE_DYNAMIC_PARTITIONS := true

# Platform
BOARD_SHIPPING_API_LEVEL := 33
PRODUCT_SHIPPING_API_LEVEL := $(BOARD_SHIPPING_API_LEVEL)

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH) \
    hardware/samsung

# Call the proprietary setup
$(call inherit-product, vendor/samsung/a14/a14-vendor.mk)

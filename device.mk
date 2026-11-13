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

PRODUCT_PRODUCT_PROPERTIES += \
    sys.brightness.disable_gamma_conversion=true

# Attention
PRODUCT_PACKAGES += \
    ScreenAttention

# Init
PRODUCT_PACKAGES += \
    init.a14.rc

PRODUCT_COPY_FILES += \
    $(foreach f,init.baseband.rc init.gps.rc init.recovery.s5e3830.rc init.vendor.rilcommon.rc,\
        $(LOCAL_PATH)/configs/init/$(f):$(TARGET_COPY_OUT_VENDOR)/etc/init/$(f))

# Linker
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/linker/public.libraries.txt:$(TARGET_COPY_OUT_VENDOR)/etc/public.libraries.txt

# Media
PRODUCT_COPY_FILES += \
    $(foreach f,media_codecs.xml media_codecs_c2.xml media_codecs_performance.xml media_codecs_performance_c2.xml media_profiles_V1_0.xml,\
        $(LOCAL_PATH)/configs/media/$(f):$(TARGET_COPY_OUT_VENDOR)/etc/$(f))

ifneq ($(TARGET_IS_A13),true)
PRODUCT_PACKAGES += \
    fstab.s5e3830 \
    fstab.s5e3830.vendor_ramdisk

PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)/configs/init/fstab
endif

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
    android.hardware.audio.effect@6.0-impl \
    android.hardware.audio.service \
    android.hardware.audio@6.0-impl \
    android.hardware.bluetooth.audio-impl \
    audio.bluetooth.default \
    audio.r_submix.default \
    audio.usb.default

# GNSS
PRODUCT_PACKAGES += \
    android.hardware.gnss@2.1-service.samsung

# Health
PRODUCT_PACKAGES += \
    android.hardware.health-service.samsung \
    android.hardware.health-service.samsung-recovery \
    vendor.lineage.health-service.default

$(call soong_config_set,lineage_health,charging_control_charging_path,/sys/class/power_supply/battery/batt_slate_mode)
$(call soong_config_set,lineage_health,charging_control_charging_enabled,0)
$(call soong_config_set,lineage_health,charging_control_charging_disabled,1)
$(call soong_config_set_bool,lineage_health,charging_control_supports_bypass,false)

# Light
PRODUCT_PACKAGES += \
    android.hardware.light-service.samsung

# RIL
PRODUCT_PACKAGES += \
    cbd \
    secril_config_svc

# Sensors
PRODUCT_PACKAGES += \
    android.hardware.sensors-service.samsung-multihal

# Vibrator
PRODUCT_PACKAGES += \
    android.hardware.vibrator-service.samsung

# NFC
ifneq ($(TARGET_IS_A13),true)
PRODUCT_VENDOR_PROPERTIES += \
    ro.vendor.nfc.feature.chipname=SLSI
endif

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/nfc/libnfc-nci.conf:$(TARGET_COPY_OUT_PRODUCT)/etc/libnfc-nci.conf \
    $(LOCAL_PATH)/sku/permissions/unavail.android.hardware.nfc.xml:$(TARGET_COPY_OUT_ODM)/etc/permissions/sku_disabled/unavail.android.hardware.nfc.xml \
    $(foreach f,android.hardware.nfc.xml android.hardware.nfc.hce.xml android.hardware.nfc.hcef.xml android.hardware.nfc.uicc.xml com.nxp.mifare.xml,\
        $(LOCAL_PATH)/configs/nfc/$(f):$(TARGET_COPY_OUT_ODM)/etc/permissions/$(f))

# Overlays
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay
PRODUCT_ENFORCE_RRO_TARGETS += *

PRODUCT_PACKAGES += \
    CarrierSettingsA14Overlay \
    NavigationBarNoHintOverlay \
    WifiOverlay

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/sysconfig/preinstalled-packages-navbar-nohint.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/sysconfig/preinstalled-packages-navbar-nohint.xml

# Permissions
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.audio.low_latency.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.audio.low_latency.xml \
    frameworks/native/data/etc/android.hardware.bluetooth.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.bluetooth.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.bluetooth_le.xml \
    frameworks/native/data/etc/android.hardware.camera.autofocus.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.autofocus.xml \
    frameworks/native/data/etc/android.hardware.camera.flash-autofocus.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.flash-autofocus.xml \
    frameworks/native/data/etc/android.hardware.camera.front.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.front.xml \
    frameworks/native/data/etc/android.hardware.camera.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.xml \
    frameworks/native/data/etc/android.hardware.fingerprint.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.fingerprint.xml \
    frameworks/native/data/etc/android.hardware.keystore.app_attest_key.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.keystore.app_attest_key.xml \
    frameworks/native/data/etc/android.hardware.location.gps.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.location.gps.xml \
    frameworks/native/data/etc/android.hardware.opengles.aep.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.opengles.aep.xml \
    frameworks/native/data/etc/android.hardware.sensor.accelerometer.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.accelerometer.xml \
    frameworks/native/data/etc/android.hardware.sensor.compass.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.compass.xml \
    frameworks/native/data/etc/android.hardware.sensor.light.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.light.xml \
    frameworks/native/data/etc/android.hardware.sensor.proximity.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.proximity.xml \
    frameworks/native/data/etc/android.hardware.sensor.stepcounter.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.stepcounter.xml \
    frameworks/native/data/etc/android.hardware.sensor.stepdetector.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.sensor.stepdetector.xml \
    frameworks/native/data/etc/android.hardware.telephony.satellite.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.telephony.satellite.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.jazzhand.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.touchscreen.multitouch.jazzhand.xml \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.usb.accessory.xml \
    frameworks/native/data/etc/android.hardware.usb.host.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.usb.host.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.wifi.passpoint.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.passpoint.xml \
    frameworks/native/data/etc/android.hardware.wifi.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.software.device_id_attestation.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.device_id_attestation.xml \
    frameworks/native/data/etc/android.software.ipsec_tunnels.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.ipsec_tunnels.xml \
    frameworks/native/data/etc/android.software.midi.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.midi.xml \
    frameworks/native/data/etc/android.software.verified_boot.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.verified_boot.xml \
    frameworks/native/data/etc/handheld_core_hardware.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/handheld_core_hardware.xml \
    frameworks/native/data/etc/android.hardware.vulkan.compute-0.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.vulkan.compute.xml \
    frameworks/native/data/etc/android.hardware.vulkan.level-1.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.vulkan.level.xml \
    frameworks/native/data/etc/android.hardware.vulkan.version-1_3.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.vulkan.version.xml \
    frameworks/native/data/etc/android.software.opengles.deqp.level-2024-03-01.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.opengles.deqp.level.xml \
    frameworks/native/data/etc/android.software.vulkan.deqp.level-2024-03-01.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.vulkan.deqp.level.xml

# Task profiles
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/processgroup/task_profiles.json:$(TARGET_COPY_OUT_VENDOR)/etc/task_profiles.json

# Kernel
PRODUCT_ENABLE_UFFD_GC := true
PRODUCT_SET_DEBUGFS_RESTRICTIONS := true

# Touchscreen firmware, requested before /vendor is mounted
ifneq ($(TARGET_IS_A13),true)
PRODUCT_COPY_FILES += \
    $(foreach f,ft8720_a14.bin ft8720_a14_ramtest.bin ili7807_a14.hex nt36672_a14_csot.bin nt36672_a14_csot_mp.bin nt36672_a14_tianma.bin nt36672_a14_tianma_mp.bin,\
        vendor/samsung/a14/proprietary/vendor/firmware/$(f):$(TARGET_COPY_OUT_VENDOR_RAMDISK)/vendor/firmware/$(f))
endif

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
    hardware/samsung \
    hardware/samsung_slsi-linaro/exynos/cpboot_v3

# Call the proprietary setup
ifneq ($(TARGET_IS_A13),true)
$(call inherit-product, vendor/samsung/a14/a14-vendor.mk)
endif

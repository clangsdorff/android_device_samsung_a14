#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

TARGET_EXCLUDE_ADB_ROOT := true
TARGET_EXCLUDES_AUDIOFX := true
TARGET_EXCLUDE_LOS_EXTRA_CLI_UTILS := true
TARGET_SKIP_BACKUPTOOL := true

PRODUCT_PACKAGES += \
    GameSpace

# Inherit some common Lineage stuff
$(call inherit-product, $(SRC_TARGET_DIR)/product/generic_system.mk)
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# GApps
$(call inherit-product-if-exists, vendor/gapps/arm64/arm64-vendor.mk)

$(foreach m,\
    AndroidAutoStub \
    GoogleCalendarSyncAdapter \
    GoogleContactsSyncAdapter \
    GoogleFeedback \
    GoogleRestore \
    GoogleServicesFramework \
    MarkupGoogle_v2 \
    Phonesky \
    PrebuiltExchange3Google \
    SpeechServicesByGoogle \
    Velvet \
    VelvetTitan \
    com.google.android.dialer.support \
    talkback,\
    $(call add-product-dex-preopt-module-config,$(m),disable))

PRODUCT_MODEL := SM-A145F
PRODUCT_BRAND := samsung
PRODUCT_MANUFACTURER := samsung

PRODUCT_SYSTEM_NAME := a14nsxx
PRODUCT_SYSTEM_BRAND := samsung
PRODUCT_SYSTEM_MANUFACTURER := samsung
PRODUCT_SYSTEM_MODEL := SM-A145F

PRODUCT_GMS_CLIENTID_BASE := android-samsung-ss

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="a14nsxx-user 15 AP3A.240905.015.A2 A145FXXSEDZF2 release-keys" \
    BuildFingerprint=samsung/a14nsxx/a14:15/AP3A.240905.015.A2/A145FXXSEDZF2:user/release-keys \
    DeviceProduct=a14nsxx \
    SystemName=a14nsxx

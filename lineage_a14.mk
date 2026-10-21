#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from device
$(call inherit-product, device/samsung/a14/device.mk)

# Inherit some common Lineage stuff
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

PRODUCT_NAME := lineage_a14
PRODUCT_DEVICE := a14
PRODUCT_MODEL := SM-A145F
PRODUCT_BRAND := samsung
PRODUCT_MANUFACTURER := samsung

PRODUCT_GMS_CLIENTID_BASE := android-samsung-ss

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="a14nsxx-user 15 AP3A.240905.015.A2 A145FXXSEDZF2 release-keys" \
    BuildFingerprint=samsung/a14nsxx/a14:15/AP3A.240905.015.A2/A145FXXSEDZF2:user/release-keys \
    DeviceProduct=a14nsxx \
    SystemName=a14nsxx

#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

$(call inherit-product, device/samsung/a14/a13/device.mk)
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)
$(call inherit-product-if-exists, vendor/gapps/arm64/arm64-vendor.mk)

PRODUCT_NAME := lineage_a13
PRODUCT_DEVICE := a13
PRODUCT_MODEL := SM-A145F
PRODUCT_BRAND := samsung
PRODUCT_MANUFACTURER := samsung

PRODUCT_GMS_CLIENTID_BASE := android-samsung-ss

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="a14nsxx-user 15 AP3A.240905.015.A2 A145FXXSEDZF2 release-keys" \
    BuildFingerprint=samsung/a14nsxx/a14:15/AP3A.240905.015.A2/A145FXXSEDZF2:user/release-keys \
    DeviceProduct=a14nsxx \
    SystemName=a14nsxx

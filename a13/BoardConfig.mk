#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

TARGET_IS_A13 := true

include device/samsung/a14/BoardConfig.mk
include vendor/samsung/a13/BoardConfigVendor.mk

# Sboot only takes a v2 boot image; os_version/patch must stay equal to stock for KeyMint
BOARD_BOOT_HEADER_VERSION := 2
BOARD_KERNEL_BASE := 0x10000000
BOARD_KERNEL_CMDLINE := androidboot.hardware=s5e3830 loop.max_part=7
BOARD_MKBOOTIMG_ARGS := --header_version 2 \
    --kernel_offset 0x00008000 --ramdisk_offset 0x01000000 \
    --tags_offset 0x00000100 --dtb_offset 0x00000000 \
    --board SRPUK09B014 \
    --os_version 14.0.0 --os_patch_level 2026-02
BOARD_MKBOOTIMG_INIT_ARGS :=
BOARD_INIT_BOOT_HEADER_VERSION :=
BOARD_BOOTCONFIG :=
BOARD_BOOTIMAGE_PARTITION_SIZE := 46137344
BOARD_INIT_BOOT_IMAGE_PARTITION_SIZE :=
BOARD_VENDOR_BOOTIMAGE_PARTITION_SIZE :=

# Recovery partition takes a self-contained v2 image: kernel, dtb, recovery_dtbo and modules
BOARD_RECOVERYIMAGE_PARTITION_SIZE := 55574528
BOARD_RECOVERY_RAMDISK_USE_ZSTD := true
BOARD_RECOVERY_MKBOOTIMG_ARGS :=
# sboot verifies the recovery footer like stock, vbmeta flags 3 skip the key check
BOARD_AVB_RECOVERY_ALGORITHM := SHA256_RSA4096
BOARD_AVB_RECOVERY_ROLLBACK_INDEX := 1

BOARD_DTB_CFG := device/samsung/a14/a13/dts/s5e3830.cfg
BOARD_DTBO_CFG := device/samsung/a14/a13/dts/a13.cfg

# Kernel
TARGET_KERNEL_CONFIG := a13noavb_defconfig
TARGET_KERNEL_SOURCE := kernel/samsung/a13

# No vendor_boot or vendor_dlkm, first stage init loads every module from the boot ramdisk
BOARD_VENDOR_RAMDISK_KERNEL_MODULES_LOAD := $(strip $(shell cat device/samsung/a14/a13/modules.load))
BOOT_KERNEL_MODULES := $(BOARD_VENDOR_RAMDISK_KERNEL_MODULES_LOAD)
# boot list minus camera, codecs, wifi/bt, audio, modem, sensors and debug, plus their dependencies
BOARD_RECOVERY_RAMDISK_KERNEL_MODULES_LOAD := $(strip $(shell cat device/samsung/a14/a13/modules.recovery.load))
RECOVERY_KERNEL_MODULES := $(BOARD_RECOVERY_RAMDISK_KERNEL_MODULES_LOAD)
BOARD_VENDOR_KERNEL_MODULES_LOAD :=
BOARD_VENDOR_RAMDISK_FRAGMENTS :=

BOARD_SUPER_PARTITION_SIZE := 6308233216
BOARD_GROUP_BASIC_SIZE := 6304038912
BOARD_GROUP_BASIC_PARTITION_LIST := system vendor product odm
BOARD_USES_SYSTEM_DLKMIMAGE :=
BOARD_SYSTEM_DLKMIMAGE_FILE_SYSTEM_TYPE :=
BOARD_VENDOR_DLKMIMAGE_FILE_SYSTEM_TYPE :=
TARGET_COPY_OUT_SYSTEM_DLKM := system/system_dlkm
TARGET_COPY_OUT_VENDOR_DLKM := vendor/vendor_dlkm

TARGET_OTA_ASSERT_DEVICE := a13,a14
TARGET_RELEASETOOLS_EXTENSIONS := device/samsung/a14/a13/releasetools

TARGET_RECOVERY_FSTAB := device/samsung/a14/a13/fstab.s5e3830
BOARD_VENDOR_SEPOLICY_DIRS += device/samsung/a14/a13/sepolicy/vendor

# Fingerprint
$(call soong_config_set_bool,samsungFingerprintVars,use_32bit_hal,true)

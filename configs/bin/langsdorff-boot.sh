#!/vendor/bin/sh
#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

# Bootloader / Verified Boot / Warranty state spoofing
/vendor/bin/resetprop -n ro.boot.verifiedbootstate green
/vendor/bin/resetprop -n vendor.boot.verifiedbootstate green
/vendor/bin/resetprop -n ro.boot.vbmeta.device_state locked
/vendor/bin/resetprop -n vendor.boot.vbmeta.device_state locked
/vendor/bin/resetprop -n ro.boot.flash.locked 1
/vendor/bin/resetprop -n ro.boot.veritymode enforcing
/vendor/bin/resetprop -n ro.boot.warranty_bit 0
/vendor/bin/resetprop -n ro.vendor.warranty_bit 0
/vendor/bin/resetprop -n ro.warranty_bit 0
/vendor/bin/resetprop -n sys.oem_unlock_allowed 0
/vendor/bin/resetprop -n init.svc.flash_recovery stopped
/vendor/bin/resetprop --delete ro.build.selinux

# Keep system/GSI fingerprint aligned with vendor fingerprint
vendor_fp="$(getprop ro.vendor.build.fingerprint)"
if [ -n "$vendor_fp" ]; then
    /vendor/bin/resetprop -n ro.system.build.fingerprint "$vendor_fp"
    /vendor/bin/resetprop -n ro.bootimage.build.fingerprint "$vendor_fp"
fi

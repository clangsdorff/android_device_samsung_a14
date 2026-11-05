#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/samsung/a14',
    'hardware/samsung',
]

blob_fixups: blob_fixups_user_type = {
    'vendor/lib/hw/audio.primary.s5e3830.so': blob_fixup()
        .replace_needed('libaudioroute.so', 'libaudioroute_sec.so')
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib/hw/audio.usb.default.so': blob_fixup()
        .replace_needed('libalsautils.so', 'libalsautils_sec.so')
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib/libaboxdump.so': blob_fixup()
        .replace_needed('libaudioroute.so', 'libaudioroute_sec.so')
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib/libalsautils_sec.so': blob_fixup()
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib/libaudioparamupdate.so': blob_fixup()
        .replace_needed('libaudioroute.so', 'libaudioroute_sec.so')
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib/libaudioproxy.so': blob_fixup()
        .replace_needed('libalsautils.so', 'libalsautils_sec.so')
        .replace_needed('libaudioroute.so', 'libaudioroute_sec.so')
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib/libaudioroute_sec.so': blob_fixup()
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib/libsensorlistener.so': blob_fixup()
        .replace_needed('libsensorndkbridge.so', 'libsensorndkbridge_sec.so'),
    'vendor/lib/soundfx/libaudioeffectoffload.so': blob_fixup()
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib64/hw/audio.usb.default.so': blob_fixup()
        .replace_needed('libalsautils.so', 'libalsautils_sec.so')
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib64/libalsautils_sec.so': blob_fixup()
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib64/libsensorlistener.so': blob_fixup()
        .replace_needed('libsensorndkbridge.so', 'libsensorndkbridge_sec.so'),
    'vendor/lib64/soundfx/libaudioeffectoffload.so': blob_fixup()
        .replace_needed('libtinyalsa.so', 'libtinyalsa_sec.so'),
    'vendor/lib64/unihal_android.so': blob_fixup()
        .replace_needed('libsensorndkbridge.so', 'libsensorndkbridge_sec.so'),
    ('vendor/lib/libsensorndkbridge_sec.so', 'vendor/lib64/libsensorndkbridge_sec.so'): blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so'),
    'vendor/etc/init/android.hardware.bluetooth@1.0-service.samsung.rc': blob_fixup()
        .regex_replace('(?m)^ +interface vendor\\.samsung\\.hardware\\.bluetooth@2\\.0::ISehBluetooth default\\n', ''),
    'vendor/etc/init/android.hardware.security.keymint-service.samsung.rc': blob_fixup()
        .regex_replace('(?m)keymint-service$', 'keymint-service.samsung'),
    'vendor/etc/init/android.hardware.sensors-service-multihal.samsung.rc': blob_fixup()
        .regex_replace('(?m)multihal$', 'multihal.samsung'),
    'vendor/etc/init/android.hardware.wifi-service.samsung.rc': blob_fixup()
        .regex_replace('(?m)wifi-service$', 'wifi-service.samsung'),
    'vendor/etc/init/android.hardware.wifi.supplicant-service.samsung.rc': blob_fixup()
        .regex_replace('hw/wpa_supplicant ', 'hw/wpa_supplicant.samsung '),
    'vendor/lib/lib_SoundAlive_play_plus_ver400.so': blob_fixup()
        .add_needed('liblog.so'),
}  # fmt: skip

def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


def lib_fixup_sec_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_sec' if partition == 'vendor' else None


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    (
        'libalsautils',
        'libaudioroute',
        'libsensorndkbridge',
        'libtinyalsa',
    ): lib_fixup_sec_suffix,
    (
        'libomp',
        'libuuid',
    ): lib_fixup_vendor_suffix,
}

module = ExtractUtilsModule(
    'a14',
    'samsung',
    namespace_imports=namespace_imports,
    lib_fixups=lib_fixups,
    blob_fixups=blob_fixups,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()

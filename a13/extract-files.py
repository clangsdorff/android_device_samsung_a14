#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

import importlib.util
import os
import shutil

from extract_utils.fixups_blob import blob_fixup
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

spec = importlib.util.spec_from_file_location(
    'a14_extract',
    os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'extract-files.py'),
)
a14 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(a14)

module = ExtractUtilsModule(
    'a13',
    'samsung',
    device_rel_path='device/samsung/a14/a13',
    namespace_imports=a14.namespace_imports,
    lib_fixups=a14.lib_fixups,
    blob_fixups={
        **a14.blob_fixups,
        'vendor/etc/init/nfc-service-nxp-samsung.rc': blob_fixup().regex_replace(
            r'android\.hardware\.nfc-service\.nxp\n', 'android.hardware.nfc-service.nxp-samsung\n'
        ),
    },
)

# Hand fixed or known good in the A14 vendor tree, the extraction source has the stock copies
A14_FILES = (
    'vendor/etc/audio_policy_configuration.xml',
    'vendor/etc/init/android.hardware.wifi.supplicant-service.samsung.rc',
    'vendor/lib/libsensorndkbridge_sec.so',
    'vendor/lib64/libsensorndkbridge_sec.so',
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    try:
        utils.run()
    finally:
        root = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', '..'))
        for f in A14_FILES:
            shutil.copy2(
                os.path.join(root, 'vendor/samsung/a14/proprietary', f),
                os.path.join(root, 'vendor/samsung/a13/proprietary', f),
            )

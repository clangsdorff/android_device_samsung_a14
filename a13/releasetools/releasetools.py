#!/bin/env python3
#
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

def FullOTA_InstallEnd(info):
    OTA_InstallEnd(info)

def IncrementalOTA_InstallEnd(info):
    info.input_zip = info.target_zip
    OTA_InstallEnd(info)

def OTA_InstallEnd(info):
    import common
    common.ZipWriteStr(info.output_zip, "dtbo.img", info.input_zip.read("IMAGES/dtbo.img"))
    info.script.AppendExtra('package_extract_file("dtbo.img", "/dev/block/by-name/dtbo");')

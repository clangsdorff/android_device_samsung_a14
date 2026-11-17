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
    for name in ("dtbo.img", "vbmeta.img"):
        common.ZipWriteStr(info.output_zip, name, info.input_zip.read("IMAGES/" + name))
        info.script.AppendExtra('package_extract_file("%s", "/dev/block/by-name/%s");' % (name, name.split(".")[0]))

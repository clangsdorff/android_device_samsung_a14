#!/bin/env python3
#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-FileCopyrightText: 2026 Langsdorff
# SPDX-License-Identifier: Apache-2.0
#

import common

def FullOTA_InstallEnd(info):
    OTA_InstallEnd(info)
    return

def IncrementalOTA_InstallEnd(info):
    info.input_zip = info.target_zip
    OTA_InstallEnd(info)
    return

def AddImage(info, basename, dest):
    data = info.input_zip.read("IMAGES/" + basename)
    common.ZipWriteStr(info.output_zip, basename, data)
    info.script.Print("Patching {} image unconditionally...".format(dest.split('/')[-1]))
    info.script.AppendExtra('package_extract_file("%s", "%s");' % (basename, dest))

def OTA_InstallEnd(info):
    AddImage(info, "dtbo.img", "/dev/block/by-name/dtbo")
    AddImage(info, "init_boot.img", "/dev/block/by-name/init_boot")
    AddImage(info, "vbmeta.img", "/dev/block/by-name/vbmeta")
    AddImage(info, "vendor_boot.img", "/dev/block/by-name/vendor_boot")
    return

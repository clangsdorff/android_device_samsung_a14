SAMSUNG_SOUNDS := device/samsung/a14/sounds

PRODUCT_COPY_FILES += \
    $(foreach f,$(notdir $(wildcard $(SAMSUNG_SOUNDS)/ringtones/*.ogg)),\
        $(SAMSUNG_SOUNDS)/ringtones/$(f):$(TARGET_COPY_OUT_PRODUCT)/media/audio/ringtones/$(f)) \
    $(foreach f,$(notdir $(wildcard $(SAMSUNG_SOUNDS)/notifications/*.ogg)),\
        $(SAMSUNG_SOUNDS)/notifications/$(f):$(TARGET_COPY_OUT_PRODUCT)/media/audio/notifications/$(f)) \
    $(foreach f,$(notdir $(wildcard $(SAMSUNG_SOUNDS)/ui/*.ogg)),\
        $(SAMSUNG_SOUNDS)/ui/$(f):$(TARGET_COPY_OUT_PRODUCT)/media/audio/ui_samsung/$(f)) \
    $(SAMSUNG_SOUNDS)/ringtones/Homecoming.ogg:$(TARGET_COPY_OUT_PRODUCT)/media/audio/alarms/Homecoming.ogg

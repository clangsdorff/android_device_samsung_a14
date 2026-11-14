#include <aidl/android/hardware/nfc/BnNfc.h>
#include <aidl/android/hardware/nfc/INfcClientCallback.h>
#include <aidl/android/hardware/nfc/NfcConfig.h>
#include <aidl/android/hardware/nfc/PresenceCheckAlgorithm.h>
#include <aidl/android/hardware/nfc/ProtocolDiscoveryConfig.h>
#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>

using ::aidl::android::hardware::nfc::BnNfc;
using ::aidl::android::hardware::nfc::INfcClientCallback;
using ::aidl::android::hardware::nfc::NfcCloseType;
using ::aidl::android::hardware::nfc::NfcConfig;
using ::aidl::android::hardware::nfc::NfcEvent;
using ::aidl::android::hardware::nfc::NfcStatus;
using ::aidl::android::hardware::nfc::PresenceCheckAlgorithm;
using ::ndk::ScopedAStatus;

extern "C" {
int phNxpNciHal_open(void (*eventCallback)(uint8_t, uint8_t), void (*dataCallback)(uint16_t, uint8_t*));
int phNxpNciHal_close(bool);
void phNxpNciHal_configDiscShutdown();
int phNxpNciHal_core_initialized(uint16_t, uint8_t*);
void phNxpNciHal_do_factory_reset();
int phNxpNciHal_power_cycle();
int phNxpNciHal_pre_discover();
int phNxpNciHal_write(uint16_t data_len, const uint8_t* p_data);
void phNxpNciHal_setVerboseLogging(bool enable);
bool phNxpNciHal_getVerboseLogging();

void phNxpNciHal_getExtVendorConfig();
bool GetNxpNumValue(const char* name, void* p_val, size_t len);
bool GetNxpByteArrayValue(const char* name, char* p_val, size_t len, size_t* p_real_len);
uint8_t phNxpNciHal_updateAutonomousPwrState(uint8_t);
}

class NxpNfc : public BnNfc {
  public:
    NxpNfc() = default;

    ScopedAStatus open(const std::shared_ptr<INfcClientCallback>& clientCallback) override {
        LOG(INFO) << "NxpNfc::open";
        if (clientCallback == nullptr) {
            LOG(ERROR) << "NxpNfc::open null clientCallback";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }

        mCallback = clientCallback;
        mDeathRecipient = AIBinder_DeathRecipient_new(onDeath);
        auto linkRet = AIBinder_linkToDeath(clientCallback->asBinder().get(), mDeathRecipient, this);
        if (linkRet != STATUS_OK) {
            LOG(ERROR) << "linkToDeath failed: " << linkRet;
        }

        int ret = phNxpNciHal_open(eventCallback, dataCallback);
        return ret == 0 ? ScopedAStatus::ok()
                        : ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
    }

    ScopedAStatus close(NfcCloseType type) override {
        LOG(INFO) << "NxpNfc::close";
        if (mCallback == nullptr) {
            LOG(ERROR) << "NxpNfc::close mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }

        int ret = 0;
        if (type == NfcCloseType::HOST_SWITCHED_OFF) {
            phNxpNciHal_configDiscShutdown();
        } else {
            ret = phNxpNciHal_close(false);
        }

        mCallback = nullptr;
        if (mDeathRecipient != nullptr) {
            AIBinder_DeathRecipient_delete(mDeathRecipient);
            mDeathRecipient = nullptr;
        }

        return ret == 0 ? ScopedAStatus::ok()
                        : ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
    }

    ScopedAStatus coreInitialized() override {
        LOG(INFO) << "NxpNfc::coreInitialized";
        if (mCallback == nullptr) {
            LOG(ERROR) << "NxpNfc::coreInitialized mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }

        int ret = phNxpNciHal_core_initialized(0, nullptr);
        return ret == 0 ? ScopedAStatus::ok()
                        : ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
    }

    ScopedAStatus factoryReset() override {
        LOG(INFO) << "NxpNfc::factoryReset";
        phNxpNciHal_do_factory_reset();
        return ScopedAStatus::ok();
    }

    ScopedAStatus getConfig(NfcConfig* _aidl_return) override {
        LOG(INFO) << "NxpNfc::getConfig";
        NfcConfig config{};
        uint32_t num = 0;
        uint8_t byte_val = 0;
        char buffer[260];
        size_t actual_len = 0;

        phNxpNciHal_getExtVendorConfig();

        if (GetNxpNumValue("NFA_POLL_BAIL_OUT_MODE", &num, sizeof(num))) {
            config.nfaPollBailOutMode = (num != 0);
        }
        if (GetNxpNumValue("ISO_DEP_MAX_TRANSCEIVE", &num, sizeof(num))) {
            config.maxIsoDepTransceiveLength = num;
        }
        if (GetNxpNumValue("DEFAULT_OFFHOST_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultOffHostRoute = byte_val;
        }
        if (GetNxpNumValue("DEFAULT_NFCF_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultOffHostRouteFelica = byte_val;
        }
        if (GetNxpNumValue("DEFAULT_SYS_CODE_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultSystemCodeRoute = byte_val;
        }
        if (GetNxpNumValue("DEFAULT_SYS_CODE_PWR_STATE", &byte_val, sizeof(byte_val))) {
            config.defaultSystemCodePowerState = phNxpNciHal_updateAutonomousPwrState(byte_val);
        }
        if (GetNxpNumValue("DEFAULT_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultRoute = byte_val;
        }
        if (GetNxpNumValue("OFF_HOST_ESE_PIPE_ID", &byte_val, sizeof(byte_val))) {
            config.offHostESEPipeId = byte_val;
        }
        if (GetNxpNumValue("OFF_HOST_SIM_PIPE_ID", &byte_val, sizeof(byte_val))) {
            config.offHostSIMPipeId = byte_val;
        }
        if (GetNxpByteArrayValue("DEVICE_HOST_WHITE_LIST", buffer, sizeof(buffer), &actual_len)) {
            config.hostAllowlist.assign(buffer, buffer + actual_len);
        }
        if (GetNxpByteArrayValue("OFFHOST_ROUTE_UICC", buffer, sizeof(buffer), &actual_len)) {
            config.offHostRouteUicc.assign(buffer, buffer + actual_len);
        }
        if (GetNxpByteArrayValue("OFFHOST_ROUTE_ESE", buffer, sizeof(buffer), &actual_len)) {
            config.offHostRouteEse.assign(buffer, buffer + actual_len);
        }
        if (GetNxpNumValue("DEFAULT_ISODEP_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultIsoDepRoute = byte_val;
        }

        *_aidl_return = config;
        return ScopedAStatus::ok();
    }

    ScopedAStatus powerCycle() override {
        LOG(INFO) << "NxpNfc::powerCycle";
        if (mCallback == nullptr) {
            LOG(ERROR) << "NxpNfc::powerCycle mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }
        return phNxpNciHal_power_cycle() ? ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED))
                                         : ScopedAStatus::ok();
    }

    ScopedAStatus preDiscover() override {
        LOG(INFO) << "NxpNfc::preDiscover";
        if (mCallback == nullptr) {
            LOG(ERROR) << "NxpNfc::preDiscover mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }
        return phNxpNciHal_pre_discover() ? ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED))
                                          : ScopedAStatus::ok();
    }

    ScopedAStatus write(const std::vector<uint8_t>& data, int32_t* _aidl_return) override {
        if (mCallback == nullptr) {
            LOG(ERROR) << "NxpNfc::write mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }
        *_aidl_return = phNxpNciHal_write(data.size(), data.data());
        return ScopedAStatus::ok();
    }

    ScopedAStatus setEnableVerboseLogging(bool enable) override {
        LOG(INFO) << "NxpNfc::setEnableVerboseLogging: " << enable;
        phNxpNciHal_setVerboseLogging(enable);
        return ScopedAStatus::ok();
    }

    ScopedAStatus isVerboseLoggingEnabled(bool* _aidl_return) override {
        *_aidl_return = phNxpNciHal_getVerboseLogging();
        return ScopedAStatus::ok();
    }

  private:
    static void eventCallback(uint8_t event, uint8_t status) {
        if (mCallback != nullptr) {
            auto ret = mCallback->sendEvent(static_cast<NfcEvent>(event), static_cast<NfcStatus>(status));
            if (!ret.isOk()) {
                LOG(ERROR) << "Failed to send event: " << static_cast<int>(event);
            }
        }
    }

    static void dataCallback(uint16_t data_len, uint8_t* p_data) {
        if (mCallback != nullptr && p_data != nullptr) {
            std::vector<uint8_t> data(p_data, p_data + data_len);
            auto ret = mCallback->sendData(data);
            if (!ret.isOk()) {
                LOG(ERROR) << "Failed to send data of len: " << data_len;
            }
        }
    }

    static void onDeath(void* cookie) {
        LOG(INFO) << "NFC client died";
        if (mCallback != nullptr && !AIBinder_isAlive(mCallback->asBinder().get())) {
            NxpNfc* nfc = static_cast<NxpNfc*>(cookie);
            nfc->close(NfcCloseType::DISABLE);
        }
    }

    static inline std::shared_ptr<INfcClientCallback> mCallback = nullptr;
    static inline AIBinder_DeathRecipient* mDeathRecipient = nullptr;
};

int main() {
    LOG(INFO) << "NXP NFC AIDL HAL starting up";
    if (!ABinderProcess_setThreadPoolMaxThreadCount(1)) {
        LOG(ERROR) << "failed to set thread pool max thread count";
        return 1;
    }
    std::shared_ptr<NxpNfc> nfc_service = ndk::SharedRefBase::make<NxpNfc>();
    const std::string instance = std::string(NxpNfc::descriptor) + "/default";
    binder_status_t status = AServiceManager_addService(nfc_service->asBinder().get(), instance.c_str());
    CHECK(status == STATUS_OK);
    ABinderProcess_joinThreadPool();
    return 0;
}

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
int nfc_hal_open(void (*eventCallback)(uint8_t, uint8_t), void (*dataCallback)(uint16_t, uint8_t*));
int nfc_hal_close();
int nfc_hal_closeForPowerOffCase();
int nfc_hal_core_initialized_for_aidl();
void nfc_hal_factory_reset();
int nfc_hal_power_cycle();
int nfc_hal_pre_discover();
int nfc_hal_write(uint16_t data_len, const uint8_t* p_data);
void nfc_hal_setLogging(bool enable);
bool nfc_hal_isLoggingEnabled();

bool GetNumValue(const char* name, void* p_val, size_t len);
bool GetByteArrayValue(const char* name, char* p_val, size_t len, size_t* p_real_len);
}

class SecNfc : public BnNfc {
  public:
    SecNfc() = default;

    ScopedAStatus open(const std::shared_ptr<INfcClientCallback>& clientCallback) override {
        LOG(INFO) << "SecNfc::open";
        if (clientCallback == nullptr) {
            LOG(ERROR) << "SecNfc::open null clientCallback";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }

        mCallback = clientCallback;
        mDeathRecipient = AIBinder_DeathRecipient_new(onDeath);
        auto linkRet = AIBinder_linkToDeath(clientCallback->asBinder().get(), mDeathRecipient, this);
        if (linkRet != STATUS_OK) {
            LOG(ERROR) << "linkToDeath failed: " << linkRet;
        }

        int ret = nfc_hal_open(eventCallback, dataCallback);
        return ret == 0 ? ScopedAStatus::ok()
                        : ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
    }

    ScopedAStatus close(NfcCloseType type) override {
        LOG(INFO) << "SecNfc::close";
        if (mCallback == nullptr) {
            LOG(ERROR) << "SecNfc::close mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }

        int ret = 0;
        if (type == NfcCloseType::HOST_SWITCHED_OFF) {
            ret = nfc_hal_closeForPowerOffCase();
        } else {
            ret = nfc_hal_close();
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
        LOG(INFO) << "SecNfc::coreInitialized";
        if (mCallback == nullptr) {
            LOG(ERROR) << "SecNfc::coreInitialized mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }

        int ret = nfc_hal_core_initialized_for_aidl();
        return ret == 0 ? ScopedAStatus::ok()
                        : ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
    }

    ScopedAStatus factoryReset() override {
        LOG(INFO) << "SecNfc::factoryReset";
        nfc_hal_factory_reset();
        return ScopedAStatus::ok();
    }

    ScopedAStatus getConfig(NfcConfig* _aidl_return) override {
        LOG(INFO) << "SecNfc::getConfig";
        NfcConfig config{};
        uint32_t num = 0;
        uint8_t byte_val = 0;
        char buffer[260];
        size_t actual_len = 0;

        if (GetNumValue("ISO_DEP_MAX_TRANSCEIVE", &num, sizeof(num))) {
            config.maxIsoDepTransceiveLength = num;
        }
        if (GetNumValue("DEFAULT_OFFHOST_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultOffHostRoute = byte_val;
        }
        if (GetNumValue("DEFAULT_NFCF_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultOffHostRouteFelica = byte_val;
        }
        if (GetNumValue("DEFAULT_SYS_CODE_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultSystemCodeRoute = byte_val;
        }
        if (GetNumValue("DEFAULT_SYS_CODE_PWR_STATE", &byte_val, sizeof(byte_val))) {
            config.defaultSystemCodePowerState = byte_val;
        }
        if (GetNumValue("DEFAULT_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultRoute = byte_val;
        }
        if (GetByteArrayValue("DEVICE_HOST_WHITE_LIST", buffer, sizeof(buffer), &actual_len)) {
            config.hostAllowlist.assign(buffer, buffer + actual_len);
        }
        if (GetNumValue("OFF_HOST_ESE_PIPE_ID", &byte_val, sizeof(byte_val))) {
            config.offHostESEPipeId = byte_val;
        }
        if (GetNumValue("OFF_HOST_SIM_PIPE_ID", &byte_val, sizeof(byte_val))) {
            config.offHostSIMPipeId = byte_val;
        }
        if (GetByteArrayValue("NFA_PROPRIETARY_CFG", buffer, sizeof(buffer), &actual_len)) {
            if (actual_len >= 1) config.nfaProprietaryCfg.protocol18092Active = buffer[0];
            if (actual_len >= 2) config.nfaProprietaryCfg.protocolBPrime = buffer[1];
            if (actual_len >= 3) config.nfaProprietaryCfg.protocolDual = buffer[2];
            if (actual_len >= 4) config.nfaProprietaryCfg.protocol15693 = buffer[3];
            if (actual_len >= 5) config.nfaProprietaryCfg.protocolKovio = buffer[4];
            if (actual_len >= 6) config.nfaProprietaryCfg.protocolMifare = buffer[5];
            if (actual_len >= 7) config.nfaProprietaryCfg.discoveryPollKovio = buffer[6];
            if (actual_len >= 8) config.nfaProprietaryCfg.discoveryPollBPrime = buffer[7];
            if (actual_len >= 9) config.nfaProprietaryCfg.discoveryListenBPrime = buffer[8];
        }
        if (GetNumValue("PRESENCE_CHECK_ALGORITHM", &byte_val, sizeof(byte_val))) {
            config.presenceCheckAlgorithm = static_cast<PresenceCheckAlgorithm>(byte_val);
        }
        if (GetByteArrayValue("OFFHOST_ROUTE_UICC", buffer, sizeof(buffer), &actual_len)) {
            config.offHostRouteUicc.assign(buffer, buffer + actual_len);
        }
        if (GetByteArrayValue("OFFHOST_ROUTE_ESE", buffer, sizeof(buffer), &actual_len)) {
            config.offHostRouteEse.assign(buffer, buffer + actual_len);
        }
        if (GetNumValue("DEFAULT_ISODEP_ROUTE", &byte_val, sizeof(byte_val))) {
            config.defaultIsoDepRoute = byte_val;
        }

        *_aidl_return = config;
        return ScopedAStatus::ok();
    }

    ScopedAStatus powerCycle() override {
        LOG(INFO) << "SecNfc::powerCycle";
        if (mCallback == nullptr) {
            LOG(ERROR) << "SecNfc::powerCycle mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }
        return nfc_hal_power_cycle() ? ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED))
                                     : ScopedAStatus::ok();
    }

    ScopedAStatus preDiscover() override {
        LOG(INFO) << "SecNfc::preDiscover";
        if (mCallback == nullptr) {
            LOG(ERROR) << "SecNfc::preDiscover mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }
        return nfc_hal_pre_discover() ? ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED))
                                      : ScopedAStatus::ok();
    }

    ScopedAStatus write(const std::vector<uint8_t>& data, int32_t* _aidl_return) override {
        if (mCallback == nullptr) {
            LOG(ERROR) << "SecNfc::write mCallback is null";
            return ScopedAStatus::fromServiceSpecificError(static_cast<int32_t>(NfcStatus::FAILED));
        }
        *_aidl_return = nfc_hal_write(data.size(), data.data());
        return ScopedAStatus::ok();
    }

    ScopedAStatus setEnableVerboseLogging(bool enable) override {
        LOG(INFO) << "SecNfc::setEnableVerboseLogging: " << enable;
        nfc_hal_setLogging(enable);
        return ScopedAStatus::ok();
    }

    ScopedAStatus isVerboseLoggingEnabled(bool* _aidl_return) override {
        *_aidl_return = nfc_hal_isLoggingEnabled();
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
            SecNfc* nfc = static_cast<SecNfc*>(cookie);
            nfc->close(NfcCloseType::DISABLE);
        }
    }

    static inline std::shared_ptr<INfcClientCallback> mCallback = nullptr;
    static inline AIBinder_DeathRecipient* mDeathRecipient = nullptr;
};

int main() {
    LOG(INFO) << "SEC NFC AIDL HAL starting up";
    if (!ABinderProcess_setThreadPoolMaxThreadCount(1)) {
        LOG(ERROR) << "failed to set thread pool max thread count";
        return 1;
    }
    std::shared_ptr<SecNfc> nfc_service = ndk::SharedRefBase::make<SecNfc>();
    const std::string instance = std::string(SecNfc::descriptor) + "/default";
    binder_status_t status = AServiceManager_addService(nfc_service->asBinder().get(), instance.c_str());
    CHECK(status == STATUS_OK);
    ABinderProcess_joinThreadPool();
    return 0;
}

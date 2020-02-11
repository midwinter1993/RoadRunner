#include "shadow.h"
#include "utils.h"


LockGuard::LockGuard(jvmtiEnv *jvmti, jrawMonitorID lock)
    : jvmti(jvmti)
    , lock_(lock) {
	jvmtiError err_code = jvmti->RawMonitorEnter(lock_);
	CHECK_ERROR("Cannot enter with raw monitor");
}

LockGuard::~LockGuard() {
	jvmtiError err_code = jvmti->RawMonitorExit(lock_);
	CHECK_ERROR("Cannot exit with raw monitor");
}

// ===============================================

jvmtiEnv* ShadowVar::jvmti = NULL;

// ===============================================

Target::Target(jobject object, jfieldID field, jclass field_klass)
    : object_(object)
    , field_(field)
    , field_klass_(field_klass) {

}

// ===============================================

Accessor::Accessor(): Accessor(NULL, NULL, NULL) {

}

Accessor::Accessor(jthread thread, jmethodID method, jlocation location)
    : thread_(thread)
    , method_(method)
    , location_(location) {
    timestamp_ = Clock::now();
}

// ===============================================

Event::Event(jthread thread, jmethodID method, jlocation location,
             jclass field_klass, jobject object, jfieldID field)
    : accessor_(thread, method, location)
    , target_(object, field, field_klass) {
}

// ===============================================

ShadowVar::ShadowVar()
    : in_trap_(false) {
    jvmtiError err_code = jvmti->CreateRawMonitor("Shadow lock", &lock_);
    CHECK_ERROR("Create raw monitor failure");
}

void ShadowVar::lock() {
	jvmtiError err_code = jvmti->RawMonitorEnter(lock_);
	CHECK_ERROR("Cannot enter with raw monitor");
}

void ShadowVar::unlock() {
	jvmtiError err_code = jvmti->RawMonitorExit(lock_);
	CHECK_ERROR("Cannot exit with raw monitor");
}

bool ShadowVar::is_in_trap() {
    LockGuard guard(jvmti, lock_);

    return this->in_trap_;
}

bool ShadowVar::is_in_trap_nolock() {
    return this->in_trap_;
}

bool ShadowVar::set_trap(const Accessor &accessor) {
    LockGuard guard(jvmti, lock_);

    if (in_trap_) {
        return false;
    }

    accessor_ = accessor;
    start_time_ = Clock::now();
    in_trap_ = true;

    return true;
}

void ShadowVar::clear_trap() {
    LockGuard guard(jvmti, lock_);

    in_trap_ = false;
}

Timestamp ShadowVar::get_last_trap_time() const {
    return start_time_;
}
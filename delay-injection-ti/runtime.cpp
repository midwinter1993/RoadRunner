#include "runtime.h"

#include <assert.h>
#include <chrono>
#include <thread>
#include <unordered_map>


// ===============================================

static thread_local std::unordered_map<uint64_t, Accessor> tl_access_history;

void tl_put_access(const Accessor &accessor, const Target &target) {
    uint64_t hash_key = target.hash();
    tl_access_history[hash_key] = accessor;
}

const Accessor& tl_get_access(const Target &target) {
    uint64_t hash_key = target.hash();
    assert(tl_access_history.find(hash_key) != tl_access_history.end());
    return tl_access_history[hash_key];
}

// ===============================================

static std::unordered_map<uint64_t, ShadowVar> g_shadow_var_map;

ShadowVar& get_shadow(const Target &target) {
    uint64_t hash_key = target.hash();
    return g_shadow_var_map[hash_key];
}

// ===============================================

bool check_shadow_var_in_trapping(const Event &e, ShadowVar &shadown_var) {
    if (!shadown_var.is_in_trap()) {
        return true;
    }
    return false;
}

bool need_trap() {
    return true;
}

void trap_on(const Event &e, ShadowVar &shadow_var) {
    if (!shadow_var.set_trap(e.accessor_)) {
        return;
    }

    std::this_thread::sleep_for(std::chrono::seconds(1));

    shadow_var.clear_trap();
}

void mbr_infer(const Event &e, ShadowVar &shadow_var) {

    shadow_var.lock();

    const Accessor &last_accessor = tl_get_access(e.target_);
    if (!last_accessor.is_valid()) {
        shadow_var.unlock();
        return;
    }

    Timestamp last_ts = tl_get_access(e.target_).get_ts();

    Timestamp current_ts = Clock::now();

    uint64_t elapsed_time_ms =  std::chrono::duration<double, std::milli>(current_ts - last_ts).count();

    if (elapsed_time_ms < 1000) {
        shadow_var.unlock();
        return;
    }

    if (shadow_var.is_in_trap_nolock()) {
        shadow_var.unlock();
        return;
    }

    if (last_ts < shadow_var.get_last_trap_time() ) {
        // Util.printf("May-HB: %s -> %s\n", trapInfo.access.getAccessInfo().toString(),
                // currentAccess.getAccessInfo().toString());
    }

    shadow_var.unlock();
}

// ===============================================

void on_event(const Event &e) {
    ShadowVar &shadow_var = get_shadow(e.target_);

    if (check_shadow_var_in_trapping(e, shadow_var)) {
        //
        // Check data race
        //
        tl_put_access(e.accessor_, e.target_);
        return;
    }
    if (need_trap()) {
        trap_on(e, shadow_var);
    } else {
        mbr_infer(e, shadow_var);
    }
    tl_put_access(e.accessor_, e.target_);
}
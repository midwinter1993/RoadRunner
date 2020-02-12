#ifndef __SHADOW_H__
#define __SHADOW_H__

#include <jvmti.h>

#include <chrono>

// typedef std::chrono::time_point<std::chrono::system_clock> Timestamp;
typedef std::chrono::high_resolution_clock Clock;
typedef std::chrono::time_point<Clock> Timestamp;


class Target {
public:
    Target(jobject object, jfieldID field, jclass field_klass);

    uint64_t hash() const { return 0xaf; }

    jobject object_;
    jfieldID field_;
    jclass field_klass_;
};

class Accessor {
public:
    Accessor();
    Accessor(jthread thread, jmethodID method, jlocation location);

    Timestamp get_ts() const { return timestamp_; }
    bool is_valid() const { return thread_ != NULL; }

private:
    jthread thread_;
    jmethodID method_;
    jlocation location_;
    Timestamp timestamp_;
};

class Event {
public:
    Event(jthread thread, jmethodID method, jlocation location,
          jclass field_klass, jobject object, jfieldID field);

    Accessor accessor_;
    Target target_;
};

class LockGuard {
public:
    LockGuard(jvmtiEnv *jvmti, jrawMonitorID lock);
    ~LockGuard();

private:
    jvmtiEnv *jvmti;
    jrawMonitorID lock_;
};

class ShadowVar {
public:
    ShadowVar();

    void lock();
    void unlock();
    bool is_in_trap();
    bool is_in_trap_nolock();
    bool set_trap(const Accessor &accessor);
    void clear_trap();
    Timestamp get_last_trap_time() const;

private:
    Accessor accessor_;
    Timestamp start_time_;
    bool in_trap_;
    jrawMonitorID lock_;

public:
    static jvmtiEnv* jvmti;
};

#endif
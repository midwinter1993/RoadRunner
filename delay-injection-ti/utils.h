#if !defined(__UTILS_H__)
#define __UTILS_H__

#include <jvmti.h>
#include <cstdint>

void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str);

#define CHECK_ERROR(msg) \
    do { \
        check_jvmti_error(jvmti, err_code, msg); \
    } while(0)

namespace utils {
    void init(jvmtiEnv *jvmti, char* options);
    void fini();
    void trace(jvmtiEnv *jvmti, const char* fmt, ...);

    void acq_big_lock(jvmtiEnv *jvmti);
    void rel_big_lock(jvmtiEnv *jvmti);

    uint64_t hash_object(JNIEnv *env, jobject object);
}

#endif // __UTILS_H__

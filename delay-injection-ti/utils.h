#if !defined(__UTILS_H__)
#define MACRO__UTILS_H__

#include <jvmti.h>

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
}

#endif // MACRO__UTILS_H__

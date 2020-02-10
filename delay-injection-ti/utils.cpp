#include "utils.h"

static FILE* out;
static jrawMonitorID vmtrace_lock;
static jlong start_time;

void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str) {
	if (errnum != JVMTI_ERROR_NONE) {
		char *errnum_str;

		errnum_str = NULL;
		(void) jvmti->GetErrorName(errnum, &errnum_str);

		printf("ERROR: JVMTI: %d(%s): %s\n", errnum,
				(errnum_str == NULL ? "Unknown" : errnum_str),
				(str == NULL ? "" : str));
	}
}

namespace utils {

void trace(jvmtiEnv* jvmti, const char* fmt, ...) {
    jlong current_time;
    jvmti->GetTime(&current_time);

    char buf[1024];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);

    jvmti->RawMonitorEnter(vmtrace_lock);

    fprintf(out, "[%.5f] %s\n", (current_time - start_time) / 1000000000.0, buf);

    jvmti->RawMonitorExit(vmtrace_lock);
}

void init(jvmtiEnv *jvmti, char* options) {
    if (options == NULL || !options[0]) {
        out = stderr;
    } else if ((out = fopen(options, "w")) == NULL) {
        fprintf(stderr, "Cannot open output file: %s\n", options);
        return;
    }

    jvmtiError err_code = jvmti->CreateRawMonitor("vmtrace_lock", &vmtrace_lock);
    CHECK_ERROR("Create raw monitor failure");

    jvmti->GetTime(&start_time);
    CHECK_ERROR("Get time failure");
}

void fini() {
    if (out != NULL && out != stderr) {
        fclose(out);
    }
}

void acq_big_lock(jvmtiEnv *jvmti) {
	jvmtiError err_code = jvmti->RawMonitorEnter(vmtrace_lock);
	CHECK_ERROR("Cannot enter with raw monitor");
}

void rel_big_lock(jvmtiEnv *jvmti) {
	jvmtiError err_code = jvmti->RawMonitorExit(vmtrace_lock);
	CHECK_ERROR("Cannot exit with raw monitor");
}

} // namespace Utils
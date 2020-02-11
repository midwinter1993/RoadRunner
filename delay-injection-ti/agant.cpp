#include <jvmti.h>
#include <stdarg.h>
#include <string>
#include <vector>
#include <thread>
#include <iostream>
#include <string.h>
#include <stdio.h>

#include "utils.h"
#include "wrapper.h"
#include "shadow.h"

// static jrawMonitorID vmtrace_lock;


void JNICALL ThreadStart(jvmtiEnv* jvmti, JNIEnv* env, jthread thread) {
    // ThreadName tn(jvmti, thread);
    // trace(jvmti, "Thread started: %s", tn.name());
}

void JNICALL ThreadEnd(jvmtiEnv* jvmti, JNIEnv* env, jthread thread) {
    // ThreadName tn(jvmti, thread);
    // trace(jvmti, "Thread finished: %s", tn.name());
}


void JNICALL
cbClassPrepare(jvmtiEnv *jvmti, JNIEnv* jni_env, jthread thread, jclass klass) {
    ClassName cn(jvmti, klass);
    if (cn.filter()) {
        return;
    }
    utils::trace(jvmti, "=== Load Class: %s", cn.get_name().c_str());

    jint field_count = 0;
    jfieldID* fields = NULL;

    jvmtiError err_code = jvmti->GetClassFields(klass, &field_count, &fields);
    CHECK_ERROR("Get class field failure");

    for (jint i = 0; i < field_count; ++i) {
        FieldName fn(jvmti, klass, fields[i]);
        utils::trace(jvmti, "Field %s %s", fn.get_name().c_str(), fn.get_signature().c_str());

        err_code = jvmti->SetFieldAccessWatch(klass, fields[i]);
        CHECK_ERROR("Set field access watch failure");
    }
}

void JNICALL cbFieldAccess(
    jvmtiEnv *jvmti,
    JNIEnv* jni_env,
    jthread thread,
    jmethodID method,
    jlocation location,
    jclass field_klass,
    jobject object,
    jfieldID field) {

    utils::acq_big_lock(jvmti);

    FieldName fn(jvmti, field_klass, field);

    std::thread::id this_id = std::this_thread::get_id();
    std::cerr << this_id << "\n";
    utils::trace(jvmti, "ACCESS Field %s", fn.get_name().c_str());

    utils::rel_big_lock(jvmti);
}

//
// Provide a global variable for easy access
//
jvmtiEnv* g_jvmti = NULL;

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM* vm, char* options, void* reserved) {
    vm->GetEnv((void**) &g_jvmti, JVMTI_VERSION_1_0);

    jvmtiEnv *jvmti = g_jvmti;
    utils::init(jvmti, options);
    ShadowVar::jvmti = g_jvmti;

    //
    // Register our capabilities
    //
    jvmtiCapabilities cap= {0};

    cap.can_generate_all_class_hook_events = 1;
    cap.can_generate_field_modification_events = 1;
    cap.can_generate_field_access_events = 1;

    jvmtiError err_code = jvmti->AddCapabilities(&cap);
    CHECK_ERROR("Add capability failure");

    //
    // Register callbacks
    //
    jvmtiEventCallbacks callbacks = {0};

    callbacks.ThreadStart = ThreadStart;
    callbacks.ThreadEnd = ThreadEnd;
    callbacks.ClassPrepare = cbClassPrepare;
    callbacks.FieldAccess = cbFieldAccess;

    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    CHECK_ERROR("Set callbacks failure");

    //
    // Register for events
    //
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL);
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, NULL);
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_ACCESS, NULL);
    CHECK_ERROR("Set event notifications failure");

    return 0;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM* vm) {
    utils::fini();
}


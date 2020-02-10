#include "wrapper.h"
#include "utils.h"

#include <vector>
#include <string.h>

//
// Strip 'L' and ';' from class signature
//
static char* fix_class_name(char* class_name) {
    class_name[strlen(class_name) - 1] = 0;
    return class_name + 1;
}

ClassName::ClassName(jvmtiEnv* jvmti, jclass klass) : jvmti_(jvmti) {
    char *name = NULL;
    jvmti_->GetClassSignature(klass, &name, NULL);

    this->name_ = std::string{fix_class_name(name)};

    jvmti_->Deallocate((unsigned char*) name);
}

const std::string& ClassName::get_name() {
    return name_;
}

bool ClassName::filter() {
    std::vector<const char*> blacklist = {
        "java/",
        "sun/",
    };
    for (auto &s: blacklist) {
        if (name_.find(s) == 0) {
            return true;
        }
    }
    return false;
}

// ===============================================

MethodName::MethodName(jvmtiEnv* jvmti, jmethodID method) : jvmti_(jvmti),
                                                    holder_name_(NULL),
                                                    method_name_(NULL) {
    jclass holder;
    if (jvmti_->GetMethodDeclaringClass(method, &holder) == 0) {
        jvmti_->GetClassSignature(holder, &holder_name_, NULL);
        jvmti_->GetMethodName(method, &method_name_, NULL, NULL);
    }
}

MethodName::~MethodName() {
    jvmti_->Deallocate((unsigned char*) method_name_);
    jvmti_->Deallocate((unsigned char*) holder_name_);
}

char* MethodName::holder() {
    return holder_name_ == NULL ? NULL : fix_class_name(holder_name_);
}

char* MethodName::name() {
    return method_name_;
}

// ===============================================

ThreadName::ThreadName(jvmtiEnv* jvmti, jthread thread) : jvmti_(jvmti), name_(NULL) {
    jvmtiThreadInfo info;
    name_ = jvmti_->GetThreadInfo(thread, &info) == 0 ? info.name : NULL;
}

ThreadName::~ThreadName() {
    jvmti_->Deallocate((unsigned char*) name_);
}

char* ThreadName::name() {
    return name_;
}

FieldName::FieldName(jvmtiEnv* jvmti, jclass klass, jfieldID field) : jvmti_(jvmti) {
    char *name = NULL;
    char *signature = NULL;
    // char *generic = NULL;

    jvmtiError err_code = jvmti->GetFieldName(klass, field, &name, &signature, NULL);
    CHECK_ERROR("Get field name failure");
    // trace(jvmti, "Get [%s] [%s]", name, signature);

    name_ = std::string{name};
    signature_ = std::string{signature};

    jvmti->Deallocate((unsigned char*) name);
}

const std::string& FieldName::get_name() {
    return name_;
}

const std::string& FieldName::get_signature() {
    return signature_;
}
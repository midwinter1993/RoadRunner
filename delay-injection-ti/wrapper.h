#ifndef __WRAPPER_H__
#define __WRAPPER_H__

#include <jvmti.h>
#include <string>

class ClassName {
  private:
    jvmtiEnv* jvmti_;
    std::string name_;

  public:
    ClassName(jvmtiEnv* jvmti, jclass klass);

    bool filter();
    const std::string& get_name();
};

class MethodName {
  private:
    jvmtiEnv* jvmti_;
    char* holder_name_;
    char* method_name_;

  public:
    MethodName(jvmtiEnv* jvmti, jmethodID method);
    ~MethodName();

    char* holder();
    char* name();
};

class ThreadName {
  private:
    jvmtiEnv* jvmti_;
    char* name_;

  public:
    ThreadName(jvmtiEnv* jvmti, jthread thread);
    ~ThreadName();

    char* name();
};

class FieldName {
private:
    jvmtiEnv* jvmti_;
    std::string name_;
    std::string signature_;
    std::string generic_;

  public:
    FieldName(jvmtiEnv* jvmti, jclass klass, jfieldID field);

    const std::string& get_name();
    const std::string& get_signature();
};


#endif
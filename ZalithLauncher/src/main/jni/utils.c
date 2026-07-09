#include <jni.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <android/dlext.h>

#include "logger/logger.h"

#include "utils.h"

typedef int (*Main_Function_t)(int, char**);
typedef void (*android_update_LD_LIBRARY_PATH_t)(char*);

long shared_awt_surface;

char** convert_to_char_array(JNIEnv *env, jobjectArray jstringArray) {
	int num_rows = (*env)->GetArrayLength(env, jstringArray);
	char **cArray = (char **) malloc(num_rows * sizeof(char*));
	jstring row;
	
	for (int i = 0; i < num_rows; i++) {
		row = (jstring) (*env)->GetObjectArrayElement(env, jstringArray, i);
		cArray[i] = (char*)(*env)->GetStringUTFChars(env, row, 0);
    }
	
    return cArray;
}

jobjectArray convert_from_char_array(JNIEnv *env, char **charArray, int num_rows) {
	jobjectArray resultArr = (*env)->NewObjectArray(env, num_rows, (*env)->FindClass(env, "java/lang/String"), NULL);
	jstring row;
	
	for (int i = 0; i < num_rows; i++) {
		row = (jstring) (*env)->NewStringUTF(env, charArray[i]);
		(*env)->SetObjectArrayElement(env, resultArr, i, row);
    }

	return resultArr;
}

void free_char_array(JNIEnv *env, jobjectArray jstringArray, const char **charArray) {
	int num_rows = (*env)->GetArrayLength(env, jstringArray);
	jstring row;
	
	for (int i = 0; i < num_rows; i++) {
		row = (jstring) (*env)->GetObjectArrayElement(env, jstringArray, i);
		(*env)->ReleaseStringUTFChars(env, row, charArray[i]);
	}
}

jstring convertStringJVM(JNIEnv* srcEnv, JNIEnv* dstEnv, jstring srcStr) {
    if (srcStr == NULL) {
        return NULL;
    }
    
    const char* srcStrC = (*srcEnv)->GetStringUTFChars(srcEnv, srcStr, 0);
    jstring dstStr = (*dstEnv)->NewStringUTF(dstEnv, srcStrC);
	(*srcEnv)->ReleaseStringUTFChars(srcEnv, srcStr, srcStrC);
    return dstStr;
}

JNIEXPORT jlong JNICALL Java_android_view_Surface_nativeGetBridgeSurfaceAWT(JNIEnv *env, jclass clazz) {
	return (jlong) shared_awt_surface;
}

JNIEXPORT jint JNICALL Java_android_os_OpenJDKNativeRegister_nativeRegisterNatives(JNIEnv *env, jclass clazz, jstring registerSymbol) {
	const char *register_symbol_c = (*env)->GetStringUTFChars(env, registerSymbol, 0);
	void *symbol = dlsym(RTLD_DEFAULT, register_symbol_c);
	if (symbol == NULL) {
		printf("dlsym %s failed: %s\n", register_symbol_c, dlerror());
		return -1;
	}
	
	int (*registerNativesForClass)(JNIEnv*) = symbol;
	int result = registerNativesForClass(env);
	(*env)->ReleaseStringUTFChars(env, registerSymbol, register_symbol_c);
	
	return (jint) result;
}

JNIEXPORT void JNICALL Java_com_movtery_zalithlauncher_bridge_ZLBridge_setLdLibraryPath(JNIEnv *env, jclass clazz, jstring ldLibraryPath) {
	// jclass exception_cls = (*env)->FindClass(env, "java/lang/UnsatisfiedLinkError");
	
	android_update_LD_LIBRARY_PATH_t android_update_LD_LIBRARY_PATH;
	
	void *libdl_handle = dlopen("libdl.so", RTLD_LAZY);
	void *updateLdLibPath = dlsym(libdl_handle, "android_update_LD_LIBRARY_PATH");
	if (updateLdLibPath == NULL) {
		updateLdLibPath = dlsym(libdl_handle, "__loader_android_update_LD_LIBRARY_PATH");
		if (updateLdLibPath == NULL) {
			char *dl_error_c = dlerror();
			LOG_TO_E("Error getting symbol android_update_LD_LIBRARY_PATH: %s", dl_error_c);
			// (*env)->ThrowNew(env, exception_cls, dl_error_c);
		}
	}
	
	android_update_LD_LIBRARY_PATH = (android_update_LD_LIBRARY_PATH_t) updateLdLibPath;
	const char* ldLibPathUtf = (*env)->GetStringUTFChars(env, ldLibraryPath, 0);
	android_update_LD_LIBRARY_PATH(ldLibPathUtf);
	(*env)->ReleaseStringUTFChars(env, ldLibraryPath, ldLibPathUtf);
}

JNIEXPORT jboolean JNICALL Java_com_movtery_zalithlauncher_bridge_ZLBridge_dlopen(JNIEnv *env, jclass clazz, jstring name) {
	const char *nameUtf = (*env)->GetStringUTFChars(env, name, 0);
	void* handle = dlopen(nameUtf, RTLD_GLOBAL | RTLD_LAZY);
	if (!handle) {
		LOG_TO_E("DLOPEN: %s , failed ( %s )", nameUtf, dlerror());
	} else {
		LOG_TO_D("DLOPEN: %s , success", nameUtf);
	}
	(*env)->ReleaseStringUTFChars(env, name, nameUtf);
	return handle != NULL;
}

JNIEXPORT jint JNICALL Java_com_movtery_zalithlauncher_bridge_ZLBridge_chdir(JNIEnv *env, jclass clazz, jstring nameStr) {
	const char *name = (*env)->GetStringUTFChars(env, nameStr, NULL);
	int retval = chdir(name);
	(*env)->ReleaseStringUTFChars(env, nameStr, name);
	return retval;
}

JNIEXPORT void JNICALL Java_com_movtery_zalithlauncher_bridge_ZLBridge_fsrInit(JNIEnv *env, jclass clazz, jint qualityPreset) {
	void (*fsr_init_fn)(int) = dlsym(RTLD_DEFAULT, "fsr_init");
	if (fsr_init_fn) {
		fsr_init_fn((int)qualityPreset);
	} else {
		LOG_TO_E("FSR: fsr_init not found");
	}
}

JNIEXPORT void JNICALL Java_com_movtery_zalithlauncher_bridge_ZLBridge_fsrSetQuality(JNIEnv *env, jclass clazz, jint qualityPreset) {
	void (*fsr_set_quality_fn)(int) = dlsym(RTLD_DEFAULT, "fsr_set_quality");
	if (fsr_set_quality_fn) {
		fsr_set_quality_fn((int)qualityPreset);
	}
}

JNIEXPORT jboolean JNICALL Java_com_movtery_zalithlauncher_bridge_ZLBridge_dlopenGlobalSphal(JNIEnv *env, jclass clazz, jstring name) {
	const char *nameUtf = (*env)->GetStringUTFChars(env, name, 0);
	LOG_TO_D("dlopenGlobalSphal: trying %s", nameUtf);

	// Önce normal dlopen RTLD_GLOBAL dene (çoğu cihazda yeterli)
	void* handle = dlopen(nameUtf, RTLD_GLOBAL | RTLD_LAZY);
	if (handle) {
		LOG_TO_D("dlopenGlobalSphal: dlopen success for %s", nameUtf);
		(*env)->ReleaseStringUTFChars(env, name, nameUtf);
		return JNI_TRUE;
	}
	LOG_TO_W("dlopenGlobalSphal: dlopen failed for %s, trying android_dlopen_ext: %s", nameUtf, dlerror());

	// dlopen başarısız — ColorOS/HyperOS gibi ROM'larda linker namespace engelleyebilir.
	// android_dlopen_ext ile doğru namespace'i kullanarak yükle.
	void *libdl_android = dlopen("libdl_android.so", RTLD_LAZY | RTLD_LOCAL);
	if (!libdl_android) {
		LOG_TO_W("dlopenGlobalSphal: cannot load libdl_android.so: %s", dlerror());
		(*env)->ReleaseStringUTFChars(env, name, nameUtf);
		return JNI_FALSE;
	}

	typedef struct android_namespace_t* (*get_namespace_fn_t)(const char*);
	get_namespace_fn_t get_namespace = (get_namespace_fn_t)dlsym(libdl_android, "android_get_exported_namespace");
	if (!get_namespace) {
		LOG_TO_W("dlopenGlobalSphal: no android_get_exported_namespace: %s", dlerror());
		dlclose(libdl_android);
		(*env)->ReleaseStringUTFChars(env, name, nameUtf);
		return JNI_FALSE;
	}

	// Sırayla namespace'leri dene: sphal, vendor, default
	const char* ns_names[] = {"sphal", "vendor", "default", NULL};
	struct android_namespace_t* ns = NULL;
	for (int i = 0; ns_names[i]; i++) {
		ns = get_namespace(ns_names[i]);
		if (ns) {
			LOG_TO_D("dlopenGlobalSphal: using namespace '%s'", ns_names[i]);
			break;
		}
	}

	if (!ns) {
		LOG_TO_W("dlopenGlobalSphal: no usable namespace found");
		dlclose(libdl_android);
		(*env)->ReleaseStringUTFChars(env, name, nameUtf);
		return JNI_FALSE;
	}

	android_dlextinfo extinfo;
	memset(&extinfo, 0, sizeof(extinfo));
	extinfo.flags = ANDROID_DLEXT_USE_NAMESPACE;
	extinfo.library_namespace = ns;

	handle = android_dlopen_ext(nameUtf, RTLD_GLOBAL | RTLD_LAZY, &extinfo);
	if (!handle) {
		LOG_TO_E("dlopenGlobalSphal: android_dlopen_ext failed for %s: %s", nameUtf, dlerror());
		dlclose(libdl_android);
		(*env)->ReleaseStringUTFChars(env, name, nameUtf);
		return JNI_FALSE;
	}

	LOG_TO_D("dlopenGlobalSphal: android_dlopen_ext success for %s", nameUtf);
	dlclose(libdl_android);
	(*env)->ReleaseStringUTFChars(env, name, nameUtf);
	return JNI_TRUE;
}


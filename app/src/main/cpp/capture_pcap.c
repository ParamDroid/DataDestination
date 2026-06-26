/*
 * THIS  IS HEAVILY MODIFIED FROM PCAPDROID GIT REPOSITORY
 * FOR BETTER UNDERSTANDABILITY REFER TO THE ORIGIN
 * https://github.com/emanuele-f/pcapdroid
 * REFER TO THE REPOSITORY FOR ABOVE FOR BETTER REFERENCING
 * */

#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <stdbool.h>

// Standard networking headers
#include <netinet/in.h>
#include <sys/socket.h>

// Project-specific headers
#include "zdtun/zdtun.h"
#include "pcapdroid.h"  // Ensure this defines pcapdroid_t and pcap_conn_t
#include "third_party/uthash.h"

#define TAG "VPN_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static volatile bool running = true;
static JavaVM *g_vm = NULL;
static jobject g_service = NULL;
static jmethodID g_protectMethod = NULL;
static jmethodID g_debugMethod = NULL;

// void zdtun_inject_pkt(zdtun_t *pZdtun, uint8_t buffer[65535], uint32_t i); //unused
// void zdtun_step(zdtun_t *pZdtun, int i); //unused


static void on_socket_open(zdtun_t *tun, socket_t socket) {

    if (!g_vm || !g_service || !g_protectMethod)
        return;

    JNIEnv *env;
    bool attached = false;

    if ((*g_vm)->GetEnv(g_vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);
        attached = true;
    }

    jboolean result;
    result = (*env)->CallBooleanMethod(
            env,
            g_service,
            g_protectMethod,
            (jint)socket
    );

    if (!result) {
        LOGE("Failed to protect socket %d", socket);
    }

    if (attached)
        (*g_vm)->DetachCurrentThread(g_vm);
}
static int on_connection_open(zdtun_t *tun, zdtun_conn_t *conn_info) {

    if (!g_vm || !g_service || !g_debugMethod)
        return 0;

    JNIEnv *env;
    bool attached = false;

    if ((*g_vm)->GetEnv(g_vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);
        attached = true;
    }

    char tuple_str[256];
    zdtun_5tuple2str(
            zdtun_conn_get_5tuple(conn_info),
            tuple_str,
            sizeof(tuple_str)
    );

    jstring jStr = (*env)->NewStringUTF(env, tuple_str);
    (*env)->CallVoidMethod(env, g_service, g_debugMethod, jStr);
    (*env)->DeleteLocalRef(env, jStr);

    if (attached)
        (*g_vm)->DetachCurrentThread(g_vm);

    return 0;
}
/**
 * Mandatory callback for zdtun.
 * This is called when zdtun has a packet ready to be sent BACK to the Android OS.
 */
static int send_to_vpn_interface(zdtun_t *tun, zdtun_pkt_t *pkt, const zdtun_conn_t *conn_info) {
    int vpn_fd = *(int*)zdtun_userdata(tun);

    // Write the raw IP packet back to the TUN interface
    ssize_t n = write(vpn_fd, pkt->buf, pkt->len);
    if (n < 0) {
        LOGE("Failed to write to VPN interface: %s", strerror(errno));
        return -1;
    }
    return 0;
}


/*
 * void zdtun_inject_pkt(zdtun_t *pZdtun, uint8_t buffer[65535], uint32_t len) {
    zdtun_inject(pZdtun, buffer, len);
}
 //Left unused
*/
/**
 * Main Packet Loop for Non-Root VpnService
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

void JNICALL
Java_com_vkmu_datadestination_vpn_VpnServiceImpl_runPacketLoop(JNIEnv *env, jobject thiz, jint vpn_fd) {

    LOGI("Starting packet loop FD=%d", vpn_fd);
    running = true;

    // Store global service reference
    g_service = (*env)->NewGlobalRef(env, thiz);

    // Cache protectSocket method
    jclass cls = (*env)->GetObjectClass(env, thiz);
    g_protectMethod = (*env)->GetMethodID(env, cls, "protectSocket", "(I)Z");
    g_debugMethod = (*env)->GetMethodID(env, cls, "onDebugIp", "(Ljava/lang/String;)V");
    if (!g_protectMethod) {
        LOGE("Failed to find protectSocket method");
        (*env)->DeleteLocalRef(env, cls);
        return;
    }

    (*env)->DeleteLocalRef(env, cls);

    zdtun_callbacks_t callbacks = {0};
    callbacks.send_client = send_to_vpn_interface;
    callbacks.on_socket_open = on_socket_open;
    callbacks.on_connection_open = on_connection_open;

    zdtun_t *tun = zdtun_init(&callbacks, &vpn_fd);
    if (!tun) {
        LOGE("zdtun_init failed");
        return;
    }

    zdtun_set_mtu(tun, 1500);

    uint8_t buffer[1500];

    while (running) {

        fd_set rd_fds, wr_fds;
        FD_ZERO(&rd_fds);
        FD_ZERO(&wr_fds);

        int max_fd = 0;
        zdtun_fds(tun, &max_fd, &rd_fds, &wr_fds);

        // Add TUN fd
        FD_SET(vpn_fd, &rd_fds);
        if (vpn_fd > max_fd) max_fd = vpn_fd;

        struct timeval tv;
        tv.tv_sec = 0;
        tv.tv_usec = 20000; // 20ms

        int ret = select(max_fd + 1, &rd_fds, &wr_fds, NULL, &tv);
        if (ret < 0) {
            if (errno == EINTR) continue;
            LOGE("select error: %s", strerror(errno));
            break;
        }

        // If packet from VPN
        if (FD_ISSET(vpn_fd, &rd_fds)) {
            ssize_t len = read(vpn_fd, buffer, sizeof(buffer));
            if (len > 0) {
                zdtun_easy_forward(tun, (const char*)buffer, len);
            }
        }

        // Let zdtun handle its sockets
        zdtun_handle_fd(tun, &rd_fds, &wr_fds);

        //Cleanup expired connections
        zdtun_purge_expired(tun);
    }

    zdtun_finalize(tun);
    LOGI("Packet loop stopped");
}

/*  void zdtun_step(zdtun_t *pZdtun, int timeout_ms) { //unused
    zdtun_poll(pZdtun, timeout_ms);
}
*/

JNIEXPORT void JNICALL
Java_com_vkmu_datadestination_vpn_VpnServiceImpl_stopPacketLoop(JNIEnv *env, jobject thiz) {
    running = false;

    if (g_service) {
        JNIEnv *env;
        if ((*g_vm)->GetEnv(g_vm, (void**)&env, JNI_VERSION_1_6) == JNI_OK) {
            (*env)->DeleteGlobalRef(env, g_service);
        }
        g_service = NULL;
    }
}

#include "socket_server.h"
#include "hooks.h"
#include <android/log.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <cstring>
#include <cstdio>
#include <atomic>
#include <pthread.h>

#define LOG_TAG "SubwayBrutal_Sock"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace SocketServer {

    static const int PORT = 19877;
    static std::atomic<bool> s_running{false};
    static int s_serverFd = -1;

    static void processCommand(const char* cmd) {
        char cmdName[64] = {0};
        int val = 0;
        if (sscanf(cmd, "%63[^:]:%d", cmdName, &val) < 1) {
            LOGE("Bad cmd: %s", cmd);
            return;
        }
        bool enable = (val != 0);
        LOGI("Cmd: %s = %d", cmdName, val);

        if (strcmp(cmdName, "UNLIMITED_COINS") == 0) Hacks::g_unlimitedCoins.store(enable);
        else if (strcmp(cmdName, "GOD_MODE") == 0) Hacks::g_godMode.store(enable);
        else if (strcmp(cmdName, "INFINITE_JUMP") == 0) Hacks::g_infiniteJump.store(enable);
        else if (strcmp(cmdName, "SPEED_HACK") == 0) Hacks::g_speedHack.store(enable);
        else if (strcmp(cmdName, "SUPER_JUMP") == 0) Hacks::g_superJump.store(enable);
        else if (strcmp(cmdName, "INFINITE_JETPACK") == 0) Hacks::g_infiniteJetpack.store(enable);
        else if (strcmp(cmdName, "NO_OBSTACLES") == 0) Hacks::g_noObstacles.store(enable);
        else if (strcmp(cmdName, "AUTO_SCORE") == 0) Hacks::g_autoScore.store(enable);
        else if (strcmp(cmdName, "UNLOCK_ALL") == 0) Hacks::g_unlockAll.store(enable);
        else if (strcmp(cmdName, "ANTI_CRASH") == 0) Hacks::g_antiCrash.store(enable);
        else if (strcmp(cmdName, "ACTIVATE_ALL") == 0) {
            Hacks::g_unlimitedCoins.store(enable);
            Hacks::g_godMode.store(enable);
            Hacks::g_infiniteJump.store(enable);
            Hacks::g_speedHack.store(enable);
            Hacks::g_superJump.store(enable);
            Hacks::g_infiniteJetpack.store(enable);
            Hacks::g_noObstacles.store(enable);
            Hacks::g_autoScore.store(enable);
            Hacks::g_unlockAll.store(enable);
            Hacks::g_antiCrash.store(enable);
        }
    }

    static void* serverThread(void*) {
        s_serverFd = socket(AF_INET, SOCK_STREAM, 0);
        if (s_serverFd < 0) {
            LOGE("socket() failed");
            s_running = false;
            return nullptr;
        }
        int opt = 1;
        setsockopt(s_serverFd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = inet_addr("127.0.0.1");
        addr.sin_port = htons(PORT);

        if (bind(s_serverFd, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
            LOGE("bind() failed on port %d", PORT);
            close(s_serverFd);
            s_running = false;
            return nullptr;
        }
        if (listen(s_serverFd, 5) < 0) {
            LOGE("listen() failed");
            close(s_serverFd);
            s_running = false;
            return nullptr;
        }
        LOGI("Listening on 127.0.0.1:%d", PORT);

        while (s_running.load()) {
            struct sockaddr_in clientAddr;
            socklen_t clientLen = sizeof(clientAddr);
            fd_set fds;
            FD_ZERO(&fds);
            FD_SET(s_serverFd, &fds);
            struct timeval tv = {1, 0};
            int sel = select(s_serverFd + 1, &fds, nullptr, nullptr, &tv);
            if (sel <= 0) continue;

            int clientFd = accept(s_serverFd, (struct sockaddr*)&clientAddr, &clientLen);
            if (clientFd < 0) continue;
            LOGI("Client connected");

            const char* welcome = "SUBWAY_BRUTAL_OK\n";
            send(clientFd, welcome, strlen(welcome), 0);

            char buf[256];
            while (s_running.load()) {
                memset(buf, 0, sizeof(buf));
                int n = recv(clientFd, buf, sizeof(buf) - 1, 0);
                if (n <= 0) break;
                char* nl = strchr(buf, '\n');
                if (nl) *nl = '\0';
                char* cr = strchr(buf, '\r');
                if (cr) *cr = '\0';
                if (strlen(buf) > 0) {
                    processCommand(buf);
                    char ack[64];
                    snprintf(ack, sizeof(ack), "ACK:%s\n", buf);
                    send(clientFd, ack, strlen(ack), 0);
                }
            }
            close(clientFd);
        }
        close(s_serverFd);
        s_serverFd = -1;
        return nullptr;
    }

    void Start() {
        if (s_running.load()) return;
        s_running = true;
        pthread_t tid;
        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
        pthread_create(&tid, &attr, serverThread, nullptr);
        pthread_attr_destroy(&attr);
    }

    void Stop() {
        s_running = false;
        if (s_serverFd >= 0) close(s_serverFd);
    }

    bool IsRunning() {
        return s_running.load();
    }
}

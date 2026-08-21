#include <jni.h>

#include <dirent.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <algorithm>

static void addLine(
        std::ostringstream &out,
        const std::string &text) {

    out << text << "\n";
}

static bool exists(
        const std::string &path) {

    struct stat info{};

    return stat(
            path.c_str(),
            &info
    ) == 0;
}

static bool readable(
        const std::string &path) {

    return access(
            path.c_str(),
            R_OK
    ) == 0;
}

static bool writable(
        const std::string &path) {

    return access(
            path.c_str(),
            W_OK
    ) == 0;
}

static std::string permissionsFor(
        const std::string &path) {

    std::ostringstream out;

    out << "exists=";

    if (exists(path)) {
        out << "YES";
    } else {
        out << "NO";
    }

    out << " read=";

    if (readable(path)) {
        out << "YES";
    } else {
        out << "NO";
    }

    out << " write=";

    if (writable(path)) {
        out << "YES";
    } else {
        out << "NO";
    }

    return out.str();
}

static std::vector<std::string> listDirectory(
        const std::string &path) {

    std::vector<std::string> result;

    DIR *dir =
            opendir(
                    path.c_str()
            );

    if (dir == nullptr) {
        return result;
    }

    struct dirent *entry;

    while (
            (entry = readdir(dir))
            != nullptr
            ) {

        std::string name =
                entry->d_name;

        if (
                name == "." ||
                name == ".."
                ) {
            continue;
        }

        result.push_back(
                name
        );
    }

    closedir(dir);

    std::sort(
            result.begin(),
            result.end()
    );

    return result;
}

static bool containsIgnoreCase(
        const std::string &text,
        const std::string &needle) {

    std::string a = text;
    std::string b = needle;

    std::transform(
            a.begin(),
            a.end(),
            a.begin(),
            ::tolower
    );

    std::transform(
            b.begin(),
            b.end(),
            b.begin(),
            ::tolower
    );

    return a.find(b)
           != std::string::npos;
}

static bool interestingName(
        const std::string &name) {

    static const std::vector<std::string> terms = {

            "camera",
            "video",
            "media",
            "sensor",
            "imgsensor",
            "cam",
            "vivo",
            "mediatek",
            "mtk",
            "remosaic",
            "isp"
    };

    for (
            const auto &term :
            terms
            ) {

        if (
                containsIgnoreCase(
                        name,
                        term
                )
                ) {

            return true;
        }
    }

    return false;
}

static void inspectDirectory(
        std::ostringstream &out,
        const std::string &path,
        bool onlyInteresting) {

    addLine(
            out,
            ""
    );

    addLine(
            out,
            "DIRECTORY: " + path
    );

    addLine(
            out,
            permissionsFor(path)
    );

    auto entries =
            listDirectory(path);

    if (entries.empty()) {

        addLine(
                out,
                "No readable entries."
        );

        return;
    }

    int count = 0;

    for (
            const auto &entry :
            entries
            ) {

        if (
                onlyInteresting &&
                !interestingName(entry)
                ) {

            continue;
        }

        std::string full =
                path + "/" + entry;

        addLine(
                out,
                "  " +
                entry +
                "   [" +
                permissionsFor(full) +
                "]"
        );

        count++;

        if (count >= 250) {

            addLine(
                    out,
                    "  ... truncated ..."
            );

            break;
        }
    }

    if (count == 0) {

        addLine(
                out,
                "No matching entries."
        );
    }
}

static void inspectDevNodes(
        std::ostringstream &out) {

    addLine(
            out,
            ""
    );

    addLine(
            out,
            "================================"
    );

    addLine(
            out,
            "/DEV CAMERA / VIDEO NODES"
    );

    addLine(
            out,
            "================================"
    );

    auto entries =
            listDirectory("/dev");

    int found = 0;

    for (
            const auto &entry :
            entries
            ) {

        if (
                interestingName(entry)
                ) {

            std::string path =
                    "/dev/" + entry;

            addLine(
                    out,
                    path
            );

            addLine(
                    out,
                    "  " +
                    permissionsFor(path)
            );

            int fd =
                    open(
                            path.c_str(),
                            O_RDONLY |
                            O_NONBLOCK
                    );

            if (fd >= 0) {

                addLine(
                        out,
                        "  OPEN TEST: SUCCESS"
                );

                close(fd);

            } else {

                addLine(
                        out,
                        "  OPEN TEST: DENIED/FAILED"
                );
            }

            found++;
        }
    }

    if (found == 0) {

        addLine(
                out,
                "No camera/video/media nodes visible."
        );
    }
}

static void inspectSys(
        std::ostringstream &out) {

    addLine(
            out,
            ""
    );

    addLine(
            out,
            "================================"
    );

    addLine(
            out,
            "/SYS PROBE"
    );

    addLine(
            out,
            "================================"
    );

    const std::vector<std::string> paths = {

            "/sys/class/video4linux",
            "/sys/class/media",
            "/sys/class/camera",
            "/sys/devices",
            "/sys/kernel/debug",
            "/sys/kernel/debug/camera",
            "/sys/kernel/debug/imgsensor",
            "/sys/module"
    };

    for (
            const auto &path :
            paths
            ) {

        if (
                exists(path)
                ) {

            inspectDirectory(
                    out,
                    path,
                    true
            );
        }
    }
}

static void inspectVendorLibraries(
        std::ostringstream &out) {

    addLine(
            out,
            ""
    );

    addLine(
            out,
            "================================"
    );

    addLine(
            out,
            "VENDOR CAMERA LIBRARY PROBE"
    );

    addLine(
            out,
            "================================"
    );

    const std::vector<std::string> paths = {

            "/vendor/lib64",
            "/vendor/lib",
            "/system/lib64",
            "/system/lib",
            "/system_ext/lib64",
            "/product/lib64",
            "/odm/lib64",
            "/odm/lib"
    };

    for (
            const auto &path :
            paths
            ) {

        if (!exists(path)) {
            continue;
        }

        auto entries =
                listDirectory(path);

        int matches = 0;

        for (
                const auto &entry :
                entries
                ) {

            if (
                    interestingName(entry)
                    ) {

                if (matches == 0) {

                    addLine(
                            out,
                            ""
                    );

                    addLine(
                            out,
                            path + ":"
                    );
                }

                std::string full =
                        path + "/" + entry;

                addLine(
                        out,
                        "  " + entry
                );

                addLine(
                        out,
                        "    " +
                        permissionsFor(full)
                );

                matches++;

                if (matches >= 100) {

                    addLine(
                            out,
                            "  ... truncated ..."
                    );

                    break;
                }
            }
        }
    }
}

static void searchReadableTextFile(
        std::ostringstream &out,
        const std::string &path) {

    if (!readable(path)) {
        return;
    }

    struct stat info{};

    if (
            stat(
                    path.c_str(),
                    &info
            ) != 0
            ) {
        return;
    }

    if (
            info.st_size <= 0 ||
            info.st_size >
            (1024 * 1024)
            ) {

        return;
    }

    std::ifstream file(
            path
    );

    if (!file.is_open()) {
        return;
    }

    static const std::vector<std::string> terms = {

            "16320",
            "12288",
            "200mp",
            "200 mp",
            "remosaic",
            "fullsize",
            "highresolution",
            "high_resolution",
            "sensorScenario",
            "forceSensorMode"
    };

    std::string line;

    int lineNumber = 0;
    int matches = 0;

    while (
            std::getline(
                    file,
                    line
            )
            ) {

        lineNumber++;

        for (
                const auto &term :
                terms
                ) {

            if (
                    containsIgnoreCase(
                            line,
                            term
                    )
                    ) {

                if (matches == 0) {

                    addLine(
                            out,
                            ""
                    );

                    addLine(
                            out,
                            "TEXT MATCHES: " +
                            path
                    );
                }

                std::ostringstream result;

                result
                        << "  L"
                        << lineNumber
                        << ": "
                        << line;

                addLine(
                        out,
                        result.str()
                );

                matches++;

                break;
            }
        }

        if (matches >= 30) {
            break;
        }
    }
}

static void inspectKnownFiles(
        std::ostringstream &out) {

    addLine(
            out,
            ""
    );

    addLine(
            out,
            "================================"
    );

    addLine(
            out,
            "KNOWN CAMERA FILE SEARCH"
    );

    addLine(
            out,
            "================================"
    );

    const std::vector<std::string> roots = {

            "/vendor/etc",
            "/odm/etc",
            "/system/etc",
            "/product/etc"
    };

    for (
            const auto &root :
            roots
            ) {

        if (!exists(root)) {
            continue;
        }

        auto entries =
                listDirectory(root);

        for (
                const auto &entry :
                entries
                ) {

            if (
                    interestingName(entry)
                    ) {

                std::string path =
                        root +
                        "/" +
                        entry;

                addLine(
                        out,
                        path +
                        " [" +
                        permissionsFor(path) +
                        "]"
                );

                searchReadableTextFile(
                        out,
                        path
                );
            }
        }
    }
}

static std::string runProbe() {

    std::ostringstream out;

    addLine(
            out,
            "VIVO 200 MP NATIVE HAL PROBE"
    );

    addLine(
            out,
            "================================"
    );

    addLine(
            out,
            ""
    );

#if defined(__aarch64__)
    addLine(
            out,
            "Architecture: arm64-v8a"
    );
#elif defined(__arm__)
    addLine(
            out,
            "Architecture: armeabi-v7a"
    );
#elif defined(__x86_64__)
    addLine(
            out,
            "Architecture: x86_64"
    );
#else
    addLine(
            out,
            "Architecture: unknown"
    );
#endif

    std::ostringstream ids;

    ids
            << "UID: "
            << getuid()
            << "   GID: "
            << getgid();

    addLine(
            out,
            ids.str()
    );

    addLine(
            out,
            ""
    );

    addLine(
            out,
            "Goal:"
    );

    addLine(
            out,
            "Find what exists below Camera2"
    );

    addLine(
            out,
            "without ADB/root."
    );

    inspectDevNodes(
            out
    );

    inspectSys(
            out
    );

    inspectVendorLibraries(
            out
    );

    inspectKnownFiles(
            out
    );

    addLine(
            out,
            ""
    );

    addLine(
            out,
            "================================"
    );

    addLine(
            out,
            "NATIVE PROBE COMPLETE"
    );

    addLine(
            out,
            "================================"
    );

    addLine(
            out,
            ""
    );

    addLine(
            out,
            "ACCESS NOTES:"
    );

    addLine(
            out,
            "DENIED is useful information."
    );

    addLine(
            out,
            "It identifies Android's security"
    );

    addLine(
            out,
            "boundary between this APK and HAL."
    );

    return out.str();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_vivo200mpprobe_NativeProbe_runNativeProbe(
        JNIEnv *env,
        jobject /* this */) {

    std::string result =
            runProbe();

    return env->NewStringUTF(
            result.c_str()
    );
}

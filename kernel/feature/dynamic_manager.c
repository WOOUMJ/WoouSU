#include <linux/err.h>
#include <linux/kernel.h>
#include <linux/mutex.h>
#include <linux/string.h>
#include <linux/types.h>

#include "feature/dynamic_manager.h"
#include "manager/manager_identity.h"
#include "manager/throne_tracker.h"
#include "klog.h" // IWYU pragma: keep

#define DYNAMIC_MANAGER_MIN_SIZE 0x100
#define DYNAMIC_MANAGER_MAX_SIZE 0x1000

struct dynamic_manager_config {
    bool is_set;
    unsigned int size;
    char hash[65];
};

static DEFINE_MUTEX(dynamic_manager_mutex);
static struct dynamic_manager_config dynamic_manager;

bool ksu_is_dynamic_manager_enabled(void)
{
    return READ_ONCE(dynamic_manager.is_set);
}

apk_sign_key_t ksu_get_dynamic_manager_sign(void)
{
    apk_sign_key_t key = {
        .size = dynamic_manager.size,
        .sha256 = dynamic_manager.hash,
    };

    return key;
}

static bool is_valid_sha256_hex(const char *hash)
{
    int len = 0;
    int i;

    while (len < 64 && hash[len] != '\0')
        len++;
    if (len != 64)
        return false;

    for (i = 0; i < len; i++) {
        char c = hash[i];

        if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
            return false;
    }

    return true;
}

static void revalidate_manager(void)
{
    ksu_throne_tracker_reset_cache();
    ksu_invalidate_manager_uid();
    track_throne(false);
}

int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd)
{
    int ret = 0;

    if (!cmd)
        return -EINVAL;

    mutex_lock(&dynamic_manager_mutex);
    switch (cmd->operation) {
    case DYNAMIC_MANAGER_OP_SET:
    case DYNAMIC_MANAGER_OP_SET_SYNCHRONOUS:
        if (cmd->size < DYNAMIC_MANAGER_MIN_SIZE || cmd->size > DYNAMIC_MANAGER_MAX_SIZE) {
            pr_err("dynamic_manager: invalid size: 0x%x\n", cmd->size);
            ret = -EINVAL;
            break;
        }
        if (!is_valid_sha256_hex((const char *)cmd->hash)) {
            pr_err("dynamic_manager: invalid sha256\n");
            ret = -EINVAL;
            break;
        }

        memcpy(dynamic_manager.hash, cmd->hash, 64);
        dynamic_manager.hash[64] = '\0';
        dynamic_manager.size = cmd->size;
        WRITE_ONCE(dynamic_manager.is_set, true);

        pr_info("dynamic_manager: enabled, size=0x%x, hash=%.16s...\n", dynamic_manager.size, dynamic_manager.hash);
        revalidate_manager();
        break;

    case DYNAMIC_MANAGER_OP_GET:
        if (dynamic_manager.is_set) {
            cmd->size = dynamic_manager.size;
            memcpy(cmd->hash, dynamic_manager.hash, 64);
        } else {
            cmd->size = 0;
            memset(cmd->hash, 0, 64);
        }
        break;

    case DYNAMIC_MANAGER_OP_WIPE:
        memset(&dynamic_manager, 0, sizeof(dynamic_manager));
        pr_info("dynamic_manager: disabled\n");
        revalidate_manager();
        break;

    default:
        pr_err("dynamic_manager: invalid operation: %d\n", cmd->operation);
        ret = -EINVAL;
        break;
    }
    mutex_unlock(&dynamic_manager_mutex);

    return ret;
}

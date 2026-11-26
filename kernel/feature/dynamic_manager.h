#ifndef __KSU_H_DYNAMIC_MANAGER
#define __KSU_H_DYNAMIC_MANAGER

#include <linux/errno.h>
#include <linux/types.h>

#include "manager/manager_sign.h"
#include "uapi/supercall.h"

#ifdef CONFIG_KSU_DISABLE_MANAGER

static inline bool ksu_is_dynamic_manager_enabled(void)
{
    return false;
}

static inline apk_sign_key_t ksu_get_dynamic_manager_sign(void)
{
    apk_sign_key_t key = { .size = 0, .sha256 = NULL };

    return key;
}

static inline int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd)
{
    (void)cmd;
    return -EOPNOTSUPP;
}

#else

bool ksu_is_dynamic_manager_enabled(void);
apk_sign_key_t ksu_get_dynamic_manager_sign(void);
int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd);

#endif

#endif

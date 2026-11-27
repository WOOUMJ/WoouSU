#ifndef __KSU_KPM_H
#define __KSU_KPM_H

#include <linux/types.h>

/* KPM control surface. The payload layout (struct ksu_kpm_cmd) lives in
 * uapi/supercall.h so ksud and the kernel stay in sync. */
int do_kpm(void __user *arg);

#endif /* __KSU_KPM_H */

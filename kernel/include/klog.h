#ifndef __KSU_H_KLOG
#define __KSU_H_KLOG

#include <linux/printk.h>

#ifdef pr_fmt
#undef pr_fmt
#ifdef CONFIG_KSU_STEALTH
#define pr_fmt(fmt) "wlan: " fmt
#else
#define pr_fmt(fmt) "KernelSU: " fmt
#endif
#endif

#ifdef CONFIG_KSU_STEALTH
/* Keep dmesg free of module-specific breadcrumbs. */
#undef pr_info
#undef pr_info_ratelimited
#undef pr_warn
#undef pr_warn_ratelimited
#undef pr_notice
#undef pr_debug
#define pr_info(fmt, ...) do { } while (0)
#define pr_info_ratelimited(fmt, ...) do { } while (0)
#define pr_warn(fmt, ...) do { } while (0)
#define pr_warn_ratelimited(fmt, ...) do { } while (0)
#define pr_notice(fmt, ...) do { } while (0)
#define pr_debug(fmt, ...) do { } while (0)
#endif

#endif

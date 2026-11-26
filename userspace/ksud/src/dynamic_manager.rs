use anyhow::{Result, ensure};
use const_format::concatcp;
use serde_json::Value;
use std::os::unix::fs::PermissionsExt;

use crate::{apk_sign, defs, ksucalls};

/// Persisted dynamic manager signature.
pub const DYNAMIC_MANAGER_CONFIG: &str = concatcp!(defs::WORKING_DIR, "dynamic_manager.json");

fn normalize_hash(hash: &str) -> Result<String> {
    let hash = hash.trim().to_lowercase();
    ensure!(
        hash.len() == 64 && hash.bytes().all(|b| b.is_ascii_hexdigit()),
        "invalid sha256: {hash}"
    );
    Ok(hash)
}

fn write_config(size: u32, hash: &str) -> Result<()> {
    let path = DYNAMIC_MANAGER_CONFIG;
    let tmp = format!("{path}.tmp");
    let content = serde_json::json!({ "size": size, "hash": hash }).to_string();
    std::fs::write(&tmp, content)?;
    let _ = std::fs::set_permissions(&tmp, std::fs::Permissions::from_mode(0o600));
    std::fs::rename(&tmp, path)?;
    Ok(())
}

fn read_config() -> Result<Option<(u32, String)>> {
    let path = DYNAMIC_MANAGER_CONFIG;
    if !std::path::Path::new(path).exists() {
        return Ok(None);
    }
    let content = std::fs::read_to_string(path)?;
    let value: Value = serde_json::from_str(&content)?;
    let size = value
        .get("size")
        .and_then(Value::as_u64)
        .map(|v| v as u32)
        .unwrap_or(0);
    let hash = value
        .get("hash")
        .and_then(Value::as_str)
        .unwrap_or_default()
        .to_string();
    if size == 0 || hash.is_empty() {
        return Ok(None);
    }
    Ok(Some((size, hash)))
}

/// Set the dynamic manager signature and persist it.
pub fn set(size: u32, hash: &str) -> Result<()> {
    ensure!(size > 0, "size must be greater than 0");
    let hash = normalize_hash(hash)?;
    ksucalls::set_dynamic_manager(size, &hash)?;
    write_config(size, &hash)?;
    log::info!("dynamic_manager: applied size=0x{size:x} hash={hash}");
    Ok(())
}

/// Set the dynamic manager signature from an installed APK.
pub fn set_apk(apk: &str) -> Result<()> {
    let (size, hash) = apk_sign::get_apk_signature(apk)?;
    set(size, &hash)
}

/// Query the kernel for the current dynamic manager signature.
pub fn get() -> Result<(u32, String)> {
    ksucalls::get_dynamic_manager()
}

/// Clear the dynamic manager signature (fall back to WoouSU manager).
pub fn clear() -> Result<()> {
    ksucalls::clear_dynamic_manager()?;
    let _ = std::fs::remove_file(DYNAMIC_MANAGER_CONFIG);
    log::info!("dynamic_manager: cleared");
    Ok(())
}

/// Re-apply the persisted signature at boot.
pub fn booted_load() {
    match read_config() {
        Ok(Some((size, hash))) => {
            if let Err(e) = ksucalls::set_dynamic_manager(size, &hash) {
                log::warn!("dynamic_manager: restore at boot failed: {e:?}");
            } else {
                log::info!("dynamic_manager: restored at boot, size=0x{size:x}");
            }
        }
        Ok(None) => {}
        Err(e) => log::warn!("dynamic_manager: parse config failed: {e:?}"),
    }
}

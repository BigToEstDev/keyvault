//! Emptying the recycle bin.
//!
//! The tiles of a home screen (`combined_category_details`) and the entry types to create from
//! (`entry_type_headers`) went with Step 43: our ui has no such tiles, and the core knows no entry
//! types - presets are the app's (Step 45).

use kdbx_rust_core::db_service;

use crate::commands::Command;
use crate::dispatch::Answer;
use crate::errors::ErrorPayload;
use crate::handlers::lifecycle::DbKeyArgs;
use crate::handlers::{args, reject_bytes, Done};

/// Empties the recycle bin: everything in it goes for good, with no undo.
///
/// Moving things *into* the bin is part of groups and entries - this is only the one irreversible
/// step at the end.
pub(crate) fn empty_trash(args_json: &str, input: Option<Vec<u8>>) -> Result<Answer, ErrorPayload> {
    reject_bytes(Command::EmptyTrash, input)?;
    let args: DbKeyArgs = args(args_json)?;

    db_service::empty_trash(&args.db_key).map_err(|e| ErrorPayload::of(&e))?;

    Done::answer()
}

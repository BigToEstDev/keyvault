//! Emptying the recycle bin, and the rated password.

use serde_json::{json, Value};

use crate::contract::assert_shape;
use crate::dispatch::run_with_bytes;
use crate::test_support::{
    close_database, key_of, ok_payload, open_database, prepare, set_form_field,
};

fn call(command: &str, args: Value) -> String {
    run_with_bytes(command, &args.to_string(), None).envelope
}

fn call_ok(command: &str, args: Value) -> Value {
    let envelope = call(command, args);
    assert!(
        envelope.starts_with(r#"{"ok":"#),
        "{} failed: {}",
        command,
        envelope
    );

    ok_payload(&envelope)
}

fn root_uuid(db_key: &str) -> String {
    call_ok("groups_summary_data", json!({ "db_key": db_key }))["root_uuid"]
        .as_str()
        .expect("the tree has to name its root")
        .to_string()
}

fn new_entry(db_key: &str, title: &str) -> String {
    let root = root_uuid(db_key);

    let mut form = call_ok("new_entry_form_data", json!({ "parent_group_uuid": root }));
    set_form_field(&mut form, "Title", title);
    call_ok(
        "insert_entry_from_form_data",
        json!({"db_key": db_key, "form_data": form}),
    );

    form["uuid"]
        .as_str()
        .expect("a form has a uuid")
        .to_string()
}

fn entry_count(db_key: &str, category: Value) -> usize {
    call_ok(
        "entry_summary_data",
        json!({"db_key": db_key, "category": category}),
    )
    .as_array()
    .expect("a list")
    .len()
}

#[test]
fn emptying_the_bin_takes_what_is_in_it_and_leaves_the_rest() {
    let db_key = key_of("catalog-empty-trash");
    open_database(&db_key);

    let doomed = new_entry(&db_key, "Doomed");
    new_entry(&db_key, "Kept");

    call_ok(
        "move_entry_to_recycle_bin",
        json!({"db_key": db_key, "entry_uuid": doomed}),
    );
    assert_eq!(entry_count(&db_key, json!({"kind": "deleted"})), 1);

    let emptied = call("empty_trash", json!({ "db_key": db_key }));
    assert_shape("empty_trash", &emptied);

    assert_eq!(
        entry_count(&db_key, json!({"kind": "deleted"})),
        0,
        "the bin is empty"
    );
    assert_eq!(
        entry_count(&db_key, json!({"kind": "all_entries"})),
        1,
        "and what was not in it is still there"
    );

    close_database(&db_key);
}

#[test]
fn a_generated_password_comes_with_its_own_rating() {
    prepare();

    let envelope = call(
        "analyzed_password",
        json!({
            "length": 24,
            "numbers": true,
            "lowercase_letters": true,
            "uppercase_letters": true,
            "symbols": true,
            "spaces": false,
            "exclude_similar_characters": true,
            "strict": true,
        }),
    );
    assert_shape("analyzed_password", &envelope);

    let analyzed = ok_payload(&envelope);
    assert_eq!(
        analyzed["password"].as_str().map(str::len),
        Some(24),
        "{}",
        analyzed
    );
    assert!(
        !analyzed["score"].is_null(),
        "the rating belongs to this password, which is why it comes with it: {}",
        analyzed
    );
}
